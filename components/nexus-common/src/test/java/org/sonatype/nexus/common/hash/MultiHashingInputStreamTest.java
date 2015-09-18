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
package org.sonatype.nexus.common.hash;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

import com.google.common.hash.HashCode;
import com.google.common.io.ByteStreams;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class MultiHashingInputStreamTest
{
  @Test
  public void sha512IsAccurate() throws IOException {
    byte[] bytes = new byte[100];

    final MultiHashingInputStream hashingStream = createAndUseHashingStream(bytes);

    final HashCode hashCode = hashingStream.hashes().get(HashAlgorithm.SHA512);

    assertThat(hashCode.toString(), is(equalTo(
        "f206f4f0ef09b90837f1d15a07c6cf4bd291d817663f9f85a0fc4341ec19910719ad571b6102a366ae848cd0f187d0daef912e05898b82c35213cd49a45ee8e0")));
  }

  @Test
  public void testCountIsAccurate() throws IOException {
    final long byteArrayLength = 100;

    final MultiHashingInputStream andUseHashingStream = createAndUseHashingStream(new byte[(int) byteArrayLength]);
    assertThat(andUseHashingStream.count(), is(equalTo(byteArrayLength)));
  }

  private MultiHashingInputStream createAndUseHashingStream(final byte[] bytes) throws IOException {
    final MultiHashingInputStream hashingStream = new MultiHashingInputStream(
        Arrays.asList(HashAlgorithm.SHA512), new ByteArrayInputStream(bytes));

    ByteStreams.copy(hashingStream, ByteStreams.nullOutputStream());
    return hashingStream;
  }
}