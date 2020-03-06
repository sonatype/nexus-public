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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.rest.api.model.AbstractApiRepository;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.rest.WebApplicationMessageException;

import org.apache.shiro.authz.annotation.RequiresAuthentication;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.sonatype.nexus.rest.APIConstants.BETA_API_PREFIX;

/**
 * @since 3.20
 */
@Named
@Singleton
@Path(RepositoriesApiResource.RESOURCE_URI)
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class RepositoriesApiResource
    extends ComponentSupport
    implements Resource, RepositoriesApiResourceDoc
{
  public static final String RESOURCE_URI = BETA_API_PREFIX + "/repositories";

  private final AuthorizingRepositoryManager authorizingRepositoryManager;

  private final Map<String, ApiRepositoryAdapter> convertersByFormat;

  private final ApiRepositoryAdapter defaultAdapter;

  @Inject
  public RepositoriesApiResource(
      final AuthorizingRepositoryManager authorizingRepositoryManager,
      @Named("default") final ApiRepositoryAdapter defaultAdapter,
      final Map<String, ApiRepositoryAdapter> convertersByFormat)
  {
    this.authorizingRepositoryManager = checkNotNull(authorizingRepositoryManager);
    this.defaultAdapter = checkNotNull(defaultAdapter);
    this.convertersByFormat = checkNotNull(convertersByFormat);
  }

  @Override
  @DELETE
  @Path("/{repositoryName}")
  @RequiresAuthentication
  public Response deleteRepository(@PathParam("repositoryName") final String repositoryName) throws Exception {
    boolean isDeleted = authorizingRepositoryManager.delete(repositoryName);
    return Response.status(isDeleted ? NO_CONTENT : NOT_FOUND).build();
  }

  @Override
  @RequiresAuthentication
  @GET
  public List<AbstractApiRepository> getRepositories() {
    return authorizingRepositoryManager.getRepositoriesWithAdmin().stream().map(this::convert)
        .collect(Collectors.toList());
  }

  private AbstractApiRepository convert(final Repository repository) {
    return convertersByFormat.getOrDefault(repository.getFormat().toString(), defaultAdapter).adapt(repository);
  }

  @POST
  @Path("/{repositoryName}/rebuild-index")
  @RequiresAuthentication
  public void rebuildIndex(@PathParam("repositoryName") final String repositoryName) {
    try {
      authorizingRepositoryManager.rebuildSearchIndex(repositoryName);
    }
    catch (IncompatibleRepositoryException e) {
      log.debug("Not a hosted or proxy repository '{}'", repositoryName, e);
      throw new WebApplicationMessageException(BAD_REQUEST, "\"" + e.getMessage() + "\"", APPLICATION_JSON);
    }
    catch (RepositoryNotFoundException e) {
      log.debug("Repository not found '{}'", repositoryName, e);
      throw new WebApplicationMessageException(NOT_FOUND, "\"" + e.getMessage() + "\"", APPLICATION_JSON);
    }
  }

  @POST
  @Path("/{repositoryName}/invalidate-cache")
  @RequiresAuthentication
  public void invalidateCache(@PathParam("repositoryName") final String repositoryName) {
    try {
      authorizingRepositoryManager.invalidateCache(repositoryName);
    }
    catch (IncompatibleRepositoryException e) {
      log.debug("Not a proxy nor group repository '{}'", repositoryName, e);
      throw new WebApplicationMessageException(BAD_REQUEST, "\"" + e.getMessage() + "\"", APPLICATION_JSON);
    }
    catch (RepositoryNotFoundException e) {
      log.debug("Repository not found '{}'", repositoryName, e);
      throw new WebApplicationMessageException(NOT_FOUND, "\"" + e.getMessage() + "\"", APPLICATION_JSON);
    }
  }
}
