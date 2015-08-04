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
package org.sonatype.security.realms.simple;

import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.Typed;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.security.usermanagement.AbstractReadOnlyUserManager;
import org.sonatype.security.usermanagement.DefaultUser;
import org.sonatype.security.usermanagement.RoleIdentifier;
import org.sonatype.security.usermanagement.User;
import org.sonatype.security.usermanagement.UserManager;
import org.sonatype.security.usermanagement.UserSearchCriteria;
import org.sonatype.security.usermanagement.UserStatus;

import org.eclipse.sisu.Description;

/**
 * This is a simple implementation that will expose a custom user store as Users. A UserManager exposes
 * users so they can be used for functions other then authentication and authorizing. Users email address, and
 * optionally Roles/Groups from an external source will be looked up this way. For example, user 'jcoder' from a JDBC
 * source might be associated with the group 'projectA-developer', when the user 'jcoder' is returned from this class
 * the association is contained in a User object.
 */
// This class must have a role of 'UserManager', and the hint, must match the result of getSource() and the hint
// of the corresponding Realm.
@Singleton
@Named("Simple")
@Typed(UserManager.class)
@Description("Simple User Manager")
public class SimpleUserManager
    extends AbstractReadOnlyUserManager
{

  public static final String SOURCE = "Simple";

  /**
   * This is a very simple in memory user Store.
   */
  private UserStore userStore = new UserStore();

  public String getSource() {
    return SOURCE;
  }

  public User getUser(String userId) {
    SimpleUser user = this.userStore.getUser(userId);
    if (user != null) {
      return this.toUser(user);
    }
    // else
    return null;
  }

  public Set<String> listUserIds() {
    // just return the userIds, if you can optimize for speed, do so
    Set<String> userIds = new HashSet<String>();
    for (SimpleUser user : this.userStore.getAllUsers()) {
      userIds.add(user.getUserId());
    }

    return userIds;
  }

  public Set<User> listUsers() {
    // return all the users in the system
    Set<User> users = new HashSet<User>();
    for (SimpleUser user : this.userStore.getAllUsers()) {
      users.add(this.toUser(user));
    }

    return users;
  }

  public Set<User> searchUsers(UserSearchCriteria criteria) {
    // if your users are not all in memory, for performance reasons
    //you would want to do the filtering yourself
    return this.filterListInMemeory(this.listUsers(), criteria);
  }

  private User toUser(SimpleUser simpleUser) {
    // simple conversion of object
    User user = new DefaultUser();
    user.setEmailAddress(simpleUser.getEmail());
    user.setName(simpleUser.getName());
    user.setUserId(simpleUser.getUserId());
    user.setStatus(UserStatus.active);
    for (String role : simpleUser.getRoles()) {
      RoleIdentifier plexusRole = new RoleIdentifier(this.getSource(), role);
      user.addRole(plexusRole);
    }
    // set the source of this user to this
    user.setSource(this.getSource());

    return user;
  }

  public String getAuthenticationRealmName() {
    return "Simple";
  }

}
