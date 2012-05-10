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
package uk.co.timwise.sqlhawk.view;

import uk.co.timwise.sqlhawk.util.Dot;

public class HtmlDiagramFormatter extends HtmlFormatter {
    private static boolean printedNoDotWarning = false;
    private static boolean printedInvalidVersionWarning = false;

    protected HtmlDiagramFormatter() {
    }

    protected Dot getDot() {
        Dot dot = Dot.getInstance();
        if (!dot.exists()) {
            if (!printedNoDotWarning) {
                printedNoDotWarning = true;
                System.err.println();
                System.err.println("Warning: Failed to run dot.");
                System.err.println("   Download " + dot.getSupportedVersions());
                System.err.println("   from www.graphviz.org and make sure that dot is either in your path");
                System.err.println("   or point to where you installed Graphviz with the -gv option.");
                System.err.println("   Generated pages will not contain a diagramtic view of table relationships.");
            }

            return null;
        }

        if (!dot.isValid()) {
            if (!printedInvalidVersionWarning) {
                printedInvalidVersionWarning = true;
                System.err.println();
                System.err.println("Warning: Invalid version of Graphviz dot detected (" + dot.getVersion() + ").");
                System.err.println("   sqlHawk requires " + dot.getSupportedVersions() + ". from www.graphviz.org.");
                System.err.println("   Generated pages will not contain a diagramatic view of table relationships.");
            }

            return null;
        }

        return dot;
    }
}
