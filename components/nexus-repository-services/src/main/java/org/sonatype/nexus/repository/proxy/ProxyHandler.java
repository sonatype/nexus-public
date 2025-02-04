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
import java.io.UncheckedIOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.io.CooperationException;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.httpclient.RemoteBlockedIOException;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Headers;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.Status;

import static java.lang.Boolean.TRUE;
import static org.sonatype.nexus.repository.http.HttpMethods.GET;
import static org.sonatype.nexus.repository.http.HttpMethods.HEAD;
import static org.sonatype.nexus.repository.proxy.ProxyFacetSupport.PROXY_REMOTE_FETCH_SKIP_MARKER;
import static org.sonatype.nexus.repository.proxy.ThrottlerInterceptor.PAYMENT_REQUIRED_MESSAGE;

/**
 * A format-neutral proxy handler which delegates to an instance of {@link ProxyFacet} for content.
 *
 * @since 3.0
 */
public class ProxyHandler
    extends ComponentSupport
    implements Handler
{
  @Inject
  private NodeAccess nodeAccess;

  @Nonnull
  @Override
  public Response handle(@Nonnull final Context context) throws Exception { // NOSONAR
    final Response response = buildMethodNotAllowedResponse(context);
    if (response != null) {
      return response;
    }

    try {
      Payload payload = proxyFacet(context).get(context);
      if (payload != null) {
        return buildPayloadResponse(context, payload);
      }
      if (context.getAttributes() != null && context.getAttributes().contains(PROXY_REMOTE_FETCH_SKIP_MARKER) &&
          context.getAttributes().get(PROXY_REMOTE_FETCH_SKIP_MARKER).equals(TRUE)) {
        return buildPaymentRequiredResponse(context);
      }
      return buildNotFoundResponse(context);
    }
    catch (BypassHttpErrorException e) {
      return buildHttpErrorResponce(e);
    }
    catch (ProxyServiceException e) {
      return HttpResponses.serviceUnavailable();
    }
    catch (CooperationException e) { // NOSONAR
      return HttpResponses.serviceUnavailable(e.getMessage());
    }
    catch (RemoteBlockedIOException e) {
      return HttpResponses.notFound(e.getMessage());
    }
    catch (IOException | UncheckedIOException e) {
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

  protected Response buildPaymentRequiredResponse(Context context) {
    if (context.getRepository().getFormat().getValue().equals("nuget")) {
      return HttpResponses.conflict(PAYMENT_REQUIRED_MESSAGE.concat(nodeAccess.getId()));
    }
    else {
      return HttpResponses.forbidden(PAYMENT_REQUIRED_MESSAGE.concat(nodeAccess.getId()));
    }
  }

  protected Response buildHttpErrorResponce(final BypassHttpErrorException proxyErrorsException) {
    return new Response.Builder()
        .status(new Status(false, proxyErrorsException.getStatusCode(), proxyErrorsException.getReason()))
        .headers(new Headers(proxyErrorsException.getHeaders()))
        .build();
  }

  private ProxyFacet proxyFacet(final Context context) {
    return context.getRepository().facet(ProxyFacet.class);
  }
}
