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
    when(escapeHelper.uriSegments("test+file.txt")).thenReturn("test+file.txt");
    when(escapeHelper.uriSegments("c++libs")).thenReturn("c++libs");
    when(escapeHelper.uriSegments("file+name#test?.txt")).thenReturn("file+name#test?.txt");
    when(escapeHelper.uriSegments("packages/ncurses-c++libs-6.2-4.rpm"))
        .thenReturn("packages/ncurses-c++libs-6.2-4.rpm");
  }

  @Test
  public void testEncodeUrlSegments_EncodesPlusSign() {
    EncodingHelper helper = new EncodingHelper(escapeHelper);

    assertThat(helper.encodeUrlSegments("test+file.txt"), is("test%2Bfile.txt"));
    verify(escapeHelper).uriSegments("test+file.txt");
  }

  @Test
  public void testEncodeUrlSegments_EncodesMultiplePlus() {
    EncodingHelper helper = new EncodingHelper(escapeHelper);

    assertThat(helper.encodeUrlSegments("c++libs"), is("c%2B%2Blibs"));
    verify(escapeHelper).uriSegments("c++libs");
  }

  @Test
  public void testEncodeUrlSegments_EncodesHashAndQuestion() {
    EncodingHelper helper = new EncodingHelper(escapeHelper);

    assertThat(helper.encodeUrlSegments("file+name#test?.txt"), is("file%2Bname%23test%3F.txt"));
    verify(escapeHelper).uriSegments("file+name#test?.txt");
  }

  @Test(expected = NullPointerException.class)
  public void testConstructor_NullEscapeHelper_ThrowsException() {
    new EncodingHelper(null);
  }

  @Test(expected = NullPointerException.class)
  public void testEncodeUrlSegments_NullUrl_ThrowsException() {
    new EncodingHelper(escapeHelper).encodeUrlSegments(null);
  }

  @Test
  public void testRealWorldAwsS3Example() {
    EncodingHelper helper = new EncodingHelper(escapeHelper);

    assertThat(helper.encodeUrlSegments("packages/ncurses-c++libs-6.2-4.rpm"),
        is("packages/ncurses-c%2B%2Blibs-6.2-4.rpm"));
  }
}
