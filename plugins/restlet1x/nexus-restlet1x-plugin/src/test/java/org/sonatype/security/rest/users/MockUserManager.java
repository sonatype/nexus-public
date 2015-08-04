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

import org.sonatype.security.usermanagement.AbstractReadOnlyUserManager;
import org.sonatype.security.usermanagement.DefaultUser;
import org.sonatype.security.usermanagement.RoleIdentifier;
import org.sonatype.security.usermanagement.User;
import org.sonatype.security.usermanagement.UserManager;
import org.sonatype.security.usermanagement.UserSearchCriteria;
import org.sonatype.security.usermanagement.UserStatus;

@Singleton
@Typed(UserManager.class)
@Named(MockUserManager.SOURCE)
public class MockUserManager
    extends AbstractReadOnlyUserManager
{
  public static final String SOURCE = "MockUserManager";

  public String getSource() {
    return SOURCE;
  }

  public Set<User> listUsers() {
    Set<User> users = new HashSet<User>();

    User a = new DefaultUser();
    a.setName("Joe Coder");
    a.setEmailAddress("jcoder@sonatype.org");
    a.setSource(this.getSource());
    a.setUserId("jcoder");
    a.setStatus(UserStatus.active);
    a.addRole(this.createFakeRole("Role1"));
    a.addRole(this.createFakeRole("Role2"));
    a.addRole(this.createFakeRole("Role3"));

    User b = new DefaultUser();
    b.setName("Christine H. Dugas");
    b.setEmailAddress("cdugas@sonatype.org");
    b.setSource(this.getSource());
    b.setUserId("cdugas");
    b.setStatus(UserStatus.active);
    b.addRole(this.createFakeRole("Role2"));
    b.addRole(this.createFakeRole("Role3"));

    User c = new DefaultUser();
    c.setName("Patricia P. Peralez");
    c.setEmailAddress("pperalez@sonatype.org");
    c.setSource(this.getSource());
    c.setUserId("pperalez");
    c.setStatus(UserStatus.active);
    c.addRole(this.createFakeRole("Role1"));
    c.addRole(this.createFakeRole("Role2"));

    User d = new DefaultUser();
    d.setName("Danille S. Knudsen");
    d.setEmailAddress("dknudsen@sonatype.org");
    d.setSource(this.getSource());
    d.setUserId("dknudsen");
    d.setStatus(UserStatus.active);
    d.addRole(this.createFakeRole("Role4"));
    d.addRole(this.createFakeRole("Role2"));

    users.add(a);
    users.add(b);
    users.add(c);
    users.add(d);

    return users;
  }

  public boolean isPrimary() {
    return true;
  }

  public User getUser(String userId) {
    Set<User> users = this.listUsers();

    for (User User : users) {
      if (User.getUserId().equals(userId)) {
        return User;
      }
    }

    return null;
  }

  public Set<String> listUserIds() {
    Set<String> result = new HashSet<String>();
    for (User User : this.listUsers()) {
      result.add(User.getUserId());
    }
    return result;
  }

  public Set<User> searchUsers(UserSearchCriteria criteria) {

    Set<User> result = new HashSet<User>();
    for (User User : this.listUsers()) {
      if (User.getUserId().toLowerCase().startsWith(criteria.getUserId())) {
        ;
      }
      {
        result.add(User);
      }
    }
    return result;
  }

  protected RoleIdentifier createFakeRole(String roleId) {
    RoleIdentifier role = new RoleIdentifier(this.getSource(), roleId);
    return role;
  }

  public String getAuthenticationRealmName() {
    return null;
  }

}
