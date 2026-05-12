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

import java.util.Collections;
import java.util.List;

import org.sonatype.nexus.rest.ValidationErrorXO;
import org.sonatype.nexus.rest.ValidationErrorsException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

class ValidationErrorsExceptionMapperTest
{
  private ValidationErrorsExceptionMapper mapper;

  @BeforeEach
  void setup() {
    mapper = new ValidationErrorsExceptionMapper();
  }

  @Test
  void testGetValidationErrors() {
    ValidationErrorXO error = new ValidationErrorXO("testField", "test error");
    ValidationErrorsException exception = new ValidationErrorsException();
    exception.withErrors(Collections.singletonList(error));

    List<ValidationErrorXO> errors = mapper.getValidationErrors(exception);

    assertThat(errors, hasSize(1));
    assertThat(errors.get(0).getId(), equalTo("testField"));
    assertThat(errors.get(0).getMessage(), equalTo("test error"));
  }
}
