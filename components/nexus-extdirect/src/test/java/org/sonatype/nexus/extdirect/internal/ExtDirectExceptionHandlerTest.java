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

import java.util.Collections;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.extdirect.model.ErrorResponse;
import org.sonatype.nexus.extdirect.model.ValidationResponse;

import com.softwarementors.extjs.djn.api.RegisteredMethod;
import fake.com.sonatype.insight.rm.rest.HttpException;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExtDirectExceptionHandlerTest
    extends TestSupport
{
  @Mock
  private RegisteredMethod registeredMethod;

  private ExtDirectExceptionHandler exceptionHandler = new ExtDirectExceptionHandler();

  @Test
  public void handleIQConnectionException() {
    HttpException exception = new HttpException(404, "Not Found", new IllegalAccessException());

    ErrorResponse response = (ErrorResponse) exceptionHandler.handleException(registeredMethod, exception);

    assertThat(response.getMessage(), is("Connection unsuccessful."));
  }

  @Test
  public void handleNonSuppressedExceptions() {
    Exception exception = new Exception("exception message");

    ErrorResponse response = (ErrorResponse) exceptionHandler.handleException(registeredMethod, exception);

    assertThat(response.getMessage(), is(exception.getMessage()));
  }

  @Test
  public void handleConstraintValidationException() {
    Path path = mock(Path.class);
    when(path.iterator()).thenReturn(Collections.emptyIterator());

    ConstraintViolation<?> violation = mock(ConstraintViolation.class);
    when(violation.getPropertyPath()).thenReturn(path);
    when(violation.getMessage()).thenReturn("violation message");

    ConstraintViolationException exception = new ConstraintViolationException(Set.of(violation));

    ValidationResponse response = (ValidationResponse) exceptionHandler.handleException(registeredMethod, exception);

    assertThat(response.getMessages(), contains(violation.getMessage()));
  }
}
