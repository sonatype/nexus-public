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

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.repository.Mirror;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.rest.model.MirrorResource;
import org.sonatype.nexus.rest.model.MirrorResourceListRequest;
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
@Path(RepositoryMirrorListPlexusResource.RESOURCE_URI)
@Produces({"application/xml", "application/json"})
@Consumes({"application/xml", "application/json"})
public class RepositoryMirrorListPlexusResource
    extends AbstractRepositoryMirrorPlexusResource
{
  public static final String RESOURCE_URI = "/repository_mirrors/{" + REPOSITORY_ID_KEY + "}";

  public RepositoryMirrorListPlexusResource() {
    setModifiable(true);
  }

  @Override
  public Object getPayloadInstance() {
    return new MirrorResourceListRequest();
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor("/repository_mirrors/*", "authcBasic,perms[nexus:repositorymirrors]");
  }

  @Override
  public String getResourceUri() {
    return RESOURCE_URI;
  }

  /**
   * Get the list of mirrors for the selected repository.
   *
   * @param repositoryId The repository to retrieve the assigned mirrors for.
   */
  @Override
  @GET
  @ResourceMethodSignature(pathParams = {@PathParam(AbstractRepositoryMirrorPlexusResource.REPOSITORY_ID_KEY)},
      output = MirrorResourceListResponse.class)
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    MirrorResourceListResponse dto = new MirrorResourceListResponse();

    // Hack to get the object created, so response contains the 'data'
    // element even if no mirrors defined
    dto.getData();

    try {
      Repository repository = getRepositoryRegistry().getRepository(getRepositoryId(request));

      for (Mirror mirror : getMirrors(repository)) {
        dto.addData(nexusToRestModel(mirror));
      }
    }
    catch (NoSuchRepositoryException e) {
      throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Invalid repository id "
          + getRepositoryId(request), e);
    }

    return dto;
  }

  /**
   * Update the list of mirrors for a repository.
   *
   * @param repositoryId The repository to update the assigned mirrors for.
   */
  @Override
  @POST
  @ResourceMethodSignature(pathParams = {@PathParam(AbstractRepositoryMirrorPlexusResource.REPOSITORY_ID_KEY)},
      input = MirrorResourceListRequest.class,
      output = MirrorResourceListResponse.class)
  public Object post(Context context, Request request, Response response, Object payload)
      throws ResourceException
  {
    MirrorResourceListResponse dto = new MirrorResourceListResponse();

    try {
      List<MirrorResource> resources = (List<MirrorResource>) ((MirrorResourceListRequest) payload).getData();

      List<Mirror> mirrors = restToNexusModel(resources);

      Repository repository = getRepositoryRegistry().getRepository(getRepositoryId(request));

      setMirrors(repository, mirrors);

      dto.setData(nexusToRestModel(mirrors));
    }
    catch (NoSuchRepositoryException e) {
      throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Invalid repository id " + getRepositoryId(request),
          e);
    }
    //        catch ( ConfigurationException e )
    //        {
    //            handleConfigurationException( e );
    //        }
    catch (IOException e) {
      throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Unable to create mirror", e);
    }

    return dto;
  }
}
