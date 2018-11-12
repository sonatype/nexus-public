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
package org.sonatype.nexus.extdirect.internal

import javax.validation.ConstraintViolation
import javax.validation.ConstraintViolationException
import javax.validation.Path

import org.sonatype.nexus.extdirect.model.ErrorResponse
import org.sonatype.nexus.extdirect.model.ValidationResponse

import com.orientechnologies.common.exception.OException
import com.orientechnologies.orient.core.sql.OCommandSQLParsingException
import com.softwarementors.extjs.djn.api.RegisteredMethod
import org.apache.shiro.authz.UnauthenticatedException
import org.junit.Test

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class ExtDirectExceptionHandlerTest
{
  private ExtDirectExceptionHandler exceptionHandler = new ExtDirectExceptionHandler();

  @Test
  void handleExceptionHidesOExceptionDetails() {
    RegisteredMethod registeredMethod = mock(RegisteredMethod)

    def exception = new OCommandSQLParsingException('error message containing orient syntax')

    ErrorResponse response = (ErrorResponse) exceptionHandler.handleException(registeredMethod, exception)

    assert response.message == 'A database error occurred'
  }

  @Test
  void handleNonSuppressedExceptions() {
    RegisteredMethod registeredMethod = mock(RegisteredMethod)

    def exception = new Exception('exception message')

    ErrorResponse response = (ErrorResponse) exceptionHandler.handleException(registeredMethod, exception)

    assert response.message == exception.getMessage()
  }

  @Test
  void handleConstraintValidationException() {
    RegisteredMethod registeredMethod = mock(RegisteredMethod)

    Path path = mock(Path)
    when(path.iterator()).thenReturn(Collections.emptyIterator())

    ConstraintViolation violation = mock(ConstraintViolation)
    when(violation.propertyPath).thenReturn(path)
    when(violation.message).thenReturn('violation message')

    def exception = new ConstraintViolationException([violation] as Set)

    ValidationResponse response = (ValidationResponse) exceptionHandler.handleException(registeredMethod, exception)

    assert response.messages == [violation.message]
  }
}
