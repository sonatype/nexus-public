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

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.annotation.JsonInclude;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

public class SimpleApiResponse
{

  private int status;

  private String message;

  @JsonInclude(NON_NULL)
  private Object data;

  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public Object getData() {
    return data;
  }

  public void setData(Object data) {
    this.data = data;
  }

  public static Response ok(final String message) {
    return ok(message, null);
  }

  public static Response ok(final String message, final Object data) {
    return response(OK, message, data);
  }

  public static Response notFound(final String message) {
    return notFound(message, null);
  }

  public static Response notFound(final String message, final Object data) {
    return response(NOT_FOUND, message, data);
  }

  public static Response badRequest(final String message) {
    return badRequest(message, null);
  }

  public static Response badRequest(final String message, final Object data) {
    return response(BAD_REQUEST, message, data);
  }

  public static Response unauthorized(final String message) {
    return unauthorized(message, null);
  }

  public static Response unauthorized(final String message, final Object data) {
    return response(UNAUTHORIZED, message, data);
  }

  @Override
  public String toString() {
    return "SimpleApiResponse{" +
        "status=" + status +
        ", message='" + message + '\'' +
        ", data=" + data +
        '}';
  }

  private static Response response(final Status status, final String message, final Object data) {
    SimpleApiResponse response = new SimpleApiResponse();
    response.setStatus(status.getStatusCode());
    response.setMessage(message);
    response.setData(data);
    return Response.status(status).entity(response).type(APPLICATION_JSON).build();
  }
}
