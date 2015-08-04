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
package org.sonatype.nexus.rest.repotargets;

import java.io.IOException;
import java.util.regex.PatternSyntaxException;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.proxy.targets.Target;
import org.sonatype.nexus.rest.model.RepositoryTargetResource;
import org.sonatype.nexus.rest.model.RepositoryTargetResourceResponse;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResourceException;
import org.sonatype.plexus.rest.resource.error.ErrorResponse;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/**
 * @author tstevens
 */
@Named
@Singleton
@Path(RepositoryTargetPlexusResource.RESOURCE_URI)
@Produces({"application/xml", "application/json"})
@Consumes({"application/xml", "application/json"})
public class RepositoryTargetPlexusResource
    extends AbstractRepositoryTargetPlexusResource
{
  public static final String REPO_TARGET_ID_KEY = "repoTargetId";

  public static final String RESOURCE_URI = "/repo_targets/{" + REPO_TARGET_ID_KEY + "}";

  public RepositoryTargetPlexusResource() {
    this.setModifiable(true);
  }

  @Override
  public Object getPayloadInstance() {
    return new RepositoryTargetResourceResponse();
  }

  @Override
  public String getResourceUri() {
    return RESOURCE_URI;
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor("/repo_targets/*", "authcBasic,perms[nexus:targets]");
  }

  private String getRepoTargetId(Request request) {
    return request.getAttributes().get(REPO_TARGET_ID_KEY).toString();
  }

  /**
   * Get the details of a repository target.
   *
   * @param repoTargetId Repository target to access.
   */
  @Override
  @GET
  @ResourceMethodSignature(pathParams = {@PathParam(RepositoryTargetPlexusResource.REPO_TARGET_ID_KEY)},
      output = RepositoryTargetResourceResponse.class)
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    RepositoryTargetResourceResponse result = new RepositoryTargetResourceResponse();

    Target target = getTargetRegistry().getRepositoryTarget(getRepoTargetId(request));

    if (target != null) {
      RepositoryTargetResource resource = getNexusToRestResource(target, request);

      result.setData(resource);
    }
    else {
      throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "No such target!");
    }
    return result;
  }

  /**
   * Update the configuration of an existing repository target.
   *
   * @param repoTargetId Repository target to access.
   */
  @Override
  @PUT
  @ResourceMethodSignature(pathParams = {@PathParam(RepositoryTargetPlexusResource.REPO_TARGET_ID_KEY)},
      input = RepositoryTargetResourceResponse.class,
      output = RepositoryTargetResourceResponse.class)
  public Object put(Context context, Request request, Response response, Object payload)
      throws ResourceException
  {
    RepositoryTargetResourceResponse requestResource = (RepositoryTargetResourceResponse) payload;
    RepositoryTargetResourceResponse resultResource = null;
    if (requestResource != null) {
      RepositoryTargetResource resource = requestResource.getData();

      Target target = getTargetRegistry().getRepositoryTarget(getRepoTargetId(request));

      if (target != null) {
        if (validate(false, resource)) {
          try {
            target = getRestToNexusResource(resource);

            // update
            getTargetRegistry().addRepositoryTarget(target);

            getNexusConfiguration().saveConfiguration();

            // response
            resultResource = new RepositoryTargetResourceResponse();

            resultResource.setData(requestResource.getData());

          }
          catch (ConfigurationException e) {
            // builds and throws an exception
            handleConfigurationException(e);
          }
          catch (PatternSyntaxException e) {
            // TODO: fix because this happens before we validate, we need to fix the validation.
            ErrorResponse errorResponse = getNexusErrorResponse("*", e.getMessage());
            throw new PlexusResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Configuration error.", errorResponse);
          }
          catch (IOException e) {
            getLogger().warn("Got IOException during creation of repository target!", e);

            throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
                "Got IOException during creation of repository target!");
          }
        }
      }
      else {
        throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "No such target!");
      }

    }
    return resultResource;
  }

  /**
   * Delete an existing repository target.
   *
   * @param repoTargetId Repository target to access.
   */
  @Override
  @DELETE
  @ResourceMethodSignature(pathParams = {@PathParam(RepositoryTargetPlexusResource.REPO_TARGET_ID_KEY)})
  public void delete(Context context, Request request, Response response)
      throws ResourceException
  {
    Target target = getTargetRegistry().getRepositoryTarget(getRepoTargetId(request));

    if (target != null) {
      try {
        getTargetRegistry().removeRepositoryTarget(getRepoTargetId(request));

        getNexusConfiguration().saveConfiguration();
      }
      catch (IOException e) {
        getLogger().warn("Got IOException during removal of repository target!", e);

        throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
            "Got IOException during removal of repository target!");
      }
    }
    else {
      throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "No such target!");
    }
  }
}
