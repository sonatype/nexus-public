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
package org.sonatype.nexus.repository.view.handlers;

import javax.annotation.Nonnull;
import jakarta.inject.Singleton;

import org.sonatype.nexus.repository.ETagHeaderUtils;
import org.sonatype.nexus.repository.http.NxrmHttpHeaders;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Response;

import com.google.common.net.HttpHeaders;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.sonatype.nexus.repository.date.DateTimeUtils.formatDateTime;
import org.springframework.stereotype.Component;

/**
 * A format-neutral content handler for decorating response using {@link Content#getAttributes()} provided attributes.
 *
 * @since 3.0
 */
@Singleton
@Component
public class ContentHeadersHandler
    implements Handler
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  @Nonnull
  @Override
  public Response handle(@Nonnull final Context context) throws Exception {
    final Response response = context.proceed();
    Payload payload = response.getPayload();

    if (response.getStatus().isSuccessful() && payload instanceof Content content) {
      final DateTime lastModified = content.getAttributes().get(Content.CONTENT_LAST_MODIFIED, DateTime.class);
      if (lastModified != null) {
        response.getHeaders().replace(HttpHeaders.LAST_MODIFIED, formatDateTime(lastModified));
      }
      final String etag = content.getAttributes().get(Content.CONTENT_ETAG, String.class);
      if (etag != null) {
        response.getHeaders().replace(HttpHeaders.ETAG, ETagHeaderUtils.quote(etag));
      }
      final String PCCSHash = content.getAttributes().get(Content.CONTENT_PCCS_HASH, String.class);
      if (PCCSHash != null) {
        response.getHeaders().replace(NxrmHttpHeaders.PCCS_HASH, PCCSHash);
      }
    }
    return response;
  }
}
