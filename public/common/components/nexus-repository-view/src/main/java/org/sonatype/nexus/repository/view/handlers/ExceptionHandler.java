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

import org.sonatype.nexus.blobstore.api.BlobStoreWarmingUpException;
import org.sonatype.nexus.common.stateguard.InvalidStateException;
import org.sonatype.nexus.repository.IllegalOperationException;
import org.sonatype.nexus.repository.InvalidContentException;
import org.sonatype.nexus.repository.MissingBlobException;
import org.sonatype.nexus.repository.RedeployDisabledException;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static org.sonatype.nexus.repository.http.HttpMethods.PUT;

/**
 * A format-neutral error handler for some exceptions. These exceptions are meant to signal some response directly
 * mappable onto a HTTP response, usually some 4xx error code.
 *
 * @since 3.0
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ExceptionHandler
    implements Handler
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  @Nonnull
  @Override
  public Response handle(@Nonnull final Context context) throws Exception { // NOSONAR
    try {
      return context.proceed();
    }
    catch (RedeployDisabledException e) {
      log.debug("Attempted redeploy in repository '{}': {} {}: {}",
          context.getRepository() != null ? context.getRepository().getName() : "unknown",
          context.getRequest().getAction(), context.getRequest().getPath(), e.toString());
      return HttpResponses.conflict(e.getMessage());
    }
    catch (IllegalOperationException e) {
      log.warn("Illegal operation in repository '{}': {} {}: {}",
          context.getRepository() != null ? context.getRepository().getName() : "unknown",
          context.getRequest().getAction(),
          context.getRequest().getPath(),
          e.toString());
      return HttpResponses.badRequest(e.getMessage());
    }
    catch (InvalidContentException e) {
      log.warn("Invalid content in repository '{}': {} {}: {}",
          context.getRepository() != null ? context.getRepository().getName() : "unknown",
          context.getRequest().getAction(),
          context.getRequest().getPath(),
          e.toString());
      if (PUT.equals(context.getRequest().getAction())) {
        return HttpResponses.badRequest(e.getMessage());
      }
      return HttpResponses.notFound(e.getMessage());
    }
    catch (BlobStoreWarmingUpException e) {
      // Blob store connection pool is still initializing (temporary, retry-able)
      log.info("Blob store '{}' warming up for {} {}, returning 503 to trigger client retry",
          e.getBlobStoreName(),
          context.getRequest().getAction(),
          context.getRequest().getPath());
      return HttpResponses.serviceUnavailable("Blob store warming up, please retry in a moment");
    }
    catch (MissingBlobException e) {
      // CRITICAL: Blob exists in metadata but missing from storage (data corruption, not retry-able)
      log.error("BLOB DATA LOSS: Blob {} missing from storage for {} {} - this indicates data corruption",
          e.getBlobRef(),
          context.getRequest().getAction(),
          context.getRequest().getPath());
      throw e; // Propagate as 500 error
    }
    catch (InvalidStateException e) {
      // Generic invalid state (e.g., stopped repository, failed repository - not retry-able)
      log.warn("Invalid state for {} {}: {}",
          context.getRequest().getAction(),
          context.getRequest().getPath(),
          e.getMessage());
      throw e; // Propagate as 500 error
    }
    catch (Exception e) {
      // Walk the cause chain to find wrapped exceptions (handles multi-level wrapping)
      Throwable cause = e.getCause();
      while (cause != null) {
        if (cause instanceof BlobStoreWarmingUpException) {
          // Blob store connection pool is still initializing (temporary, retry-able)
          log.info("Blob store '{}' warming up for {} {} (wrapped), returning 503",
              ((BlobStoreWarmingUpException) cause).getBlobStoreName(),
              context.getRequest().getAction(),
              context.getRequest().getPath());
          return HttpResponses.serviceUnavailable("Blob store warming up, please retry in a moment");
        }
        if (cause instanceof MissingBlobException) {
          // CRITICAL: Blob exists in metadata but missing from storage (data corruption, not retry-able)
          log.error("BLOB DATA LOSS: Blob {} missing from storage for {} {} (wrapped) - data corruption",
              ((MissingBlobException) cause).getBlobRef(),
              context.getRequest().getAction(),
              context.getRequest().getPath());
          throw e; // Propagate as 500 error
        }
        if (cause instanceof InvalidStateException) {
          log.warn("Invalid state for {} {} (wrapped): {}",
              context.getRequest().getAction(),
              context.getRequest().getPath(),
              cause.getMessage());
          throw e; // Propagate as 500 error
        }
        cause = cause.getCause();
      }
      String exceptionName = e.getClass().getSimpleName();
      if (exceptionName.contains("OModificationOperationProhibitedException")
          || exceptionName.contains("OWriteOperationNotPermittedException")) {
        return readOnly(context, e);
      }
      throw e;
    }
  }

  private Response readOnly(final Context context, Exception e) {
    log.warn("Nexus Repository Manager is in read-only mode for repository '{}': {} {}: {}",
        context.getRepository() != null ? context.getRepository().getName() : "unknown",
        context.getRequest().getAction(),
        context.getRequest().getPath(),
        e.toString());

    return HttpResponses.serviceUnavailable("Nexus Repository Manager is in read-only mode");
  }
}
