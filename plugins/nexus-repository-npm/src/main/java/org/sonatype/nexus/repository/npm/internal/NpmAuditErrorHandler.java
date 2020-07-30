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
package org.sonatype.nexus.repository.npm.internal;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nonnull;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.npm.internal.audit.exceptions.PackageLockParsingException;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.vulnerability.exceptions.CompatibilityException;
import org.sonatype.nexus.repository.vulnerability.exceptions.ConfigurationException;
import org.sonatype.nexus.repository.vulnerability.exceptions.InternalException;
import org.sonatype.nexus.repository.vulnerability.exceptions.TarballLoadingException;
import org.sonatype.nexus.repository.vulnerability.exceptions.VulnerabilityFetchingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.System.lineSeparator;
import static org.sonatype.nexus.repository.http.HttpStatus.BAD_REQUEST;
import static org.sonatype.nexus.repository.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.sonatype.nexus.repository.http.HttpStatus.NOT_FOUND;
import static org.sonatype.nexus.repository.npm.internal.NpmAuditFacet.QUICK_AUDIT_ATTR_NAME;

/**
 * Handle all exceptions which could be thrown from npm audit.
 *
 * @since 3.24
 */
@Named
@Singleton
public class NpmAuditErrorHandler
    implements Handler
{
  private static final Logger log = LoggerFactory.getLogger(NpmAuditErrorHandler.class);

  private static final String USER_ERROR_MSG =
      "Error fetching npm audit data. " + lineSeparator() +
      "See NXRM logs for more details or contact your NXRM administrator.";

  @Nonnull
  @Override
  public Response handle(@Nonnull final Context context)
  {
    try {
      return context.proceed();
    }
    catch (PackageLockParsingException e) {
      return NpmResponses.npmErrorAuditResponse(BAD_REQUEST, e.getMessage());
    }
    catch (TarballLoadingException e) {
      log.warn(e.getMessage(), e);
      return NpmResponses.npmErrorAuditResponse(NOT_FOUND, USER_ERROR_MSG);
    }
    catch (ExecutionException e) {
      boolean quickAudit = (boolean) context.getAttributes().require(QUICK_AUDIT_ATTR_NAME);
      return handleAuditExceptions(e, quickAudit);
    }
    catch (TimeoutException e) {
      log.warn(e.getMessage(), e);
      return NpmResponses.npmErrorAuditResponse(INTERNAL_SERVER_ERROR, USER_ERROR_MSG);
    }
    catch (Exception e) { // NOSONAR ignore this exception, just notify a user
      log.error(e.getMessage(), e);
      return NpmResponses.npmErrorAuditResponse(INTERNAL_SERVER_ERROR, USER_ERROR_MSG);
    }
  }

  private Response handleAuditExceptions(final ExecutionException e, final boolean quickAudit) {
    // list of all general exceptions in org.sonatype.nexus.repository.vulnerability.exceptions
    Throwable cause = e.getCause();
    if (cause instanceof CompatibilityException) {
      return NpmResponses.npmErrorAuditResponse(BAD_REQUEST, cause.getMessage());
    }
    if (cause instanceof VulnerabilityFetchingException || cause instanceof InternalException) {
      log.warn(cause.getMessage(), e);
      return NpmResponses.npmErrorAuditResponse(INTERNAL_SERVER_ERROR, USER_ERROR_MSG);
    }
    else if (cause instanceof ConfigurationException) {
      if (quickAudit) {
        // don't spam NXRM logs in case of quick audit, see NEXUS-24334
        log.debug(cause.getMessage(), e);
      }
      else {
        log.warn(cause.getMessage(), log.isDebugEnabled() ? e : null);
      }
      return NpmResponses.npmErrorAuditResponse(BAD_REQUEST, cause.getMessage());
    }
    else {
      log.error(e.getMessage(), e);
      return NpmResponses.npmErrorAuditResponse(INTERNAL_SERVER_ERROR, USER_ERROR_MSG);
    }
  }
}
