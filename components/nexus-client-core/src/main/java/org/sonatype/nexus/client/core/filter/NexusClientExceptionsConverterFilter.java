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
package org.sonatype.nexus.client.core.filter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import org.sonatype.nexus.client.core.exception.NexusClientAccessForbiddenException;
import org.sonatype.nexus.client.core.exception.NexusClientBadRequestException;
import org.sonatype.nexus.client.core.exception.NexusClientErrorResponseException;
import org.sonatype.nexus.client.core.exception.NexusClientErrorResponseException.ErrorMessage;
import org.sonatype.nexus.client.core.exception.NexusClientException;
import org.sonatype.nexus.client.core.exception.NexusClientNotFoundException;
import org.sonatype.nexus.client.internal.msg.ErrorResponse;

import com.google.common.collect.Lists;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.ClientFilter;
import org.apache.commons.io.IOUtils;

/**
 * A filter that converts some known http error codes to specific {@link NexusClientException}:
 * <ul>
 * <li>400 -> {@link NexusClientBadRequestException}</li>
 * <li>400 with errors body -> {@link NexusClientErrorResponseException}</li>
 * <li>403 -> {@link NexusClientAccessForbiddenException}</li>
 * <li>404 -> {@link NexusClientNotFoundException}</li>
 * </ul>
 *
 * @since 2.7
 */
public class NexusClientExceptionsConverterFilter
    extends ClientFilter
{

  @Override
  public ClientResponse handle(final ClientRequest request) throws ClientHandlerException {
    final ClientResponse response = getNext().handle(request);
    throwIf404(response);
    throwIf403(response);
    throwIf400WithErrorMessage(response);
    throwIf400(response);
    return response;
  }

  private void throwIf400WithErrorMessage(final ClientResponse response) {
    if (ClientResponse.Status.BAD_REQUEST.equals(response.getClientResponseStatus())) {
      final String body = getResponseBody(response);
      response.bufferEntity();
      ErrorResponse errorResponse = null;
      try {
        errorResponse = response.getEntity(ErrorResponse.class);
      }
      catch (Exception ignore) {
        // most probably we do not have an error response
      }
      if (errorResponse != null) {
        // convert them to hide stupid "old" REST model, and not have it leak out
        final List<ErrorMessage> errors = Lists.newArrayList();
        for (org.sonatype.nexus.client.internal.msg.ErrorMessage message : errorResponse.getErrors()) {
          errors.add(new NexusClientErrorResponseException.ErrorMessage(message.getId(), message.getMsg()));
        }
        throw new NexusClientErrorResponseException(
            response.getClientResponseStatus().getReasonPhrase(),
            body,
            errors
        );
      }
    }
  }

  private void throwIf400(final ClientResponse response) {
    if (ClientResponse.Status.BAD_REQUEST.equals(response.getClientResponseStatus())) {
      throw new NexusClientBadRequestException(
          response.getClientResponseStatus().getReasonPhrase(),
          getResponseBody(response)
      );
    }
  }

  private void throwIf403(final ClientResponse response) {
    if (ClientResponse.Status.FORBIDDEN.equals(response.getClientResponseStatus())) {
      throw new NexusClientAccessForbiddenException(
          response.getClientResponseStatus().getReasonPhrase(),
          getResponseBody(response)
      );
    }
  }

  private void throwIf404(final ClientResponse response) {
    if (ClientResponse.Status.NOT_FOUND.equals(response.getClientResponseStatus())) {
      throw new NexusClientNotFoundException(
          response.getClientResponseStatus().getReasonPhrase(),
          getResponseBody(response)
      );
    }
  }

  private String getResponseBody(final ClientResponse response) {
    try {
      final byte[] body = IOUtils.toByteArray(response.getEntityInputStream());
      response.setEntityInputStream(new ByteArrayInputStream(body));
      return IOUtils.toString(body, "UTF-8");
    }
    catch (IOException e) {
      throw new IllegalStateException("Jersey unexpectedly refused to rewind buffered entity.");
    }
  }

}
