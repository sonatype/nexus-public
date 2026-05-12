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
package org.sonatype.nexus.repository.proxy;

import org.sonatype.nexus.common.template.EscapeHelper;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests for {@link EncodingHelper}
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class EncodingHelperTest
{
  @Mock
  private EscapeHelper escapeHelper;

  @Before
  public void setUp() {
    // Set up EscapeHelper to return a predictable encoded string
    // The mock simulates how EscapeHelper behaves: it encodes spaces but not + or other special chars
    when(escapeHelper.uriSegments("test+file.txt")).thenReturn("test+file.txt");
    when(escapeHelper.uriSegments("file with spaces.txt")).thenReturn("file%20with%20spaces.txt");
    when(escapeHelper.uriSegments("special#chars?.txt")).thenReturn("special#chars?.txt");
    when(escapeHelper.uriSegments("c++libs")).thenReturn("c++libs");
    when(escapeHelper.uriSegments("libgit2-sys-0.13.1+1.4.2.crate")).thenReturn("libgit2-sys-0.13.1+1.4.2.crate");
    when(escapeHelper.uriSegments("file+name#test?.txt")).thenReturn("file+name#test?.txt");
    when(escapeHelper.uriSegments("packages/ncurses-c++libs-6.2-4.rpm"))
        .thenReturn("packages/ncurses-c++libs-6.2-4.rpm");
    when(escapeHelper.uriSegments("crates/libgit2-sys/libgit2-sys-0.13.1+1.4.2.crate"))
        .thenReturn("crates/libgit2-sys/libgit2-sys-0.13.1+1.4.2.crate");
  }

  @Test
  public void testPreserveEncodedCharactersFalse_AppliesEscapeHelperRules() {
    EncodingHelper helper = new EncodingHelper(escapeHelper, false);

    String result = helper.encodeUrlSegments("test+file.txt");

    // When false, uses EscapeHelper, which doesn't encode + (backward compatible)
    assertThat(result, is("test+file.txt"));
    verify(escapeHelper).uriSegments("test+file.txt");
  }

  @Test
  public void testPreserveEncodedCharactersTrue_EncodesPlusSign() {
    EncodingHelper helper = new EncodingHelper(escapeHelper, true);

    // Should encode + to %2B
    String result = helper.encodeUrlSegments("test+file.txt");

    assertThat(result, is("test%2Bfile.txt"));
    verify(escapeHelper).uriSegments("test+file.txt");
  }

  @Test
  public void testPreserveEncodedCharactersTrue_EncodesMultiplePlus() {
    EncodingHelper helper = new EncodingHelper(escapeHelper, true);

    // Should encode all + to %2B
    String result = helper.encodeUrlSegments("c++libs");

    assertThat(result, is("c%2B%2Blibs"));
    verify(escapeHelper).uriSegments("c++libs");
  }

  @Test
  public void testPreserveEncodedCharactersFalse_KeepsLiteralPlus() {
    EncodingHelper helper = new EncodingHelper(escapeHelper, false);

    // When false, keeps + as + (backward compatible, works for crates.io)
    String result = helper.encodeUrlSegments("libgit2-sys-0.13.1+1.4.2.crate");

    assertThat(result, is("libgit2-sys-0.13.1+1.4.2.crate"));
    verify(escapeHelper).uriSegments("libgit2-sys-0.13.1+1.4.2.crate");
  }

  @Test
  public void testPreserveEncodedCharactersFalse_HandlesSpaces() {
    EncodingHelper helper = new EncodingHelper(escapeHelper, false);

    String result = helper.encodeUrlSegments("file with spaces.txt");

    assertThat(result, is("file%20with%20spaces.txt"));
    verify(escapeHelper).uriSegments("file with spaces.txt");
  }

  @Test
  public void testPreserveEncodedCharactersFalse_HandlesMultipleSpecialChars() {
    EncodingHelper helper = new EncodingHelper(escapeHelper, false);

    String result = helper.encodeUrlSegments("special#chars?.txt");

    assertThat(result, is("special#chars?.txt"));
    verify(escapeHelper).uriSegments("special#chars?.txt");
  }

  @Test
  public void testPreserveEncodedCharactersTrue_EncodesHashAndQuestion() {
    EncodingHelper helper = new EncodingHelper(escapeHelper, true);

    // Should encode #, ?, and +
    String result = helper.encodeUrlSegments("file+name#test?.txt");

    assertThat(result, is("file%2Bname%23test%3F.txt"));
    verify(escapeHelper).uriSegments("file+name#test?.txt");
  }

  @Test
  public void testShouldPreserveEncodedCharacters_ReturnsCorrectValue() {
    EncodingHelper falseHelper = new EncodingHelper(escapeHelper, false);
    EncodingHelper trueHelper = new EncodingHelper(escapeHelper, true);

    assertThat(falseHelper.shouldPreserveEncodedCharacters(), is(false));
    assertThat(trueHelper.shouldPreserveEncodedCharacters(), is(true));
  }

  @Test(expected = NullPointerException.class)
  public void testConstructor_NullEscapeHelper_ThrowsException() {
    new EncodingHelper(null, false);
  }

  @Test(expected = NullPointerException.class)
  public void testEncodeUrlSegments_NullUrl_ThrowsException() {
    EncodingHelper helper = new EncodingHelper(escapeHelper, false);
    helper.encodeUrlSegments(null);
  }

  @Test
  public void testRealWorldCratesIoExample() {
    EncodingHelper helper = new EncodingHelper(escapeHelper, false);

    // Real-world crates.io example with version containing +
    String result = helper.encodeUrlSegments("crates/libgit2-sys/libgit2-sys-0.13.1+1.4.2.crate");

    // When false, keeps + as + for crates.io compatibility (backward compatible)
    assertThat(result, is("crates/libgit2-sys/libgit2-sys-0.13.1+1.4.2.crate"));
  }

  @Test
  public void testRealWorldAwsS3Example() {
    EncodingHelper helper = new EncodingHelper(escapeHelper, true);

    // Real-world AWS S3 example with literal + (C++ package)
    String result = helper.encodeUrlSegments("packages/ncurses-c++libs-6.2-4.rpm");

    // Should encode + to %2B for AWS S3 compatibility
    assertThat(result, is("packages/ncurses-c%2B%2Blibs-6.2-4.rpm"));
  }
}
