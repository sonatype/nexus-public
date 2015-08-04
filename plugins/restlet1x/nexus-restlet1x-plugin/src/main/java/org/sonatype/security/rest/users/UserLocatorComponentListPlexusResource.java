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

import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.sonatype.plexus.rest.resource.AbstractPlexusResource;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.security.rest.model.PlexusComponentListResource;
import org.sonatype.security.rest.model.PlexusComponentListResourceResponse;
import org.sonatype.security.usermanagement.UserManager;

import org.apache.commons.lang.StringUtils;
import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.eclipse.sisu.BeanEntry;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/**
 * REST resource for listing the types of {@link UserManager} that are configured in the system. Each
 * {@link UserManager} manages a list of users from a spesific source.
 *
 * @author bdemers
 */
@Singleton
@Typed(PlexusResource.class)
@Named("UserLocatorComponentListPlexusResource")
@Produces({"application/xml", "application/json"})
@Consumes({"application/xml", "application/json"})
@Path(UserLocatorComponentListPlexusResource.RESOURCE_URI)
public class UserLocatorComponentListPlexusResource
    extends AbstractPlexusResource
{
  public static final String RESOURCE_URI = "/components/userLocators";

  @Inject
  private Iterable<BeanEntry<Named, UserManager>> userManagers;

  @Override
  public Object getPayloadInstance() {
    return null;
  }

  @Override
  public String getResourceUri() {
    return RESOURCE_URI;
  }

  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor(getResourceUri(), "authcBasic,perms[security:componentsuserlocatortypes]");
  }

  /**
   * Retrieves a list of User Managers.
   */
  @Override
  @GET
  @ResourceMethodSignature(output = PlexusComponentListResourceResponse.class)
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    PlexusComponentListResourceResponse result = new PlexusComponentListResourceResponse();

    if (userManagers != null) {
      for (BeanEntry<Named, UserManager> entry : userManagers) {
        String hint = entry.getKey().value();
        String description = entry.getDescription();

        PlexusComponentListResource resource = new PlexusComponentListResource();
        resource.setRoleHint(hint);
        resource.setDescription((StringUtils.isNotEmpty(description)) ? description : hint);

        // add it to the collection
        result.addData(resource);
      }
    }

    if (result.getData().isEmpty()) {
      throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND);
    }

    return result;
  }
}
