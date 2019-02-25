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
package org.sonatype.nexus.selector;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Transforms path regular expressions with leading slashes so they match paths without leading slashes.
 * Detection of leading slashes for transformation is limited to a common subset of regular expressions
 * in order to keep the code simple and fast. This subset includes the use of negative lookahead/behind.
 * Leading slashes in complex expressions using the extended features of Java regex may not be detected.
 * In such cases the original expression will be returned unchanged.
 * <p>
 * The general approach is to remove a leading slash if there is no preceding content:
 *
 * <pre>
 * /foo/.* --> foo/.*
 * </pre>
 *
 * Leading slashes preceded by wildcarded content are made optional with:
 *
 * <pre>
 * .*&#47;foo/.* --> .*(^|/)foo/.*
 * </pre>
 *
 * The trick here is to use the start metacharacter ^ to handle the case when there is no leading content.
 * <p>
 * Most of the complexity in this implementation is around detecting when a slash is leading, especially
 * since you may have several alternate clauses mixed with a variety of wildcarded and nested expressions.
 *
 * @since 3.15
 */
class LeadingSlashRegexTransformer
{
  private final String regex;

  private StringBuilder buf;

  private int mark;

  private int cursor;

  /**
   * Finds leading slashes in the regex and transforms them to handle paths that don't begin with a slash.
   *
   * @param regex the regular expression to transform
   * @return the transformed regular expression
   */
  public static String trimLeadingSlashes(final String regex) {
    return new LeadingSlashRegexTransformer(regex).transform();
  }

  /**
   * Instances can't be re-used, so only let this class construct instances.
   */
  private LeadingSlashRegexTransformer(final String regex) {
    this.regex = checkNotNull(regex);
  }

  /**
   * Finds leading slashes in the regex and transforms them to handle paths that don't begin with a slash.
   */
  public String transform() {
    transformGroup(false);

    // return original if unchanged, otherwise complete and return transformed result
    return buf == null ? regex : buf.append(regex, mark, regex.length()).toString();
  }

  /**
   * Finds leading slashes in the group and transforms them to handle paths that don't begin with a slash.
   *
   * @param hasPrecedingWildcard was there any wildcarded content before this group
   * @return {@code true} if this group has no empty clauses, otherwise {@code false}
   */
  private boolean transformGroup(final boolean hasPrecedingWildcard) {

    // local flags for the group
    boolean noEmptyClauses = true;
    boolean hasLeadingText = false;
    boolean hasWildcard = false;

    while (hasChar()) {
      switch (consumeNextChar()) {

        // leading content

        case '/':
          if (!hasLeadingText) {
            transformLeadingSlash(hasPrecedingWildcard || hasWildcard);
          }
          break;
        case '\\':
          consumeNextChar();
          break;
        case '[':
          consumeRange();
          break;
        case '(':
          if (!hasLeadingText) {
            // dive into nested group to continue looking for leading slashes
            boolean lookaround = consumeLookaroundOptions();
            hasLeadingText = transformGroup(hasPrecedingWildcard || hasWildcard);
            if (lookaround) {
              hasLeadingText = false; // lookaround doesn't count as leading text
            }
            continue; // flags already updated, jump straight to next character
          }
          consumeGroup();
          break;

        // trailing content

        case '|': // NOSONAR
          noEmptyClauses &= hasLeadingText;
          hasLeadingText = false;
          hasWildcard = false;
          continue;
        case ')':
          noEmptyClauses &= hasLeadingText;
          return noEmptyClauses;
        case '*':
        case '?': // NOSONAR
          hasLeadingText = false;
          hasWildcard = true;
          continue;
        case '^':
        case '$': // NOSONAR
          continue;

        default:
          break;
      }

      if (hasLeadingText) {
        consumeClause(); // text followed by more text means this clause cannot contain leading slashes
      }
      else {
        hasLeadingText = true; // record we found some text, but next token might end up wildcarding it
      }
    }
    return noEmptyClauses;
  }

  /**
   * Transforms a leading slash so the regex will match against paths that don't begin with a slash.
   * In most cases this just involves removing the slash, but when there's a preceding wildcard then
   * we replace the slash with {@code (^|/)}. This matches paths without leading content, as well as
   * correctly matching paths where the pattern appears as a substring.
   *
   * @param hasPrecedingWildcard was there any wildcarded content before this leading slash
   */
  private void transformLeadingSlash(final boolean hasPrecedingWildcard) {
    if (hasChar() && (peekNextChar() == '?' || peekNextChar() == '*')) {
      return; // leading slash is already optional
    }
    if (buf == null) {
      buf = new StringBuilder(regex.length() + 16);
    }
    buf.append(regex, mark, cursor - 1);
    if (hasPrecedingWildcard || (hasChar() && peekNextChar() == '+')) {
      buf.append("(^|/)");
    }
    mark = cursor;
  }

  /**
   * Consumes optional lookaround options at the beginning of a group. Note we don't cover all potential group
   * options here, just negative lookaround which is sometimes used to avoid matching against source jars, etc.
   *
   * @return {@code true} if this is a lookaround group, otherwise {@code false}
   */
  private boolean consumeLookaroundOptions() {
    if (hasChar() && peekNextChar() == '?') {
      consumeNextChar();
      if (hasChar() && peekNextChar() == '<') {
        consumeNextChar();
      }
      if (hasChar() && peekNextChar() == '!') {
        consumeNextChar();
        return true;
      }
    }
    return false;
  }

  /**
   * Consumes a regex clause up to, but not including, the character marking the end of the clause.
   */
  private void consumeClause() {
    while (hasChar() && peekNextChar() != ')' && peekNextChar() != '|') {
      switch (consumeNextChar()) {
        case '\\':
          consumeNextChar();
          break;
        case '[':
          consumeRange();
          break;
        case '(':
          consumeGroup();
          break;
        default:
          break;
      }
    }
  }

  /**
   * Consumes a complete regex group; includes any nested/aggregate groups.
   */
  private void consumeGroup() {
    int nesting = 1;
    while (nesting > 0 && hasChar()) {
      switch (consumeNextChar()) {
        case '\\':
          consumeNextChar();
          break;
        case '[':
          consumeRange();
          break;
        case '(':
          nesting++;
          break;
        case ')':
          nesting--;
          break;
        default:
          break;
      }
    }
  }

  /**
   * Consumes a complete regex range; includes any nested/aggregate ranges.
   */
  private void consumeRange() {
    int nesting = 1;
    while (nesting > 0 && hasChar()) {
      switch (consumeNextChar()) {
        case '\\':
          consumeNextChar();
          break;
        case '[':
          nesting++;
          break;
        case ']':
          nesting--;
          break;
        default:
          break;
      }
    }
  }

  private boolean hasChar() {
    return cursor < regex.length();
  }

  private char peekNextChar() {
    return regex.charAt(cursor);
  }

  private char consumeNextChar() {
    return regex.charAt(cursor++);
  }
}
