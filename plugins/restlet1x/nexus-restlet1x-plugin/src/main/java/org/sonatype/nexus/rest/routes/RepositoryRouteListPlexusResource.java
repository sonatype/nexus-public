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
package org.sonatype.nexus.rest.routes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.mapping.RepositoryPathMapping;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.rest.NoSuchRepositoryAccessException;
import org.sonatype.nexus.rest.model.RepositoryRouteListResource;
import org.sonatype.nexus.rest.model.RepositoryRouteListResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryRouteMemberRepository;
import org.sonatype.nexus.rest.model.RepositoryRouteResource;
import org.sonatype.nexus.rest.model.RepositoryRouteResourceResponse;
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
 * A resource list for Repository route list.
 *
 * @author cstamas
 * @author tstevens
 */
@Named
@Singleton
@Path(RepositoryRouteListPlexusResource.RESOURCE_URI)
@Produces({"application/xml", "application/json"})
@Consumes({"application/xml", "application/json"})
public class RepositoryRouteListPlexusResource
    extends AbstractRepositoryRoutePlexusResource
{
  public static final String RESOURCE_URI = "/repo_routes";

  public RepositoryRouteListPlexusResource() {
    this.setModifiable(true);
  }

  @Override
  public Object getPayloadInstance() {
    return new RepositoryRouteResourceResponse();
  }

  @Override
  public String getResourceUri() {
    return RESOURCE_URI;
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor(getResourceUri(), "authcBasic,perms[nexus:routes]");
  }

  /**
   * Get the list of repository routes.
   */
  @Override
  @GET
  @ResourceMethodSignature(output = RepositoryRouteListResourceResponse.class)
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    RepositoryRouteListResourceResponse result = new RepositoryRouteListResourceResponse();

    Map<String, RepositoryPathMapping> mappings = getRepositoryMapper().getMappings();

    RepositoryRouteListResource resource = null;

    for (RepositoryPathMapping item : mappings.values()) {
      resource = new RepositoryRouteListResource();

      if (!item.getGroupId().equals("*")) {
        // XXX: added to check access to group
        try {
          this.getRepositoryRegistry().getRepositoryWithFacet(item.getGroupId(), GroupRepository.class);
        }
        catch (NoSuchRepositoryAccessException e) {
          getLogger().debug(
              "Access Denied to Group '" + item.getGroupId() + "' contained within route: + '" + item.getId()
                  + "'!", e);
          continue;
        }
        catch (NoSuchRepositoryException e) {
          getLogger().warn(
              "Cannot find group '" + item.getGroupId() + "' declared within route: + '" + item.getId()
                  + "'!", e);
          continue;
        }
      }
      resource.setGroupId(item.getGroupId());

      resource.setResourceURI(createChildReference(request, this, item.getId()).toString());

      resource.setRuleType(config2resourceType(item.getMappingType()));

      // XXX: cstamas -- a hack!
      resource.setPattern(item.getPatterns().get(0).toString());

      try {
        resource.setRepositories(getRepositoryRouteMemberRepositoryList(request.getResourceRef(),
            item.getMappedRepositories(), request, item.getId()));
      }
      catch (NoSuchRepositoryAccessException e) {
        getLogger().debug(
            "Access Denied to Group '" + item.getGroupId() + "' contained within route: + '" + item.getId()
                + "'!", e);
        continue;
      }

      result.addData(resource);
    }

    return result;
  }

  /**
   * Add a new repository route.
   */
  @Override
  @POST
  @ResourceMethodSignature(input = RepositoryRouteResourceResponse.class,
      output = RepositoryRouteResourceResponse.class)
  public Object post(Context context, Request request, Response response, Object payload)
      throws ResourceException
  {
    RepositoryRouteResourceResponse routeRequest = (RepositoryRouteResourceResponse) payload;

    RepositoryRouteResourceResponse result = null;

    if (routeRequest != null) {
      RepositoryRouteResource resource = routeRequest.getData();

      if (!RepositoryRouteResource.BLOCKING_RULE_TYPE.equals(resource.getRuleType())
          && (resource.getRepositories() == null || resource.getRepositories().size() == 0)) {
        throw new PlexusResourceException(
            Status.CLIENT_ERROR_BAD_REQUEST,
            "The route cannot have zero repository members!",
            getNexusErrorResponse("repositories",
                "The route cannot have zero repository members!"));
      }
      else if (RepositoryRouteResource.BLOCKING_RULE_TYPE.equals(resource.getRuleType())) {
        resource.setRepositories(null);
      }

      resource.setId(Long.toHexString(System.nanoTime()));

      try {
        ArrayList<String> mappedReposes = new ArrayList<String>(resource.getRepositories().size());

        for (RepositoryRouteMemberRepository member : resource.getRepositories()) {
          mappedReposes.add(member.getId());
        }

        RepositoryPathMapping route =
            new RepositoryPathMapping(resource.getId(), resource2configType(resource.getRuleType()),
                resource.getGroupId(), Arrays.asList(new String[]{
                resource
                    .getPattern()
            }), mappedReposes);

        getRepositoryMapper().addMapping(route);

        getNexusConfiguration().saveConfiguration();

        resource.setGroupId(route.getGroupId());

        result = new RepositoryRouteResourceResponse();

        result.setData(resource);
      }
      catch (ConfigurationException e) {
        if (e.getCause() != null && e.getCause() instanceof PatternSyntaxException) {
          throw new PlexusResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Configuration error.",
              getNexusErrorResponse("pattern", e.getMessage()));
        }
        else {
          handleConfigurationException(e);
        }
      }
      catch (PatternSyntaxException e) {
        // TODO: fix because this happens before we validate, we need to fix the validation.
        ErrorResponse errorResponse = getNexusErrorResponse("*", e.getMessage());
        throw new PlexusResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Configuration error.", errorResponse);
      }
/*            catch ( NoSuchRepositoryException e )
            {
                getLogger().warn( "Cannot find a repository referenced within a route!", e );

                throw new PlexusResourceException(
                                                   Status.CLIENT_ERROR_BAD_REQUEST,
                                                   "Cannot find a repository referenced within a route!",
                                                   getNexusErrorResponse( "repositories",
                                                                          "Cannot find a repository referenced within a route!" ) );
            }*/
      catch (IOException e) {
        getLogger().warn("Got IO Exception!", e);

        throw new ResourceException(Status.SERVER_ERROR_INTERNAL);

      }
    }
    return result;
  }

}
