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
package org.sonatype.nexus.repository.apt.rest;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.validation.Valid;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.sonatype.nexus.repository.rest.api.AbstractRepositoriesApiResource;
import org.sonatype.nexus.repository.rest.api.AbstractRepositoryApiRequestToConfigurationConverter;
import org.sonatype.nexus.repository.rest.api.AuthorizingRepositoryManager;
import org.sonatype.nexus.repository.rest.api.RepositoriesApiResource;
import org.sonatype.nexus.rest.Resource;

import org.apache.shiro.authz.annotation.RequiresAuthentication;

/**
 * @since 3.next
 */
@Named
@Singleton
@Path(AptHostedRepositoriesApiResource.RESOURCE_URI)
public class AptHostedRepositoriesApiResource
    extends AbstractRepositoriesApiResource<AptHostedRepositoryApiRequest>
    implements Resource, AptHostedRepositoriesApiResourceDoc
{
  public static final String RESOURCE_URI = RepositoriesApiResource.RESOURCE_URI + "/apt/hosted";

  @Inject
  public AptHostedRepositoriesApiResource(
      final AuthorizingRepositoryManager authorizingRepositoryManager,
      final Provider<Validator> validatorProvider,
      final AbstractRepositoryApiRequestToConfigurationConverter<AptHostedRepositoryApiRequest> configurationAdapter)
  {
    super(authorizingRepositoryManager, validatorProvider, configurationAdapter);
  }

  @POST
  @Path("/")
  @RequiresAuthentication
  public Response createHostedRepository(@Valid @NotNull final AptHostedRepositoryApiRequest request) {
    return createRepository(request);
  }

  @PUT
  @Path("/{repositoryName}")
  @RequiresAuthentication
  public Response updateHostedRepository(
      @Valid @NotNull final AptHostedRepositoryApiRequest request,
      @PathParam("repositoryName") final String repositoryName)
  {
    Status status = updateRepository(request) ? Status.NO_CONTENT : Status.NOT_FOUND;
    return Response.status(status).build();
  }
}
