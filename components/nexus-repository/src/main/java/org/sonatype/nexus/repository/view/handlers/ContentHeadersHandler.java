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
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Response;

import com.google.common.net.HttpHeaders;
import org.joda.time.DateTime;

import static org.sonatype.nexus.repository.date.DateTimeUtils.formatDateTime;

/**
 * A format-neutral content handler for decorating response using {@link Content#getAttributes()} provided attributes.
 *
 * @since 3.0
 */
@Singleton
@Named
public class ContentHeadersHandler
    extends ComponentSupport
    implements Handler
{
  @Nonnull
  @Override
  public Response handle(@Nonnull final Context context) throws Exception {
    final Response response = context.proceed();
    Payload payload = response.getPayload();

    if (response.getStatus().isSuccessful() && payload instanceof Content) {
      final Content content = (Content) payload;
      final DateTime lastModified = content.getAttributes().get(Content.CONTENT_LAST_MODIFIED, DateTime.class);
      if (lastModified != null) {
        response.getHeaders().set(HttpHeaders.LAST_MODIFIED, formatDateTime(lastModified));
      }
      final String etag = content.getAttributes().get(Content.CONTENT_ETAG, String.class);
      if (etag != null) {
        response.getHeaders().set(HttpHeaders.ETAG, "\"" + etag + "\"");
      }
    }
    return response;
  }
}
