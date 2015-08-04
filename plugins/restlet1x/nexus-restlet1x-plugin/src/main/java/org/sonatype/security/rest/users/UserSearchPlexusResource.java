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
package org.sonatype.security.rest.users;

import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.Typed;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.security.authorization.NoSuchAuthorizationManagerException;
import org.sonatype.security.authorization.Role;
import org.sonatype.security.rest.model.PlexusUserListResourceResponse;
import org.sonatype.security.rest.model.PlexusUserSearchCriteriaResource;
import org.sonatype.security.rest.model.PlexusUserSearchCriteriaResourceRequest;
import org.sonatype.security.usermanagement.UserSearchCriteria;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/**
 * REST resource that searches for users based on a users source and other search criteria.
 *
 * @author bdemers
 */
@Singleton
@Typed(PlexusResource.class)
@Named("UserSearchPlexusResource")
@Produces({"application/xml", "application/json"})
@Consumes({"application/xml", "application/json"})
@Path(UserSearchPlexusResource.RESOURCE_URI)
public class UserSearchPlexusResource
    extends AbstractUserSearchPlexusResource
{
  public static final String USER_ID_KEY = "userId";

  public static final String USER_SOURCE_KEY = "userSource";

  public static final String RESOURCE_URI = "/user_search/{" + USER_SOURCE_KEY + "}";

  public UserSearchPlexusResource() {
    setModifiable(true);
    setRequireStrictChecking(false);
  }

  @Override
  public Object getPayloadInstance() {
    return new PlexusUserSearchCriteriaResourceRequest();
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor("/user_search/**", "authcBasic,perms[security:users]");
  }

  @Override
  public String getResourceUri() {
    return RESOURCE_URI;
  }

  /**
   * Returns a list of users that match the search criteria.
   *
   * @param sourceId The Id of the source. A source specifies where the users/roles came from, for example the source
   *                 Id of 'LDAP' identifies the users/roles as coming from an LDAP source.
   */
  @Override
  @PUT
  @ResourceMethodSignature(input = PlexusUserSearchCriteriaResourceRequest.class,
      output = PlexusUserListResourceResponse.class, pathParams = {@PathParam("sourceId")})
  public Object put(Context context, Request request, Response response, Object payload)
      throws ResourceException
  {
    PlexusUserSearchCriteriaResource criteriaResource =
        ((PlexusUserSearchCriteriaResourceRequest) payload).getData();

    UserSearchCriteria criteria = this.toPlexusSearchCriteria(criteriaResource);
    criteria.setSource(this.getUserSource(request));

    return this.search(criteria);
  }

  private UserSearchCriteria toPlexusSearchCriteria(PlexusUserSearchCriteriaResource criteriaResource) {
    UserSearchCriteria criteria = new UserSearchCriteria();
    criteria.setUserId(criteriaResource.getUserId());

    // NOTE: in the future we could expand the REST resource to send back a list of roles, (or a single role)
    // to get a list of all users of Role 'XYZ'
    if (criteriaResource.isEffectiveUsers()) {
      Set<String> roleIds = new HashSet<String>();

      Set<Role> roles = null;
      try {
        roles = this.getSecuritySystem().listRoles("default");
      }
      catch (NoSuchAuthorizationManagerException e) {
        this.getLogger().error("Cannot find default UserManager,  effective user search may not work properly.",
            e);
        roles = this.getSecuritySystem().listRoles();
      }

      for (Role role : roles) {
        roleIds.add(role.getRoleId());
      }

      criteria.setOneOfRoleIds(roleIds);
    }

    return criteria;
  }

  /**
   * Returns a list of all the users managed by this a source.
   *
   * @param sourceId The Id of the source. A source specifies where the users/roles came from, for example the source
   *                 Id of 'LDAP' identifies the users/roles as coming from an LDAP source.
   */
  @Override
  @GET
  @ResourceMethodSignature(output = PlexusUserListResourceResponse.class, pathParams = {@PathParam("sourceId")})
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    UserSearchCriteria criteria = new UserSearchCriteria();

    // match all userIds
    criteria.setUserId("");
    criteria.setSource(this.getUserSource(request));

    return this.search(criteria);
  }

}
