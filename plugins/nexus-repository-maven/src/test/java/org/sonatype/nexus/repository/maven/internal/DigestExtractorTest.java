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
package org.sonatype.nexus.repository.maven.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * UT for {@link DigestExtractor}
 *
 * @since 3.0
 */
public class DigestExtractorTest
    extends TestSupport
{
  private String[][] validDigests =
      {
          {"MD5 (pom.xml) = 68da13206e9dcce2db9ec45a9f7acd52", "68da13206e9dcce2db9ec45a9f7acd52"},
          {"68da13206e9dcce2db9ec45a9f7acd52 pom.xml", "68da13206e9dcce2db9ec45a9f7acd52"},
          {"68da13206e9dcce2db9ec45a9f7acd52        pom.xml", "68da13206e9dcce2db9ec45a9f7acd52"},
          {"93f402a80b5c40b7f32f68771ee57c27", "93f402a80b5c40b7f32f68771ee57c27"},
          {"bbb603f9f7a32a10eb539c1067992dabab58d33a", "bbb603f9f7a32a10eb539c1067992dabab58d33a"},
          {"ant-1.5.jar: 90 2A 36 0E CA D9 8A 34  B5 98 63 C1 E6 5B CF 71", "902a360ecad98a34b59863c1e65bcf71"},
          {
              "ant-1.5.jar: DCAB 88FC 2A04 3C24 79A6  DE67 6A2F 8179 E9EA 2167",
              "dcab88fc2a043c2479a6de676a2f8179e9ea2167"
          },
          {"90 2A 36 0E CA D9 8A 34  B5 98 63 C1 E6 5B CF 71", "902a360ecad98a34b59863c1e65bcf71"},
          {"DCAB 88FC 2A04 3C24 79A6  DE67 6A2F 8179 E9EA 2167", "dcab88fc2a043c2479a6de676a2f8179e9ea2167"},
          {"90 2A 36 0E CA D9 8A 34  B5 98 63 C1 E6 5B CF 71     pom.xml", "902a360ecad98a34b59863c1e65bcf71"},
          {
              "DCAB 88FC 2A04 3C24 79A6  DE67 6A2F 8179 E9EA 2167     pom.xml",
              "dcab88fc2a043c2479a6de676a2f8179e9ea2167"
          },
      };

  private InputStream stream(String string) throws IOException
  {
    return new ByteArrayInputStream(string.getBytes("UTF-8"));
  }

  @Test
  public void acceptedDigests() throws Exception
  {
    for (int i = 0; i < validDigests.length; i++) {
      String test = validDigests[i][0];
      String expected = validDigests[i][1];

      String digest = DigestExtractor.extract(stream(test));

      assertThat("DigestExtractor did not accept " + test, digest, notNullValue());
      assertThat("DigestExtractor did not accept " + test, digest, equalTo(expected));
    }
  }

  @Test
  public void rejectedDigests() {
    assertThat(DigestExtractor.extract("123456"), is(nullValue())); // too short
    assertThat(DigestExtractor.extract(""), is(nullValue())); // empty
    assertThat(DigestExtractor.extract("   "), is(nullValue())); // blank
    assertThat(DigestExtractor.extract("902a360Xcad98a34b59863c1e65bcf71"), is(nullValue())); // invalid, there is an non-hex X in there
  }
}
