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

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.sonatype.nexus.rest.ExceptionMapperSupport;
import org.sonatype.nexus.rest.SimpleApiResponse;

import org.apache.commons.fileupload2.core.FileUploadException;
import org.springframework.stereotype.Component;

/**
 * Maps {@link FileUploadException} from commons-fileupload 2.x to a
 * {@link Status#BAD_REQUEST 400} response with the exception's descriptive
 * message in the body.
 *
 * <p>
 * NEXUS-46395: commons-fileupload 2.x raises {@link FileUploadException} for
 * client-side multipart errors (malformed boundaries, illegal filenames such as
 * NUL bytes or OS-illegal path characters, oversized parts, etc.). Without a
 * targeted mapper these surfaced through {@code UnexpectedExceptionMapper} as
 * 500 responses, which is wrong: the client sent a malformed multipart body, so
 * 400 is the appropriate status. The exception message is already structured
 * (e.g. {@code "Invalid filename in multipart/form-data upload (field 'asset1'):
 * evil\0.zip at index 4: ..."}), so we surface it verbatim.
 *
 * <p>
 * The {@link org.sonatype.nexus.repository.upload.internal.UploadComponentMultipartHelper}
 * already catches {@code java.nio.file.InvalidPathException} from the
 * commons-fileupload 2.x parser and re-throws as {@code FileUploadException}
 * with a descriptive message; this mapper is what closes the loop and surfaces
 * that message to the HTTP client at the correct status code.
 */
@Component
public class FileUploadExceptionMapper
    extends ExceptionMapperSupport<FileUploadException>
{
  @Override
  protected Response convert(final FileUploadException exception, final String id) {
    SimpleApiResponse body = new SimpleApiResponse();
    body.setStatus(Status.BAD_REQUEST.getStatusCode());
    body.setMessage(exception.getMessage());
    return Response.status(Status.BAD_REQUEST)
        .entity(body)
        .type(MediaType.APPLICATION_JSON)
        .build();
  }
}
