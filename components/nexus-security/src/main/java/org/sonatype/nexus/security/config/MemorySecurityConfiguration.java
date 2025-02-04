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
package org.sonatype.nexus.security.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Nullable;

import org.sonatype.nexus.security.config.memory.MemoryCPrivilege;
import org.sonatype.nexus.security.config.memory.MemoryCRole;
import org.sonatype.nexus.security.config.memory.MemoryCUser;
import org.sonatype.nexus.security.config.memory.MemoryCUserRoleMapping;
import org.sonatype.nexus.security.privilege.NoSuchPrivilegeException;
import org.sonatype.nexus.security.role.NoSuchRoleException;
import org.sonatype.nexus.security.user.NoSuchRoleMappingException;
import org.sonatype.nexus.security.user.UserManager;
import org.sonatype.nexus.security.user.UserNotFoundException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.apache.shiro.util.CollectionUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.sonatype.nexus.security.config.SecuritySourceUtil.isCaseInsensitiveSource;

/**
 * Memory based {@link SecurityConfiguration}.
 */
public class MemorySecurityConfiguration
    implements SecurityConfiguration, Serializable, Cloneable
{
  private final ConcurrentMap<String, CUser> users;

  private final ConcurrentMap<String, CRole> roles;

  private final ConcurrentMap<String, CPrivilege> privileges;

  private final ConcurrentMap<String, CUserRoleMapping> userRoleMappings;

  public MemorySecurityConfiguration() {
    users = Maps.newConcurrentMap();
    roles = Maps.newConcurrentMap();
    privileges = Maps.newConcurrentMap();
    userRoleMappings = Maps.newConcurrentMap();
  }

  @Override
  public List<CUser> getUsers() {
    return ImmutableList.copyOf(users.values());
  }

  @Override
  public CUser getUser(final String id) {
    checkNotNull(id);
    return users.get(id);
  }

  private void addUser(final CUser user) {
    checkNotNull(user);
    checkNotNull(user.getId());
    checkState(users.putIfAbsent(user.getId(), user) == null, "%s already exists", user.getId());
  }

  @Override
  public void addUser(final CUser user, final Set<String> roles) {
    addUser(user);

    CUserRoleMapping mapping = new MemoryCUserRoleMapping();
    mapping.setUserId(user.getId());
    mapping.setSource(UserManager.DEFAULT_SOURCE);
    mapping.setRoles(roles);
    addUserRoleMapping(mapping);
  }

  @Override
  public void addRoleMapping(final String userId, final Set<String> roles, final String source) {
    // No op
  }

  @Override
  public CUser newUser() {
    return new MemoryCUser();
  }

  public void setUsers(final Collection<CUser> users) {
    this.users.clear();
    if (users != null) {
      for (CUser user : users) {
        addUser(user);
      }
    }
  }

  public MemorySecurityConfiguration withUsers(final CUser... users) {
    setUsers(Arrays.asList(users));
    return this;
  }

  @Override
  public void updateUser(final CUser user) throws UserNotFoundException {
    checkNotNull(user);
    checkNotNull(user.getId());
    if (users.replace(user.getId(), user) == null) {
      throw new UserNotFoundException(user.getId());
    }
  }

  @Override
  public void updateUser(final CUser user, final Set<String> roles) throws UserNotFoundException {
    updateUser(user);

    CUserRoleMapping mapping = new MemoryCUserRoleMapping();
    mapping.setUserId(user.getId());
    mapping.setSource(UserManager.DEFAULT_SOURCE);
    mapping.setRoles(roles);
    try {
      updateUserRoleMapping(mapping);
    }
    catch (NoSuchRoleMappingException e) {
      addUserRoleMapping(mapping);
    }
  }

  @Override
  public boolean removeUser(final String id) {
    checkNotNull(id);
    if (users.remove(id) != null) {
      removeUserRoleMapping(id, UserManager.DEFAULT_SOURCE);
      return true;
    }
    return false;
  }

  @Override
  public List<CUserRoleMapping> getUserRoleMappings() {
    return ImmutableList.copyOf(userRoleMappings.values());
  }

  @Override
  public CUserRoleMapping getUserRoleMapping(final String userId, final String source) {
    checkNotNull(userId);
    checkNotNull(source);
    return userRoleMappings.get(userRoleMappingKey(userId, source));
  }

  @Override
  public void addUserRoleMapping(final CUserRoleMapping mapping) {
    checkNotNull(mapping);
    checkNotNull(mapping.getUserId());
    checkNotNull(mapping.getSource());
    checkState(
        userRoleMappings.putIfAbsent(userRoleMappingKey(mapping.getUserId(), mapping.getSource()), mapping) == null,
        "%s/%s already exists", mapping.getUserId(), mapping.getSource());
  }

  public void setUserRoleMappings(final Collection<CUserRoleMapping> mappings) {
    this.userRoleMappings.clear();
    if (mappings != null) {
      for (CUserRoleMapping mapping : mappings) {
        addUserRoleMapping(mapping);
      }
    }
  }

  public MemorySecurityConfiguration withUserRoleMappings(final CUserRoleMapping... mappings) {
    setUserRoleMappings(Arrays.asList(mappings));
    return this;
  }

  @Override
  public void updateUserRoleMapping(final CUserRoleMapping mapping) throws NoSuchRoleMappingException {
    checkNotNull(mapping);
    checkNotNull(mapping.getUserId());
    checkNotNull(mapping.getSource());
    if (userRoleMappings.replace(userRoleMappingKey(mapping.getUserId(), mapping.getSource()), mapping) == null) {
      throw new NoSuchRoleMappingException(mapping.getUserId());
    }
  }

  @Override
  public boolean removeUserRoleMapping(final String userId, final String source) {
    checkNotNull(userId);
    checkNotNull(source);
    return userRoleMappings.remove(userRoleMappingKey(userId, source)) != null;
  }

  @Override
  public List<CPrivilege> getPrivileges() {
    return ImmutableList.copyOf(privileges.values());
  }

  @Override
  public CPrivilege getPrivilege(final String id) {
    checkNotNull(id);
    return privileges.get(id);
  }

  @Nullable
  @Override
  public CPrivilege getPrivilegeByName(final String name) {
    return Optional.ofNullable(name)
        .flatMap(n -> privileges.values()
            .stream()
            .filter(p -> p.getName().equals(n))
            .findFirst())
        .orElse(null);
  }

  @Override
  public List<CPrivilege> getPrivileges(final Set<String> ids) {
    if (CollectionUtils.isEmpty(ids)) {
      return Collections.emptyList();
    }

    return ids.stream()
        .map(privileges::get)
        .filter(Objects::nonNull)
        .toList();
  }

  @Override
  public CPrivilege addPrivilege(final CPrivilege privilege) {
    checkNotNull(privilege);
    checkNotNull(privilege.getId());
    checkState(privileges.putIfAbsent(privilege.getId(), privilege) == null, "%s already exists", privilege.getId());
    return privilege;
  }

  public void setPrivileges(final Collection<CPrivilege> privileges) {
    this.privileges.clear();
    if (privileges != null) {
      for (CPrivilege privilege : privileges) {
        addPrivilege(privilege);
      }
    }
  }

  public MemorySecurityConfiguration withPrivileges(final CPrivilege... privileges) {
    setPrivileges(new ArrayList<>(Arrays.asList(privileges)));
    return this;
  }

  @Override
  public void updatePrivilege(final CPrivilege privilege) {
    checkNotNull(privilege);
    checkNotNull(privilege.getId());
    if (privileges.replace(privilege.getId(), privilege) == null) {
      throw new NoSuchPrivilegeException(privilege.getId());
    }
  }

  @Override
  public void updatePrivilegeByName(final CPrivilege privilege) {
    updatePrivilege(privilege);
  }

  @Override
  public boolean removePrivilege(final String id) {
    checkNotNull(id);
    return privileges.remove(id) != null;
  }

  @Override
  public boolean removePrivilegeByName(final String name) {
    return Optional.ofNullable(name)
        .map(this::getPrivilegeByName)
        .map(CPrivilege::getId)
        .map(this::removePrivilege)
        .orElse(false);
  }

  @Override
  public List<CRole> getRoles() {
    return ImmutableList.copyOf(roles.values());
  }

  @Override
  public CRole getRole(final String id) {
    checkNotNull(id);
    return roles.get(id);
  }

  @Override
  public void addRole(final CRole role) {
    checkNotNull(role);
    checkNotNull(role.getId());
    checkState(roles.putIfAbsent(role.getId(), role) == null, "%s already exists", role.getId());
  }

  public void setRoles(final Collection<CRole> roles) {
    this.roles.clear();
    if (roles != null) {
      for (CRole role : roles) {
        addRole(role);
      }
    }
  }

  public MemorySecurityConfiguration withRoles(final CRole... roles) {
    setRoles(Arrays.asList(roles));
    return this;
  }

  @Override
  public void updateRole(final CRole role) {
    checkNotNull(role);
    checkNotNull(role.getId());
    if (roles.replace(role.getId(), role) == null) {
      throw new NoSuchRoleException(role.getId());
    }
  }

  @Override
  public boolean removeRole(final String id) {
    checkNotNull(id);
    return roles.remove(id) != null;
  }

  @Override
  public MemorySecurityConfiguration clone() throws CloneNotSupportedException {
    MemorySecurityConfiguration copy = (MemorySecurityConfiguration) super.clone();

    copy.users.putAll(this.users);
    copy.roles.putAll(this.roles);
    copy.privileges.putAll(this.privileges);
    copy.userRoleMappings.putAll(this.userRoleMappings);

    return copy;
  }

  private String userRoleMappingKey(final String userId, final String source) {
    return (isCaseInsensitiveSource(source) ? userId.toLowerCase() : userId) + "|" + source;
  }

  @Override
  public CUserRoleMapping newUserRoleMapping() {
    return new MemoryCUserRoleMapping();
  }

  @Override
  public CRole newRole() {
    return new MemoryCRole();
  }

  @Override
  public CPrivilege newPrivilege() {
    return new MemoryCPrivilege();
  }
}
