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
package org.sonatype.nexus.repository.content.rest.internal.resources;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.repository.IllegalOperationException;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.maintenance.MaintenanceService;
import org.sonatype.nexus.repository.content.rest.ComponentsResourceExtension;
import org.sonatype.nexus.repository.content.rest.internal.resources.doc.ComponentsResourceDoc;
import org.sonatype.nexus.repository.rest.api.AssetXODescriptor;
import org.sonatype.nexus.repository.rest.api.ComponentXO;
import org.sonatype.nexus.repository.rest.api.ComponentXOFactory;
import org.sonatype.nexus.repository.rest.api.RepositoryItemIDXO;
import org.sonatype.nexus.repository.rest.api.RepositoryManagerRESTAdapter;
import org.sonatype.nexus.repository.selector.ContentAuthHelper;
import org.sonatype.nexus.repository.upload.UploadConfiguration;
import org.sonatype.nexus.repository.upload.UploadManager;
import org.sonatype.nexus.rest.Page;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.rest.WebApplicationMessageException;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_ENABLED;
import static org.sonatype.nexus.repository.content.rest.AssetXOBuilder.fromAsset;
import static org.sonatype.nexus.repository.content.store.InternalIds.internalComponentId;
import static org.sonatype.nexus.repository.content.store.InternalIds.toExternalId;
import static org.sonatype.nexus.repository.http.HttpStatus.NOT_FOUND;
import static org.sonatype.nexus.repository.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.sonatype.nexus.repository.rest.api.RepositoryItemIDXO.fromString;
import static org.sonatype.nexus.rest.APIConstants.V1_API_PREFIX;

/**
 * @since 3.24
 */
@FeatureFlag(name = DATASTORE_ENABLED)
@Named
@Singleton
@Path(ComponentsResource.RESOURCE_URI)
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class ComponentsResource
    extends ComponentsResourceSupport
    implements Resource, ComponentsResourceDoc
{
  public static final String RESOURCE_URI = V1_API_PREFIX + "/components";

  static final int PAGE_SIZE = 10;

  private final RepositoryManagerRESTAdapter repositoryManagerRESTAdapter;

  private final MaintenanceService maintenanceService;

  private final UploadManager uploadManager;

  private final UploadConfiguration uploadConfiguration;

  private final ComponentXOFactory componentXOFactory;

  private final Map<String, AssetXODescriptor> assetDescriptors;

  private final Set<ComponentsResourceExtension> componentsResourceExtensions;

  @Inject
  public ComponentsResource(
      final RepositoryManagerRESTAdapter repositoryManagerRESTAdapter,
      final MaintenanceService maintenanceService,
      final UploadManager uploadManager,
      final UploadConfiguration uploadConfiguration,
      final ComponentXOFactory componentXOFactory,
      final ContentAuthHelper contentAuthHelper,
      final Set<ComponentsResourceExtension> componentsResourceExtensions,
      @Nullable final Map<String, AssetXODescriptor> assetDescriptors)
  {
    super(contentAuthHelper, repositoryManagerRESTAdapter);
    this.repositoryManagerRESTAdapter = checkNotNull(repositoryManagerRESTAdapter);
    this.maintenanceService = checkNotNull(maintenanceService);
    this.uploadManager = checkNotNull(uploadManager);
    this.uploadConfiguration = checkNotNull(uploadConfiguration);
    this.componentXOFactory = checkNotNull(componentXOFactory);
    this.componentsResourceExtensions = checkNotNull(componentsResourceExtensions);
    this.assetDescriptors = assetDescriptors;
  }

  /**
   * @since 3.26
   */
  @Override
  @GET
  public Page<ComponentXO> getComponents(@QueryParam("continuationToken") final String continuationToken,
                                         @QueryParam("repository") final String repositoryId)
  {
    Repository repository = repositoryManagerRESTAdapter.getRepository(repositoryId);
    List<FluentComponent> components = browse(repository, continuationToken);
    return new Page<>(toComponentXOs(components, repository), nextContinuationToken(components));
  }

  /**
   * @since 3.26
   */
  @Override
  @GET
  @Path("/{id}")
  public ComponentXO getComponentById(@PathParam("id") final String id) {
    RepositoryItemIDXO repositoryItemIDXO = fromString(id);
    Repository repository = repositoryManagerRESTAdapter.getRepository(repositoryItemIDXO.getRepositoryId());
    return fromComponent(getComponent(repositoryItemIDXO, repository), repository);
  }

  private FluentComponent getComponent(final RepositoryItemIDXO repositoryItemIDXO, final Repository repository) {
    try {
      return repository.facet(ContentFacet.class).components()
          .find(new DetachedEntityId(repositoryItemIDXO.getId()))
          .filter(componentPermitted(repository.getFormat().getValue(), repository.getName()))
          .orElseThrow(() ->
              new NotFoundException("Unable to locate component with id " + repositoryItemIDXO.getValue()));
    }
    catch (IllegalArgumentException e) {
      log.debug("IllegalArgumentException caught retrieving component with id {}", repositoryItemIDXO.getId(), e);
      throw new WebApplicationException(format("Unable to process component with id %s", repositoryItemIDXO.getId()),
          UNPROCESSABLE_ENTITY);
    }
  }

  /**
   * @since 3.26
   */
  @Override
  @DELETE
  @Path("/{id}")
  public void deleteComponent(@PathParam("id") final String id) {
    RepositoryItemIDXO repositoryItemIdXO = fromString(id);
    Repository repository = repositoryManagerRESTAdapter.getRepository(repositoryItemIdXO.getRepositoryId());

    ofNullable(repository)
        .map(r -> getComponent(repositoryItemIdXO, r))
        .ifPresent(c -> maintenanceService.deleteComponent(repository, c));
  }

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
    if (request.getContentType() == null || !request.getContentType().startsWith("multipart/")) {
      throw new WebApplicationMessageException(Status.BAD_REQUEST, "\"Expected multipart Content-Type\"",
          MediaType.APPLICATION_JSON);
    }

    Repository repository = repositoryManagerRESTAdapter.getRepository(repositoryId);

    try {
      uploadManager.handle(repository, request);
    } catch (IllegalOperationException e) {
      throw new WebApplicationMessageException(Status.BAD_REQUEST, e.getMessage());
    }
  }
  private List<ComponentXO> toComponentXOs(final List<FluentComponent> components, final Repository repository) {
    return components.stream()
        .map(component -> fromComponent(component, repository))
        .collect(toList());
  }

  private ComponentXO fromComponent(final FluentComponent component, final Repository repository) {
    String externalId = toExternalId(internalComponentId(component)).getValue();

    ComponentXO componentXO = componentXOFactory.createComponentXO();

    componentXO.setAssets(component.assets().stream()
        .filter(assetPermitted(repository))
        .map(asset -> fromAsset(asset, repository, this.assetDescriptors))
        .collect(Collectors.toList()));

    componentXO.setGroup(component.namespace());
    componentXO.setName(component.name());
    componentXO.setVersion(component.version());
    componentXO.setId(new RepositoryItemIDXO(repository.getName(), externalId).getValue());
    componentXO.setRepository(repository.getName());
    componentXO.setFormat(repository.getFormat().getValue());

    for (ComponentsResourceExtension componentsResourceExtension : componentsResourceExtensions) {
      componentXO = componentsResourceExtension.updateComponentXO(componentXO, component);
    }

    return componentXO;
  }

  private static String nextContinuationToken(final List<FluentComponent> components) {
    int size = components.size();
    return size < PAGE_SIZE ? null : toExternalId(internalComponentId(components.get(size - 1))).getValue();
  }
}
