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
import java.util.Set;

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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.entity.ContinuationTokenHelper;
import org.sonatype.nexus.repository.browse.BrowseService;
import org.sonatype.nexus.repository.maintenance.MaintenanceService;
import org.sonatype.nexus.repository.rest.ComponentUploadExtension;
import org.sonatype.nexus.repository.rest.ComponentsResourceExtension;
import org.sonatype.nexus.repository.rest.api.ComponentXO;
import org.sonatype.nexus.repository.rest.api.ComponentXOFactory;
import org.sonatype.nexus.repository.storage.ComponentEntityAdapter;
import org.sonatype.nexus.repository.upload.UploadConfiguration;
import org.sonatype.nexus.repository.upload.UploadManager;
import org.sonatype.nexus.rest.Page;
import org.sonatype.nexus.rest.Resource;

import org.jboss.resteasy.plugins.providers.multipart.MultipartInput;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.sonatype.nexus.rest.APIConstants.BETA_API_PREFIX;

/**
 * @deprecated since 3.14, use {@link ComponentsResource} instead.
 */
@Deprecated
@Named
@Singleton
@Path(ComponentsResourceBeta.RESOURCE_URI)
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class ComponentsResourceBeta
    extends ComponentSupport
    implements Resource
{
  static final String RESOURCE_URI = BETA_API_PREFIX + "/components";

  private final ComponentsResource delegate;

  @Inject
  public ComponentsResourceBeta(
      final RepositoryManagerRESTAdapter repositoryManagerRESTAdapter,
      final BrowseService browseService,
      final ComponentEntityAdapter componentEntityAdapter,
      final MaintenanceService maintenanceService,
      final ContinuationTokenHelper continuationTokenHelper,
      final UploadManager uploadManager,
      final UploadConfiguration uploadConfiguration,
      final ComponentXOFactory componentXOFactory,
      final Set<ComponentsResourceExtension> componentsResourceExtensions,
      final Set<ComponentUploadExtension> componentsUploadExtensions)
  {
    delegate = new ComponentsResource(repositoryManagerRESTAdapter, browseService, componentEntityAdapter,
        maintenanceService, continuationTokenHelper, uploadManager, uploadConfiguration, componentXOFactory,
        componentsResourceExtensions, componentsUploadExtensions);
  }

  @GET
  public Page<ComponentXO> getComponents(@QueryParam("continuationToken") final String continuationToken,
                                         @QueryParam("repository") final String repositoryId)
  {
    log.warn("Deprecated endpoint: {}, please use: {}", RESOURCE_URI, ComponentsResource.RESOURCE_URI);
    return delegate.getComponents(continuationToken, repositoryId);
  }

  @GET
  @Path("/{id}")
  public ComponentXO getComponentById(@PathParam("id") final String id)
  {
    log.warn("Deprecated endpoint: {}, please use: {}", RESOURCE_URI, ComponentsResource.RESOURCE_URI);
    return delegate.getComponentById(id);
  }

  @DELETE
  @Path("/{id}")
  public void deleteComponent(@PathParam("id") final String id)
  {
    log.warn("Deprecated endpoint: {}, please use: {}", RESOURCE_URI, ComponentsResource.RESOURCE_URI);
    delegate.deleteComponent(id);
  }

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public void uploadComponent(@QueryParam("repository") final String repositoryId, final MultipartInput multipartInput)
      throws IOException
  {
    log.warn("Deprecated endpoint: {}, please use: {}", RESOURCE_URI, ComponentsResource.RESOURCE_URI);
    delegate.uploadComponent(repositoryId, multipartInput);
  }
}
