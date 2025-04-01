package org.geoserver.eumetsat.pinning;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.eumetsat.pinning.views.TestContext;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** Docker needs to run to execute the tests */
public class PinningServiceTest extends GeoServerSystemTestSupport {

    private static final boolean WRITE_RESULTS = false;

    private static final String TEMPLATE_PIN_REQUEST =
            "SELECT fid,%s,pin FROM %s where pin > 0 order by %s asc";

    private static final String EXPECTED_RESULTS_FOLDER = "src/test/resources/expected/";
    public static final String GET_VIEWS_QUERY = "SELECT * FROM pinning.views";
    public static final String VIEW_ID = "view_id";
    public static final String FID = "fid";

    public static final String MSG_FES_RDT = "msg_fes_rdt";

    public static final String MSG_FES = "msg_fes";
    protected static QName MSG_FES_RDT_QNAME = new QName(MSG_FES, MSG_FES_RDT, MSG_FES);

    public static final String M01_ORBITAL_TRACKS = "m01_orbital_tracks";
    protected static QName MSG_FES_ORBITAL_TRACKS = new QName(MSG_FES, M01_ORBITAL_TRACKS, MSG_FES);

    static DockerImageName postgisImage;

    static PostgreSQLContainer<?> postgres;

    private WireMockServer wireMockServer;

    @Before
    public void setup() {
        wireMockServer =
                new WireMockServer(
                        WireMockConfiguration.options()
                                .port(8080)
                                .usingFilesUnderDirectory("src/test/resources/wiremock"));

        // Add request listener to log to System.out
        wireMockServer.addMockServiceRequestListener(
                (Request request, Response response) -> {
                    /*System.out.println("WireMock received request: " + request.getUrl());
                    System.out.println("RESPONSE status: " + response.getStatus());
                    String bodyString = new String(response.getBody(), StandardCharsets.UTF_8);
                    System.out.println("RESPONSE body: " + bodyString);*/
                });

        wireMockServer.start();
    }

    @BeforeClass
    @SuppressWarnings("PMD.SystemPrintln")
    public static void startContainers() throws Exception {
        if (!DockerClientFactory.instance().isDockerAvailable()) {
            System.out.println(
                    "==============================================================\n"
                            + "Docker not available, skipping tests.\n"
                            + "Make sure Docker is running before running the pinning tests\n"
                            + "==============================================================\n");
            Assume.assumeTrue(false);
        }

        postgisImage =
                DockerImageName.parse("postgis/postgis:15-3.3")
                        .asCompatibleSubstituteFor("postgres");
        postgres =
                new PostgreSQLContainer<>(postgisImage)
                        .withDatabaseName("testdb")
                        .withUsername("test")
                        .withPassword("test")
                        .withEnv("TZ", "UTC");
        try {
            postgres.start();
        } catch (Exception e) {
            Assume.assumeNoException("Could not start PostgreSQL container", e);
        }

        try (Connection connection =
                        DriverManager.getConnection(
                                postgres.getJdbcUrl(),
                                postgres.getUsername(),
                                postgres.getPassword());
                Statement stmt = connection.createStatement()) {
            String sql = getSql("sql/msg_fes_rdt.sql");
            stmt.execute(sql);
            sql = getSql("sql/m01_orbital_tracks.sql");
            stmt.execute(sql);
            sql = getSql("sql/viewstable.sql");
            stmt.execute(sql);
        }
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setUrl(postgres.getJdbcUrl());
        ds.setUser(postgres.getUsername());
        ds.setPassword(postgres.getPassword());

        SimpleNamingContextBuilder builder = new SimpleNamingContextBuilder();
        builder.bind("java:/comp/env/jdbc/eumetsat", ds);
        builder.activate();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        if (postgres != null && postgres.isRunning()) {
            postgres.stop();
        }
    }

    private static String getSql(String sqlFile) throws IOException, URISyntaxException {
        return Files.readString(
                Paths.get(PinningServiceTest.class.getClassLoader().getResource(sqlFile).toURI()));
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);

        // Copy an entire directory into the data directory
        File dataDir = testData.getDataDirectoryRoot();
        File pinningDir = new File(dataDir, "pinning");
        if (!pinningDir.exists()) {
            boolean created = pinningDir.mkdirs();
            if (!created) {
                throw new IOException(
                        "Failed to create directory: " + pinningDir.getAbsolutePath());
            }
        }

        InputStream stream =
                getClass().getResource("/test-data/pinning/service.properties").openStream();
        testData.copyTo(stream, "pinning/service.properties");

        // Copy a single file
        InputStream stream2 =
                getClass().getResource("/test-data/pinning/layers_mapping.csv").openStream();
        testData.copyTo(stream2, "pinning/layers_mapping.csv");
    }

    protected void addLayers() throws Exception {
        Catalog catalog = getCatalog();

        WorkspaceInfo ws = catalog.getFactory().createWorkspace();
        ws.setName(MSG_FES_RDT_QNAME.getPrefix());
        catalog.add(ws);
        ws = catalog.getWorkspaceByName(MSG_FES_RDT_QNAME.getPrefix());
        catalog.save(ws);

        addLayer(MSG_FES_RDT_QNAME, "testtime");
        addLayer(MSG_FES_ORBITAL_TRACKS, "time");
    }

    protected void addLayer(QName name, String timeAttribute) {
        Catalog catalog = getCatalog();
        WorkspaceInfo ws = catalog.getWorkspaceByName(name.getPrefix());
        DataStoreInfo ds = catalog.getFactory().createDataStore();

        ds.setName(name.getLocalPart());
        ds.setWorkspace(ws);

        Map<String, Serializable> params = new HashMap<>();
        params.put("dbtype", "postgis");
        params.put("host", postgres.getHost());
        params.put("port", postgres.getMappedPort(5432));
        params.put("database", postgres.getDatabaseName());
        params.put("user", postgres.getUsername());
        params.put("passwd", postgres.getPassword());
        params.put("Expose primary keys", true);
        Map<String, Serializable> cParams = ds.getConnectionParameters();
        cParams.putAll(params);
        catalog.add(ds);
        ds = catalog.getDataStoreByName(name.getPrefix(), name.getLocalPart());
        catalog.save(ds);

        // Register the feature type as a layer
        FeatureTypeInfo fti = catalog.getFactory().createFeatureType();
        fti.setName(name.getLocalPart());
        fti.setStore(ds);
        fti.setSRS("EPSG:4326");
        fti.setNativeName(name.getLocalPart());
        fti.setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);

        catalog.add(fti);
        fti = catalog.getFeatureTypeByName(name.getLocalPart());
        catalog.save(fti);

        setupVectorDimension(
                name.getLocalPart(),
                ResourceInfo.TIME,
                timeAttribute,
                DimensionPresentation.LIST,
                null,
                null,
                null);

        LayerInfo layer = catalog.getFactory().createLayer();
        layer.setResource(fti);
        catalog.add(layer);
        layer = catalog.getLayerByName(name.getLocalPart());
        catalog.save(layer);

        DimensionInfo di = fti.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        di.setNearestMatchEnabled(true);
        di.setAcceptableInterval("PT1H/PT0H");
        di.setRawNearestMatchEnabled(false);
        di.setNearestFailBehavior(DimensionInfo.NearestFailBehavior.EXCEPTION);
        catalog.save(fti);

        LayersMapper mapper = GeoServerExtensions.bean(LayersMapper.class);
        // Reload the mappings
        mapper.onReset();
    }

    private void wait(PinningService pinningService) {
        PinningService.StatusValue status = pinningService.getStatus().getStatus();
        while ((status = pinningService.getStatus().getStatus())
                != PinningService.StatusValue.COMPLETED) {
            // System.out.println(status);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    @SuppressWarnings("PMD.SystemPrintln")
    public void testPinning() throws Exception {
        addLayers();
        Connection conn =
                DriverManager.getConnection(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());

        PinningService pinningService = GeoServerExtensions.bean(PinningService.class);
        // RESET
        System.out.println("Resetting pins");
        pinningService.reset();
        wait(pinningService);
        writeQueryResultToJsonFile(conn, GET_VIEWS_QUERY, "reset_views.json");
        assertSameResult(conn, GET_VIEWS_QUERY, "reset_views.json", VIEW_ID);

        writeQueryResultToJsonFile(conn, TEMPLATE_PIN_REQUEST, MSG_FES_RDT, "reset");
        assertSameResult(conn, TEMPLATE_PIN_REQUEST, MSG_FES_RDT, "reset", FID);
        writeQueryResultToJsonFile(conn, TEMPLATE_PIN_REQUEST, M01_ORBITAL_TRACKS, "reset");
        assertSameResult(conn, TEMPLATE_PIN_REQUEST, M01_ORBITAL_TRACKS, "reset", FID);

        // DELETE ONE VIEW
        System.out.println("Disabling 1 view");
        TestContext.setUpdateTime("2025-03-20T15:00:00Z");
        pinningService.incremental();
        wait(pinningService);
        writeQueryResultToJsonFile(conn, GET_VIEWS_QUERY, "views_after_delete.json");
        assertSameResult(conn, GET_VIEWS_QUERY, "views_after_delete.json", VIEW_ID);
        writeQueryResultToJsonFile(conn, TEMPLATE_PIN_REQUEST, M01_ORBITAL_TRACKS, "after_delete");
        assertSameResult(conn, TEMPLATE_PIN_REQUEST, M01_ORBITAL_TRACKS, "after_delete", FID);

        // INSERT BACK the VIEW
        System.out.println("Inserting 1 view");
        TestContext.setUpdateTime("2025-03-20T16:00:00Z");
        pinningService.incremental();
        wait(pinningService);
        writeQueryResultToJsonFile(conn, GET_VIEWS_QUERY, "views_after_insert.json");
        assertSameResult(conn, GET_VIEWS_QUERY, "views_after_insert.json", VIEW_ID);
        writeQueryResultToJsonFile(conn, TEMPLATE_PIN_REQUEST, M01_ORBITAL_TRACKS, "after_insert");
        assertSameResult(conn, TEMPLATE_PIN_REQUEST, M01_ORBITAL_TRACKS, "after_insert", FID);
        writeQueryResultToJsonFile(conn, TEMPLATE_PIN_REQUEST, MSG_FES_RDT, "after_insert");
        assertSameResult(conn, TEMPLATE_PIN_REQUEST, MSG_FES_RDT, "after_insert", FID);

        // Update the VIEWS, by adding one layer to the first and removing one layer from the second
        System.out.println("Change 2 views, adding and removing a layer");
        TestContext.setUpdateTime("2025-03-20T17:00:00Z");
        pinningService.incremental();
        wait(pinningService);
        writeQueryResultToJsonFile(conn, GET_VIEWS_QUERY, "views_after_update.json");
        assertSameResult(conn, GET_VIEWS_QUERY, "views_after_update.json", VIEW_ID);
        writeQueryResultToJsonFile(conn, TEMPLATE_PIN_REQUEST, M01_ORBITAL_TRACKS, "after_update");
        assertSameResult(conn, TEMPLATE_PIN_REQUEST, M01_ORBITAL_TRACKS, "after_update", FID);
        writeQueryResultToJsonFile(conn, TEMPLATE_PIN_REQUEST, MSG_FES_RDT, "after_update");
        assertSameResult(conn, TEMPLATE_PIN_REQUEST, MSG_FES_RDT, "after_update", FID);

        // Update one VIEW, by moving it out of the pinning window
        System.out.println("Updating 1 view, out of the pinning window");
        TestContext.setUpdateTime("2025-03-20T18:00:00Z");
        pinningService.incremental();
        wait(pinningService);
        writeQueryResultToJsonFile(conn, GET_VIEWS_QUERY, "views_move_out.json");
        assertSameResult(conn, GET_VIEWS_QUERY, "views_move_out.json", VIEW_ID);
        writeQueryResultToJsonFile(
                conn, TEMPLATE_PIN_REQUEST, M01_ORBITAL_TRACKS, "after_move_out");
        assertSameResult(conn, TEMPLATE_PIN_REQUEST, M01_ORBITAL_TRACKS, "after_move_out", FID);

        // Update one VIEW, by moving it within the pinning window
        System.out.println("Updating 1 view, within the pinning window");
        TestContext.setUpdateTime("2025-03-20T19:00:00Z");
        pinningService.incremental();
        wait(pinningService);
        writeQueryResultToJsonFile(conn, GET_VIEWS_QUERY, "views_move_in.json");
        assertSameResult(conn, GET_VIEWS_QUERY, "views_move_in.json", VIEW_ID);
        writeQueryResultToJsonFile(conn, TEMPLATE_PIN_REQUEST, M01_ORBITAL_TRACKS, "after_move_in");
        assertSameResult(conn, TEMPLATE_PIN_REQUEST, M01_ORBITAL_TRACKS, "after_move_in", FID);
    }

    private void assertSameResult(Connection conn, String query, String expectedJson, String id)
            throws SQLException, IOException {
        // Actual result from DB
        List<Map<String, Object>> actualResult = runQueryAndMapToList(conn, query);

        // Expected result from file
        ObjectMapper mapper = new ObjectMapper();
        Path expectedPath = Paths.get(EXPECTED_RESULTS_FOLDER + expectedJson);
        List<Map<String, Object>> expectedResult =
                mapper.readValue(Files.newInputStream(expectedPath), new TypeReference<>() {});

        // Optional: sort for consistent order
        Comparator<Map<String, Object>> byId = Comparator.comparing(row -> row.get(id).toString());
        actualResult.sort(byId);
        expectedResult.sort(byId);

        // Assertion
        assertEquals(expectedResult, actualResult);
    }

    public static void writeQueryResultToJsonFile(Connection conn, String sql, String outputFile)
            throws Exception {
        if (WRITE_RESULTS) {
            List<Map<String, Object>> result = runQueryAndMapToList(conn, sql);

            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(Paths.get(EXPECTED_RESULTS_FOLDER + outputFile).toFile(), result);
        }
    }

    private void assertSameResult(
            Connection conn, String template, String table, String suffix, String id)
            throws SQLException, IOException {
        assertSameResult(conn, prepareQuery(template, table), table + "_" + suffix + ".json", id);
    }

    public static void writeQueryResultToJsonFile(
            Connection conn, String template, String table, String suffix) throws Exception {
        writeQueryResultToJsonFile(
                conn, prepareQuery(template, table), table + "_" + suffix + ".json");
    }

    private static String prepareQuery(String template, String table) {
        String sql;
        if (template.contains(",pin")) {
            String timeAttribute = MSG_FES_RDT.equalsIgnoreCase(table) ? "testtime" : "time";
            sql = String.format(template, timeAttribute, table, timeAttribute);
        } else {
            sql = String.format(template, table);
        }
        return sql;
    }

    public static List<Map<String, Object>> runQueryAndMapToList(Connection conn, String sql)
            throws SQLException {
        List<Map<String, Object>> resultList = new ArrayList<>();

        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object raw = rs.getObject(i);
                    Object value;

                    if (raw instanceof java.sql.Array) {
                        Object[] arr = (Object[]) ((Array) raw).getArray();
                        value = Arrays.asList(arr); // Get Java array
                    } else if (raw instanceof Timestamp) {
                        value =
                                ((Timestamp) raw)
                                        .toLocalDateTime()
                                        .toInstant(ZoneOffset.UTC)
                                        .toString();
                    } else {
                        value = raw;
                    }

                    row.put(columnName, value);
                }
                resultList.add(row);
            }
        }

        return resultList;
    }
}
