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

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import org.sonatype.nexus.repository.manager.RepositoryDeletionService;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.rest.WebApplicationMessageException;

import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.NO_CONTENT;

/**
 * @since 3.20
 */
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class RepositoriesApiResource
    implements Resource, RepositoriesApiResourceDoc
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final AuthorizingRepositoryManager authorizingRepositoryManager;

  private final RepositoryDeletionService repositoryDeletionService;

  @Autowired
  public RepositoriesApiResource(
      final AuthorizingRepositoryManager authorizingRepositoryManager,
      final RepositoryDeletionService repositoryDeletionService)
  {
    this.authorizingRepositoryManager = requireNonNull(authorizingRepositoryManager);
    this.repositoryDeletionService = requireNonNull(repositoryDeletionService);
  }

  @Override
  @DELETE
  @Path("/{repositoryName}")
  @RequiresAuthentication
  public Response deleteRepository(@PathParam("repositoryName") final String repositoryName) throws Exception {
    // The soft-delete-vs-hard-delete decision and the per-repository DELETE authorization are owned
    // by RepositoryDeletionService (NEXUS-49147). Authorization/validation failures and missing
    // repositories map to 404, preserving this endpoint's existing contract.
    try {
      repositoryDeletionService.delete(repositoryName);
      return Response.status(NO_CONTENT).build();
    }
    catch (AuthorizationException | IllegalArgumentException | IllegalStateException e) {
      // MissingRepositoryException extends IllegalStateException and is covered above.
      return Response.status(NOT_FOUND).build();
    }
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
