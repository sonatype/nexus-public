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
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.mapping.RepositoryPathMapping;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.rest.NoSuchRepositoryAccessException;
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
 * Handles GET, PUT, DELETE for Repository route resources.
 *
 * @author cstamas
 * @author tstevens
 */
@Named
@Singleton
@Path(RepositoryRoutePlexusResource.RESOURCE_URI)
@Produces({"application/xml", "application/json"})
@Consumes({"application/xml", "application/json"})
public class RepositoryRoutePlexusResource
    extends AbstractRepositoryRoutePlexusResource
{
  public static final String RESOURCE_URI = "/repo_routes/{" + ROUTE_ID_KEY + "}";

  public RepositoryRoutePlexusResource() {
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
    return new PathProtectionDescriptor("/repo_routes/*", "authcBasic,perms[nexus:routes]");
  }

  protected String getRouteId(Request request) {
    return request.getAttributes().get(ROUTE_ID_KEY).toString();
  }

  /**
   * Get the details of a repository route.
   *
   * @param routeId The repository route to access.
   */
  @Override
  @GET
  @ResourceMethodSignature(pathParams = {@PathParam(AbstractRepositoryRoutePlexusResource.ROUTE_ID_KEY)},
      output = RepositoryRouteResourceResponse.class)
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {

    RepositoryRouteResourceResponse result = null;

    try {
      RepositoryPathMapping route = getRepositoryMapper().getMappings().get(getRouteId(request));

      if (route == null) {
        throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "No such route");
      }

      RepositoryRouteResource resource = new RepositoryRouteResource();

      resource.setId(getRouteId(request));

      if (!route.isAllGroups()) {
        // XXX: added to check access to group
        this.getRepositoryRegistry().getRepositoryWithFacet(route.getGroupId(), GroupRepository.class);
      }
      resource.setGroupId(route.getGroupId());

      resource.setRuleType(config2resourceType(route.getMappingType()));

      // XXX: cstamas -- a hack!
      resource.setPattern(route.getPatterns().get(0).toString());

      resource.setRepositories(getRepositoryRouteMemberRepositoryList(request.getResourceRef().getParentRef(),
          route.getMappedRepositories(), request, route.getId()));

      result = new RepositoryRouteResourceResponse();

      result.setData(resource);
    }
    catch (NoSuchRepositoryAccessException e) {
      getLogger().debug("Access Denied to a repository declared within a group!", e);

      throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, e);
    }
    catch (NoSuchRepositoryException e) {
      getLogger().warn("Cannot find a repository declared within a group!", e);

      throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
    }
    catch (IndexOutOfBoundsException e) {
      getLogger().warn("Repository route not found, id=" + getRouteId(request));

      throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "Repository Route Not Found");
    }

    return result;
  }

  /**
   * Update an existing repository route.
   *
   * @param routeId The repository route to access.
   */
  @Override
  @PUT
  @ResourceMethodSignature(pathParams = {@PathParam(AbstractRepositoryRoutePlexusResource.ROUTE_ID_KEY)},
      input = RepositoryRouteResourceResponse.class,
      output = RepositoryRouteResourceResponse.class)
  public Object put(Context context, Request request, Response response, Object payload)
      throws ResourceException
  {
    RepositoryRouteResourceResponse routeRequest = (RepositoryRouteResourceResponse) payload;
    RepositoryRouteResourceResponse result = null;

    if (routeRequest != null) {
      RepositoryRouteResource resource = routeRequest.getData();

      if ((!RepositoryRouteResource.BLOCKING_RULE_TYPE.equals(resource.getRuleType()) && (resource
          .getRepositories() == null || resource.getRepositories().size() == 0))
          || resource.getId() == null || !resource.getId().equals(getRouteId(request))) {
        throw new PlexusResourceException(
            Status.CLIENT_ERROR_BAD_REQUEST,
            "The route cannot have zero repository members!",
            getNexusErrorResponse("repositories",
                "The route cannot have zero repository members!"));
      }
      else if (RepositoryRouteResource.BLOCKING_RULE_TYPE.equals(resource.getRuleType())) {
        resource.setRepositories(null);
      }

      try {
        RepositoryPathMapping route = getRepositoryMapper().getMappings().get(getRouteId(request));

        if (route == null) {
          throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "Route not found!");
        }

        ArrayList<String> mappedReposes = new ArrayList<String>(resource.getRepositories().size());

        for (RepositoryRouteMemberRepository member : resource.getRepositories()) {
          mappedReposes.add(member.getId());
        }

        RepositoryPathMapping newRoute =
            new RepositoryPathMapping(route.getId(), resource2configType(resource.getRuleType()),
                resource.getGroupId(), Arrays.asList(new String[]{
                resource
                    .getPattern()
            }), mappedReposes);

        getRepositoryMapper().addMapping(newRoute);

        getNexusConfiguration().saveConfiguration();

        response.setStatus(Status.SUCCESS_NO_CONTENT);
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
/*            catch ( NoSuchRepositoryAccessException e )
            {
                getLogger().debug( "Access Denied to a repository referenced within a route!", e );

                throw new ResourceException( Status.CLIENT_ERROR_FORBIDDEN );
            }
            catch ( NoSuchRepositoryException e )
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
    // TODO: this is null, because we return a 204
    return result;
  }

  /**
   * Delete an existing repository route.
   *
   * @param routeId The repository route to access.
   */
  @Override
  @DELETE
  @ResourceMethodSignature(pathParams = {@PathParam(AbstractRepositoryRoutePlexusResource.ROUTE_ID_KEY)})
  public void delete(Context context, Request request, Response response)
      throws ResourceException
  {
    try {
      RepositoryPathMapping route = getRepositoryMapper().getMappings().get(getRouteId(request));

      if (route == null) {
        throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "Route not found!");
      }

      getRepositoryMapper().removeMapping(getRouteId(request));

      getNexusConfiguration().saveConfiguration();

      response.setStatus(Status.SUCCESS_NO_CONTENT);
    }
    catch (IOException e) {
      getLogger().warn("Got IO Exception!", e);

      throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
    }
  }

}
