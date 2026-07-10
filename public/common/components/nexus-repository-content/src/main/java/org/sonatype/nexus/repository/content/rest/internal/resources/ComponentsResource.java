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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

import org.sonatype.nexus.blobstore.api.BlobStoreWarmingUpException;
import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.common.stateguard.InvalidStateException;
import org.sonatype.nexus.repository.IllegalOperationException;
import org.sonatype.nexus.repository.MissingBlobException;
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
import org.sonatype.nexus.repository.upload.UploadManager;
import org.sonatype.nexus.repository.upload.UploadRepositoryContext;
import org.sonatype.nexus.rest.Page;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.rest.WebApplicationMessageException;

import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.sonatype.nexus.repository.content.rest.AssetXOBuilder.fromAsset;
import static org.sonatype.nexus.repository.content.store.InternalIds.internalComponentId;
import static org.sonatype.nexus.repository.content.store.InternalIds.toExternalId;
import static org.sonatype.nexus.repository.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.sonatype.nexus.repository.rest.api.RepositoryItemIDXO.fromString;
import static org.sonatype.nexus.rest.APIConstants.V1_API_PREFIX;
import static org.sonatype.nexus.security.internal.uploadermetadata.UploaderMetadataSecurityContributor.UPLOADER_METADATA_READ_PERMISSION;

/**
 * @since 3.24
 */
@Component
@Path(ComponentsResource.RESOURCE_URI)
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class ComponentsResource
    extends ComponentsResourceSupport
    implements Resource, ComponentsResourceDoc
{
  public static final String RESOURCE_URI = V1_API_PREFIX + "/components";

  private final RepositoryManagerRESTAdapter repositoryManagerRESTAdapter;

  private final MaintenanceService maintenanceService;

  private final UploadManager uploadManager;

  private final ComponentXOFactory componentXOFactory;

  private final Map<String, AssetXODescriptor> assetDescriptors;

  private final Set<ComponentsResourceExtension> componentsResourceExtensions;

  @Autowired
  public ComponentsResource(
      final RepositoryManagerRESTAdapter repositoryManagerRESTAdapter,
      final MaintenanceService maintenanceService,
      final UploadManager uploadManager,
      final ComponentXOFactory componentXOFactory,
      final ContentAuthHelper contentAuthHelper,
      final Set<ComponentsResourceExtension> componentsResourceExtensions,
      @Nullable final List<AssetXODescriptor> assetDescriptorsList)
  {
    super(contentAuthHelper, repositoryManagerRESTAdapter);
    this.repositoryManagerRESTAdapter = checkNotNull(repositoryManagerRESTAdapter);
    this.maintenanceService = checkNotNull(maintenanceService);
    this.uploadManager = checkNotNull(uploadManager);
    this.componentXOFactory = checkNotNull(componentXOFactory);
    this.componentsResourceExtensions = checkNotNull(componentsResourceExtensions);
    this.assetDescriptors = QualifierUtil.buildQualifierBeanMap(assetDescriptorsList);
  }

  /**
   * @since 3.26
   */
  @Override
  @GET
  public Page<ComponentXO> getComponents(
      @QueryParam("continuationToken") final String continuationToken,
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
    boolean uploaderVisible = SecurityUtils.getSubject().isPermitted(UPLOADER_METADATA_READ_PERMISSION);
    return fromComponent(getComponent(repositoryItemIDXO, repository), repository, uploaderVisible);
  }

  private FluentComponent getComponent(final RepositoryItemIDXO repositoryItemIDXO, final Repository repository) {
    try {
      return repository.facet(ContentFacet.class)
          .components()
          .find(new DetachedEntityId(repositoryItemIDXO.getId()))
          .filter(componentPermitted(repository.getFormat().getValue(), repository.getName()))
          .orElseThrow(
              () -> new NotFoundException("Unable to locate component with id " + repositoryItemIDXO.getValue()));
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
  public void uploadComponent(
      @QueryParam("repository") final String repositoryId,
      @Context final HttpServletRequest request) throws IOException
  {
    if (request.getContentType() == null || !request.getContentType().startsWith("multipart/")) {
      throw new WebApplicationMessageException(Status.BAD_REQUEST, "\"Expected multipart Content-Type\"",
          MediaType.APPLICATION_JSON);
    }

    Repository repository = repositoryManagerRESTAdapter.getRepository(repositoryId);

    // CLM-39871: bind the repository on the upload thread so the hosted-policy
    // enforcement ComponentUploadExtension (UiUploadEnforcementInterceptor) can
    // resolve it from validate(ComponentUpload). The UI service does the same
    // bind+clear pair; without it, REST uploads silently bypass the policy gate.
    // set() lives inside the try so any future code added between repository
    // resolution and this point cannot leave a stale binding on the thread when
    // it throws — finally always clears.
    try {
      UploadRepositoryContext.set(repository);
      uploadManager.handle(repository, request);
    }
    catch (IllegalOperationException e) {
      throw new WebApplicationMessageException(Status.BAD_REQUEST, e.getMessage());
    }
    catch (BlobStoreWarmingUpException e) {
      // Blob store connection pool is still initializing (temporary, retry-able)
      log.info("Blob store '{}' warming up, returning 503", e.getBlobStoreName());
      throw new WebApplicationMessageException(Status.SERVICE_UNAVAILABLE,
          "\"Blob store warming up, please retry in a moment\"",
          MediaType.APPLICATION_JSON);
    }
    catch (MissingBlobException e) {
      // CRITICAL: Blob exists in metadata but missing from storage (data corruption, not retry-able)
      log.error("BLOB DATA LOSS: Blob {} missing from storage - data corruption", e.getBlobRef());
      throw new WebApplicationMessageException(Status.INTERNAL_SERVER_ERROR,
          "\"Blob missing from storage - possible data corruption\"",
          MediaType.APPLICATION_JSON);
    }
    catch (InvalidStateException e) {
      // Generic invalid state (e.g., stopped repository - not retry-able)
      log.warn("Invalid state: {}", e.getMessage());
      throw new WebApplicationMessageException(Status.INTERNAL_SERVER_ERROR,
          "\"" + e.getMessage() + "\"",
          MediaType.APPLICATION_JSON);
    }
    catch (RuntimeException e) {
      // CLM-39871: hosted-policy block / unavailable surfaces as a RuntimeException
      // whose message is prefixed with the HOSTED_ENFORCEMENT:: contract. Detect by
      // the prefix because the throwing exception class lives in a private module
      // that the public REST module cannot depend on. Strip the prefix and pick the
      // HTTP status from the errorCode in the JSON envelope: BLOCKED → 403 (permanent
      // policy rejection, do not retry); UNAVAILABLE → 503 (transient IQ issue, safe
      // to retry). Routing UNAVAILABLE through 403 would tell CI pipelines that treat
      // 403 as fatal to stop retrying on every transient IQ blip.
      String message = e.getMessage();
      if (message != null && message.startsWith("HOSTED_ENFORCEMENT::")) {
        String json = message.substring("HOSTED_ENFORCEMENT::".length());
        Status status = json.contains("HOSTED_ENFORCEMENT_UNAVAILABLE")
            ? Status.SERVICE_UNAVAILABLE
            : Status.FORBIDDEN;
        throw new WebApplicationMessageException(status, json, MediaType.APPLICATION_JSON);
      }
      throw e;
    }
    finally {
      UploadRepositoryContext.clear();
    }
  }

  private List<ComponentXO> toComponentXOs(final List<FluentComponent> components, final Repository repository) {
    boolean uploaderVisible = SecurityUtils.getSubject().isPermitted(UPLOADER_METADATA_READ_PERMISSION);
    return components.stream()
        .map(component -> fromComponent(component, repository, uploaderVisible))
        .collect(toList());
  }

  private ComponentXO fromComponent(
      final FluentComponent component,
      final Repository repository,
      final boolean uploaderVisible)
  {
    String externalId = toExternalId(internalComponentId(component)).getValue();

    ComponentXO componentXO = componentXOFactory.createComponentXO();

    componentXO.setAssets(component.assets()
        .stream()
        .filter(assetPermitted(repository))
        .map(asset -> fromAsset(asset, repository, this.assetDescriptors, uploaderVisible))
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
    return size < PAGE_SIZE_LIMIT ? null : toExternalId(internalComponentId(components.get(size - 1))).getValue();
  }
}
