/* This file is a part of the sqlHawk project.
 * http://timabell.github.com/sqlHawk/
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
package uk.co.timwise.sqlhawk.html;

import java.io.File;
import java.io.IOException;


public class ImageWriter extends ResourceWriter {
	private static ImageWriter instance = new ImageWriter();

	private ImageWriter() {
	}

	public static ImageWriter getInstance() {
		return instance;
	}

	public void writeImages(File outputDir) throws IOException {
		new File(outputDir, "images").mkdir();

		writeResource("/images/tabLeft.gif", new File(outputDir, "/images/tabLeft.gif"));
		writeResource("/images/tabRight.gif", new File(outputDir, "/images/tabRight.gif"));
		writeResource("/images/background.gif", new File(outputDir, "/images/background.gif"));
	}
}
