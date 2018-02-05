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
package org.sonatype.nexus.orient.internal;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.crypto.internal.CryptoHelperImpl;
import org.sonatype.nexus.crypto.internal.PbeCipherFactoryImpl;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Tests for {@link PbeCompression}.
 */
public class PbeCompressionTest
  extends TestSupport
{
  private PbeCompression underTest;

  @Before
  public void setUp() throws Exception {
    this.underTest = new PbeCompression(
        new PbeCipherFactoryImpl(new CryptoHelperImpl()),
        "password",
        "salt",
        "0123456789ABCDEF"
    );
  }

  @Test
  public void encodeAndDecodeDivisibleByEight() {
    encodeAndDecodeBytes(8);
    encodeAndDecodeBytes(80);
  }

  @Test
  public void encodeAndDecodeNotDivisibleByEight() {
    encodeAndDecodeBytes(1);
    encodeAndDecodeBytes(10);
  }

  private void encodeAndDecodeBytes(int numBytes) {
    // build test byte array with values 0 to numBytes - 1
    byte[] testBytes = new byte[numBytes];
    for (byte i = 0x00; i < (byte)numBytes; i++) {
      testBytes[i] = i;
    }

    // encrypting it should return a different array
    byte[] encryptedBytes = underTest.compress(testBytes);
    assertThat(encryptedBytes.length > 0, is(true));
    assertThat(encryptedBytes, not(testBytes));

    // decrypting the encrypted array should return the original array
    byte[] decryptedBytes = underTest.uncompress(encryptedBytes);
    assertThat(decryptedBytes, is(testBytes));
  }
}
