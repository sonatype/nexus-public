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
package org.sonatype.security.mock;

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
import org.sonatype.security.usermanagement.UserNotFoundException;
import org.sonatype.security.usermanagement.UserSearchCriteria;
import org.sonatype.security.usermanagement.UserStatus;

@Singleton
@Named("Mock")
@Typed(UserManager.class)
public class MockUserManager
    extends AbstractReadOnlyUserManager
{

  public String getSource() {
    return "Mock";
  }

  public String getAuthenticationRealmName() {
    return "Mock";
  }

  public Set<User> listUsers() {
    Set<User> users = new HashSet<User>();

    User jcohen = new DefaultUser();
    jcohen.setEmailAddress("JamesDCohen@example.com");
    jcohen.setFirstName("James");
    jcohen.setLastName("Cohen");
    // jcohen.setName( "James E. Cohen" );
    // jcohen.setReadOnly( true );
    jcohen.setSource("Mock");
    jcohen.setStatus(UserStatus.active);
    jcohen.setUserId("jcohen");
    jcohen.addRole(new RoleIdentifier("Mock", "mockrole1"));
    users.add(jcohen);

    return users;
  }

  public Set<String> listUserIds() {
    Set<String> userIds = new HashSet<String>();
    for (User user : this.listUsers()) {
      userIds.add(user.getUserId());
    }
    return userIds;
  }

  public Set<User> searchUsers(UserSearchCriteria criteria) {
    return null;
  }

  public User getUser(String userId)
      throws UserNotFoundException
  {
    for (User user : this.listUsers()) {
      if (user.getUserId().equals(userId)) {
        return user;
      }
    }
    throw new UserNotFoundException(userId);
  }

}
