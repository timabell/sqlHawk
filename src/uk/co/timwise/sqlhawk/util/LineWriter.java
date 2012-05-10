/* This file is a part of the sqlHawk project.
 * http://github.com/timabell/sqlHawk
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package uk.co.timwise.sqlhawk.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

/**
 * BufferedWriter that adds a <code>writeln()</code> method
 * to output a <i>lineDelimited</i> line of text without
 * cluttering up code.
 */
public class LineWriter extends BufferedWriter {
    private final Writer out;

    public LineWriter(String filename, String charset) throws UnsupportedEncodingException, FileNotFoundException {
        this(new FileOutputStream(filename), charset);
    }

    public LineWriter(String filename, int sz, String charset) throws UnsupportedEncodingException, FileNotFoundException {
        this(new FileOutputStream(filename), sz, charset);
    }

    public LineWriter(File file, String charset) throws UnsupportedEncodingException, FileNotFoundException {
        this(new FileOutputStream(file), charset);
    }

    public LineWriter(File file, int sz, String charset) throws UnsupportedEncodingException, IOException {
        this(new FileOutputStream(file), sz, charset);
    }

    public LineWriter(OutputStream out, String charset) throws UnsupportedEncodingException {
        this(new OutputStreamWriter(out, charset), 8192);
    }

    public LineWriter(OutputStream out, int sz, String charset) throws UnsupportedEncodingException {
        this(new OutputStreamWriter(out, charset), sz);
    }

    private LineWriter(Writer out, int sz) {
        // by this point a charset has already been specified
        super(out, sz);
        this.out = out;
    }

    public void writeln(String str) throws IOException {
        write(str);
        newLine();
    }

    public void writeln() throws IOException {
        newLine();
    }

    /**
     * Intended to simplify use when wrapping StringWriters.
     */
    @Override
    public String toString() {
        try {
            flush();
        } catch (IOException exc) {
            throw new RuntimeException(exc);
        }

        return out.toString();
    }
}