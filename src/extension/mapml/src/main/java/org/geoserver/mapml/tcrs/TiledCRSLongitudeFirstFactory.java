/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.mapml.tcrs;

import org.geotools.api.metadata.citation.Citation;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CRSAuthorityFactory;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.factory.AbstractAuthorityFactory;
import org.geotools.referencing.factory.DeferredAuthorityFactory;
import org.geotools.referencing.factory.OrderedAxisCRSAuthorityFactory;
import org.geotools.referencing.factory.epsg.ThreadedEpsgFactory;
import org.geotools.util.factory.FactoryNotFoundException;
import org.geotools.util.factory.FactoryRegistryException;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.factory.Hints;

public class TiledCRSLongitudeFirstFactory extends DeferredAuthorityFactory
        implements CRSAuthorityFactory {

    /**
     * Creates a default factory. The {@link Hints#FORCE_LONGITUDE_FIRST_AXIS_ORDER
     * FORCE_LONGITUDE_FIRST_AXIS_ORDER} hint is always set to {@link Boolean#TRUE TRUE}. The {@link
     * Hints#FORCE_STANDARD_AXIS_DIRECTIONS FORCE_STANDARD_AXIS_DIRECTIONS} and {@link
     * Hints#FORCE_STANDARD_AXIS_UNITS FORCE_STANDARD_AXIS_UNITS} hints are set to {@link
     * Boolean#FALSE FALSE} by default. A different value for those two hints can be specified using
     * the {@linkplain IAULongitudeFirstFactory (Hints) constructor below}.
     */
    public TiledCRSLongitudeFirstFactory() {
        this(null);
    }

    /**
     * Creates a factory from the specified set of hints.
     *
     * @param userHints An optional set of hints, or {@code null} for the default values.
     */
    public TiledCRSLongitudeFirstFactory(final Hints userHints) {
        super(userHints, NORMAL_PRIORITY + relativePriority());
        // See comment in createBackingStore() method body.
        hints.put(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);
        put(userHints, Hints.FORCE_STANDARD_AXIS_DIRECTIONS);
        put(userHints, Hints.FORCE_STANDARD_AXIS_UNITS);
    }

    /** Stores a value from the specified hints. */
    private void put(final Hints userHints, final Hints.Key key) {
        Object value = null;
        if (userHints != null) {
            value = userHints.get(key);
        }
        if (value == null) {
            value = Boolean.FALSE;
        }
        hints.put(key, value);
    }

    /**
     * Returns the priority to use relative to the {@link ThreadedEpsgFactory} priority. The default
     * priority should be lower, except if the <code>
     * {@value GeoTools#FORCE_LONGITUDE_FIRST_AXIS_ORDER}</code> system property is set to {@code
     * true}.
     */
    private static int relativePriority() {
        return -7;
    }

    @Override
    public Citation getAuthority() {
        return TiledCRSFactory.MAPML;
    }

    /**
     * Returns the factory instance (usually {@link ThreadedEpsgFactory}) to be used as the backing
     * store.
     *
     * @throws FactoryException If no suitable factory instance was found.
     */
    @Override
    protected AbstractAuthorityFactory createBackingStore() throws FactoryException {
        /*
         * Set the hints for the backing store to fetch. I'm not sure that we should request a
         * org.geotools.referencing.factory.epsg.ThreadedEpsgFactory implementation; for now we are
         * making this requirement mostly as a safety in order to get an implementation that is
         * known to work, but we could relax that in a future version. AbstractAuthorityFactory
         * is the minimal class required with current OrderedAxisAuthorityFactory API.
         *
         * The really important hints are the FORCE_*_AXIS_* handled by this class, which MUST
         * be set to FALSE. This is especially important for FORCE_LONGITUDE_FIRST_AXIS_ORDER,
         * which must be set to a different value than the value set by the constructor in order
         * to prevent LongitudeFirstFactory to fetch itself. The other hints must also be set to
         * false since forcing axis directions / units is handled by OrderedAxisAuthorityFactory
         * and we don't want the backing store to interfer with that.
         */
        final Hints backingStoreHints =
                new Hints(Hints.CRS_AUTHORITY_FACTORY, TiledCRSFactory.class);
        backingStoreHints.put(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.FALSE);
        backingStoreHints.put(Hints.FORCE_STANDARD_AXIS_DIRECTIONS, Boolean.FALSE);
        backingStoreHints.put(Hints.FORCE_STANDARD_AXIS_UNITS, Boolean.FALSE);
        final AbstractAuthorityFactory factory;
        try {
            factory =
                    (AbstractAuthorityFactory)
                            ReferencingFactoryFinder.getCRSAuthorityFactory(
                                    "IAU", backingStoreHints);
        } catch (FactoryNotFoundException exception) {
            throw new org.geotools.referencing.factory.FactoryNotFoundException(exception);
        } catch (FactoryRegistryException exception) {
            throw new FactoryException(exception);
        }
        return new OrderedAxisCRSAuthorityFactory(factory, new Hints(hints), null);
    }
}
