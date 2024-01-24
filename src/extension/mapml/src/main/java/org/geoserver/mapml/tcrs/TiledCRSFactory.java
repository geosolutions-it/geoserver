/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml.tcrs;

import java.util.Set;
import java.util.stream.Collectors;
import org.geotools.api.metadata.citation.Citation;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.IdentifiedObject;
import org.geotools.api.referencing.NoSuchAuthorityCodeException;
import org.geotools.api.referencing.crs.CRSAuthorityFactory;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.gml2.SrsSyntax;
import org.geotools.gml2.bindings.GML2EncodingUtils;
import org.geotools.metadata.iso.IdentifierImpl;
import org.geotools.metadata.iso.citation.CitationImpl;
import org.geotools.metadata.iso.citation.Citations;
import org.geotools.referencing.CRS;
import org.geotools.referencing.factory.AllAuthoritiesFactory;
import org.geotools.referencing.factory.AuthorityFactoryAdapter;
import org.geotools.util.factory.Hints;

/**
 * Exposes available {@link TiledCRS} to GeoServer as valid Coordinate Reference Systems. The
 * current implementation loads a database of TiledCRS from TiledCRSConstant, will use the TCRS
 * database in the future, if those are made to be configurable
 */
public class TiledCRSFactory extends AuthorityFactoryAdapter implements CRSAuthorityFactory {
    /** The authority prefix */
    private static final String AUTHORITY = "MapML";

    public static final Citation MAPML;

    static {
        final CitationImpl c = new CitationImpl("MapML");
        c.getIdentifiers().add(new IdentifierImpl(AUTHORITY));
        c.freeze();
        MAPML = c;
    }

    /**
     * Returns {@code false} if {@link Hints#FORCE_LONGITUDE_FIRST_AXIS_ORDER} should be set to
     * {@link Boolean#FALSE}. This method compares {@link Hints#FORCE_AXIS_ORDER_HONORING} with the
     * specified authority.
     *
     * @param hints The hints to use (may be {@code null}).
     * @param authority The authority factory under creation.
     * @todo Should not looks at system hints; this is {@link ReferencingFactoryFinder}'s job.
     */
    static boolean defaultAxisOrderHints(final Hints hints, final String authority) {
        Object value = null;
        if (hints != null) {
            value = hints.get(Hints.FORCE_AXIS_ORDER_HONORING);
        }
        if (value == null) {
            value = Hints.getSystemDefault(Hints.FORCE_AXIS_ORDER_HONORING);
        }
        if (value instanceof CharSequence) {
            final String list = value.toString();
            int i = 0;
            while ((i = list.indexOf(authority, i)) >= 0) {
                if (i == 0 || !Character.isJavaIdentifierPart(list.charAt(i - 1))) {
                    final int j = i + authority.length();
                    if (j == list.length() || !Character.isJavaIdentifierPart(list.charAt(j))) {
                        // Found the authority in the list: we need to use the global setting.
                        return true;
                    }
                }
                i++;
            }
        }
        return false;
    }

    /** Builds the MapML CRS factory with no hints. */
    public TiledCRSFactory() {
        this(null);
    }

    /** Constructs a default factory for the {@code CRS} authority. */
    public TiledCRSFactory(Hints hints) {
        super(new AllAuthoritiesFactory(hints));
    }

    /** Returns the authority for this factory, which is {@link Citations#CRS CRS}. */
    @Override
    public Citation getAuthority() {
        return MAPML;
    }

    @Override
    public Set<String> getAuthorityCodes(Class<? extends IdentifiedObject> type)
            throws FactoryException {
        return TiledCRSConstants.tiledCRSDefinitions.values().stream()
                .map(TiledCRSParams::getName)
                .collect(Collectors.toSet());
    }

    @Override
    protected String toBackingFactoryCode(String code) throws FactoryException {
        String identifier = getIdentifier(code);

        TiledCRSParams definition = TiledCRSConstants.lookupTCRS(identifier);
        if (definition == null) {
            throw new NoSuchAuthorityCodeException("No such CRS: " + code, AUTHORITY, code);
        }

        // get the backing store code in form "authority:code", axis order handling
        // happens in wrappers of this factory
        String definitionCode = definition.getCode();
        CoordinateReferenceSystem crs = CRS.decode(definitionCode);
        return GML2EncodingUtils.toURI(crs, SrsSyntax.AUTH_CODE, true);
    }

    /**
     * Grabs the identifier from the code, which may be in the form "authority:code" or just "code"
     *
     * @param code
     * @return
     */
    private String getIdentifier(String code) {
        String identifier = trimAuthority(code).toUpperCase();
        if (identifier.startsWith(AUTHORITY)) {
            identifier = identifier.substring(AUTHORITY.length());
        }
        return identifier;
    }
}
