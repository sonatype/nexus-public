/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.repository.search.sql.index;

import java.util.regex.Pattern;

public class SearchTokenizer
{
  public static final Pattern TOKENIZER = Pattern.compile("[_.\\-/\\\\ ]");

  private static final String QUOTE = "'";

  public static String tsEscape(final String term) {
    return QUOTE + term.toLowerCase().replace("\\", "\\\\").replace(QUOTE, "\\'") + QUOTE;
  }

  /**
   * Strips leading token separators from the provided term string
   */
  public static String stripLeadingSeparators(final String term) {
    int index = -1;

    for (int i = 0; i < term.length(); i++) {
      if (!isSeparator(term.charAt(i))) {
        break;
      }
      index = i;
    }

    return index > -1 ? term.substring(index + 1) : term;
  }

  /**
   * Strips trailing token separators from the provided term string
   */
  public static String stripTrailingSeparators(final String term) {
    int index = term.length();

    for (int i = term.length() - 1; i >= 0; i--) {
      if (!isSeparator(term.charAt(i))) {
        break;
      }
      index = i;
    }

    return index < term.length() ? term.substring(0, index) : term;
  }

  public static boolean isSeparator(final char c) {
    return c == ' ' || c == '-' || c == '\\' || c == '/' || c == '.' || c == '_';
  }
}
