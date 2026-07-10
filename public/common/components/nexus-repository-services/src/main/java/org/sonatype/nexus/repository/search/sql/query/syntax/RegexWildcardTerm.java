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
package org.sonatype.nexus.repository.search.sql.query.syntax;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A term representing a wildcard pattern that should be converted to a regex for positional matching.
 * This preserves the order and position of wildcard characters (* and ?) in the search pattern.
 *
 * <p>
 * Example transformations:
 * <ul>
 * <li>{@code goo*com*} → regex matching "goo" followed by "com" in that order</li>
 * <li>{@code foo?bar*} → regex matching "foo", any single char, "bar", then anything</li>
 * </ul>
 */
public record RegexWildcardTerm(String term)
    implements StringTerm
{
  public RegexWildcardTerm {
    checkNotNull(term);
  }

  /**
   * Converts the wildcard pattern to a regex pattern string.
   * Wildcard '*' becomes '.*' (match any characters)
   * Wildcard '?' becomes '.' (match any single character)
   * Other special regex characters are escaped.
   *
   * <p>
   * Note: This method escapes characters using backslash notation (e.g., \\. for literal dot)
   * instead of Java's Pattern.quote() which uses \Q...\E, because PostgreSQL does not support
   * the \Q...\E escape sequence in its regex implementation.
   *
   * <p>
   * Note: The resulting regex pattern is case-sensitive. When used with PostgreSQL's ~ operator
   * or H2's REGEXP_LIKE, the matching will be case-sensitive, unlike LIKE with LOWER which is
   * case-insensitive. For example, "goo*Com*" will NOT match "goo-stuff-com" or "goo.COM".
   *
   * @return the regex pattern string
   */
  public String toRegexPattern() {
    StringBuilder regex = new StringBuilder("^");

    for (int i = 0; i < term.length(); i++) {
      char c = term.charAt(i);

      if (c == '\\') {
        // Handle escape sequences
        if (i + 1 < term.length()) {
          char next = term.charAt(i + 1);
          if (next == '*' || next == '?') {
            // Escaped wildcard - treat as literal character, escape it for regex
            regex.append("\\").append(next);
            i++; // Skip the next character
          }
          else {
            // Other escape - the backslash is literal, escape it for regex
            regex.append("\\\\");
          }
        }
        else {
          // Trailing backslash - escape it for regex
          regex.append("\\\\");
        }
      }
      else if (c == '*') {
        // Wildcard: match zero or more characters
        regex.append(".*");
      }
      else if (c == '?') {
        // Wildcard: match exactly one character
        regex.append(".");
      }
      else {
        // Regular character - may need escaping for regex special chars
        if (isRegexSpecialChar(c)) {
          regex.append("\\").append(c);
        }
        else {
          regex.append(c);
        }
      }
    }

    regex.append("$");
    return regex.toString();
  }

  /**
   * Checks if a character is a regex special character that needs escaping.
   * Regex special chars: . [ ] { } ( ) | ^ $ +
   * Note: * and ? are handled separately as wildcards.
   * Note: backslash is handled separately in the main conversion method.
   */
  private static boolean isRegexSpecialChar(char c) {
    return c == '.' || c == '[' || c == ']' || c == '{' || c == '}' || c == '(' || c == ')'
        || c == '|' || c == '^' || c == '$' || c == '+';
  }

  @Override
  public String get() {
    return term;
  }
}
