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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.BrowseResult;
import org.sonatype.nexus.repository.browse.BrowseService;
import org.sonatype.nexus.repository.browse.QueryOptions;
import org.sonatype.nexus.repository.browse.api.ComponentXO;
import org.sonatype.nexus.repository.maintenance.MaintenanceService;
import org.sonatype.nexus.repository.browse.internal.api.RepositoryItemIDXO;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentEntityAdapter;
import org.sonatype.nexus.rest.Page;
import org.sonatype.nexus.rest.Resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.getLast;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.sonatype.nexus.common.entity.EntityHelper.id;
import static org.sonatype.nexus.repository.browse.api.AssetXO.fromAsset;
import static org.sonatype.nexus.repository.browse.internal.api.RepositoryItemIDXO.fromString;
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

  private final RepositoryManagerRESTAdapter repositoryManagerRESTAdapter;

  private final BrowseService browseService;

  private final ComponentEntityAdapter componentEntityAdapter;

  private final MaintenanceService maintenanceService;

  @Inject
  public ComponentsResource(final RepositoryManagerRESTAdapter repositoryManagerRESTAdapter,
                            final BrowseService browseService,
                            final ComponentEntityAdapter componentEntityAdapter,
                            final MaintenanceService maintenanceService)
  {
    this.repositoryManagerRESTAdapter = checkNotNull(repositoryManagerRESTAdapter);
    this.browseService = checkNotNull(browseService);
    this.componentEntityAdapter = checkNotNull(componentEntityAdapter);
    this.maintenanceService = checkNotNull(maintenanceService);
  }

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
    Repository repository = repositoryManagerRESTAdapter.getRepository(repositoryId);

    final String lastId;
    if (continuationToken != null) {
      lastId = componentEntityAdapter.recordIdentity(new DetachedEntityId(continuationToken)).toString();
    }
    else {
      lastId = null;
    }

    //must explicitly order by id or the generate sql will automatically order on group/name/version. (see BrowseComponentsSqlBuider)
    BrowseResult<Component> componentBrowseResult = browseService
        .browseComponents(repository, new QueryOptions(null, "id", "asc", 0, 10, lastId));

    List<ComponentXO> componentXOs = componentBrowseResult.getResults().stream()
        .map(component -> fromComponent(component, repository))
        .collect(toList());

    return new Page<>(componentXOs, componentBrowseResult.getTotal() > componentBrowseResult.getResults().size() ?
        id(getLast(componentBrowseResult.getResults())).getValue() : null);
  }

  private ComponentXO fromComponent(Component component, Repository repository) {
    String internalId = id(component).getValue();

    ComponentXO componentXO = new ComponentXO();


    componentXO
        .setAssets(browseService.browseComponentAssets(repository, component.getEntityMetadata().getId().getValue())
            .getResults()
            .stream()
            .map(asset -> fromAsset(asset, repository))
            .collect(toList()));

    componentXO.setCoordinates(getCoordinates(component));
    componentXO.setId(new RepositoryItemIDXO(repository.getName(), internalId).getValue());

    return componentXO;
  }

  private String getCoordinates(final Component component) {
    StringBuilder coordinatesBuilder = new StringBuilder();

    if (component.group() != null) {
      coordinatesBuilder.append(component.group()).append("/");
    }
    coordinatesBuilder.append(component.name());
    if (component.version() != null) {
      coordinatesBuilder.append("/").append(component.version());
    }
    return coordinatesBuilder.toString();
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
    RepositoryItemIDXO repositoryItemXOID = fromString(id);
    Repository repository = repositoryManagerRESTAdapter.getRepository(repositoryItemXOID.getRepositoryId());

    Component component = getComponent(repositoryItemXOID, repository);

    return fromComponent(component, repository);
  }

  private Component getComponent(final RepositoryItemIDXO repositoryItemIDXO, final Repository repository)
  {
    return ofNullable(browseService
        .getComponentById(componentEntityAdapter.recordIdentity(new DetachedEntityId(repositoryItemIDXO.getId())),
            repository)).orElseThrow(
        () -> new NotFoundException("Unable to locate component with id " + repositoryItemIDXO.getValue()));
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
    RepositoryItemIDXO repositoryItemIdXO = fromString(id);
    Repository repository = repositoryManagerRESTAdapter.getRepository(repositoryItemIdXO.getRepositoryId());

    Component component = getComponent(repositoryItemIdXO, repository);

    maintenanceService.deleteComponent(repository, component);
  }
}
