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
package org.sonatype.nexus.repository.browse.internal.resources;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.browse.api.ComponentXO;
import org.sonatype.nexus.rest.Page;
import org.sonatype.nexus.rest.Resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.sonatype.nexus.repository.browse.internal.resources.ComponentsResource.RESOURCE_URI;

/**
 * @since 3.4
 */
@Named
@Singleton
@Path(RESOURCE_URI)
@Api(value = "components", description = "Operations to get and delete components")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class ComponentsResource
    extends ComponentSupport
    implements Resource
{
  public static final String RESOURCE_URI = "/rest/v1/components";

  @GET
  @ApiOperation("List components")
  public Page<ComponentXO> getComponents(
      @ApiParam(value = "A token returned by a prior request. If present, the next page of results are returned")
      @QueryParam("continuationToken")
      final String continuationToken,

      @ApiParam(value = "ID of the repository from which you would like to retrieve components", required = true)
      @QueryParam("repositoryId")
      final String repositoryId)
  {
    throw new WebApplicationException(501);
  }

  @GET
  @Path("/{id}")
  @ApiOperation("Get a single component")
  @ApiResponses(value = {
      @ApiResponse(code = 404, message = "Component not found")
  })
  public ComponentXO getComponentById(@ApiParam(value = "ID of the component to retrieve")
                                      @PathParam("id")
                                      final String id)
  {
    throw new WebApplicationException(501);
  }

  @DELETE
  @Path("/{id}")
  @ApiOperation(value = "Delete a single component")
  @ApiResponses(value = {
      @ApiResponse(code = 404, message = "Component not found")
  })
  public void deleteComponent(@ApiParam(value = "ID of the component to delete")
                              @PathParam("id")
                              final String id)
  {
    throw new WebApplicationException(501);
  }
}
