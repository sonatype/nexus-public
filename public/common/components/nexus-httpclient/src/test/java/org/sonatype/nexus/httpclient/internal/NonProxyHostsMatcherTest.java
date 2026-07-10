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
package org.sonatype.nexus.httpclient.internal;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

class NonProxyHostsMatcherTest
{
  @Test
  void compile_returnsNullForNullInput() {
    assertThat(NonProxyHostsMatcher.compile(null), is(nullValue()));
  }

  @Test
  void compile_returnsNullForEmptyArray() {
    assertThat(NonProxyHostsMatcher.compile(new String[]{}), is(nullValue()));
  }

  @Test
  void compile_returnsNullWhenAllPatternsBlank() {
    assertThat(NonProxyHostsMatcher.compile(new String[]{"", "  ", null}), is(nullValue()));
  }

  @Test
  void compile_returnsCompiledPatternForValidGlobs() {
    Pattern pattern = NonProxyHostsMatcher.compile(new String[]{"*.internal"});
    assertThat(pattern, is(notNullValue()));
    assertThat(pattern.matcher("host.internal").matches(), is(true));
  }

  @Test
  void matches_exactHostname() {
    assertThat(NonProxyHostsMatcher.matches("localhost", new String[]{"localhost"}), is(true));
    assertThat(NonProxyHostsMatcher.matches("notlocalhost", new String[]{"localhost"}), is(false));
  }

  @Test
  void matches_leadingWildcard() {
    String[] globs = {"*.internal"};
    assertThat(NonProxyHostsMatcher.matches("host.internal", globs), is(true));
    assertThat(NonProxyHostsMatcher.matches("a.b.internal", globs), is(true));
    assertThat(NonProxyHostsMatcher.matches("internal", globs), is(false));
    assertThat(NonProxyHostsMatcher.matches("host.external", globs), is(false));
  }

  @Test
  void matches_trailingWildcard() {
    String[] globs = {"10.*"};
    assertThat(NonProxyHostsMatcher.matches("10.0.0.5", globs), is(true));
    assertThat(NonProxyHostsMatcher.matches("10.", globs), is(true));
    assertThat(NonProxyHostsMatcher.matches("100.0.0.5", globs), is(false));
  }

  @Test
  void matches_isCaseInsensitive() {
    String[] globs = {"*.Internal"};
    assertThat(NonProxyHostsMatcher.matches("HOST.internal", globs), is(true));
    assertThat(NonProxyHostsMatcher.matches("host.INTERNAL", globs), is(true));
  }

  @Test
  void matches_anyOfMultiplePatterns() {
    String[] globs = {"*.internal", "localhost", "10.*"};
    assertThat(NonProxyHostsMatcher.matches("host.internal", globs), is(true));
    assertThat(NonProxyHostsMatcher.matches("localhost", globs), is(true));
    assertThat(NonProxyHostsMatcher.matches("10.0.0.5", globs), is(true));
    assertThat(NonProxyHostsMatcher.matches("repo.maven.apache.org", globs), is(false));
  }

  @Test
  void matches_returnsFalseForNullOrEmptyGlobs() {
    assertThat(NonProxyHostsMatcher.matches("host", null), is(false));
    assertThat(NonProxyHostsMatcher.matches("host", new String[]{}), is(false));
  }

  @Test
  void matches_returnsFalseForNullOrBlankHost() {
    String[] globs = {"*.internal"};
    assertThat(NonProxyHostsMatcher.matches(null, globs), is(false));
    assertThat(NonProxyHostsMatcher.matches("", globs), is(false));
    assertThat(NonProxyHostsMatcher.matches("   ", globs), is(false));
  }

  @Test
  void compile_skipsBlankPatternsButCompilesRest() {
    Pattern pattern = NonProxyHostsMatcher.compile(new String[]{"", "*.internal", null, "  "});
    assertThat(pattern, is(notNullValue()));
    assertThat(pattern.matcher("host.internal").matches(), is(true));
  }

  @Test
  void compile_handlesIpv6BracketsLiterally() {
    Pattern pattern = NonProxyHostsMatcher.compile(new String[]{"[::1]"});
    assertThat(pattern, is(notNullValue()));
    assertThat(pattern.matcher("[::1]").matches(), is(true));
  }
}
