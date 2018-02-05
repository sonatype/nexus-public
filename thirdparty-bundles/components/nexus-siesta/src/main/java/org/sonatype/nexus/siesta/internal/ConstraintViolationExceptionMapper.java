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
package org.sonatype.nexus.siesta.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.ElementKind;
import javax.validation.Path;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.sonatype.nexus.rest.ValidationErrorXO;
import org.sonatype.nexus.siesta.ValidationExceptionMapperSupport;

/**
 * Maps {@link ConstraintViolationException} to {@link Status#BAD_REQUEST} or {@link Status#INTERNAL_SERVER_ERROR}
 * in case of a violation on a methods return value, with a list of {@link ValidationErrorXO} as body.
 *
 * @since 3.0
 */
@Named
@Singleton
@Provider
public class ConstraintViolationExceptionMapper
    extends ValidationExceptionMapperSupport<ConstraintViolationException>
{
  @Override
  protected List<ValidationErrorXO> getValidationErrors(final ConstraintViolationException exception) {
    return getValidationErrors(exception.getConstraintViolations());
  }

  @Override
  protected Status getStatus(final ConstraintViolationException exception) {
    return getResponseStatus(exception.getConstraintViolations());
  }

  private List<ValidationErrorXO> getValidationErrors(final Set<ConstraintViolation<?>> violations) {
    final List<ValidationErrorXO> errors = new ArrayList<>();

    for (final ConstraintViolation violation : violations) {
      errors.add(new ValidationErrorXO(getPath(violation), violation.getMessage()));
    }

    return errors;
  }

  private Status getResponseStatus(final Set<ConstraintViolation<?>> violations) {
    final Iterator<ConstraintViolation<?>> iterator = violations.iterator();

    if (iterator.hasNext()) {
      return getResponseStatus(iterator.next());
    }
    else {
      return Status.BAD_REQUEST;
    }
  }

  private Status getResponseStatus(final ConstraintViolation<?> violation) {
    for (Path.Node node : violation.getPropertyPath()) {
      ElementKind kind = node.getKind();

      if (ElementKind.RETURN_VALUE.equals(kind)) {
        return Status.INTERNAL_SERVER_ERROR;
      }
    }

    return Status.BAD_REQUEST;
  }

  private String getPath(final ConstraintViolation violation) {
    String leafBeanName = violation.getLeafBean().getClass().getSimpleName();
    final int proxySuffix = leafBeanName.indexOf("$$EnhancerByGuice");
    if (proxySuffix > 0) {
      leafBeanName = leafBeanName.substring(0, proxySuffix);
    }

    final String propertyPath = violation.getPropertyPath().toString();

    return leafBeanName + (!"".equals(propertyPath) ? '.' + propertyPath : "");
  }
}
