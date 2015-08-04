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
package org.sonatype.nexus.util;

import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests for {@link Tokens}.
 *
 * @since 2.7
 */
@SuppressWarnings("HardCodedStringLiteral")
public class TokensTest
    extends TestSupport
{
  @Test
  public void encodeLikeNodeId() {
    String input = "05F4743FA756584643FDF9D0577BE4FB079289C6";
    String output = Tokens.encode(input, '-', 8);
    assertThat(output, equalTo("05F4743F-A7565846-43FDF9D0-577BE4FB-079289C6"));
  }

  @Test
  public void encodeLikeFingerprint() {
    String input = "05F4743FA756584643FDF9D0577BE4FB079289C6";
    String output = Tokens.encode(input, ':', 2);
    assertThat(output, equalTo("05:F4:74:3F:A7:56:58:46:43:FD:F9:D0:57:7B:E4:FB:07:92:89:C6"));
  }

  @Test
  public void maskNull() throws Exception {
    assertThat(Tokens.mask(null), nullValue());
  }

  @Test
  public void maskWtihValue() throws Exception {
    // any non-null returns MASK
    assertThat(Tokens.mask("a"), is(Tokens.MASK));
    assertThat(Tokens.mask("ab"), is(Tokens.MASK));
    assertThat(Tokens.mask("abc"), is(Tokens.MASK));
    assertThat(Tokens.mask("abcd"), is(Tokens.MASK));
    assertThat(Tokens.mask("abcde"), is(Tokens.MASK));
  }

  @Test
  public void isEmptyWithNull() throws Exception {
    assertThat(Tokens.isEmpty(null), is(true));
  }

  @Test
  public void isEmptyWithEmptyString() throws Exception {
    assertThat(Tokens.isEmpty(""), is(true));
  }

  @Test
  public void isEmptyWithBlankString() throws Exception {
    assertThat(Tokens.isEmpty("   "), is(true));
  }

  @Test
  public void isEmptyFalse() throws Exception {
    assertThat(Tokens.isEmpty("this is not empty"), is(false));
  }
}
