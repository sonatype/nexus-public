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
package org.sonatype.nexus.rest;

import com.google.common.collect.ImmutableList;
import junit.framework.TestCase;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class WebApplicationMessageExceptionTest
    extends TestCase
{

  /**
   * Method under test:
   * {@link WebApplicationMessageException#WebApplicationMessageException(Response.Status, Object, String)}
   */
  @Test
  public void testConstructor() {
    WebApplicationMessageException exception = new WebApplicationMessageException(
        Response.Status.BAD_REQUEST, "Message", MediaType.APPLICATION_JSON);
    Response response = exception.getResponse();

    assertEquals(400, response.getStatus());

    Object entity = response.getEntity();

    assertTrue(entity instanceof ValidationErrorXO);
    assertEquals("Message", ((ValidationErrorXO) entity).getMessage());
    assertEquals(ValidationErrorXO.GENERIC, ((ValidationErrorXO) entity).getId());
    assertEquals(ImmutableList.of(MediaType.APPLICATION_JSON), response.getHeaders().get("Content-Type"));
  }

  /**
   * Method under test: {@link WebApplicationMessageException#WebApplicationMessageException(Response.Status, String)}
   */
  @Test
  public void testConstructorNoMediaType() {
    WebApplicationMessageException exception = new WebApplicationMessageException(
        Response.Status.NOT_FOUND, "Message");
    Response response = exception.getResponse();

    assertEquals(404, response.getStatus());

    Object entity = response.getEntity();

    assertTrue(entity instanceof ValidationErrorXO);
    assertEquals("Message", ((ValidationErrorXO) entity).getMessage());
    assertEquals(ValidationErrorXO.GENERIC, ((ValidationErrorXO) entity).getId());
    assertEquals(ImmutableList.of(MediaType.TEXT_PLAIN), response.getHeaders().get("Content-Type"));
  }
}
