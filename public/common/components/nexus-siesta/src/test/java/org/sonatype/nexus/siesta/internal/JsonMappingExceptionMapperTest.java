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

import java.util.List;

import javax.ws.rs.core.Response;

import org.sonatype.nexus.rest.ValidationErrorXO;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonMappingException.Reference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

class JsonMappingExceptionMapperTest
{
  private JsonMappingExceptionMapper mapper;

  @BeforeEach
  void setup() {
    mapper = new JsonMappingExceptionMapper();
  }

  @Test
  void testConvert() {
    Reference reference = new Reference(null, "testField");
    JsonMappingException exception = new JsonMappingException("test json mapping exception");
    exception.prependPath(reference);

    try (Response response = mapper.convert(exception, "testId")) {
      assertThat(response.getStatus(), equalTo(Response.Status.BAD_REQUEST.getStatusCode()));
      List<ValidationErrorXO> errors = (List<ValidationErrorXO>) response.getEntity();
      assertThat(errors, hasSize(1));
      ValidationErrorXO error = errors.get(0);
      assertThat(error.getId(), equalTo("testField"));
      assertThat(error.getMessage(), equalTo("test json mapping exception"));
    }
  }
}
