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
package org.sonatype.nexus.siesta.internal.resteasy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.sonatype.nexus.rest.ValidationErrorXO;
import org.sonatype.nexus.siesta.ValidationExceptionMapperSupport;

import org.jboss.resteasy.api.validation.ResteasyConstraintViolation;
import org.jboss.resteasy.api.validation.ResteasyViolationException;

import static org.jboss.resteasy.api.validation.ConstraintType.Type.RETURN_VALUE;

/**
 * Maps {@link ResteasyViolationException} to {@link Status#BAD_REQUEST} or {@link Status#INTERNAL_SERVER_ERROR}
 * in case of a violation on a methods return value, with a list of {@link ValidationErrorXO} as body.
 *
 * @since 3.0
 */
@Named
@Singleton
@Provider
public class ResteasyViolationExceptionMapper
    extends ValidationExceptionMapperSupport<ResteasyViolationException>
{
  @Override
  protected List<ValidationErrorXO> getValidationErrors(final ResteasyViolationException exception) {
    return getValidationErrors(exception.getViolations());
  }

  @Override
  protected Status getStatus(final ResteasyViolationException exception) {
    return getResponseStatus(exception.getViolations());
  }

  private List<ValidationErrorXO> getValidationErrors(final List<ResteasyConstraintViolation> violations) {
    final List<ValidationErrorXO> errors = new ArrayList<>();

    for (final ResteasyConstraintViolation violation : violations) {
      errors.add(new ValidationErrorXO(getPath(violation), violation.getMessage()));
    }

    return errors;
  }

  private Status getResponseStatus(final List<ResteasyConstraintViolation> violations) {
    final Iterator<ResteasyConstraintViolation> iterator = violations.iterator();

    if (iterator.hasNext()) {
      return getResponseStatus(iterator.next());
    }
    else {
      return Status.BAD_REQUEST;
    }
  }

  private Status getResponseStatus(final ResteasyConstraintViolation violation) {
    if (RETURN_VALUE.equals(violation.getConstraintType())) {
      return Status.INTERNAL_SERVER_ERROR;
    }
    return Status.BAD_REQUEST;
  }

  private String getPath(final ResteasyConstraintViolation violation) {
    final String propertyPath = violation.getPath();

    return violation.type() + (!"".equals(propertyPath) ? ' ' + propertyPath : "");
  }
}
