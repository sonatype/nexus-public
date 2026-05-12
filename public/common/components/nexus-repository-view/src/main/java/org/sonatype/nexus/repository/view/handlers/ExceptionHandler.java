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

import org.sonatype.nexus.repository.IllegalOperationException;
import org.sonatype.nexus.repository.InvalidContentException;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Response;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    catch (Exception e) {
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
