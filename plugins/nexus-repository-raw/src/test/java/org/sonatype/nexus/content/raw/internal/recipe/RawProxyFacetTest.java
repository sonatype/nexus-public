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
package org.sonatype.nexus.content.raw.internal.recipe;

import java.io.UnsupportedEncodingException;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RawProxyFacetTest {

  private RawProxyFacet rawProxyFacet;

  @Before
  public void setUp() {
    rawProxyFacet = new RawProxyFacet();
  }

  @Test
  public void testEncodeUrlWithCaret() throws UnsupportedEncodingException {
    String url = "http://example.com/path^test";
    String expectedEncodedUrl = "http://example.com/path%5Etest";
    assertEncodedUrl(url, expectedEncodedUrl, "^");
  }

  @Test
  public void testEncodeUrlWithHash() throws UnsupportedEncodingException {
    String url = "http://example.com/path#test";
    String expectedEncodedUrl = "http://example.com/path%23test";
    assertEncodedUrl(url, expectedEncodedUrl, "#");
  }

  @Test
  public void testEncodeUrlWithQuestionMark() throws UnsupportedEncodingException {
    String url = "http://example.com/path?test";
    String expectedEncodedUrl = "http://example.com/path%3Ftest";
    assertEncodedUrl(url, expectedEncodedUrl, "?");
  }

  @Test
  public void testEncodedUrlWithNarrowNoBreakSpace() throws UnsupportedEncodingException {
    String url = "http://example.com/path\u202Ftest";
    String expectedEncodedUrl = "http://example.com/path%E2%80%AFtest";
    assertEncodedUrl(url, expectedEncodedUrl, "\u202F");
  }

  @Test
  public void testEncodedUrlWithLeftSquareBracket() throws UnsupportedEncodingException {
    String url = "http://example.com/path[test";
    String expectedEncodedUrl = "http://example.com/path%5Btest";
    assertEncodedUrl(url, expectedEncodedUrl, "[");
  }

  @Test
  public void testEncodedUrlWithRightSquareBracket() throws UnsupportedEncodingException {
    String url = "http://example.com/path]test";
    String expectedEncodedUrl = "http://example.com/path%5Dtest";
    assertEncodedUrl(url, expectedEncodedUrl, "]");
  }

  private void assertEncodedUrl(String url, String expectedEncodedUrl, String character) throws UnsupportedEncodingException {
    String actualEncodedUrl = rawProxyFacet.encodeUrl(url);
    assertEquals("Failed to encode character: " + character, expectedEncodedUrl, actualEncodedUrl);
  }
}

