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
package org.sonatype.nexus.blobstore;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Test;

import static com.google.common.io.ByteStreams.copy;
import static com.google.common.io.ByteStreams.nullOutputStream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Tests for {@link MetricsInputStream}.
 */
public class MetricsInputStreamTest
    extends TestSupport
{
  @Test
  public void testLength() throws Exception {
    assertThat(measure("ABC".getBytes("UTF-8")).getSize(), is(equalTo(3L)));
    assertThat(measure(new byte[10000]).getSize(), is(equalTo(10000L)));
  }

  @Test
  public void testHashesDiffer() throws Exception {
    final String hash1 = measure("ABC".getBytes("UTF-8")).getMessageDigest();
    final String hash2 = measure(new byte[10000]).getMessageDigest();

    assertThat(hash1, not(equalTo(hash2)));
  }

  @Test
  public void referenceHashMatches() throws Exception {
    final MetricsInputStream measure = measure(
        getClass().getResourceAsStream("sha1_is_2589766c6dac3402cab552602d457e7e8af12efd.bytes"));
    assertThat(measure.getMessageDigest(), is(equalTo("2589766c6dac3402cab552602d457e7e8af12efd")));
  }

  private MetricsInputStream measure(final byte[] testData) throws Exception {
    return measure(new ByteArrayInputStream(testData));
  }

  private MetricsInputStream measure(final InputStream inputStream) throws Exception {
    final MetricsInputStream metricStream = new MetricsInputStream(inputStream);
    copy(metricStream, nullOutputStream());
    return metricStream;
  }
}
