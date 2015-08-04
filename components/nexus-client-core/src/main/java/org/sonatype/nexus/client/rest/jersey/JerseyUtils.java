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
package org.sonatype.nexus.client.rest.jersey;

import java.util.List;
import java.util.concurrent.Callable;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.sonatype.sisu.siesta.common.error.ErrorXO;
import org.sonatype.sisu.siesta.common.validation.ValidationErrorXO;
import org.sonatype.sisu.siesta.common.validation.ValidationErrorsException;

import com.google.common.base.Throwables;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.UniformInterfaceException;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.sonatype.sisu.siesta.common.SiestaMediaType.VND_ERROR_V1_JSON_TYPE;
import static org.sonatype.sisu.siesta.common.SiestaMediaType.VND_VALIDATION_ERRORS_V1_JSON_TYPE;

/**
 * Jersey utilities.
 *
 * @since 1.0
 */
public class JerseyUtils
{

  public static final MediaType CONTENT_TYPE = APPLICATION_JSON_TYPE;

  public static final MediaType[] ACCEPTS = new MediaType[]{
      APPLICATION_JSON_TYPE, VND_ERROR_V1_JSON_TYPE, VND_VALIDATION_ERRORS_V1_JSON_TYPE
  };

  private JerseyUtils() {
  }

  public static boolean isError(final ClientResponse response) {
    return VND_ERROR_V1_JSON_TYPE.equals(response.getType());
  }

  public static boolean isValidationError(final ClientResponse response) {
    return VND_VALIDATION_ERRORS_V1_JSON_TYPE.equals(response.getType());
  }

  public static ClientResponse handle(final JerseyNexusClient nexusClient,
                                      final Callable<ClientResponse> callable)
  {
    try {
      final ClientResponse response = callable.call();
      response.bufferEntity();
      if (isValidationError(response)) {
        throw new ValidationErrorsException()
            .withErrors(response.getEntity(new GenericType<List<ValidationErrorXO>>()
            {
            }));
      }
      if (isError(response)) {
        final ErrorXO error = response.getEntity(ErrorXO.class);
        throw nexusClient.convert(
            new ContextAwareUniformInterfaceException(response)
            {
              @Override
              public String getMessage(final int status) {
                return error.getMessage() + " (" + error.getId() + ")";
              }
            }
        );
      }
      if (!Response.Status.Family.SUCCESSFUL.equals(response.getClientResponseStatus().getFamily())) {
        throw nexusClient.convert(new UniformInterfaceException(response));
      }
      response.close();
      return response;
    }
    catch (ClientHandlerException e) {
      throw new NexusClientHandlerException(e);
    }
    catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  public static <T> T handle(final JerseyNexusClient nexusClient,
                             final Callable<ClientResponse> callable,
                             final Class<T> expectedEntityType)
  {
    return getEntity(nexusClient, handle(nexusClient, callable), expectedEntityType);
  }

  public static <T> T handle(final JerseyNexusClient nexusClient,
                             final Callable<ClientResponse> callable,
                             final GenericType<T> expectedEntityType)
  {
    return getEntity(nexusClient, handle(nexusClient, callable), expectedEntityType);
  }

  public static <T> T getEntity(final JerseyNexusClient nexusClient,
                                final ClientResponse response,
                                final Class<T> expectedEntityType)
  {
    try {
      return response.getEntity(expectedEntityType);
    }
    catch (ClientHandlerException e) {
      throw nexusClient.convert(e);
    }
    catch (UniformInterfaceException e) {
      throw nexusClient.convert(e);
    }
  }

  public static <T> T getEntity(final JerseyNexusClient nexusClient,
                                final ClientResponse response,
                                final GenericType<T> expectedEntityType)
  {
    try {
      return response.getEntity(expectedEntityType);
    }
    catch (ClientHandlerException e) {
      throw nexusClient.convert(e);
    }
    catch (UniformInterfaceException e) {
      throw nexusClient.convert(e);
    }
  }

}
