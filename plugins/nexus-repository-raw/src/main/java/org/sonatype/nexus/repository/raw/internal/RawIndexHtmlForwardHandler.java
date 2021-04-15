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
package org.sonatype.nexus.repository.raw.internal;

import javax.annotation.Nonnull;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.handlers.IndexHtmlForwardHandler;

/**
 * Handler which will forward current request to {@code {request.path}/index.html} or {@code {request.path}/index.htm}.
 * If there are no index.html and index.htm files then just proceed to avoid 404 error
 *
 * @since 3.29
 */
@Named
@Singleton
public class RawIndexHtmlForwardHandler
    extends IndexHtmlForwardHandler
{
  @Nonnull
  @Override
  public Response handle(@Nonnull final Context context) throws Exception {
    Response response = forward(context, context.getRequest().getPath() + ".");
    if (HttpStatus.NOT_FOUND == response.getStatus().getCode()) {
      response = super.handle(context);
      if (HttpStatus.NOT_FOUND == response.getStatus().getCode()) {
        return HttpResponses.notFound("You can’t browse this way");
      }
    }

    return response;
  }
}
