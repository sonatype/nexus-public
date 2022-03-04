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
package org.sonatype.nexus.repository.view.payloads;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.view.Payload;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 3.next
 */
public class TempBlobPayload implements Payload
{
  private final TempBlob tempBlob;

  private String contentType;

  public TempBlobPayload(final TempBlob tempBlob, @Nullable final String contentType) {
    this.tempBlob = checkNotNull(tempBlob);
    this.contentType = contentType;
  }

  @Override
  public InputStream openInputStream() throws IOException {
    return tempBlob.get();
  }

  @Override
  public long getSize() {
    return tempBlob.getBlob().getMetrics().getContentSize();
  }

  @Nullable
  @Override
  public String getContentType() {
    return contentType;
  }

  @SuppressWarnings("unchecked")
  public <T extends TempBlob> T getTempBlob() {
    return (T) tempBlob;
  }

  @Override
  public void close() throws IOException {
    tempBlob.close();
  }
}
