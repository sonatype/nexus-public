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
import java.util.List;
import java.util.Map;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA512;

/**
 * Tests for {@link Hashes}.
 */
public class HashesTest
{
  private static final String DATA = "This is a test message for hashing!";

  private static final String MD5_HASH = "b4b91fa27dd64d4f14cd1e22e6a3c714";
  private static final String SHA1_HASH = "410fee1895a6af9449ae1647276259fd69a75b15";
  private static final String SHA512_HASH = "b90de0708205534bf3bc4e478c3718c7bf78b5ec60902dbbea234aadd748c004cdf94deda2034b0fa8bdc559ac59d6ac622211956bf782da33444d29e8d9f160";

  @Test
  public void hashOne() throws Exception {
    HashCode hashCode = Hashes.hash(MD5, inputStream());

    assertThat(hashCode.toString(), is(MD5_HASH));
  }

  @Test
  public void hashThree() throws Exception {
    Map<HashAlgorithm, HashCode> hashes = Hashes.hash(ImmutableList.of(MD5, SHA1, SHA512), inputStream());

    assertThat(hashes.size(), is(3));
    assertThat(hashes.get(MD5).toString(), is(MD5_HASH));
    assertThat(hashes.get(SHA1).toString(), is(SHA1_HASH));
    assertThat(hashes.get(SHA512).toString(), is(SHA512_HASH));
  }

  @Test
  public void hashZero() throws Exception {
    List<HashAlgorithm> zeroAlgorithms = ImmutableList.of();
    Map<HashAlgorithm, HashCode> hashes = Hashes.hash(zeroAlgorithms, inputStream());

    assertThat(hashes.size(), is(0));
  }

  private static InputStream inputStream() {
    return new ByteArrayInputStream(DATA.getBytes(Charsets.UTF_8));
  }

  @Test
  public void hashStreamWithFunction() throws Exception {
    byte[] bytes = DATA.getBytes(Charsets.UTF_8);
    String expected = Hashing.sha1().hashBytes(bytes).toString();
    HashCode found = Hashes.hash(Hashing.sha1(), new ByteArrayInputStream(bytes));
    assertThat(found.toString(), is(expected));
  }
}
