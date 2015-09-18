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

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Adapts {@link HttpEntity} to {@link Payload}.
 *
 * @since 3.0
 */
public class HttpEntityPayload
    implements Payload
{
  private final HttpResponse response;

  private final HttpEntity entity;

  public HttpEntityPayload(final HttpResponse response, final HttpEntity entity) {
    this.response = checkNotNull(response);
    this.entity = checkNotNull(entity);
  }

  @Override
  public InputStream openInputStream() throws IOException {
    return entity.getContent();
  }

  @Override
  public long getSize() {
    return entity.getContentLength();
  }

  @Nullable
  @Override
  public String getContentType() {
    Header header = entity.getContentType();
    if (header != null) {
      return header.getValue();
    }
    return null;
  }
}