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

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.sonatype.nexus.rest.ValidationErrorXO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebappExceptionMapperTest
{
  private WebappExceptionMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new WebappExceptionMapper();
  }

  @Test
  void testConvert() {
    WebApplicationException exception = mock(WebApplicationException.class);
    Response response = mock(Response.class);
    when(response.getEntity())
        .thenReturn(new GenericEntity<>(new ValidationErrorXO("testWebExMapper"), ValidationErrorXO.class));
    when(exception.getResponse()).thenReturn(response);
    when(response.getStatus()).thenReturn(Status.PAYMENT_REQUIRED.getStatusCode());

    try (Response result = mapper.convert(exception, "testId")) {
      assertThat(result.getStatus(), is(Status.PAYMENT_REQUIRED.getStatusCode()));
      assertThat(result.getEntity(), is(nullValue()));
    }
  }
}
