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
package org.sonatype.security.realms.tools;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.sonatype.security.model.CPrivilege;
import org.sonatype.security.model.CRole;
import org.sonatype.security.model.CUser;
import org.sonatype.security.model.CUserRoleMapping;
import org.sonatype.security.model.Configuration;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class EnhancedConfiguration
{
  private final Configuration delegate;

  public EnhancedConfiguration(final Configuration configuration) {
    this.delegate = Preconditions.checkNotNull(configuration);
    rebuildId2UsersLookupMap();
    rebuildId2RolesLookupMap();
    rebuildId2PrivilegesLookupMap();
    rebuildId2RoleMappingsLookupMap();
  }

  // ==

  public void addPrivilege(final CPrivilege cPrivilege) {
    final CPrivilege cp = cPrivilege.clone();
    delegate.addPrivilege(cp);
    id2privileges.put(cp.getId(), cp);
  }

  public void addRole(final CRole cRole) {
    final CRole cr = cRole.clone();
    delegate.addRole(cr);
    id2roles.put(cr.getId(), cr);
  }

  public void addUser(final CUser cUser) {
    final CUser cu = cUser.clone();
    delegate.addUser(cu);
    id2users.put(cu.getId(), cu);
  }

  public void addUserRoleMapping(final CUserRoleMapping cUserRoleMapping) {
    final CUserRoleMapping curm = cUserRoleMapping.clone();
    delegate.addUserRoleMapping(curm);
    id2roleMappings.put(getUserRoleMappingKey(curm.getUserId(), curm.getSource()), curm);
  }

  // ==

  public List<CPrivilege> getPrivileges() {
    // we are intentionally breaking code that will try to _modify_ the list
    // as the old config manager was before we fixed it
    return ImmutableList.copyOf(delegate.getPrivileges());
  }

  public List<CRole> getRoles() {
    // we are intentionally breaking code that will try to _modify_ the list
    // as the old config manager was before we fixed it
    return ImmutableList.copyOf(delegate.getRoles());
  }

  public List<CUserRoleMapping> getUserRoleMappings() {
    // we are intentionally breaking code that will try to _modify_ the list
    // as the old config manager was before we fixed it
    return ImmutableList.copyOf(delegate.getUserRoleMappings());
  }

  public List<CUser> getUsers() {
    // we are intentionally breaking code that will try to _modify_ the list
    // as the old config manager was before we fixed it
    return ImmutableList.copyOf(delegate.getUsers());
  }

  // ==

  public void removePrivilege(final CPrivilege cPrivilege) {
    id2privileges.remove(cPrivilege.getId());
    delegate.removePrivilege(cPrivilege);
  }

  public void removeRole(final CRole cRole) {
    id2roles.remove(cRole.getId());
    delegate.removeRole(cRole);
  }

  public void removeUser(final CUser cUser) {
    id2users.remove(cUser.getId());
    delegate.removeUser(cUser);
  }

  public void removeUserRoleMapping(final CUserRoleMapping cUserRoleMapping) {
    id2roleMappings.remove(getUserRoleMappingKey(cUserRoleMapping.getUserId(), cUserRoleMapping.getSource()));
    delegate.removeUserRoleMapping(cUserRoleMapping);
  }

  // ==

  public void setPrivileges(final List<CPrivilege> privileges) {
    delegate.setPrivileges(privileges);
    rebuildId2PrivilegesLookupMap();
  }

  public void setRoles(final List<CRole> roles) {
    delegate.setRoles(roles);
    rebuildId2RolesLookupMap();
  }

  public void setUserRoleMappings(final List<CUserRoleMapping> userRoleMappings) {
    delegate.setUserRoleMappings(userRoleMappings);
    rebuildId2RoleMappingsLookupMap();
  }

  public void setUsers(final List<CUser> users) {
    delegate.setUsers(users);
    rebuildId2UsersLookupMap();
  }

  // ==
  // Enhancements

  public CUser getUserById(final String id) {
    return getUserById(id, true);
  }

  public CUser getUserById(final String id, final boolean clone) {
    final CUser user = id2users.get(id);
    if (user != null) {
      return clone ? user.clone() : user;
    }
    else {
      return null;
    }
  }

  public boolean removeUserById(final String id) {
    final CUser user = getUserById(id, false);
    if (user != null) {
      delegate.removeUser(user);
      return id2users.remove(id) != null;
    }
    else {
      return false;
    }
  }

  public CRole getRoleById(final String id) {
    return getRoleById(id, true);
  }

  public CRole getRoleById(final String id, final boolean clone) {
    final CRole role = id2roles.get(id);
    if (role != null) {
      return clone ? role.clone() : role;
    }
    else {
      return null;
    }
  }

  public boolean removeRoleById(final String id) {
    final CRole role = getRoleById(id, false);
    if (role != null) {
      delegate.removeRole(role);
      return id2roles.remove(id) != null;
    }
    else {
      return false;
    }
  }

  public CPrivilege getPrivilegeById(final String id) {
    return getPrivilegeById(id, true);
  }

  public CPrivilege getPrivilegeById(final String id, final boolean clone) {
    final CPrivilege privilege = id2privileges.get(id);
    if (privilege != null) {
      return clone ? privilege.clone() : privilege;
    }
    else {
      return null;
    }
  }

  public boolean removePrivilegeById(final String id) {
    final CPrivilege privilege = getPrivilegeById(id, false);
    if (privilege != null) {
      delegate.removePrivilege(privilege);
      return id2privileges.remove(id) != null;
    }
    else {
      return false;
    }
  }

  public CUserRoleMapping getUserRoleMappingByUserId(final String id, final String source) {
    return getUserRoleMappingByUserId(id, source, true);
  }

  public CUserRoleMapping getUserRoleMappingByUserId(final String id, final String source, final boolean clone) {
    final CUserRoleMapping mapping = id2roleMappings.get(getUserRoleMappingKey(id, source));
    if (mapping != null) {
      return clone ? mapping.clone() : mapping;
    }
    else {
      return null;
    }
  }

  public boolean removeUserRoleMappingByUserId(final String id, final String source) {
    final CUserRoleMapping mapping = getUserRoleMappingByUserId(id, source, false);
    if (mapping != null) {
      delegate.removeUserRoleMapping(mapping);
      return id2roleMappings.remove(getUserRoleMappingKey(id, source)) != null;
    }
    else {
      return false;
    }
  }

  // ==

  @Override
  public String toString() {
    return super.toString() + " delegating to " + delegate.toString();
  }

  // ==

  private final ConcurrentHashMap<String, CUser> id2users = new ConcurrentHashMap<String, CUser>();

  private final ConcurrentHashMap<String, CRole> id2roles = new ConcurrentHashMap<String, CRole>();

  private final ConcurrentHashMap<String, CPrivilege> id2privileges = new ConcurrentHashMap<String, CPrivilege>();

  private final ConcurrentHashMap<String, CUserRoleMapping> id2roleMappings =
      new ConcurrentHashMap<String, CUserRoleMapping>();

  protected void rebuildId2UsersLookupMap() {
    id2users.clear();
    for (CUser user : delegate.getUsers()) {
      id2users.put(user.getId(), user);
    }
  }

  protected void rebuildId2RolesLookupMap() {
    id2roles.clear();
    for (CRole role : delegate.getRoles()) {
      id2roles.put(role.getId(), role);
    }
  }

  protected void rebuildId2PrivilegesLookupMap() {
    id2privileges.clear();
    for (CPrivilege privilege : delegate.getPrivileges()) {
      id2privileges.put(privilege.getId(), privilege);
    }
  }

  protected void rebuildId2RoleMappingsLookupMap() {
    id2roleMappings.clear();
    for (CUserRoleMapping user2role : delegate.getUserRoleMappings()) {
      id2roleMappings.put(getUserRoleMappingKey(user2role.getUserId(), user2role.getSource()), user2role);
    }
  }

  // ==

  protected String getUserRoleMappingKey(final String userId, final String source) {
    return userId.toLowerCase() + "|" + source;
  }
}
