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
package org.sonatype.nexus.repository.rest;

import java.nio.file.InvalidPathException;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.sonatype.nexus.rest.ExceptionMapperSupport;
import org.sonatype.nexus.rest.SimpleApiResponse;

import org.apache.commons.fileupload2.core.FileUploadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FileUploadExceptionMapper}.
 *
 * <p>
 * NEXUS-46395: commons-fileupload 2.x raises {@link FileUploadException} for malformed multipart
 * bodies (NUL-byte filenames, oversized parts, etc.). Pre-fix these were caught by
 * {@code UnexpectedExceptionMapper} and surfaced as 500. The mapper under test maps them to a 400
 * with the original descriptive message, which is the appropriate response to a malformed client
 * request.
 *
 * <p>
 * The realistic call site is {@code UploadComponentMultipartHelper.createField}: it catches
 * {@link InvalidPathException} from commons-fileupload 2.x's filename validation and re-throws as
 * {@link FileUploadException} with a structured message. These tests exercise both the bare-message
 * and the cause-chained variants.
 */
class FileUploadExceptionMapperTest
{
  private FileUploadExceptionMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new FileUploadExceptionMapper();
  }

  @Test
  void mapsToBadRequest() {
    Response response = mapper.convert(
        new FileUploadException("malformed multipart"), "fault-id-1");

    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  void responseIsApplicationJson() {
    Response response = mapper.convert(
        new FileUploadException("malformed multipart"), "fault-id-2");

    assertThat(response.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);
  }

  @Test
  void bodyIsSimpleApiResponseWithExceptionMessage() {
    String message = "Invalid filename in multipart/form-data upload (field 'asset1'): evil\\0.zip at index 4: ...";
    Response response = mapper.convert(new FileUploadException(message), "fault-id-3");

    Object entity = response.getEntity();
    assertThat(entity).isInstanceOf(SimpleApiResponse.class);
    SimpleApiResponse body = (SimpleApiResponse) entity;
    assertThat(body.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    assertThat(body.getMessage()).isEqualTo(message);
  }

  /**
   * Realistic shape from {@code UploadComponentMultipartHelper.createField}: an
   * {@link InvalidPathException} caught and re-thrown with a descriptive message and the original
   * exception as cause. The mapper must expose the wrapper's message (the structured one), not the
   * cause's raw {@code "evil\0.zip at index 4: ..."} string.
   */
  @Test
  void preservesWrapperMessageEvenWhenCauseIsInvalidPathException() {
    InvalidPathException cause = new InvalidPathException("evil\\0.zip", "Nul character not allowed", 4);
    String wrapperMessage = String.format(
        "Invalid filename in multipart/form-data upload (field '%s'): %s",
        "asset1", cause.getMessage());
    FileUploadException exception = new FileUploadException(wrapperMessage, cause);

    Response response = mapper.convert(exception, "fault-id-4");

    SimpleApiResponse body = (SimpleApiResponse) response.getEntity();
    assertThat(body.getMessage()).isEqualTo(wrapperMessage);
    assertThat(body.getMessage()).startsWith("Invalid filename in multipart/form-data upload");
  }

  @Test
  void nullMessagePassesThroughWithoutNpe() {
    Response response = mapper.convert(new FileUploadException(null), "fault-id-5");

    SimpleApiResponse body = (SimpleApiResponse) response.getEntity();
    assertThat(body.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    assertThat(body.getMessage()).isNull();
  }

  /**
   * The mapper inherits {@link ExceptionMapperSupport#toResponse} which adds the
   * {@code X-Siesta-FaultId} header. End-to-end through the public {@code toResponse} entry point,
   * the response carries that header so operators can correlate to the server-side log line.
   */
  @Test
  void publicToResponseSetsFaultIdHeader() {
    Response response = mapper.toResponse(new FileUploadException("malformed multipart"));

    String faultId = response.getHeaderString(ExceptionMapperSupport.X_SIESTA_FAULT_ID);
    assertThat(faultId).isNotNull().isNotBlank();
  }

  /**
   * {@code FileUploadException} extends {@code IOException} in commons-fileupload 2.x. Confirms
   * the mapper still fires when the upstream code propagates the exception via an
   * {@code IOException}-typed reference, as long as the runtime type is still
   * {@code FileUploadException}. JAX-RS dispatch is type-based; an {@code IOException} that is
   * structurally a wrapping {@code new IOException(fue)} would NOT fire this mapper, which is why
   * {@code UploadManagerImpl.create} no longer wraps.
   */
  @Test
  void fileUploadExceptionIsAssignableToIoException() {
    FileUploadException exception = new FileUploadException("test");
    assertThat(exception).isInstanceOf(java.io.IOException.class);
  }
}
