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
  private HttpResponses() {
    // empty
  }

  // Ok: 200

  public static Response ok(@Nullable final String message) {
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

  public static Response created(@Nullable final String message) {
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

  // Accepted: 202

  public static Response accepted() {
    return new Response.Builder()
        .status(Status.success(ACCEPTED))
        .build();
  }

  // No Content: 204

  public static Response noContent(@Nullable final String message) {
    return new Response.Builder()
        .status(Status.success(NO_CONTENT, message))
        .build();
  }

  public static Response noContent() {
    return noContent(null);
  }

  // Not Found: 404

  public static Response notFound(@Nullable final String message) {
    return new Response.Builder()
        .status(Status.failure(NOT_FOUND, message))
        .build();
  }

  public static Response notFound() {
    return notFound(null);
  }

  // Bad request: 400

  public static Response badRequest(@Nullable final String message) {
    return new Response.Builder()
        .status(Status.failure(BAD_REQUEST, message))
        .build();
  }

  public static Response badRequest() {
    return badRequest(null);
  }

  // Unauthorized: 401

  public static Response unauthorized(@Nullable final String message) {
    return new Response.Builder()
        .status(Status.failure(UNAUTHORIZED, message))
        .build();
  }

  public static Response unauthorized() {
    return unauthorized(null);
  }

  /**
   * Builds the challenge for authorization by setting a HTTP <code>401</code> (Unauthorized) status as well as the
   * response's <code>WWW-Authenticate</code> header.
   * <p/>
   * The header value constructed is equal to:
   * <p/>
   * <code>String.format("%s realm=\"%s\"", authenticationScheme, realmName)</code>
   *
   * @param message to be shown next to the status code
   * @param authenticationScheme authentication scheme used in the authentication challenge header
   * @param realmName realm name used in the authentication challenge header
   * @return a 401 authentication challenge response
   */
  public static Response unauthorized(
      final String message,
      final String authenticationScheme,
      final String realmName)
  {
    checkNotNull(message);
    checkNotNull(authenticationScheme);
    checkNotNull(realmName);
    return new Response.Builder()
        .status(Status.failure(UNAUTHORIZED, message))
        .header(HttpHeaders.WWW_AUTHENTICATE, String.format("%s realm=\"%s\"", authenticationScheme, realmName))
        .build();
  }

  // Payment required: 402

  public static Response paymentRequired(@Nullable final String message) {
    return new Response.Builder()
        .status(Status.failure(PAYMENT_REQUIRED, message))
        .build();
  }

  public static Response paymentRequired() {
    return paymentRequired(null);
  }

  // Forbidden: 403

  public static Response forbidden(@Nullable final String message) {
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

  // Conflict: 409

  public static Response conflict(@Nullable final String message) {
    return new Response.Builder()
        .status(Status.failure(CONFLICT, message))
        .build();
  }

  public static Response conflict() {
    return conflict(null);
  }

  public static Response serviceUnavailable(@Nullable final String message) {
    return new Response.Builder()
        .status(Status.failure(SERVICE_UNAVAILABLE, message))
        .build();
  }

  public static Response serviceUnavailable() {
    return serviceUnavailable(null);
  }

  public static Response badGateway(@Nullable final String message) {
    return new Response.Builder()
        .status(Status.failure(BAD_GATEWAY, message))
        .build();
  }

  public static Response badGateway() {
    return badGateway(null);
  }

  public static Response gatewayTimeout(@Nullable final String message) {
    return new Response.Builder()
        .status(Status.failure(GATEWAY_TIMEOUT, message))
        .build();
  }

  public static Response notImplemented(@Nullable final String message) {
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
