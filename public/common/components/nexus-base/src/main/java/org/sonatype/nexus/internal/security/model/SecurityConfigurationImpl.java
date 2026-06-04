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
package org.sonatype.nexus.internal.security.model;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.datastore.api.DuplicateKeyException;
import org.sonatype.nexus.security.config.CPrivilege;
import org.sonatype.nexus.security.config.CRole;
import org.sonatype.nexus.security.config.CUser;
import org.sonatype.nexus.security.config.CUserRoleMapping;
import org.sonatype.nexus.security.config.SecurityConfiguration;
import org.sonatype.nexus.security.privilege.DuplicatePrivilegeException;
import org.sonatype.nexus.security.privilege.NoSuchPrivilegeException;
import org.sonatype.nexus.security.role.DuplicateRoleException;
import org.sonatype.nexus.security.role.NoSuchRoleException;
import org.sonatype.nexus.security.user.DuplicateUserException;
import org.sonatype.nexus.security.user.NoSuchRoleMappingException;
import org.sonatype.nexus.security.user.UserNotFoundException;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.TransactionalStore;

import com.google.common.collect.ImmutableList;
import org.apache.shiro.util.CollectionUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;
import static org.sonatype.nexus.security.user.UserManager.DEFAULT_SOURCE;

/**
 * MyBatis {@link SecurityConfiguration} implementation.
 *
 * @since 3.21
 */
@Component
@Qualifier("mybatis")
public class SecurityConfigurationImpl
    extends StateGuardLifecycleSupport
    implements SecurityConfiguration, TransactionalStore<DataSession<?>>
{
  private final DataSessionSupplier sessionSupplier;

  private final CPrivilegeStore privilegeStore;

  private final CRoleStore roleStore;

  private final CUserRoleMappingStore userRoleMappingStore;

  private final CUserStore userStore;

  @Autowired
  public SecurityConfigurationImpl(
      final DataSessionSupplier sessionSupplier,
      final CPrivilegeStore privilegeStore,
      final CRoleStore roleStore,
      final CUserRoleMappingStore userRoleMappingStore,
      final CUserStore userStore)
  {
    this.sessionSupplier = checkNotNull(sessionSupplier);
    this.privilegeStore = checkNotNull(privilegeStore);
    this.roleStore = checkNotNull(roleStore);
    this.userRoleMappingStore = checkNotNull(userRoleMappingStore);
    this.userStore = checkNotNull(userStore);
  }

  @Override
  public DataSession<?> openSession() {
    return sessionSupplier.openSession(DEFAULT_DATASTORE_NAME);
  }

  // PRIVILEGES
  @Override
  public List<CPrivilege> getPrivileges() {
    return ImmutableList.copyOf(privilegeStore.browse());
  }

  @Override
  public CPrivilege getPrivilege(final String id) {
    checkNotNull(id);

    return privilegeStore.read(id).orElse(null);
  }

  @Nullable
  @Override
  public CPrivilege getPrivilegeByName(final String name) {
    return Optional.of(name)
        .flatMap(n -> privilegeStore.readByName(n))
        .orElse(null);
  }

  @Override
  public List<CPrivilege> getPrivileges(final Set<String> ids) {
    if (CollectionUtils.isEmpty(ids)) {
      return Collections.emptyList();
    }

    return privilegeStore.findByIds(ids);
  }

  @Override
  public CPrivilege newPrivilege() {
    return new CPrivilegeData();
  }

  @Override
  public CPrivilege addPrivilege(final CPrivilege privilege) {
    checkNotNull(privilege);
    try {
      privilegeStore.create(convert(privilege));
      return privilege;
    }
    catch (DuplicateKeyException e) {
      throw new DuplicatePrivilegeException(privilege.getId());
    }
  }

  @Override
  public void updatePrivilege(final CPrivilege privilege) {
    checkNotNull(privilege);

    privilege.setVersion(privilege.getVersion() + 1);
    if (!privilegeStore.update(convert(privilege))) {
      throw new NoSuchPrivilegeException(privilege.getId());
    }
  }

  @Override
  public void updatePrivilegeByName(final CPrivilege privilege) {
    Optional.of(privilege)
        .map(p -> {
          p.setVersion(p.getVersion() + 1);
          return p;
        })
        .filter(p -> privilegeStore.updateByName(convert(p)))
        .orElseThrow(() -> new NoSuchPrivilegeException(privilege.getName()));
  }

  @Override
  public boolean removePrivilege(final String id) {
    checkNotNull(id);

    if (!privilegeStore.delete(id)) {
      throw new NoSuchPrivilegeException(id);
    }
    return true;
  }

  @Override
  public boolean removePrivilegeByName(final String name) {
    return Optional.of(name)
        .map(n -> privilegeStore.deleteByName(n))
        .filter(Boolean.TRUE::equals)
        .orElseThrow(() -> new NoSuchPrivilegeException(name));
  }

  // ROLES
  @Override
  public List<CRole> getRoles() {
    return ImmutableList.copyOf(roleStore.browse());
  }

  @Override
  public CRole getRole(final String id) {
    checkNotNull(id);

    return roleStore.read(id).orElse(null);
  }

  @Override
  public List<CRole> getRoles(final java.util.Collection<String> ids) {
    checkNotNull(ids);

    if (ids.isEmpty()) {
      return ImmutableList.of();
    }

    return ImmutableList.copyOf(roleStore.readByIds(ids));
  }

  @Override
  public CRole newRole() {
    return new CRoleData();
  }

  @Override
  public void addRole(final CRole role) {
    try {
      roleStore.create(convert(role));
    }
    catch (DuplicateKeyException e) {
      throw new DuplicateRoleException(role.getId());
    }
  }

  @Override
  public void updateRole(final CRole role) {
    checkNotNull(role);

    role.setVersion(role.getVersion() + 1);
    if (!roleStore.update(convert(role))) {
      throw new NoSuchRoleException(role.getId());
    }
  }

  @Override
  public boolean removeRole(final String id) {
    checkNotNull(id);

    if (!roleStore.delete(id)) {
      throw new NoSuchRoleException(id);
    }
    return true;
  }

  // USERS
  @Override
  public List<CUser> getUsers() {
    return ImmutableList.copyOf(userStore.browse());
  }

  @Override
  public CUser getUser(final String id) {
    checkNotNull(id);

    return userStore.read(id).orElse(null);
  }

  @Override
  public CUser newUser() {
    return new CUserData();
  }

  public void addUser(final CUser user) {
    checkNotNull(user);
    try {
      userStore.create(convert(user));
    }
    catch (DuplicateKeyException e) {
      throw new DuplicateUserException(user.getId());
    }
  }

  @Transactional
  @Override
  public void addUser(final CUser user, final Set<String> roles) {
    checkNotNull(user);
    checkNotNull(roles);
    addUser(user);
    addRoleMapping(user.getId(), roles, DEFAULT_SOURCE);
  }

  @Override
  public void addRoleMapping(final String userId, final Set<String> roles, final String source) {
    CUserRoleMappingData mapping = new CUserRoleMappingData();
    mapping.setUserId(userId);
    mapping.setSource(source);
    mapping.setRoles(roles);
    userRoleMappingStore.create(mapping);
  }

  @Override
  public void updateUser(final CUser user) throws UserNotFoundException {
    checkNotNull(user);

    user.setVersion(user.getVersion() + 1);
    if (!userStore.update(convert(user))) {
      throw new UserNotFoundException(user.getId());
    }
  }

  @Transactional
  @Override
  public void updateUser(final CUser user, final Set<String> roles) throws UserNotFoundException {
    checkNotNull(user);
    checkNotNull(roles);

    updateUser(user);

    Optional<CUserRoleMappingData> existingMapping = userRoleMappingStore.read(user.getId(), DEFAULT_SOURCE);
    if (existingMapping.isPresent()) {
      CUserRoleMappingData mapping = existingMapping.get();
      mapping.setRoles(roles);
      userRoleMappingStore.update(mapping);
    }
    else {
      CUserRoleMappingData mapping = new CUserRoleMappingData();
      mapping.setUserId(user.getId());
      mapping.setSource(DEFAULT_SOURCE);
      mapping.setRoles(roles);
      userRoleMappingStore.create(mapping);
    }
  }

  @Override
  public boolean removeUser(final String id) {
    checkNotNull(id);

    if (userStore.delete(id)) {
      removeUserRoleMapping(id, DEFAULT_SOURCE);
      return true;
    }
    return false;
  }

  // USER-ROLE MAPPINGS
  @Override
  public List<CUserRoleMapping> getUserRoleMappings() {
    return ImmutableList.copyOf(userRoleMappingStore.browse());
  }

  @Override
  public CUserRoleMapping getUserRoleMapping(final String userId, final String source) {
    checkNotNull(userId);
    checkNotNull(source);

    return userRoleMappingStore.read(userId, source).orElse(null);
  }

  @Override
  public CUserRoleMapping newUserRoleMapping() {
    return new CUserRoleMappingData();
  }

  @Override
  public void addUserRoleMapping(final CUserRoleMapping mapping) {
    checkNotNull(mapping);

    userRoleMappingStore.create(convert(mapping));
  }

  @Override
  public void updateUserRoleMapping(final CUserRoleMapping mapping) throws NoSuchRoleMappingException {
    checkNotNull(mapping);

    if (!userRoleMappingStore.update(convert(mapping))) {
      throw new NoSuchRoleMappingException(mapping.getUserId());
    }
  }

  @Override
  public boolean removeUserRoleMapping(final String userId, final String source) {
    checkNotNull(userId);
    checkNotNull(source);

    return userRoleMappingStore.delete(userId, source);
  }

  private static CPrivilegeData convert(final CPrivilege privilege) {
    if (privilege instanceof CPrivilegeData) {
      return (CPrivilegeData) privilege;
    }
    CPrivilegeData privilegeData = new CPrivilegeData();
    privilegeData.setId(privilege.getId());
    privilegeData.setVersion(privilege.getVersion());
    privilegeData.setName(privilege.getName());
    privilegeData.setDescription(privilege.getDescription());
    privilegeData.setReadOnly(privilege.isReadOnly());
    privilegeData.setProperties(privilege.getProperties());
    privilegeData.setType(privilege.getType());
    return privilegeData;
  }

  private static CRoleData convert(final CRole role) {
    if (role instanceof CRoleData) {
      return (CRoleData) role;
    }
    CRoleData roleData = new CRoleData();
    roleData.setId(role.getId());
    roleData.setVersion(role.getVersion());
    roleData.setName(role.getName());
    roleData.setDescription(role.getDescription());
    roleData.setReadOnly(role.isReadOnly());
    roleData.setRoles(role.getRoles());
    roleData.setPrivileges(role.getPrivileges());
    return roleData;
  }

  private static CUserData convert(final CUser user) {
    if (user instanceof CUserData) {
      return (CUserData) user;
    }
    CUserData userData = new CUserData();
    userData.setId(user.getId());
    userData.setVersion(user.getVersion());
    userData.setFirstName(user.getFirstName());
    userData.setLastName(user.getLastName());
    userData.setEmail(user.getEmail());
    userData.setStatus(user.getStatus());
    userData.setPassword(user.getPassword());
    return userData;
  }

  private static CUserRoleMappingData convert(final CUserRoleMapping mapping) {
    if (mapping instanceof CUserRoleMappingData) {
      return (CUserRoleMappingData) mapping;
    }
    CUserRoleMappingData mappingData = new CUserRoleMappingData();
    mappingData.setUserId(mapping.getUserId());
    mappingData.setSource(mapping.getSource());
    mappingData.setVersion(mapping.getVersion());
    mappingData.setRoles(mapping.getRoles());
    return mappingData;
  }
}
