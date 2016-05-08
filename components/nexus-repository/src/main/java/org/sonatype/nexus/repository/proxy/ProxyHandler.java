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
package org.sonatype.nexus.repository.proxy;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Response;

import static org.sonatype.nexus.repository.http.HttpMethods.GET;
import static org.sonatype.nexus.repository.http.HttpMethods.HEAD;

/**
 * A format-neutral proxy handler which delegates to an instance of {@link ProxyFacet} for content.
 *
 * @since 3.0
 */
public class ProxyHandler
    extends ComponentSupport
    implements Handler
{
  @Nonnull
  @Override
  public Response handle(@Nonnull final Context context) throws Exception {

    final Response response = buildMethodNotAllowedResponse(context);
    if (response != null) {
      return response;
    }

    try {
      Payload payload = proxyFacet(context).get(context);
      if (payload != null) {
        return buildPayloadResponse(context, payload);
      }
      return buildNotFoundResponse(context);
    }
    catch (ProxyServiceException e) {
      return HttpResponses.serviceUnavailable();
    }
    catch (IOException e) {
      return HttpResponses.badGateway();
    }
  }

  /**
   * Builds a not-allowed response if the specified method is unsupported under the specified context, null otherwise.
   */
  @Nullable
  protected Response buildMethodNotAllowedResponse(final Context context) {
    final String action = context.getRequest().getAction();
    if (!GET.equals(action) && !HEAD.equals(action)) {
      return HttpResponses.methodNotAllowed(action, GET, HEAD);
    }
    return null;
  }

  protected Response buildPayloadResponse(final Context context, final Payload payload) {
    return HttpResponses.ok(payload);
  }
  
  protected Response buildNotFoundResponse(final Context context) {
    return HttpResponses.notFound();
  }

  private ProxyFacet proxyFacet(final Context context) {
    return context.getRepository().facet(ProxyFacet.class);
  }
}
