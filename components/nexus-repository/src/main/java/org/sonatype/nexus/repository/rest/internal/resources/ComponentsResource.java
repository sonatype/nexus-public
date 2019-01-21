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
package org.sonatype.nexus.repository.rest.internal.resources;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.entity.ContinuationTokenHelper;
import org.sonatype.nexus.common.entity.ContinuationTokenHelper.ContinuationTokenException;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.repository.IllegalOperationException;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.BrowseResult;
import org.sonatype.nexus.repository.browse.BrowseService;
import org.sonatype.nexus.repository.browse.QueryOptions;
import org.sonatype.nexus.repository.maintenance.MaintenanceService;
import org.sonatype.nexus.repository.rest.ComponentsResourceExtension;
import org.sonatype.nexus.repository.rest.api.ComponentXO;
import org.sonatype.nexus.repository.rest.api.ComponentXOFactory;
import org.sonatype.nexus.repository.rest.internal.api.RepositoryItemIDXO;
import org.sonatype.nexus.repository.rest.internal.resources.doc.ComponentsResourceDoc;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentEntityAdapter;
import org.sonatype.nexus.repository.upload.UploadConfiguration;
import org.sonatype.nexus.repository.upload.UploadManager;
import org.sonatype.nexus.rest.Page;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.rest.WebApplicationMessageException;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.getLast;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.sonatype.nexus.common.entity.EntityHelper.id;
import static org.sonatype.nexus.repository.http.HttpStatus.NOT_ACCEPTABLE;
import static org.sonatype.nexus.repository.http.HttpStatus.NOT_FOUND;
import static org.sonatype.nexus.repository.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.sonatype.nexus.repository.rest.api.AssetXO.fromAsset;
import static org.sonatype.nexus.repository.rest.internal.api.RepositoryItemIDXO.fromString;
import static org.sonatype.nexus.rest.APIConstants.V1_API_PREFIX;

/**
 * @since 3.4
 */
@Named
@Singleton
@Path(ComponentsResource.RESOURCE_URI)
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class ComponentsResource
    extends ComponentSupport
    implements Resource, ComponentsResourceDoc
{

  public static final String RESOURCE_URI = V1_API_PREFIX + "/components";

  private final RepositoryManagerRESTAdapter repositoryManagerRESTAdapter;

  private final BrowseService browseService;

  private final ComponentEntityAdapter componentEntityAdapter;

  private final MaintenanceService maintenanceService;

  private final ContinuationTokenHelper continuationTokenHelper;

  private final UploadManager uploadManager;

  private final UploadConfiguration uploadConfiguration;

  private final ComponentXOFactory componentXOFactory;

  private final Set<ComponentsResourceExtension> componentsResourceExtensions;

  @Inject
  public ComponentsResource(final RepositoryManagerRESTAdapter repositoryManagerRESTAdapter,
                            final BrowseService browseService,
                            final ComponentEntityAdapter componentEntityAdapter,
                            final MaintenanceService maintenanceService,
                            @Named("component") final ContinuationTokenHelper continuationTokenHelper,
                            final UploadManager uploadManager,
                            final UploadConfiguration uploadConfiguration,
                            final ComponentXOFactory componentXOFactory,
                            final Set<ComponentsResourceExtension> componentsResourceExtensions)
  {
    this.repositoryManagerRESTAdapter = checkNotNull(repositoryManagerRESTAdapter);
    this.browseService = checkNotNull(browseService);
    this.componentEntityAdapter = checkNotNull(componentEntityAdapter);
    this.maintenanceService = checkNotNull(maintenanceService);
    this.continuationTokenHelper = checkNotNull(continuationTokenHelper);
    this.uploadManager = checkNotNull(uploadManager);
    this.uploadConfiguration = checkNotNull(uploadConfiguration);
    this.componentXOFactory = checkNotNull(componentXOFactory);
    this.componentsResourceExtensions = checkNotNull(componentsResourceExtensions);
  }

  @Override
  @GET
  public Page<ComponentXO> getComponents(@QueryParam("continuationToken") final String continuationToken,
                                         @QueryParam("repository") final String repositoryId)
  {
    Repository repository = repositoryManagerRESTAdapter.getRepository(repositoryId);

    //must explicitly order by id or the generate sql will automatically order on group/name/version. (see BrowseComponentsSqlBuider)
    BrowseResult<Component> componentBrowseResult = browseService
        .browseComponents(repository,
            new QueryOptions(null, "id", "asc", 0, 10, lastIdFromContinuationToken(continuationToken)));

    List<ComponentXO> componentXOs = componentBrowseResult.getResults().stream()
        .map(component -> fromComponent(component, repository))
        .collect(toList());

    return new Page<>(componentXOs, componentBrowseResult.getTotal() > componentBrowseResult.getResults().size() ?
        continuationTokenHelper.getTokenFromId(getLast(componentBrowseResult.getResults())) : null);
  }

  @Nullable
  private String lastIdFromContinuationToken(final String continuationToken) {
    try {
      return continuationTokenHelper.getIdFromToken(continuationToken);
    }
    catch (ContinuationTokenException e) {
      log.debug(e.getMessage(), e);
      throw new WebApplicationException(NOT_ACCEPTABLE);
    }
  }

  private ComponentXO fromComponent(final Component component, final Repository repository) {
    String internalId = id(component).getValue();

    ComponentXO componentXO = componentXOFactory.createComponentXO();

    componentXO
        .setAssets(browseService.browseComponentAssets(repository, component.getEntityMetadata().getId().getValue())
            .getResults()
            .stream()
            .map(asset -> fromAsset(asset, repository))
            .collect(toList()));

    componentXO.setGroup(component.group());
    componentXO.setName(component.name());
    componentXO.setVersion(component.version());
    componentXO.setId(new RepositoryItemIDXO(repository.getName(), internalId).getValue());
    componentXO.setRepository(repository.getName());
    componentXO.setFormat(repository.getFormat().getValue());

    for (ComponentsResourceExtension componentsResourceExtension : componentsResourceExtensions) {
      componentXO = componentsResourceExtension.updateComponentXO(componentXO, component);
    }

    return componentXO;
  }

  @Override
  @GET
  @Path("/{id}")
  public ComponentXO getComponentById(@PathParam("id") final String id)
  {
    RepositoryItemIDXO repositoryItemXOID = fromString(id);
    Repository repository = repositoryManagerRESTAdapter.getRepository(repositoryItemXOID.getRepositoryId());

    Component component = getComponent(repositoryItemXOID, repository);

    return fromComponent(component, repository);
  }

  private Component getComponent(final RepositoryItemIDXO repositoryItemIDXO, final Repository repository)
  {
    try {
      return ofNullable(browseService
          .getComponentById(componentEntityAdapter.recordIdentity(new DetachedEntityId(repositoryItemIDXO.getId())),
              repository)).orElseThrow(
          () -> new NotFoundException("Unable to locate component with id " + repositoryItemIDXO.getValue()));
    }
    catch (IllegalArgumentException e) {
      log.debug("IllegalArgumentException caught retrieving component with id {}", repositoryItemIDXO.getId(), e);
      throw new WebApplicationException(format("Unable to process component with id %s", repositoryItemIDXO.getId()),
          UNPROCESSABLE_ENTITY);
    }
  }

  @Override
  @DELETE
  @Path("/{id}")
  public void deleteComponent(@PathParam("id") final String id)
  {
    RepositoryItemIDXO repositoryItemIdXO = fromString(id);
    Repository repository = repositoryManagerRESTAdapter.getRepository(repositoryItemIdXO.getRepositoryId());

    Component component = getComponent(repositoryItemIdXO, repository);

    if (repository != null && component != null) {
      maintenanceService.deleteComponent(repository, component);
    }
  }

  /**
   * @since 3.8
   */
  @Override
  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public void uploadComponent(@QueryParam("repository") final String repositoryId,
                              @Context final HttpServletRequest request)
      throws IOException
  {
    if (!uploadConfiguration.isEnabled()) {
      throw new WebApplicationException(NOT_FOUND);
    }

    Repository repository = repositoryManagerRESTAdapter.getRepository(repositoryId);

    try {
      uploadManager.handle(repository, request);
    } catch (IllegalOperationException e) {
      throw new WebApplicationMessageException(Status.BAD_REQUEST, e.getMessage());
    }
  }
}
