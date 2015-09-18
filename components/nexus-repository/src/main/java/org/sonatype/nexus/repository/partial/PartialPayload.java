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
package org.sonatype.nexus.repository.partial;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.view.Payload;

import com.google.common.collect.Range;

import static com.google.common.io.ByteStreams.limit;

/**
 * A wrapper {@link Payload} that returns only a portion of the original payload.
 */
class PartialPayload
    implements Payload
{
  private final Payload payload;

  private final Range<Long> rangeToSend;

  private final long partialSize;

  /**
   * The endpoints of the Range are interpreted as the first and last byte positions to send.
   */
  public PartialPayload(final Payload payload, final Range<Long> rangeToSend) {
    this.payload = payload;
    this.rangeToSend = rangeToSend;
    this.partialSize = 1 + rangeToSend.upperEndpoint() - rangeToSend.lowerEndpoint();
  }

  @Override
  public InputStream openInputStream() throws IOException {
    final InputStream payloadStream = payload.openInputStream();
    payloadStream.skip(rangeToSend.lowerEndpoint());
    return limit(payloadStream, partialSize);
  }

  @Override
  public long getSize() {
    return partialSize;
  }

  @Nullable
  @Override
  public String getContentType() {
    return payload.getContentType();
  }
}
