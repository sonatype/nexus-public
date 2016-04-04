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
package org.sonatype.nexus.repository.cache;

import javax.annotation.Nonnull;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.http.HttpMethods;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.Status;

/**
 * Handler that caches 404 responses.
 *
 * When context invocation returns 404, it caches the 404 status to avoid future invocations (if cached status is
 * present).
 *
 * @since 3.0
 */
public class NegativeCacheHandler
    extends ComponentSupport
    implements Handler
{
  @Nonnull
  @Override
  public Response handle(@Nonnull final Context context) throws Exception {
    String action = context.getRequest().getAction();
    if (!HttpMethods.GET.equals(action)) {
      return context.proceed();
    }
    NegativeCacheFacet negativeCache = context.getRepository().facet(NegativeCacheFacet.class);
    NegativeCacheKey key = negativeCache.getCacheKey(context);

    Response response;
    Status status = negativeCache.get(key);
    if (status == null) {
      response = context.proceed();
      if (isNotFound(response)) {
        negativeCache.put(key, response.getStatus());
      }
      else if (response.getStatus().isSuccessful()) {
        negativeCache.invalidate(key);
      }
    }
    else {
      response = buildResponse(status, context);

      log.debug("Found {} in negative cache, returning {}", key, response);
    }
    return response;
  }
  
  protected Response buildResponse(final Status status, final Context context) {
    return new Response.Builder()
        .status(status)
        .build();  
  }
  
  private boolean isNotFound(final Response response) {
    return HttpStatus.NOT_FOUND == response.getStatus().getCode();
  }

}
