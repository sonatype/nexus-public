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
package org.sonatype.nexus.repository.view;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.sonatype.nexus.common.collect.AttributesMap;

import com.google.common.annotations.VisibleForTesting;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Content, that is wrapped {@link Payload} with {@link AttributesMap}.
 *
 * @since 3.0
 */
public class Content
    implements Payload
{
  /**
   * Key of nested map of content related properties.
   */
  @VisibleForTesting
  public static final String CONTENT = "content";

  /**
   * Key of content "Last-Modified" attribute, with type {@link DateTime}.
   */
  public static final String CONTENT_LAST_MODIFIED = "last_modified";

  /**
   * Key of content "ETag" attribute, with type {@link String}.
   */
  public static final String CONTENT_ETAG = "etag";

  private final Payload payload;

  private final AttributesMap attributes;

  public Content(final Payload payload) {
    this.payload = checkNotNull(payload);
    this.attributes = new AttributesMap();
  }

  /**
   * @since 3.4
   */
  protected Content(final Payload payload, final AttributesMap attributes) {
    this.payload = checkNotNull(payload);
    this.attributes = checkNotNull(attributes);
  }

  @Override
  public InputStream openInputStream() throws IOException {
    return payload.openInputStream();
  }

  @Override
  public long getSize() {
    return payload.getSize();
  }

  @Nullable
  @Override
  public String getContentType() {
    return payload.getContentType();
  }

  @Override
  public void close() throws IOException {
    payload.close();
  }

  @Override
  public void copy(final InputStream inputStream, final OutputStream outputStream) throws IOException {
    payload.copy(inputStream, outputStream);
  }

  @Nonnull
  public AttributesMap getAttributes() {
    return attributes;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "payload=" + payload +
        ", attributes='" + attributes + '\'' +
        '}';
  }
}
