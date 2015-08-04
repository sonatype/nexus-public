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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.sonatype.nexus.client.core.Condition;
import org.sonatype.nexus.client.core.NexusClient;
import org.sonatype.nexus.client.core.NexusStatus;
import org.sonatype.nexus.client.core.exception.NexusClientAccessForbiddenException;
import org.sonatype.nexus.client.core.exception.NexusClientBadRequestException;
import org.sonatype.nexus.client.core.exception.NexusClientErrorResponseException;
import org.sonatype.nexus.client.core.exception.NexusClientException;
import org.sonatype.nexus.client.core.exception.NexusClientNotFoundException;
import org.sonatype.nexus.client.core.exception.NexusClientResponseException;
import org.sonatype.nexus.client.core.spi.SubsystemProvider;
import org.sonatype.nexus.client.internal.msg.ErrorMessage;
import org.sonatype.nexus.client.internal.msg.ErrorResponse;
import org.sonatype.nexus.client.internal.rest.AbstractXStreamNexusClient;
import org.sonatype.nexus.client.internal.util.Check;
import org.sonatype.nexus.client.rest.ConnectionInfo;
import org.sonatype.nexus.rest.model.StatusResource;
import org.sonatype.nexus.rest.model.StatusResourceResponse;
import org.sonatype.sisu.siesta.client.ClientBuilder;
import org.sonatype.sisu.siesta.client.ClientBuilder.Target.Factory;

import com.google.common.collect.Maps;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.thoughtworks.xstream.XStream;
import org.apache.commons.io.IOUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Jersey client with some extra fluff: it maintains reference to XStream used by Provider it uses, to make it able to
 * pass XStream around (toward subsystems) to apply needed XStream configuration. As Nexus currently is married to
 * XStream, this will probably change, hence, this class, as one of the implementations keeps the fact of XStream use
 * encapsulated, I did not want to proliferate it through all of Nexus Client. This class should not be instantiated
 * manually, use {@link JerseyNexusClientFactory} for it.
 *
 * @since 2.1
 */
public class JerseyNexusClient
    extends AbstractXStreamNexusClient
{

  private Client client;

  private final MediaType mediaType;

  private final List<SubsystemProvider> subsystemProviders;

  private final Map<Object, Object> context;

  public JerseyNexusClient(final Condition connectionCondition,
                           final List<SubsystemProvider> subsystemProviders,
                           final ConnectionInfo connectionInfo,
                           final XStream xstream,
                           final Client client,
                           final MediaType mediaType)
  {
    super(connectionInfo, xstream);
    this.client = Check.notNull(client, Client.class);
    this.mediaType = Check.notNull(mediaType, MediaType.class);
    this.subsystemProviders = subsystemProviders;
    getLogger().debug("Client created for media-type {} and connection {}", mediaType, connectionInfo);

    initializeConnection(connectionCondition);

    context = Maps.newHashMap();
    context.put(NexusClient.class, this);
    context.put(Factory.class, ClientBuilder.using(client).toAccess(connectionInfo.getBaseUrl().toUrl()));
  }

  public Client getClient() {
    return client;
  }

  public MediaType getMediaType() {
    return mediaType;
  }

  public String resolvePath(final String path) {
    // we need more logic here, but for now will do it ;)
    return getConnectionInfo().getBaseUrl() + path;
  }

  public String resolveServicePath(final String path) {
    // we need more logic here, but for now will do it ;)
    return resolvePath("service/local/" + path);
  }

  public WebResource.Builder serviceResource(final String uri) {
    return getClient()
        .resource(resolveServicePath(uri))
        .type(getMediaType())
        .accept(getMediaType());
  }

  public WebResource.Builder serviceResource(final String uri, final MultivaluedMap<String, String> queryParameters) {
    return getClient()
        .resource(resolveServicePath(uri))
        .queryParams(queryParameters)
        .type(getMediaType())
        .accept(getMediaType());
  }

  public WebResource.Builder uri(final String uri) {
    return getClient()
        .resource(resolvePath(uri))
        .getRequestBuilder();
  }

  public WebResource.Builder uri(final String uri, final MultivaluedMap<String, String> queryParameters) {
    return getClient()
        .resource(resolvePath(uri))
        .queryParams(queryParameters)
        .getRequestBuilder();
  }

  @Override
  public NexusStatus getStatus() {
    try {
      final StatusResource response = serviceResource("status")
          .get(StatusResourceResponse.class)
          .getData();
      return new NexusStatus(response.getAppName(), response.getFormattedAppName(), response.getVersion(),
          response.getApiVersion(), response.getEditionLong(), response.getEditionShort(), response.getState(),
          response.getInitializedAt(), response.getStartedAt(), response.getLastConfigChange(), -1,
          response.isFirstStart(), response.isInstanceUpgraded(), response.isConfigurationUpgraded(),
          response.getBaseUrl());
    }
    catch (UniformInterfaceException e) {
      throw convert(e);
    }
    catch (ClientHandlerException e) {
      throw convert(e);
    }
  }

  @Override
  public synchronized void close() {
    try {
      if (client != null) {
        client.destroy();
        client = null;
      }
    }
    finally {
      super.close();
    }
  }

  // ==

  @Override
  protected <S> S createSubsystem(final Class<S> subsystemType)
      throws IllegalArgumentException
  {
    checkNotNull(subsystemType, "subsystemType cannot be null");
    for (final SubsystemProvider subsystemProvider : subsystemProviders) {
      final Object subsystem = subsystemProvider.get(subsystemType, context);
      if (subsystem != null) {
        checkState(
            subsystemType.isAssignableFrom(subsystem.getClass()),
            "Subsystem '%s' created by '%s' is not an instance of '%s'",
            subsystem, subsystemProvider, subsystemType.getSimpleName()
        );
        return subsystemType.cast(subsystem);
      }
    }
    throw new IllegalArgumentException(
        "No " + SubsystemProvider.class.getName() + " was able to create a subsystem of type"
            + subsystemType.getName()
    );
  }

  public NexusClientNotFoundException convertIf404(final UniformInterfaceException e) {
    final ClientResponse response = e.getResponse();
    if (ClientResponse.Status.NOT_FOUND.equals(response.getClientResponseStatus())) {
      return new NexusClientNotFoundException(
          getMessageIfPresent(ClientResponse.Status.NOT_FOUND.getStatusCode(), e),
          response.getClientResponseStatus().getReasonPhrase(),
          getResponseBody(response)
      );
    }
    return null;
  }

  public NexusClientBadRequestException convertIf400(final UniformInterfaceException e) {
    final ClientResponse response = e.getResponse();
    if (ClientResponse.Status.BAD_REQUEST.equals(response.getClientResponseStatus())) {
      return new NexusClientBadRequestException(
          getMessageIfPresent(ClientResponse.Status.BAD_REQUEST.getStatusCode(), e),
          response.getClientResponseStatus().getReasonPhrase(),
          getResponseBody(response)
      );
    }
    return null;
  }

  public NexusClientAccessForbiddenException convertIf403(final UniformInterfaceException e) {
    final ClientResponse response = e.getResponse();
    if (ClientResponse.Status.FORBIDDEN.equals(response.getClientResponseStatus())) {
      return new NexusClientAccessForbiddenException(
          getMessageIfPresent(ClientResponse.Status.FORBIDDEN.getStatusCode(), e),
          response.getClientResponseStatus().getReasonPhrase(),
          getResponseBody(response)
      );
    }
    return null;
  }

  public NexusClientErrorResponseException convertIf400WithErrorMessage(final UniformInterfaceException e) {
    final ClientResponse response = e.getResponse();
    if (ClientResponse.Status.BAD_REQUEST.equals(response.getClientResponseStatus())) {
      final String body = getResponseBody(response);
      ErrorResponse errorResponse = null;
      try {
        errorResponse = (ErrorResponse) getXStream().fromXML(body, new ErrorResponse());
      }
      catch (Exception e1) {
        // ignore
        // XStreamException if body is not ErrorResponse
      }
      if (errorResponse != null) {
        // convert them to hide stupid "old" REST model, and not have it leak out
        final ArrayList<NexusClientErrorResponseException.ErrorMessage> errors =
            new ArrayList<NexusClientErrorResponseException.ErrorMessage>(errorResponse.getErrors().size());
        for (ErrorMessage message : errorResponse.getErrors()) {
          errors.add(
              new NexusClientErrorResponseException.ErrorMessage(message.getId(), message.getMsg()));
        }
        return new NexusClientErrorResponseException(
            response.getClientResponseStatus().getReasonPhrase(),
            body,
            errors
        );
      }
    }
    return null;
  }

  public NexusClientException convert(final UniformInterfaceException e) {
    NexusClientException exception = convertIfKnown(e);
    if (exception != null) {
      return exception;
    }

    return new NexusClientResponseException(
        getMessageIfPresent(e.getResponse().getClientResponseStatus().getStatusCode(), e),
        e.getResponse().getClientResponseStatus().getStatusCode(),
        e.getResponse().getClientResponseStatus().getReasonPhrase(),
        getResponseBody(e.getResponse())
    );
  }

  public NexusClientException convertIfKnown(final UniformInterfaceException e) {
    NexusClientException exception = convertIf404(e);
    if (exception != null) {
      return exception;
    }

    exception = convertIf403(e);
    if (exception != null) {
      return exception;
    }

    exception = convertIf400WithErrorMessage(e);
    if (exception != null) {
      return exception;
    }

    exception = convertIf400(e);
    if (exception != null) {
      return exception;
    }

    return null;
  }

  public NexusClientException convert(final ClientHandlerException e) {
    throw new NexusClientHandlerException(e);
  }

  public String getResponseBody(final ClientResponse response) {
    try {
      final byte[] body = IOUtils.toByteArray(response.getEntityInputStream());
      response.setEntityInputStream(new ByteArrayInputStream(body));
      return IOUtils.toString(body, "UTF-8");
    }
    catch (IOException e) {
      throw new IllegalStateException("Jersey unexpectedly refused to rewind buffered entity.");
    }
  }

  private String getMessageIfPresent(final int status, final UniformInterfaceException e) {
    if (e instanceof ContextAwareUniformInterfaceException) {
      return ((ContextAwareUniformInterfaceException) e).getMessage(status);
    }
    return null;
  }

}
