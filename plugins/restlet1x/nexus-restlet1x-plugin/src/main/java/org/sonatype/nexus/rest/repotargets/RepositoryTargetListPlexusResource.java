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
import java.util.Collection;
import java.util.regex.PatternSyntaxException;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.proxy.targets.Target;
import org.sonatype.nexus.rest.model.RepositoryTargetListResource;
import org.sonatype.nexus.rest.model.RepositoryTargetListResourceResponse;
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
@Path(RepositoryTargetListPlexusResource.RESOURCE_URI)
@Produces({"application/xml", "application/json"})
@Consumes({"application/xml", "application/json"})
public class RepositoryTargetListPlexusResource
    extends AbstractRepositoryTargetPlexusResource
{
  public static final String RESOURCE_URI = "/repo_targets";

  public RepositoryTargetListPlexusResource() {
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
    return new PathProtectionDescriptor(getResourceUri(), "authcBasic,perms[nexus:targets]");
  }

  /**
   * Get the list of configuration repository targets.
   */
  @Override
  @GET
  @ResourceMethodSignature(output = RepositoryTargetListResourceResponse.class)
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    RepositoryTargetListResourceResponse result = new RepositoryTargetListResourceResponse();

    Collection<Target> targets = getTargetRegistry().getRepositoryTargets();

    RepositoryTargetListResource res = null;

    for (Target target : targets) {
      res = new RepositoryTargetListResource();

      res.setId(target.getId());

      res.setName(target.getName());

      res.setContentClass(target.getContentClass().getId());

      res.setResourceURI(this.createChildReference(request, this, target.getId()).toString());

      for (String pattern : target.getPatternTexts()) {
        res.addPattern(pattern);
      }

      result.addData(res);
    }

    return result;
  }

  /**
   * Add a new repository target to nexus.
   */
  @Override
  @POST
  @ResourceMethodSignature(input = RepositoryTargetResourceResponse.class,
      output = RepositoryTargetResourceResponse.class)
  public Object post(Context context, Request request, Response response, Object payload)
      throws ResourceException
  {
    RepositoryTargetResourceResponse result = (RepositoryTargetResourceResponse) payload;
    RepositoryTargetResourceResponse resourceResponse = null;

    if (result != null) {
      RepositoryTargetResource resource = result.getData();

      if (validate(true, resource)) {
        try {
          // create
          Target target = getRestToNexusResource(resource);

          getTargetRegistry().addRepositoryTarget(target);

          getNexusConfiguration().saveConfiguration();

          // response
          resourceResponse = new RepositoryTargetResourceResponse();

          resourceResponse.setData(result.getData());
        }
        catch (ConfigurationException e) {
          // build an exception and throws it
          handleConfigurationException(e);
        }
        catch (PatternSyntaxException e) {
          // TODO: fix because this happens before we validate, we need to fix the validation.
          ErrorResponse errorResponse = getNexusErrorResponse("*", e.getMessage());
          throw new PlexusResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Configuration error.", errorResponse);
        }
        catch (IOException e) {
          getLogger().warn("Got IOException during creation of repository target!", e);

          throw new ResourceException(
              Status.SERVER_ERROR_INTERNAL,
              "Got IOException during creation of repository target!");
        }
      }
    }
    return resourceResponse;
  }
}
