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
package org.sonatype.nexus.coreui;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.wonderland.AuthTicketService;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.anonymous.AnonymousConfiguration;
import org.sonatype.nexus.security.anonymous.AnonymousManager;
import org.sonatype.nexus.security.user.NoSuchUserManagerException;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserNotFoundException;
import org.sonatype.nexus.validation.Validate;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresUser;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.sonatype.nexus.security.user.UserManager.DEFAULT_SOURCE;

/**
 * User resource.
 *
 * @since 3.24
 */
@Named
@Singleton
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(UserResource.RESOURCE_PATH)
public class UserResource
    extends ComponentSupport
    implements Resource
{
  static final String RESOURCE_PATH = "internal/ui/user";

  private final SecuritySystem securitySystem;

  private final AuthTicketService authTickets;

  private final AnonymousManager anonymousManager;

  @Inject
  public UserResource(final SecuritySystem securitySystem,
                      final AuthTicketService authTickets,
                      final AnonymousManager anonymousManager)
  {
    this.securitySystem = checkNotNull(securitySystem);
    this.authTickets = checkNotNull(authTickets);
    this.anonymousManager = checkNotNull(anonymousManager);
  }

  /**
   * Retrieves user account (logged in user info).
   *
   * @return current logged in user account.
   */
  @GET
  @RequiresUser
  public UserAccountXO readAccount()
      throws UserNotFoundException
  {
    return convert(getCurrentUser());
  }

  @PUT
  @RequiresUser
  @RequiresAuthentication
  @Validate
  public void updateAccount(@NotNull @Valid final UserAccountXO xo)
      throws UserNotFoundException, NoSuchUserManagerException
  {
    User user = getCurrentUser();
    user.setFirstName(xo.getFirstName());
    user.setLastName(xo.getLastName());
    user.setEmailAddress(xo.getEmail());
    securitySystem.updateUser(user);
  }

  @PUT
  @Path("/{userId}/password")
  @RequiresUser
  @RequiresAuthentication
  @RequiresPermissions("nexus:userschangepw:create")
  @Validate
  public void changePassword(@PathParam("userId") @NotNull final String userId,
                             @NotNull @Valid final UserAccountPasswordXO xo)
      throws Exception
  {
    if (authTickets.redeemTicket(xo.getAuthToken())) {
      if (isAnonymousUser(userId)) {
        throw new Exception("Password cannot be changed for user ${userId}, since is marked as the Anonymous user");
      }
      securitySystem.changePassword(userId, xo.getPassword());
    }
    else {
      throw new IllegalAccessException("Invalid authentication ticket");
    }
  }

  private User getCurrentUser() throws UserNotFoundException {
    User user = securitySystem.currentUser();
    if (user != null) {
      return user;
    }
    else {
      throw new UserNotFoundException("Unable to get current user");
    }
  }

  UserAccountXO convert(final User user) {
    UserAccountXO xo = new UserAccountXO();
    xo.setUserId(user.getUserId());
    xo.setFirstName(user.getFirstName());
    xo.setLastName(user.getLastName());
    xo.setEmail(user.getEmailAddress());
    xo.setExternal(!DEFAULT_SOURCE.equals(user.getSource()));
    return xo;
  }

  private boolean isAnonymousUser(final String userId) {
    AnonymousConfiguration config = anonymousManager.getConfiguration();
    return config.isEnabled() && config.getUserId().equals(userId);
  }
}
