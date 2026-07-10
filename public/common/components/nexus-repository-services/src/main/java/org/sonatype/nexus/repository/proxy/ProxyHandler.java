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

import org.springframework.beans.factory.annotation.Autowired;

import org.sonatype.nexus.blobstore.api.BlobStoreWarmingUpException;
import org.sonatype.nexus.common.io.CooperationException;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.stateguard.InvalidStateException;
import org.sonatype.nexus.repository.MissingBlobException;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.httpclient.RemoteBlockedIOException;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Headers;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.Status;
import org.sonatype.nexus.repository.view.payloads.StringPayload;

import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Boolean.TRUE;
import static org.sonatype.nexus.repository.http.HttpMethods.GET;
import static org.sonatype.nexus.repository.http.HttpMethods.HEAD;
import static org.sonatype.nexus.repository.proxy.ProxyFacetSupport.PROXY_REMOTE_FETCH_SKIP_MARKER;
import static org.sonatype.nexus.repository.proxy.ProxyFacetSupport.PROXY_TELEMETRY_BLOCKING_MARKER;
import static org.sonatype.nexus.repository.proxy.ThrottlerInterceptor.PAYMENT_REQUIRED_MESSAGE;

/**
 * A format-neutral proxy handler which delegates to an instance of {@link ProxyFacet} for content.
 *
 * @since 3.0
 */
@Primary
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ProxyHandler
    implements Handler
{
  private static final String TELEMETRY_BLOCKING_MESSAGE =
      "TELEMETRY REQUIRED: This instance has failed to submit required telemetry data. " +
          "Please check network configuration or contact support. " +
          "See https://links.sonatype.com/telemetry-troubleshooting. INSTANCE ID: ";

  protected final Logger log = LoggerFactory.getLogger(getClass());

  @Autowired
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
      if (isMarkerSet(context, PROXY_REMOTE_FETCH_SKIP_MARKER)) {
        return buildPaymentRequiredResponse(context);
      }
      if (isMarkerSet(context, PROXY_TELEMETRY_BLOCKING_MARKER)) {
        return buildTelemetryBlockingResponse(context);
      }
      return buildNotFoundResponse(context);
    }
    catch (BypassHttpErrorException e) {
      return buildHttpErrorResponse(e);
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
    catch (BlobStoreWarmingUpException e) {
      // Blob store connection pool is still initializing (temporary, retry-able)
      log.info("Blob store '{}' warming up for {}, returning 503 to trigger client retry",
          e.getBlobStoreName(), context.getRequest().getPath());
      return HttpResponses.serviceUnavailable("Blob store warming up, please retry in a moment");
    }
    catch (MissingBlobException e) {
      // CRITICAL: Blob exists in metadata but missing from storage (data corruption, not retry-able)
      log.error("BLOB DATA LOSS: Blob {} missing from storage for {} - data corruption",
          e.getBlobRef(), context.getRequest().getPath());
      throw e; // Propagate as 500 error
    }
    catch (InvalidStateException e) {
      // Generic invalid state (e.g., stopped repository - not retry-able)
      log.warn("Invalid state for {}: {}",
          context.getRequest().getPath(), e.getMessage());
      throw e; // Propagate as 500 error
    }
    catch (IOException | UncheckedIOException e) {
      return HttpResponses.badGateway();
    }
    catch (Exception e) {
      // Walk the cause chain to find wrapped BlobStoreWarmingUpException (handles multi-level wrapping)
      Throwable cause = e.getCause();
      while (cause != null) {
        if (cause instanceof BlobStoreWarmingUpException) {
          log.info("Blob store '{}' warming up for {} (wrapped), returning 503 to trigger client retry",
              ((BlobStoreWarmingUpException) cause).getBlobStoreName(),
              context.getRequest().getPath());
          return HttpResponses.serviceUnavailable("Blob store warming up, please retry in a moment");
        }
        cause = cause.getCause();
      }
      throw e;
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

  protected Response buildPaymentRequiredResponse(final Context context) {
    if ("nuget".equals(context.getRepository().getFormat().getValue())) {
      return HttpResponses.conflict(PAYMENT_REQUIRED_MESSAGE.concat(nodeAccess.getId()));
    }
    else {
      return HttpResponses.forbidden(PAYMENT_REQUIRED_MESSAGE.concat(nodeAccess.getId()));
    }
  }

  private Response buildTelemetryBlockingResponse(final Context context) {
    String message = TELEMETRY_BLOCKING_MESSAGE + nodeAccess.getId();
    if ("nuget".equals(context.getRepository().getFormat().getValue())) {
      return HttpResponses.conflict(message);
    }
    else {
      return HttpResponses.forbidden(message);
    }
  }

  private static boolean isMarkerSet(final Context context, final String markerName) {
    return context.getAttributes() != null &&
        context.getAttributes().contains(markerName) &&
        TRUE.equals(context.getAttributes().get(markerName));
  }

  protected Response buildHttpErrorResponse(final BypassHttpErrorException proxyErrorsException) {
    return new Response.Builder()
        .payload(getPayload(proxyErrorsException.getBody(), proxyErrorsException.getContentType()))
        .status(new Status(false, proxyErrorsException.getStatusCode(), proxyErrorsException.getReason()))
        .headers(new Headers(proxyErrorsException.getHeaders()))
        .build();
  }

  private Payload getPayload(final String body, final String contentType) {
    return new StringPayload(body, contentType);
  }

  private ProxyFacet proxyFacet(final Context context) {
    return context.getRepository().facet(ProxyFacet.class);
  }
}
