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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ResourceWriter {
	private static ResourceWriter instance = new ResourceWriter();

	protected ResourceWriter() {
	}

	public static ResourceWriter getInstance() {
		return instance;
	}

	/**
	 * Write the specified resource to the specified filename
	 *
	 * @param resourceName
	 * @param writeTo
	 * @throws IOException
	 */
	public void writeResource(String resourceName, File writeTo) throws IOException {
		writeTo.getParentFile().mkdirs();
		InputStream in = getClass().getResourceAsStream(resourceName);
		if (in == null)
			throw new IOException("Resource \"" + resourceName + "\" not found");

		byte[] buf = new byte[4096];

		OutputStream out = new FileOutputStream(writeTo);
		int numBytes = 0;
		while ((numBytes = in.read(buf)) != -1) {
			out.write(buf, 0, numBytes);
		}
		in.close();
		out.close();
	}
}