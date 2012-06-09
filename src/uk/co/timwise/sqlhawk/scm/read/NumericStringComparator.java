package uk.co.timwise.sqlhawk.scm.read;

import java.io.File;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compares filenames to provide number based ordering.
 * Interprets numbers at the start of file/directory names
 * to ensure that file 22-foo.sql is run after 9-foo.sql.
 * Directories come after files.
 * Remaining matches are compared with normal string comparison.
 * Used when running upgrade scripts.
 */
public final class NumericStringComparator implements Comparator<File> {
	Pattern fileNumbers = Pattern.compile("^([0-9]+)(.*)");

	@Override
	public int compare(File o1, File o2) {
		if (o1.isDirectory() ^ o2.isDirectory()){
			// run directories last
			return o1.isDirectory() ? 1 : -1;
		}
		Matcher o1Number = fileNumbers.matcher(o1.getName());
		Matcher o2Number = fileNumbers.matcher(o2.getName());
		if (!o1Number.find() || !o2Number.find()) {
			// not both numeric, so simple string compare
			return o1.getName().compareTo(o2.getName());
		}
		Integer o1n = Integer.parseInt(o1Number.group(1));
		Integer o2n = Integer.parseInt(o2Number.group(1));
		if (o1n != o2n) {
			return o1n.compareTo(o2n);
		}
		// same number, compare rest of script (avoiding leading zero differences)
		return o1Number.group(2).compareTo(o2Number.group(2));
	}
}