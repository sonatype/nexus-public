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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.view.Payload;

import com.google.common.base.Charsets;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * String payload.
 *
 * @since 3.0
 */
public class StringPayload
    implements Payload
{
  private final byte[] contentBytes;

  private final Charset charset;

  private final String contentType;

  public StringPayload(final String content, final Charset charset, @Nullable final String contentType) {
    this.contentBytes = checkNotNull(content).getBytes(charset);
    this.charset = checkNotNull(charset);
    this.contentType = contentType;
  }

  public StringPayload(final String content, @Nullable final String contentType) {
    this(content, Charsets.UTF_8, contentType);
  }

  public Charset getCharset() {
    return charset;
  }

  @Override
  public InputStream openInputStream() throws IOException {
    return new ByteArrayInputStream(contentBytes);
  }

  @Override
  public long getSize() {
    return contentBytes.length;
  }

  @Nullable
  @Override
  public String getContentType() {
    return contentType;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "size=" + contentBytes.length +
        ", charset=" + charset +
        ", contentType='" + contentType + '\'' +
        '}';
  }
}
