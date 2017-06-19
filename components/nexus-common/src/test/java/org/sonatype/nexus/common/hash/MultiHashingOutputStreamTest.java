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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;

import com.google.common.collect.ImmutableList;
import com.google.common.hash.HashCode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA256;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA512;

public class MultiHashingOutputStreamTest
    extends TestSupport
{
  List<HashAlgorithm> hashes;

  @Mock
  OutputStream outputStream;

  MultiHashingOutputStream underTest;

  @Before
  public void setup() throws Exception {
    hashes = ImmutableList.of(SHA256, SHA1, SHA512, MD5);
    underTest = new MultiHashingOutputStream(hashes, outputStream);
  }

  @Test(expected = NullPointerException.class)
  public void throwNpeWhenPassedNullHashes() throws Exception {
    underTest = new MultiHashingOutputStream(null, outputStream);
  }

  @Test(expected = NullPointerException.class)
  public void throwNpeWhenPassedNullOutputStream() throws Exception {
    underTest = new MultiHashingOutputStream(hashes, null);
  }

  @Test
  public void writeIntegerToOutputStream() throws Exception {
    underTest.write(1);
    verify(outputStream).write(1);
  }

  @Test
  public void writeArrayToOutputStream() throws Exception {
    underTest.write(new byte[50]);
    verify(outputStream, times(50)).write(0);
  }

  @Test
  public void writeOffsetToOutputStream() throws Exception {
    underTest.write(new byte[50], 10, 30);
    verify(outputStream, times(30)).write(0);
  }

  @Test
  public void writeArrayToHashes() throws Exception {
    underTest.write(new byte[100]);

    Map<HashAlgorithm, HashCode> hashes = underTest.hashes();

    HashCode sha1 = hashes.get(SHA1);
    HashCode sha256 = hashes.get(SHA256);
    assertThat(sha1.toString(), is(equalTo("ed4a77d1b56a118938788fc53037759b6c501e3d")));
    assertThat(sha256.toString(), is(equalTo("cd00e292c5970d3c5e2f0ffa5171e555bc46bfc4faddfb4a418b6840b86e79a3")));
  }

  @Test
  public void writeIntegerToHashes() throws Exception {
    InputStream inputStream = new ByteArrayInputStream("test".getBytes());
    int b;
    while ((b = inputStream.read()) != -1) {
      underTest.write(b);
    }

    Map<HashAlgorithm, HashCode> hashes = underTest.hashes();

    HashCode sha1 = hashes.get(SHA1);
    HashCode sha256 = hashes.get(SHA256);
    assertThat(sha1.toString(), is(equalTo("a94a8fe5ccb19ba61c4c0873d391e987982fbbd3")));
    assertThat(sha256.toString(), is(equalTo("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08")));
  }

  @Test
  public void writeArrayWithOffsetToHashes() throws Exception {
    underTest.write(new byte[50], 10, 30);

    Map<HashAlgorithm, HashCode> hashes = underTest.hashes();

    HashCode sha1 = hashes.get(SHA1);
    HashCode sha256 = hashes.get(SHA256);
    assertThat(sha1.toString(), is(equalTo("deb6c11e1971aa61dbbcbc76e5ea7553a5bea7b7")));
    assertThat(sha256.toString(), is(equalTo("0679246d6c4216de0daa08e5523fb2674db2b6599c3b72ff946b488a15290b62")));
  }

  @Test(expected = IllegalStateException.class)
  public void exceptionOnMultipleHashesCalls() throws Exception {
    underTest.write(new byte[50], 10, 30);

    underTest.hashes();
    underTest.hashes();
  }
}
