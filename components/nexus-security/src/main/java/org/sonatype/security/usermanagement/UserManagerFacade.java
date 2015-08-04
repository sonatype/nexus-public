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
package org.sonatype.security.usermanagement;

import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.sisu.goodies.common.ComponentSupport;

@Singleton
@Typed(UserManagerFacade.class)
@Named("default")
public class UserManagerFacade
  extends ComponentSupport
{
  private final Map<String, UserManager> userManagers;

  @Inject
  public UserManagerFacade(Map<String, UserManager> userManagers) {
    this.userManagers = userManagers;
  }

  public User getUser(String userId, String source)
      throws UserNotFoundException, NoSuchUserManagerException
  {
    // first get the user
    // this is the UserManager that owns the user
    UserManager userManager = getUserManager(source);
    User user = userManager.getUser(userId);

    if (user == null) {
      throw new UserNotFoundException(userId);
    }

    // add roles from other user managers
    this.addOtherRolesToUser(user);

    return user;
  }

  public Map<String, UserManager> getUserManagers() {
    return userManagers;
  }

  public UserManager getUserManager(String sourceId)
      throws NoSuchUserManagerException
  {
    if (!userManagers.containsKey(sourceId)) {
      throw new NoSuchUserManagerException("UserManager with source: '" + sourceId + "' could not be found.");
    }

    return userManagers.get(sourceId);
  }

  private void addOtherRolesToUser(User user) {
    // then save the users Roles
    for (UserManager tmpUserManager : userManagers.values()) {
      // skip the user manager that owns the user, we already did that
      // these user managers will only have roles
      if (!tmpUserManager.getSource().equals(user.getSource())
          && RoleMappingUserManager.class.isInstance(tmpUserManager)) {
        try {
          RoleMappingUserManager roleMappingUserManager = (RoleMappingUserManager) tmpUserManager;
          Set<RoleIdentifier> roleIdentifiers =
              roleMappingUserManager.getUsersRoles(user.getUserId(), user.getSource());
          if (roleIdentifiers != null) {
            user.addAllRoles(roleIdentifiers);
          }
        }
        catch (UserNotFoundException e) {
          log.debug("User '" + user.getUserId() + "' is not managed by the usermanager: "
              + tmpUserManager.getSource());
        }
      }
    }
  }
}
