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
package org.sonatype.nexus.extdirect.internal;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.extdirect.model.Response;

import com.orientechnologies.common.exception.OException;
import com.softwarementors.extjs.djn.api.RegisteredMethod;
import org.apache.commons.collections.ListUtils;
import org.apache.shiro.authz.UnauthenticatedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SERVICES;
import static org.sonatype.nexus.extdirect.model.Responses.error;
import static org.sonatype.nexus.extdirect.model.Responses.invalid;

/**
 * @since 3.15
 */
@Named
@ManagedLifecycle(phase = SERVICES)
@Singleton
public class ExtDirectExceptionHandler
{
  private static final Logger log = LoggerFactory.getLogger(ExtDirectExceptionHandler.class);

  private static final List<Class<Throwable>> SUPPRESSED_EXCEPTIONS = ListUtils
      .unmodifiableList(Arrays.asList(UnauthenticatedException.class));

  public Response handleException(final RegisteredMethod method, final Throwable e) {
    // debug logging for sanity (without stacktrace for suppressed exception)
    log.debug("Failed to invoke action method: {}, java-method: {}, exception message: {}",
        method.getFullName(), method.getFullJavaMethodName(), e.getMessage(),
        isSuppressedException(e) ? null : e);

    // handle validation message responses which have contents
    if (e instanceof ConstraintViolationException) {
      ConstraintViolationException cause = (ConstraintViolationException) e;
      Set<ConstraintViolation<?>> violations = cause.getConstraintViolations();
      if (violations != null && !violations.isEmpty()) {
        return invalid(cause);
      }
    }

    // exception logging for all non-suppressed exceptions
    if (!isSuppressedException(e)) {
      log.error("Failed to invoke action method: {}, java-method: {}",
          method.getFullName(), method.getFullJavaMethodName(), e);
    }

    if (e instanceof OException) {
      return error(new Exception("A database error occurred"));
    }

    return error(e);
  }

  private boolean isSuppressedException(final Throwable e) {
    return SUPPRESSED_EXCEPTIONS.stream().anyMatch(ex -> ex.isInstance(e));
  }
}
