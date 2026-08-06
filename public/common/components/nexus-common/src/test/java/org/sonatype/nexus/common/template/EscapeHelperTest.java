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
package org.sonatype.nexus.common.template;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class EscapeHelperTest
{
  EscapeHelper underTest;

  @Before
  public void setup() {
    underTest = new EscapeHelper();
  }

  @Test
  public void testStripJavaEl() {
    String test = "${badstuffinhere}";
    String result = underTest.stripJavaEl(test);
    assertThat(result, is("{badstuffinhere}"));
  }

  @Test
  public void testStripJavaEl_multiple_dollar_signs() {
    String test = "$$$$${badstuffinhere}";
    String result = underTest.stripJavaEl(test);
    assertThat(result, is("{badstuffinhere}"));
  }

  @Test
  public void testStripJavaEl_bugged_interpolator() {
    String test = "$\\A{badstuffinhere}";
    String result = underTest.stripJavaEl(test);
    assertThat(result, is("{badstuffinhere}"));
  }

  @Test
  public void testUriSegmentsEncoding() {
    assertThat(underTest.uriSegments("foo/bar+baz"), is("foo/bar+baz"));
    assertThat(underTest.uriSegments("foo/bar%baz"), is("foo/bar%25baz"));
    assertThat(underTest.uriSegments("foo/bar baz"), is("foo/bar%20baz"));
    assertThat(underTest.uriSegments("foo:path/bar:baz"), is("foo%3Apath/bar%3Abaz"));
  }

  /**
   * The base {@link EscapeHelper#uriSegments} method uses {@code String.split("/")}, which drops
   * trailing empty segments — this is the original behavior and other formats (raw, apt, yum,
   * huggingface) rely on it. This test locks that behavior in.
   */
  @Test
  public void testUriSegments_dropsTrailingSlash() {
    assertThat(underTest.uriSegments("simple/flask/"), is("simple/flask"));
    assertThat(underTest.uriSegments("a/b/c/"), is("a/b/c"));
  }

  /**
   * {@link EscapeHelper#uriSegmentsPreserveTrailing} is the PyPI-specific variant that preserves
   * trailing empty segments. Upstream PEP 503 simple indexes serve DIFFERENT link prefixes for
   * {@code /simple/pkg/} ({@code ../../packages/...}) vs {@code /simple/pkg} ({@code ../packages/...}),
   * so silently dropping the trailing slash on the outbound proxy request pulls back one-level-short
   * links that then resolve to a wrong upstream URL on the next fetch.
   */
  @Test
  public void testUriSegmentsPreserveTrailing_preservesTrailingSlash() {
    assertThat(underTest.uriSegmentsPreserveTrailing("simple/flask/"), is("simple/flask/"));
    assertThat(underTest.uriSegmentsPreserveTrailing("simple/flask"), is("simple/flask"));
    assertThat(underTest.uriSegmentsPreserveTrailing("a/b/c/"), is("a/b/c/"));
    // Trailing double slash preserved too (signed URLs sign exact bytes; NEXUS-52769).
    assertThat(underTest.uriSegmentsPreserveTrailing("simple/flask//"), is("simple/flask//"));
    // Empty leading segment (absolute path style) preserved.
    assertThat(underTest.uriSegmentsPreserveTrailing("/simple/flask/"), is("/simple/flask/"));
    // Same encoding of special chars as the base method.
    assertThat(underTest.uriSegmentsPreserveTrailing("foo/bar baz/"), is("foo/bar%20baz/"));
    assertThat(underTest.uriSegmentsPreserveTrailing("foo:path/bar:baz"), is("foo%3Apath/bar%3Abaz"));
  }

  @Test
  public void testCustomRules_emptyRules() {
    EscapeHelper customHelper = new EscapeHelper("");

    assertThat(customHelper.uriSegments("foo/bar+baz"), is("foo/bar+baz"));
    assertThat(customHelper.uriSegments("foo/bar%baz"), is("foo/bar%baz"));
    assertThat(customHelper.uriSegments("foo/bar baz"), is("foo/bar baz"));
    assertThat(customHelper.uriSegments("foo:path/bar:baz"), is("foo:path/bar:baz"));
  }

  @Test
  public void testCustomRules_onlyEscapePlus() {
    EscapeHelper customHelper = new EscapeHelper("+:%2B");

    assertThat(customHelper.uriSegments("foo/bar+baz"), is("foo/bar%2Bbaz"));
    assertThat(customHelper.uriSegments("foo/bar%baz"), is("foo/bar%baz"));
    assertThat(customHelper.uriSegments("foo/bar baz"), is("foo/bar baz"));
  }

  @Test
  public void testCustomRules_preserveAlreadyEscaped() {
    EscapeHelper customHelper = new EscapeHelper("");

    assertThat(customHelper.uriSegments("ncurses-c%2B%2B-libs"), is("ncurses-c%2B%2B-libs"));
  }

  @Test
  public void testCustomRules_nullRulesUsesDefaults() {
    EscapeHelper customHelper = new EscapeHelper((String) null);

    assertThat(customHelper.uriSegments("foo/bar+baz"), is("foo/bar+baz"));
    assertThat(customHelper.uriSegments("foo/bar%baz"), is("foo/bar%25baz"));
    assertThat(customHelper.uriSegments("foo/bar baz"), is("foo/bar%20baz"));
    assertThat(customHelper.uriSegments("foo:path/bar:baz"), is("foo%3Apath/bar%3Abaz"));
  }

  @Test
  public void testCustomRules_customOrder() {
    EscapeHelper customHelper = new EscapeHelper("a:b,b:c");

    assertThat(customHelper.uriSegments("a/b"), is("b/c"));
    assertThat(customHelper.uriSegments("abc"), is("bcc"));
  }

  @Test
  public void testCustomRules_specialCharacters() {
    EscapeHelper customHelper = new EscapeHelper("^:%5E,#:%23");

    assertThat(customHelper.uriSegments("test^file#name"), is("test%5Efile%23name"));
  }

  @Test
  public void testAlternationOrder_overlapping_longerFirstWins() {
    EscapeHelper h = new EscapeHelper("ab:X,a:Y");

    assertThat(h.uriSegments("ab"), is("X"));
    assertThat(h.uriSegments("ababa"), is("XXY"));
  }

  @Test
  public void testAlternationOrder_overlapping_shorterFirstWins() {
    EscapeHelper h = new EscapeHelper("a:Y,ab:X");

    assertThat(h.uriSegments("ab"), is("Yb"));
    assertThat(h.uriSegments("ababa"), is("YbYbY"));
  }

  @Test
  public void testAlternationOrder_insideSegments_only() {
    EscapeHelper h = new EscapeHelper("ab:X,a:Y");

    assertThat(h.uriSegments("ab/a/aba"), is("X/Y/XY"));
  }
}
