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
package org.sonatype.nexus.repository.rest.api;

import java.util.StringJoiner;

import javax.inject.Inject;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.rest.api.model.AbstractRepositoryApiRequest;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.rest.ValidationErrorsException;
import org.sonatype.nexus.rest.WebApplicationMessageException;
import org.sonatype.nexus.validation.Validate;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.annotation.RequiresAuthentication;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

/**
 * @since 3.20
 */
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public abstract class AbstractRepositoriesApiResource<T extends AbstractRepositoryApiRequest>
    extends ComponentSupport
    implements Resource
{
  private final AuthorizingRepositoryManager authorizingRepositoryManager;

  private final AbstractRepositoryApiRequestToConfigurationConverter<T> configurationAdapter;

  @Inject
  public AbstractRepositoriesApiResource(
      final AuthorizingRepositoryManager authorizingRepositoryManager,
      final AbstractRepositoryApiRequestToConfigurationConverter<T> configurationAdapter)
  {
    this.authorizingRepositoryManager = checkNotNull(authorizingRepositoryManager);
    this.configurationAdapter = checkNotNull(configurationAdapter);
  }

  @POST
  @RequiresAuthentication
  @Validate
  public Response createRepository(@NotNull @Valid final T request) {
    try {
      authorizingRepositoryManager.create(configurationAdapter.convert(request));
      return Response.status(Status.CREATED).build();
    }
    catch (AuthorizationException | AuthenticationException | ConstraintViolationException e) {
      throw e;
    }
    catch (Exception e) {
      StringJoiner stringJoiner = new StringJoiner("\n", "\"", "\"");
      stringJoiner.add(e.getMessage());
      for (Throwable t : e.getSuppressed()) {
        stringJoiner.add(t.getMessage());
      }
      String message = stringJoiner.toString();
      log.debug("Failed to create a new repository via REST: {}", message, e);
      throw new WebApplicationMessageException(BAD_REQUEST, message, APPLICATION_JSON);
    }
  }

  @PUT
  @Path("/{repositoryName}")
  @RequiresAuthentication
  @Validate
  public Response updateRepository(
      @NotNull @Valid final T request,
      @PathParam("repositoryName") final String repositoryName)
  {
    try {
      Configuration newConfiguration = configurationAdapter.convert(request);
      ensureRepositoryNameMatches(request, repositoryName);
      boolean updated = authorizingRepositoryManager.update(newConfiguration);
      Status status = updated ? Status.NO_CONTENT : Status.NOT_FOUND;
      return Response.status(status).build();
    }
    catch (AuthorizationException | AuthenticationException | ConstraintViolationException e) {
      throw e;
    }
    catch (Exception e) {
      StringJoiner stringJoiner = new StringJoiner("\n", "\"", "\"");
      stringJoiner.add(e.getMessage());
      for (Throwable t : e.getSuppressed()) {
        stringJoiner.add(t.getMessage());
      }
      String message = stringJoiner.toString();
      log.debug("Failed to edit a repository via REST: {}", message, e);
      throw new WebApplicationMessageException(BAD_REQUEST, message, APPLICATION_JSON);
    }
  }

  private void ensureRepositoryNameMatches(final T request, final String repositoryName) {
    if (!repositoryName.equals(request.getName())) {
      throw new ValidationErrorsException("name", "Renaming a repository is not supported");
    }
  }
}
