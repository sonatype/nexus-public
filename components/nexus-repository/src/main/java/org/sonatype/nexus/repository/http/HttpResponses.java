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
package org.sonatype.nexus.repository.http;

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.Status;

import com.google.common.base.Joiner;
import com.google.common.net.HttpHeaders;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.http.HttpStatus.*;

/**
 * Convenience methods for constructing various commonly used HTTP responses.
 *
 * @since 3.0
 */
public class HttpResponses
{
  private HttpResponses() {}

  // Ok: 200

  public static Response ok(final @Nullable String message) {
    return new Response.Builder()
        .status(Status.success(OK, message))
        .build();
  }

  public static Response ok() {
    return ok((String) null);
  }

  public static Response ok(final Payload payload) {
    return new Response.Builder()
        .status(Status.success(OK))
        .payload(payload)
        .build();
  }

  // Created: 201

  public static Response created(final @Nullable String message) {
    return new Response.Builder()
        .status(Status.success(CREATED, message))
        .build();
  }

  public static Response created() {
    return created((String) null);
  }

  public static Response created(final Payload payload) {
    return new Response.Builder()
        .status(Status.success(CREATED))
        .payload(payload)
        .build();
  }

  // No Content: 204

  public static Response noContent(final @Nullable String message) {
    return new Response.Builder()
        .status(Status.success(NO_CONTENT, message))
        .build();
  }

  public static Response noContent() {
    return noContent(null);
  }

  // Not Found: 404

  public static Response notFound(final @Nullable String message) {
    return new Response.Builder()
        .status(Status.failure(NOT_FOUND, message))
        .build();
  }

  public static Response notFound() {
    return notFound(null);
  }

  // Bad request: 400

  public static Response badRequest(final @Nullable String message) {
    return new Response.Builder()
        .status(Status.failure(BAD_REQUEST, message))
        .build();
  }

  public static Response badRequest() {
    return badRequest(null);
  }

  // Unauthorized: 401

  public static Response unauthorized(final @Nullable String message) {
    return new Response.Builder()
        .status(Status.failure(UNAUTHORIZED, message))
        .build();
  }

  public static Response unauthorized() {
    return unauthorized(null);
  }

  // Forbidden: 403

  public static Response forbidden(final @Nullable String message) {
    return new Response.Builder()
        .status(Status.failure(FORBIDDEN, message))
        .build();
  }

  public static Response forbidden() {
    return forbidden(null);
  }

  // Method not allowed: 405

  public static Response methodNotAllowed(final String methodName, final String... allowedMethods) {
    checkNotNull(methodName);
    checkNotNull(allowedMethods);
    checkArgument(allowedMethods.length != 0);
    return new Response.Builder()
        .status(Status.failure(METHOD_NOT_ALLOWED, methodName))
        .header(HttpHeaders.ALLOW, Joiner.on(',').join(allowedMethods))
        .build();
  }

  public static Response serviceUnavailable(final @Nullable String message) {
    return new Response.Builder()
        .status(Status.failure(SERVICE_UNAVAILABLE, message))
        .build();
  }

  public static Response serviceUnavailable() {
    return serviceUnavailable(null);
  }

  public static Response badGateway(final @Nullable String message) {
    return new Response.Builder()
        .status(Status.failure(BAD_GATEWAY, message))
        .build();
  }

  public static Response badGateway() {
    return badGateway(null);
  }

  public static Response notImplemented(final @Nullable String message) {
    return new Response.Builder()
        .status(Status.failure(NOT_IMPLEMENTED, message))
        .build();
  }

  public static Response rangeNotSatisfiable(final long contentSize) {
    return new Response.Builder()
        .status(Status.failure(REQUESTED_RANGE_NOT_SATISFIABLE))
        .header(HttpHeaders.CONTENT_LENGTH, "0")
        .header(HttpHeaders.CONTENT_RANGE, "bytes */" + contentSize)
        .build();
  }
}
