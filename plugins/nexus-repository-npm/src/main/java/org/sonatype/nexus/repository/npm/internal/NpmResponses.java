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
package org.sonatype.nexus.repository.npm.internal;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.view.ContentTypes;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.Status;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;
import org.sonatype.nexus.repository.view.payloads.StringPayload;

import com.google.common.collect.Maps;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.System.lineSeparator;
import static org.sonatype.nexus.repository.http.HttpStatus.BAD_REQUEST;
import static org.sonatype.nexus.repository.http.HttpStatus.NOT_FOUND;
import static org.sonatype.nexus.repository.http.HttpStatus.UNAUTHORIZED;
import static org.sonatype.nexus.repository.view.ContentTypes.APPLICATION_JSON;

/**
 * npm response utility, that sends status messages along with JSON, as npm CLI expects.
 *
 * @since 3.0
 */
public final class NpmResponses
{
  private static final String DELIMITER = "==================================================================";

  private NpmResponses() {
    // nop
  }

  private static final Payload successPayload = statusPayload(true, null);

  private static Payload statusPayload(final boolean success, @Nullable final String error) {
    NestedAttributesMap errorObject = new NestedAttributesMap("error", Maps.<String, Object>newHashMap());
    errorObject.set("success", Boolean.valueOf(success));
    if (error != null) {
      errorObject.set("error", error);
    }
    return new BytesPayload(NpmJsonUtils.bytes(errorObject), ContentTypes.APPLICATION_JSON);
  }

  @Nonnull
  static Response ok() {
    return HttpResponses.ok(successPayload);
  }

  @Nonnull
  public static Response ok(@Nonnull final Payload payload) {
    return HttpResponses.ok(checkNotNull(payload));
  }

  @Nonnull
  public static Response notFound(@Nullable final String message) {
    return new Response.Builder()
        .status(Status.failure(NOT_FOUND, "Not Found"))
        .payload(statusPayload(false, message))
        .build();
  }

  @Nonnull
  static Response packageNotFound(final NpmPackageId packageId) {
    checkNotNull(packageId);
    return notFound("Package '" + packageId + "' not found");
  }

  @Nonnull
  static Response tarballNotFound(final NpmPackageId packageId, final String tarballName) {
    checkNotNull(packageId);
    checkNotNull(tarballName);
    return notFound("Tarball '" + tarballName + "' in package '" + packageId + "' not found");
  }

  @Nonnull
  public static Response badRequest(@Nullable final String message) {
    return new Response.Builder()
        .status(Status.failure(BAD_REQUEST))
        .payload(statusPayload(false, message))
        .build();
  }

  @Nonnull
  public static Response badCredentials(@Nullable final String message) {
    return new Response.Builder()
        .status(Status.failure(UNAUTHORIZED))
        .payload(statusPayload(false, message))
        .build();
  }

  @Nonnull
  static Response npmErrorAuditResponse(
      final int statusCode,
      @Nonnull final String message)
  {
    String newLineDelimiter = String.format("%s%s", lineSeparator(), DELIMITER);
    String errorMsg = String.format("%s%s%s%s", newLineDelimiter, lineSeparator(), message, newLineDelimiter);
    return new Response.Builder()
        .status(Status.failure(statusCode))
        .payload(new StringPayload(errorMsg, APPLICATION_JSON))
        .build();
  }
}
