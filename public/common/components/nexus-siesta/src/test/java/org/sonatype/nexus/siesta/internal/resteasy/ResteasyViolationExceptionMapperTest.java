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

import java.util.Collections;
import java.util.List;

import jakarta.ws.rs.core.Response.Status;

import org.sonatype.nexus.rest.ValidationErrorXO;

import org.jboss.resteasy.api.validation.ConstraintType.Type;
import org.jboss.resteasy.api.validation.ResteasyConstraintViolation;
import org.jboss.resteasy.api.validation.ResteasyViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResteasyViolationExceptionMapperTest
{
  private ResteasyViolationExceptionMapper mapper;

  @BeforeEach
  void setup() {
    mapper = new ResteasyViolationExceptionMapper();
  }

  @Test
  void testGetValidationErrors() {
    ResteasyConstraintViolation violation = mock(ResteasyConstraintViolation.class);
    ResteasyViolationException exception = mock(ResteasyViolationException.class);
    when(exception.getViolations()).thenReturn(Collections.singletonList(violation));
    when(violation.getPath()).thenReturn("myPath");
    when(violation.type()).thenReturn("myType");
    when(violation.getMessage()).thenReturn("Test message");

    List<ValidationErrorXO> errors = mapper.getValidationErrors(exception);

    assertThat(errors, hasSize(1));
    assertThat(errors.get(0).getMessage(), equalTo("Test message"));
    assertThat(errors.get(0).getId(), equalTo("myType myPath"));
  }

  @Test
  void testGetStatusInternalServerError() {
    ResteasyConstraintViolation violation = mock(ResteasyConstraintViolation.class);
    when(violation.getConstraintType()).thenReturn(Type.RETURN_VALUE);

    ResteasyViolationException exception = mock(ResteasyViolationException.class);
    when(exception.getViolations()).thenReturn(Collections.singletonList(violation));

    Status status = mapper.getStatus(exception);

    assertThat(status, equalTo(Status.INTERNAL_SERVER_ERROR));
  }

  @Test
  void testGetStatusBadRequest() {
    ResteasyConstraintViolation violation = mock(ResteasyConstraintViolation.class);
    when(violation.getConstraintType()).thenReturn(Type.PARAMETER);

    ResteasyViolationException exception = mock(ResteasyViolationException.class);
    when(exception.getViolations()).thenReturn(Collections.singletonList(violation));

    Status status = mapper.getStatus(exception);

    assertThat(status, equalTo(Status.BAD_REQUEST));
  }

  @Test
  void testGetStatusBadRequestEmptyViolations() {
    ResteasyViolationException exception = mock(ResteasyViolationException.class);
    when(exception.getViolations()).thenReturn(Collections.emptyList());

    Status status = mapper.getStatus(exception);

    assertThat(status, equalTo(Status.BAD_REQUEST));
  }
}
