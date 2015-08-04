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
package org.sonatype.nexus.rest.mirrors;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.repositories.metadata.NexusRepositoryMetadataHandler;
import org.sonatype.nexus.repository.metadata.MetadataHandlerException;
import org.sonatype.nexus.repository.metadata.model.RepositoryMetadata;
import org.sonatype.nexus.repository.metadata.model.RepositoryMirrorMetadata;
import org.sonatype.nexus.rest.model.MirrorResource;
import org.sonatype.nexus.rest.model.MirrorResourceListResponse;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

@Named
@Singleton
@Path(RepositoryPredefinedMirrorListPlexusResource.RESOURCE_URI)
@Consumes({"application/xml", "application/json"})
public class RepositoryPredefinedMirrorListPlexusResource
    extends AbstractRepositoryMirrorPlexusResource
{
  public static final String RESOURCE_URI = "/repository_predefined_mirrors/{" + REPOSITORY_ID_KEY + "}";

  private final NexusRepositoryMetadataHandler repoMetadata;

  @Inject
  public RepositoryPredefinedMirrorListPlexusResource(final NexusRepositoryMetadataHandler repoMetadata) {
    this.repoMetadata = repoMetadata;
    setModifiable(false);
  }

  @Override
  public Object getPayloadInstance() {
    return null;
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor("/repository_predefined_mirrors/*",
        "authcBasic,perms[nexus:repositorypredefinedmirrors]");
  }

  @Override
  public String getResourceUri() {
    return RESOURCE_URI;
  }

  /**
   * Get the predefined list of mirrors for a repository (as defined in the repository metadata).  These
   * mirrors may NOT be enabled, simply listed as available.
   *
   * @param repositoryId The repository to retrieve the predefined mirrors for.
   */
  @Override
  @GET
  @ResourceMethodSignature(pathParams = {@PathParam(AbstractRepositoryMirrorPlexusResource.REPOSITORY_ID_KEY)},
      output = MirrorResourceListResponse.class)
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    MirrorResourceListResponse dto = new MirrorResourceListResponse();

    String repositoryId = this.getRepositoryId(request);

    // get remote metadata
    RepositoryMetadata metadata = this.getMetadata(repositoryId);

    if (metadata != null) {
      for (RepositoryMirrorMetadata mirror : (List<RepositoryMirrorMetadata>) metadata.getMirrors()) {
        MirrorResource resource = new MirrorResource();
        resource.setId(mirror.getId());
        resource.setUrl(mirror.getUrl());
        dto.addData(resource);
      }
    }

    return dto;
  }

  private RepositoryMetadata getMetadata(String repositoryId)
      throws ResourceException
  {
    RepositoryMetadata metadata = null;
    try {
      metadata = repoMetadata.readRepositoryMetadata(repositoryId);
    }
    catch (NoSuchRepositoryException e) {
      getLogger().error("Unable to retrieve metadata", e);
      throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Invalid repository ID", e);
    }
    catch (MetadataHandlerException e) {
      getLogger().info("Unable to retrieve metadata, returning no items: " + e.getMessage());
    }
    catch (IOException e) {
      getLogger().error("Unable to retrieve metadata", e);
      throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Metadata handling error", e);
    }

    return metadata;
  }

}
