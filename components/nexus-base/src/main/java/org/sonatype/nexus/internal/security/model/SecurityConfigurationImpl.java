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
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.datastore.api.DataAccess;
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
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.collect.ImmutableList;
import org.apache.shiro.util.CollectionUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;
import static org.sonatype.nexus.security.user.UserManager.DEFAULT_SOURCE;

/**
 * MyBatis {@link SecurityConfiguration} implementation.
 *
 * @since 3.21
 */
@Named("mybatis")
@Singleton
public class SecurityConfigurationImpl
    extends StateGuardLifecycleSupport
    implements TransactionalStore<DataSession<?>>, SecurityConfiguration
{
  private final DataSessionSupplier sessionSupplier;

  @Inject
  public SecurityConfigurationImpl(final DataSessionSupplier sessionSupplier) {
    this.sessionSupplier = checkNotNull(sessionSupplier);
  }

  // SESSION

  @Override
  public DataSession<?> openSession() {
    return sessionSupplier.openSession(DEFAULT_DATASTORE_NAME);
  }

  private DataSession<?> thisSession() {
    return UnitOfWork.currentSession();
  }

  private <T extends DataAccess> T dao(final Class<T> daoClass) {
    return thisSession().access(daoClass);
  }

  private CPrivilegeDAO privilegeDAO() {
    return dao(CPrivilegeDAO.class);
  }

  private CRoleDAO roleDAO() {
    return dao(CRoleDAO.class);
  }

  private CUserDAO userDAO() {
    return dao(CUserDAO.class);
  }

  private CUserRoleMappingDAO userRoleMappingDAO() {
    return dao(CUserRoleMappingDAO.class);
  }

  // PRIVILEGES

  @Transactional
  @Override
  public List<CPrivilege> getPrivileges() {
    return ImmutableList.copyOf(privilegeDAO().browse());
  }

  @Transactional
  @Override
  public CPrivilege getPrivilege(final String id) {
    checkNotNull(id);

    return privilegeDAO().read(id).orElse(null);
  }

  @Nullable
  @Transactional
  @Override
  public CPrivilege getPrivilegeByName(final String name) {
    return Optional.of(name)
        .flatMap(n -> privilegeDAO().readByName(n))
        .orElse(null);
  }

  @Transactional
  @Override
  public List<CPrivilege> getPrivileges(final Set<String> ids) {
    if (CollectionUtils.isEmpty(ids)) {
      return Collections.emptyList();
    }

    return privilegeDAO().findByIds(ids);
  }

  @Transactional
  @Override
  public CPrivilege newPrivilege() {
    return new CPrivilegeData();
  }

  @Transactional
  @Override
  public CPrivilege addPrivilege(final CPrivilege privilege) {
    checkNotNull(privilege);
    try {
      privilegeDAO().create(convert(privilege));
      return privilege;
    }
    catch (DuplicateKeyException e) {
      throw new DuplicatePrivilegeException(privilege.getId());
    }
  }

  @Transactional
  @Override
  public void updatePrivilege(final CPrivilege privilege) {
    checkNotNull(privilege);

    privilege.setVersion(privilege.getVersion() + 1);
    if (!privilegeDAO().update(convert(privilege))) {
      throw new NoSuchPrivilegeException(privilege.getId());
    }
  }

  @Transactional
  @Override
  public void updatePrivilegeByName(final CPrivilege privilege) {
    Optional.of(privilege)
        .map(p -> {
          p.setVersion(p.getVersion() + 1);
          return p;
        })
        .filter(p -> privilegeDAO().updateByName(convert(p)))
        .orElseThrow(() -> new NoSuchPrivilegeException(privilege.getName()));
  }

  @Transactional
  @Override
  public boolean removePrivilege(final String id) {
    checkNotNull(id);

    if (!privilegeDAO().delete(id)) {
      throw new NoSuchPrivilegeException(id);
    }
    return true;
  }

  @Transactional
  @Override
  public boolean removePrivilegeByName(final String name) {
    return Optional.of(name)
        .map(n -> privilegeDAO().deleteByName(n))
        .filter(Boolean.TRUE::equals)
        .orElseThrow(() -> new NoSuchPrivilegeException(name));
  }

  // ROLES

  @Transactional
  @Override
  public List<CRole> getRoles() {
    return ImmutableList.copyOf(roleDAO().browse());
  }

  @Transactional
  @Override
  public CRole getRole(final String id) {
    checkNotNull(id);

    return roleDAO().read(id).orElse(null);
  }

  @Transactional
  @Override
  public CRole newRole() {
    return new CRoleData();
  }

  @Transactional
  @Override
  public void addRole(final CRole role) {
    try {
      roleDAO().create(convert(role));
    }
    catch (DuplicateKeyException e) {
      throw new DuplicateRoleException(role.getId());
    }
  }

  @Transactional
  @Override
  public void updateRole(final CRole role) {
    checkNotNull(role);

    role.setVersion(role.getVersion() + 1);
    if (!roleDAO().update(convert(role))) {
      throw new NoSuchRoleException(role.getId());
    }
  }

  @Transactional
  @Override
  public boolean removeRole(final String id) {
    checkNotNull(id);

    if (!roleDAO().delete(id)) {
      throw new NoSuchRoleException(id);
    }
    return true;
  }

  // USERS

  @Transactional
  @Override
  public List<CUser> getUsers() {
    return ImmutableList.copyOf(userDAO().browse());
  }

  @Transactional
  @Override
  public CUser getUser(final String id) {
    checkNotNull(id);

    return userDAO().read(id).orElse(null);
  }

  @Transactional
  @Override
  public CUser newUser() {
    return new CUserData();
  }

  @Transactional
  public void addUser(final CUser user) {
    checkNotNull(user);
    try {
      userDAO().create(convert(user));
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

  @Transactional
  @Override
  public void addRoleMapping(final String userId, final Set<String> roles, String source) {
    CUserRoleMappingData mapping = new CUserRoleMappingData();
    mapping.setUserId(userId);
    mapping.setSource(source);
    mapping.setRoles(roles);
    userRoleMappingDAO().create(mapping);
  }

  @Transactional
  @Override
  public void updateUser(final CUser user) throws UserNotFoundException {
    checkNotNull(user);

    user.setVersion(user.getVersion() + 1);
    if (!userDAO().update(convert(user))) {
      throw new UserNotFoundException(user.getId());
    }
  }

  @Transactional
  @Override
  public void updateUser(final CUser user, final Set<String> roles) throws UserNotFoundException {
    checkNotNull(user);
    checkNotNull(roles);

    updateUser(user);

    Optional<CUserRoleMappingData> existingMapping = userRoleMappingDAO().read(user.getId(), DEFAULT_SOURCE);
    if (existingMapping.isPresent()) {
      CUserRoleMappingData mapping = existingMapping.get();
      mapping.setRoles(roles);
      userRoleMappingDAO().update(mapping);
    }
    else {
      CUserRoleMappingData mapping = new CUserRoleMappingData();
      mapping.setUserId(user.getId());
      mapping.setSource(DEFAULT_SOURCE);
      mapping.setRoles(roles);
      userRoleMappingDAO().create(mapping);
    }
  }

  @Transactional
  @Override
  public boolean removeUser(final String id) {
    checkNotNull(id);

    if (userDAO().delete(id)) {
      removeUserRoleMapping(id, DEFAULT_SOURCE);
      return true;
    }
    return false;
  }

  // USER-ROLE MAPPINGS

  @Transactional
  @Override
  public List<CUserRoleMapping> getUserRoleMappings() {
    return ImmutableList.copyOf(userRoleMappingDAO().browse());
  }

  @Transactional
  @Override
  public CUserRoleMapping getUserRoleMapping(String userId, String source) {
    checkNotNull(userId);
    checkNotNull(source);

    return userRoleMappingDAO().read(userId, source).orElse(null);
  }

  @Transactional
  @Override
  public CUserRoleMapping newUserRoleMapping() {
    return new CUserRoleMappingData();
  }

  @Transactional
  @Override
  public void addUserRoleMapping(final CUserRoleMapping mapping) {
    checkNotNull(mapping);

    userRoleMappingDAO().create(convert(mapping));
  }

  @Transactional
  @Override
  public void updateUserRoleMapping(final CUserRoleMapping mapping) throws NoSuchRoleMappingException {
    checkNotNull(mapping);

    if (!userRoleMappingDAO().update(convert(mapping))) {
      throw new NoSuchRoleMappingException(mapping.getUserId());
    }
  }

  @Transactional
  @Override
  public boolean removeUserRoleMapping(final String userId, final String source) {
    checkNotNull(userId);
    checkNotNull(source);

    return userRoleMappingDAO().delete(userId, source);
  }

  private CPrivilegeData convert(final CPrivilege privilege) {
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

  private CRoleData convert(final CRole role) {
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

  private CUserData convert(final CUser user) {
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

  private CUserRoleMappingData convert(final CUserRoleMapping mapping) {
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
