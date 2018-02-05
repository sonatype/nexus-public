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
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.ViewFacet;

/**
 * Handler which will forward current request to {@code {request.path}/index.html} or {@code {request.path}/index.htm}.
 *
 * @since 3.0
 */
@Named
@Singleton
public class IndexHtmlForwardHandler
    extends ComponentSupport
    implements Handler
{
  private static final String[] INDEX_FILES = {
      "index.html",
      "index.htm"
  };

  @Nonnull
  @Override
  public Response handle(@Nonnull final Context context) throws Exception {
    String path = context.getRequest().getPath();

    // sanity, to ensure we don't corrupt the path
    if (!path.endsWith("/")) {
      path = path + "/";
    }

    for (String file : INDEX_FILES) {
      Response response = forward(context, path + file);
      // return response if it was successful or an error which was not not found
      if (response.getStatus().isSuccessful() || response.getStatus().getCode() != HttpStatus.NOT_FOUND) {
        return response;
      }
      // otherwise try next index file
    }

    // or there is no such file, give up not found
    return HttpResponses.notFound(context.getRequest().getPath());
  }

  private Response forward(final Context context, final String path) throws Exception {
    log.trace("Forwarding request to path: {}", path);

    Request request = new Request.Builder()
        .copy(context.getRequest())
        .path(path)
        .build();

    return context.getRepository()
        .facet(ViewFacet.class)
        .dispatch(request);
  }
}
