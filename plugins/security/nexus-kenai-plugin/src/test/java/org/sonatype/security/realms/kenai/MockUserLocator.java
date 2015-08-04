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
package org.sonatype.security.realms.kenai;

import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.Typed;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.security.usermanagement.AbstractReadOnlyUserManager;
import org.sonatype.security.usermanagement.DefaultUser;
import org.sonatype.security.usermanagement.User;
import org.sonatype.security.usermanagement.UserManager;
import org.sonatype.security.usermanagement.UserSearchCriteria;

@Singleton
@Typed(UserManager.class)
@Named("test")
public class MockUserLocator
    extends AbstractReadOnlyUserManager
{
  private Set<String> userIds = new HashSet<String>();

  public MockUserLocator() {
    userIds.add("bob");
    userIds.add("jcoder");
  }

  public String getSource() {
    return "test";
  }

  public User getUser(String userId) {
    if (this.userIds.contains(userId)) {
      return this.toUser(userId);
    }
    return null;
  }

  public Set<String> listUserIds() {
    return userIds;
  }

  public Set<User> listUsers() {
    Set<User> users = new HashSet<User>();

    for (String userId : this.userIds) {
      users.add(this.toUser(userId));
    }

    return users;
  }

  public Set<User> searchUsers(UserSearchCriteria criteria) {
    return this.filterListInMemeory(this.listUsers(), criteria);
  }

  private User toUser(String userId) {
    DefaultUser user = new DefaultUser();

    user.setUserId(userId);
    user.setName(userId);
    user.setEmailAddress(userId + "@foo.com");
    user.setSource(this.getSource());

    return user;
  }

  public String getAuthenticationRealmName() {
    return null;
  }
}
