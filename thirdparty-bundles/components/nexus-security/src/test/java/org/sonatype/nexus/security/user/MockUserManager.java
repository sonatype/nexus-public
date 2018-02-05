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
package org.sonatype.nexus.security.user;

import java.util.HashSet;
import java.util.Set;

import org.sonatype.nexus.security.role.ExternalRoleMappedTest;
import org.sonatype.nexus.security.role.RoleIdentifier;

/**
 * @see ExternalRoleMappedTest
 * @see UserManagementTest
 */
public class MockUserManager
    extends AbstractReadOnlyUserManager
{
  @Override
  public String getSource() {
    return "Mock";
  }

  @Override
  public String getAuthenticationRealmName() {
    return "Mock";
  }

  @Override
  public Set<User> listUsers() {
    Set<User> users = new HashSet<User>();

    User jcohen = new User();
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

  @Override
  public Set<String> listUserIds() {
    Set<String> userIds = new HashSet<String>();
    for (User user : this.listUsers()) {
      userIds.add(user.getUserId());
    }
    return userIds;
  }

  @Override
  public Set<User> searchUsers(UserSearchCriteria criteria) {
    return null;
  }

  @Override
  public User getUser(String userId) throws UserNotFoundException {
    for (User user : this.listUsers()) {
      if (user.getUserId().equals(userId)) {
        return user;
      }
    }
    throw new UserNotFoundException(userId);
  }
}
