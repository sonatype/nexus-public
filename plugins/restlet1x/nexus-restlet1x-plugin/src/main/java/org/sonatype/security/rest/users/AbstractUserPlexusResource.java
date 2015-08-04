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

import org.sonatype.plexus.rest.resource.PlexusResourceException;
import org.sonatype.security.rest.AbstractSecurityPlexusResource;
import org.sonatype.security.rest.model.UserResource;
import org.sonatype.security.usermanagement.User;

import org.restlet.data.Request;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;

public abstract class AbstractUserPlexusResource
    extends AbstractSecurityPlexusResource
{
  public static final String USER_ID_KEY = "userId";

  public static final String USER_EMAIL_KEY = "email";

  private static final String ROLE_VALIDATION_ERROR = "The user cannot have zero roles!";

  protected boolean validateFields(UserResource resource, Representation representation)
      throws PlexusResourceException
  {
    if (resource.getRoles() == null || resource.getRoles().size() == 0) {
      getLogger().info("The userId (" + resource.getUserId() + ") cannot have 0 roles!");

      throw new PlexusResourceException(Status.CLIENT_ERROR_BAD_REQUEST, ROLE_VALIDATION_ERROR,
          getErrorResponse("users", ROLE_VALIDATION_ERROR));
    }

    return true;
  }

  protected boolean isAnonymousUser(String username, Request request)
      throws ResourceException
  {
    return getSecuritySystem().isAnonymousAccessEnabled()
        && getSecuritySystem().getAnonymousUsername().equals(username);
  }

  protected void validateUserContainment(User user)
      throws ResourceException
  {
    if (user.getRoles().size() == 0) {
      throw new PlexusResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Configuration error.",
          getErrorResponse("roles", "User requires one or more roles."));
    }
  }
}
