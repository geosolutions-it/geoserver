/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.wps.gs.rasteralgebra;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;

import javax.media.jai.operator.AbsoluteDescriptor;
import javax.media.jai.operator.AndDescriptor;
import javax.media.jai.operator.ExpDescriptor;
import javax.media.jai.operator.LogDescriptor;
import javax.media.jai.operator.NotDescriptor;
import javax.media.jai.operator.OrDescriptor;

/**
 * {@link Enum} instance that contains the operations implemented so far for the
 * {@link FilterRasterAlgebraProcess}.
 * 
 * @author Simone Giannecchini, GeoSolutions SAS TODO add aritmetic operations
 *         TODO add geometric operations
 */
enum Operator {
    NOT {
        @Override
        public RenderedImage process(RenderedImage[] sources,
                final RenderingHints hints) {
            assert sources.length == 1;
            return NotDescriptor.create(sources[0], hints);
        }
    },
    AND {
        @Override
        public RenderedImage process(RenderedImage[] sources,
                final RenderingHints hints) {
            assert sources.length >= 2;
            if (sources.length == 2) {
                return AndDescriptor.create(sources[0], sources[1], hints);
            }

            // more than 2
            RenderedImage out = sources[0];
            for (int i = 1; i < sources.length; i++) {
                out = AndDescriptor.create(out, sources[i], hints);
            }
            return out;

        }
    },
    OR {
        @Override
        public RenderedImage process(RenderedImage[] sources,
                final RenderingHints hints) {
            assert sources.length >= 2;
            if (sources.length == 2) {
                return OrDescriptor.create(sources[0], sources[1], hints);
            }

            // more than 2
            RenderedImage out = sources[0];
            for (int i = 1; i < sources.length; i++) {
                out = OrDescriptor.create(out, sources[i], hints);
            }
            return out;
        }
    },
    NULL {
        @Override
        public RenderedImage process(RenderedImage[] sources,
                final RenderingHints hints) {
            assert sources.length == 1;
            return sources[0];
        }
        // },MAX2 {
        // @Override
        // public RenderedImage process(RenderedImage[] sources, final
        // RenderingHints hints) {
        // if(sources.length==1){
        // return sources[0];
        // }
        // return MaxDescriptor.create(Arrays.asList(sources), hints);
        // }
    },
    MAX2 {
        @Override
        public RenderedImage process(RenderedImage[] sources,
                final RenderingHints hints) {
            if (sources.length == 1) {
                return sources[0];
            }
            // more than 2
            RenderedImage out = sources[0];
            for (int i = 1; i < sources.length; i++) {
                out = javax.media.jai.operator.MaxDescriptor.create(out,
                        sources[i], hints);
            }
            return out;
        }
    },
    MIN2 {
        @Override
        public RenderedImage process(RenderedImage[] sources,
                final RenderingHints hints) {
            if (sources.length == 1) {
                return sources[0];
            }
            // more than 2
            RenderedImage out = sources[0];
            for (int i = 1; i < sources.length; i++) {
                out = javax.media.jai.operator.MinDescriptor.create(out,
                        sources[i], hints);
            }
            return out;
        }
    },
    ABS {
        @Override
        public RenderedImage process(RenderedImage[] sources,
                final RenderingHints hints) {
            assert sources.length == 1;
            return AbsoluteDescriptor.create(sources[0], hints);
        }
    },
    ABS_2 {
        @Override
        public RenderedImage process(RenderedImage[] sources,
                final RenderingHints hints) {
            assert sources.length == 1;
            return AbsoluteDescriptor.create(sources[0], hints);
        }
    },
    ABS_3 {
        @Override
        public RenderedImage process(RenderedImage[] sources,
                final RenderingHints hints) {
            assert sources.length == 1;
            return AbsoluteDescriptor.create(sources[0], hints);
        }
    },
    ABS_4 {
        @Override
        public RenderedImage process(RenderedImage[] sources,
                final RenderingHints hints) {
            assert sources.length == 1;
            return AbsoluteDescriptor.create(sources[0], hints);
        }
    },
    EXP {
        @Override
        public RenderedImage process(RenderedImage[] sources,
                final RenderingHints hints) {
            assert sources.length == 1;
            return ExpDescriptor.create(sources[0], hints);
        }
    },
    LOG {
        @Override
        public RenderedImage process(RenderedImage[] sources,
                final RenderingHints hints) {
            assert sources.length == 1;
            return LogDescriptor.create(sources[0], hints);
        }
    };

    public abstract RenderedImage process(final RenderedImage sources[],
            final RenderingHints hints);
}
