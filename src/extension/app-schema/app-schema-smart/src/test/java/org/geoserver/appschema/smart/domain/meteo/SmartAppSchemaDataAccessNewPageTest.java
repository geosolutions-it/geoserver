package org.geoserver.appschema.smart.domain.meteo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.Serializable;
import java.util.Map;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.appschema.smart.SmartAppSchemaGeoServerTestSupport;
import org.geoserver.appschema.smart.data.PostgisSmartAppSchemaDataAccessFactory;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.web.data.layer.NewLayerPage;
import org.geoserver.web.data.store.DataAccessNewPage;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test suite for {@link DataAccessNewPage}, using {@link PostgisSmartAppSchemaDataAccessFactory}.
 *
 * @author Jose Macchi - GeoSolutions
 */
public class SmartAppSchemaDataAccessNewPageTest extends SmartAppSchemaGeoServerTestSupport {

    public SmartAppSchemaDataAccessNewPageTest() {
        SCHEMA = "smartappschematest";
        NAMESPACE_PREFIX = "mt";
        TARGET_NAMESPACE = "http://www.geo-solutions.it/smartappschema/1.0";
        MOCK_SQL_SCRIPT = "meteo_db.sql";
    }

    @Ignore
    @Test
    public void testNewDataStoreForMeteoSave() {
        // fill parameters in a DataAccessNewPage for SmartAppSchema and save it
        // saving it implies:
        // 1. connects to postgis online database
        // 2. get metadata and generate domainmodel based on parameters
        // 3. build docs for appschema
        // 4. create an appschema datastore based on generated schemas
        DataAccessNewPage page = (DataAccessNewPage) startPage();
        FormTester ft = tester.newFormTester("dataStoreForm");
        Map<String, Serializable> params = getSmartAppSchemaDataStoreParams();
        String rootentity = "meteo_stations";
        String datastoreName = "meteo";
        setFormValues(ft, datastoreName, params, rootentity);
        ft.submit("save");

        tester.assertNoErrorMessage();
        tester.assertRenderedPage(NewLayerPage.class);
        DataStoreInfo store = getCatalog().getDataStoreByName(datastoreName);
        assertNotNull(store);
        assertEquals(
                (String) params.get(PostgisNGDataStoreFactory.USER.key),
                store.getConnectionParameters().get("user"));
        assertEquals(
                (String) params.get(PostgisNGDataStoreFactory.SCHEMA.key),
                store.getConnectionParameters().get("schema"));
        assertEquals(rootentity, store.getConnectionParameters().get("root entity"));
    }
}
