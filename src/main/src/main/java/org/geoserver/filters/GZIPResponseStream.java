/*
 * Copyright (c) 2007 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * Copyright 2003 Jayson Falkner (jayson@jspinsider.com)
 * This code is from "Servlets and JavaServer pages; the J2EE Web Tier",
 * http://www.jspbook.com. You may freely use the code both commercially
 * and non-commercially. If you like the code, please pick up a copy of
 * the book and help support the authors, development of more free code,
 * and the JSP/Servlet/J2EE community.
 *
 * Modified by David Winslow <dwinslow@openplans.org> on 2007-12-13.
 */
package org.geoserver.filters;

import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

/**
 * A simple streaming gzipping servlet output stream wrapper
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class GZIPResponseStream extends ServletOutputStream {
    protected GZIPOutputStream gzipstream = null;

    protected boolean closed = false;

    public GZIPResponseStream(HttpServletResponse response) throws IOException {
        super();
        closed = false;
        gzipstream = new GZIPOutputStream(response.getOutputStream(), 4096, true);
    }

    public void close() throws IOException {
        if (closed) {
            throw new IOException("This output stream has already been closed");
        }
        gzipstream.finish();
        closed = true;
    }

    public void flush() throws IOException {
        if (closed) {
            throw new IOException("Cannot flush a closed output stream");
        }
        gzipstream.flush();
    }

    public void write(int b) throws IOException {
        if (closed) {
            throw new IOException("Cannot write to a closed output stream");
        }
        gzipstream.write((byte) b);
    }

    public void write(byte b[]) throws IOException {
        write(b, 0, b.length);
    }

    public void write(byte b[], int off, int len) throws IOException {
        if (closed) {
            throw new IOException("Cannot write to a closed output stream");
        }
        gzipstream.write(b, off, len);
    }

    public boolean closed() {
        return (this.closed);
    }

}
