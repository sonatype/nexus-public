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
package org.sonatype.security.locators;

import java.util.HashSet;
import java.util.Set;

import org.sonatype.security.usermanagement.AbstractReadOnlyUserManager;
import org.sonatype.security.usermanagement.RoleIdentifier;
import org.sonatype.security.usermanagement.User;
import org.sonatype.security.usermanagement.UserSearchCriteria;

public abstract class AbstractTestUserManager
    extends AbstractReadOnlyUserManager
{

  public User getUser(String userId) {
    Set<User> users = this.listUsers();

    for (User plexusUser : users) {
      if (plexusUser.getUserId().equals(userId)) {
        return plexusUser;
      }
    }

    return null;
  }

  public Set<String> listUserIds() {
    Set<String> result = new HashSet<String>();
    for (User plexusUser : this.listUsers()) {
      result.add(plexusUser.getUserId());
    }
    return result;
  }

  public Set<User> searchUsers(UserSearchCriteria criteria) {
    return this.filterListInMemeory(this.listUsers(), criteria);
  }

  protected RoleIdentifier createFakeRole(String roleId) {
    RoleIdentifier role = new RoleIdentifier(this.getSource(), roleId);
    return role;
  }
}
