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
package org.sonatype.nexus.security.internal.rest;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.rest.WebApplicationMessageException;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.user.UserNotFoundException;

import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.rest.APIConstants.V1_API_PREFIX;

/**
 * @since 3.next
 */
@Named
@Singleton
@Path(ChangePasswordResource.RESOURCE_URI)
public class ChangePasswordResource
    extends ComponentSupport
    implements Resource, ChangePasswordResourceDoc
{
  public static final String RESOURCE_URI = V1_API_PREFIX + "/users/{username}/changepassword";

  private final SecuritySystem securitySystem;

  @Inject
  public ChangePasswordResource(final SecuritySystem securitySystem) {
    this.securitySystem = checkNotNull(securitySystem);
  }

  @PUT
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  @Consumes(MediaType.TEXT_PLAIN)
  public void changePassword(@PathParam("username") String username, String password) {
    if (StringUtils.isBlank(password)) {
      throw new WebApplicationMessageException(Status.BAD_REQUEST, "Password must be supplied.");
    }

    try {
      securitySystem.changePassword(username, password);
    }
    catch (UserNotFoundException e) { //NOSONAR
      log.debug("Request to change password for invalid user '{}'.", username);
      throw new WebApplicationMessageException(Status.NOT_FOUND, "User '" + username + "' not found.");
    }
  }
}
