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
package uk.co.timwise.sqlhawk.db;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Methods for handling / modifying T-SQL statements.
 */
public class SqlManagement {

	/**
	 * Change definition from CREATE to ALTER before saving (or vice versa before
	 * running - this is to make using scm .sql scripts manually easier. A single
	 * change to CREATE the first time you use a proc/function/view is easier than
	 * repeatedly changing to ALTER. TODO: maybe make this a configurable option
	 * at some point.
	 *
	 * @param sqlText
	 *          the sql text
	 * @return the string
	 */
	public static String ConvertCreateToAlter(String sqlText) {
		return ConvertAction(sqlText, Convertion.ToAlter);
	}

	public static String ConvertAlterToCreate(String sqlText) {
		return ConvertAction(sqlText, Convertion.ToCreate);
	}

	private static String ConvertAction(String sqlText, Convertion convertion) {
		Pattern pattern;
		Matcher matcher;
		switch (convertion){
			case ToCreate:
				pattern = Pattern.compile("^ALTER", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
				matcher = pattern.matcher(sqlText);
				return matcher.replaceFirst("CREATE");
			case ToAlter:
				pattern = Pattern.compile("^CREATE", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
				matcher = pattern.matcher(sqlText);
				return matcher.replaceFirst("ALTER");
			default:
				throw new IllegalArgumentException();
		}
	}

	private enum Convertion {
		ToCreate, ToAlter
	}
}
