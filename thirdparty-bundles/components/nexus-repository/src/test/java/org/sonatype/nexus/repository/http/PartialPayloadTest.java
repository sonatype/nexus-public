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
package org.sonatype.nexus.repository.http;

import java.io.IOException;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;

import com.google.common.collect.Range;
import com.google.common.io.ByteStreams;
import com.google.common.primitives.Bytes;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests {@link PartialPayload}.
 */
public class PartialPayloadTest
    extends TestSupport
{
  private final byte[] input = Bytes.toArray(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));

  private final BytesPayload bytesPayload = new BytesPayload(input, "n/a");

  @Test
  public void oneBytePartial() throws IOException {
    final byte[] output = partial(bytesPayload, Range.closed(0L, 0L));

    assertThat(output, is(Bytes.toArray(asList(0))));
  }

  @Test
  public void threeBytePartial() throws IOException {
    final byte[] output = partial(bytesPayload, Range.closed(0L, 2L));

    assertThat(output, is(Bytes.toArray(asList(0, 1, 2))));
  }

  @Test
  public void entireStream() throws IOException {
    final byte[] output = partial(bytesPayload, Range.closed(0L, 9L));

    assertThat(output, is(input));
  }

  private byte[] partial(final BytesPayload bytes, final Range<Long> closed) throws IOException {
    final PartialPayload partial = new PartialPayload(bytes, closed);
    return ByteStreams.toByteArray(partial.openInputStream());
  }
}