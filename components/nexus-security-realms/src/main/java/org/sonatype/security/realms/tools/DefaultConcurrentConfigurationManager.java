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
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.security.authorization.NoSuchPrivilegeException;
import org.sonatype.security.authorization.NoSuchRoleException;
import org.sonatype.security.model.CPrivilege;
import org.sonatype.security.model.CRole;
import org.sonatype.security.model.CUser;
import org.sonatype.security.model.CUserRoleMapping;
import org.sonatype.security.realms.privileges.PrivilegeDescriptor;
import org.sonatype.security.realms.validator.SecurityValidationContext;
import org.sonatype.security.usermanagement.UserNotFoundException;
import org.sonatype.sisu.goodies.common.ComponentSupport;

/**
 * Default implementation of the ConfigurationManager interface. Intended to
 * provide a way to access the ConfigurationManager in a thread-safe manner.
 *
 * The old implementations (DefaultConfigurationManager and ResourceMergingConfigurationManager), should not
 * be used directly, as they cannot be used in a thread-safe manner. Instead, this implementation should be used.
 * It wraps ResourceMergingConfigurationManager, which wraps DefaultConfigurationManager.
 *
 * @author Steve Carlucci
 */
@Singleton
@Typed(ConfigurationManager.class)
@Named("default")
public class DefaultConcurrentConfigurationManager
    extends ComponentSupport
    implements ConfigurationManager
{
  private final ConfigurationManager configurationManager;

  private final ReentrantReadWriteLock readWriteLock;

  private final Lock readLock;

  private final Lock writeLock;

  private final long lockTimeout;

  @Inject
  public DefaultConcurrentConfigurationManager(@Named("resourceMerging") ConfigurationManager configurationManager,
                                               @Named("${security.configmgr.locktimeout:-60}") long lockTimeout)
  {
    this.configurationManager = configurationManager;

    this.readWriteLock = new ReentrantReadWriteLock();
    this.readLock = this.readWriteLock.readLock();
    this.writeLock = this.readWriteLock.writeLock();
    this.lockTimeout = lockTimeout;
    log.debug("Lock timeout: {} seconds", lockTimeout);
  }

  @Override
  public void runRead(ConfigurationManagerAction action) throws Exception {
    acquireLock(readLock);
    try {
      action.run();
    }
    finally {
      releaseLock(readLock);
    }
  }

  @Override
  public void runWrite(ConfigurationManagerAction action) throws Exception {
    acquireLock(writeLock);
    try {
      action.run();
    }
    finally {
      releaseLock(writeLock);
    }
  }

  @Override
  public void deleteUserRoleMapping(String userId, String source)
      throws NoSuchRoleMappingException
  {
    checkWriteLock();
    configurationManager.deleteUserRoleMapping(userId, source);
  }

  @Override
  public void deleteUser(String id)
      throws UserNotFoundException
  {
    checkWriteLock();
    configurationManager.deleteUser(id);
  }

  @Override
  public void deleteRole(String id)
      throws NoSuchRoleException
  {
    checkWriteLock();
    configurationManager.deleteRole(id);
  }

  @Override
  public void deletePrivilege(String id)
      throws NoSuchPrivilegeException
  {
    checkWriteLock();
    configurationManager.deletePrivilege(id);
  }

  @Override
  public String getPrivilegeProperty(String id, String key)
      throws NoSuchPrivilegeException
  {
    readLock.lock();
    try {
      return configurationManager.getPrivilegeProperty(id, key);
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public void clearCache() {
    checkWriteLock();
    configurationManager.clearCache();
  }

  @Override
  public void save() {
    checkWriteLock();
    configurationManager.save();
  }

  @Override
  public void cleanRemovedRole(String roleId) {
    checkWriteLock();
    configurationManager.cleanRemovedRole(roleId);
  }

  @Override
  public void cleanRemovedPrivilege(String privilegeId) {
    checkWriteLock();
    configurationManager.cleanRemovedPrivilege(privilegeId);
  }

  @Override
  public List<CUser> listUsers() {
    readLock.lock();
    try {
      return configurationManager.listUsers();
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public List<CRole> listRoles() {
    readLock.lock();
    try {
      return configurationManager.listRoles();
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public List<CPrivilege> listPrivileges() {
    readLock.lock();
    try {
      return configurationManager.listPrivileges();
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public List<PrivilegeDescriptor> listPrivilegeDescriptors() {
    readLock.lock();
    try {
      return configurationManager.listPrivilegeDescriptors();
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public void createUser(CUser user, Set<String> roles)
      throws InvalidConfigurationException
  {
    checkWriteLock();
    configurationManager.createUser(user, roles);
  }

  @Override
  public void createUser(CUser user, String password, Set<String> roles)
      throws InvalidConfigurationException
  {
    checkWriteLock();
    configurationManager.createUser(user, password, roles);
  }

  @Override
  public void createUser(CUser user, Set<String> roles, SecurityValidationContext context)
      throws InvalidConfigurationException
  {
    checkWriteLock();
    configurationManager.createUser(user, roles, context);
  }

  @Override
  public void createUser(CUser user, String password, Set<String> roles, SecurityValidationContext context)
      throws InvalidConfigurationException
  {
    checkWriteLock();
    configurationManager.createUser(user, password, roles, context);
  }

  @Override
  public void createRole(CRole role)
      throws InvalidConfigurationException
  {
    checkWriteLock();
    configurationManager.createRole(role);
  }

  @Override
  public void createRole(CRole role, SecurityValidationContext context)
      throws InvalidConfigurationException
  {
    checkWriteLock();
    configurationManager.createRole(role, context);
  }

  @Override
  public void createPrivilege(CPrivilege privilege)
      throws InvalidConfigurationException
  {
    checkWriteLock();
    configurationManager.createPrivilege(privilege);
  }

  @Override
  public void createPrivilege(CPrivilege privilege, SecurityValidationContext context)
      throws InvalidConfigurationException
  {
    checkWriteLock();
    configurationManager.createPrivilege(privilege, context);
  }

  @Override
  public CUser readUser(String id)
      throws UserNotFoundException
  {
    readLock.lock();
    try {
      return configurationManager.readUser(id);
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public CRole readRole(String id)
      throws NoSuchRoleException
  {
    readLock.lock();
    try {
      return configurationManager.readRole(id);
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public CPrivilege readPrivilege(String id)
      throws NoSuchPrivilegeException
  {
    readLock.lock();
    try {
      return configurationManager.readPrivilege(id);
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public void updateUser(CUser user)
      throws InvalidConfigurationException, UserNotFoundException
  {
    checkWriteLock();
    configurationManager.updateUser(user);
  }

  @Override
  public void updateUser(CUser user, Set<String> roles)
      throws InvalidConfigurationException, UserNotFoundException
  {
    checkWriteLock();
    configurationManager.updateUser(user, roles);
  }

  @Override
  public void updateUser(CUser user, Set<String> roles, SecurityValidationContext context)
      throws InvalidConfigurationException, UserNotFoundException
  {
    checkWriteLock();
    configurationManager.updateUser(user, roles, context);
  }

  @Override
  public void updateRole(CRole role)
      throws InvalidConfigurationException, NoSuchRoleException
  {
    checkWriteLock();
    configurationManager.updateRole(role);
  }

  @Override
  public void updateRole(CRole role, SecurityValidationContext context)
      throws InvalidConfigurationException, NoSuchRoleException
  {
    checkWriteLock();
    configurationManager.updateRole(role, context);
  }

  @Override
  public void createUserRoleMapping(CUserRoleMapping userRoleMapping)
      throws InvalidConfigurationException
  {
    checkWriteLock();
    configurationManager.createUserRoleMapping(userRoleMapping);
  }

  @Override
  public void createUserRoleMapping(CUserRoleMapping userRoleMapping, SecurityValidationContext context)
      throws InvalidConfigurationException
  {
    checkWriteLock();
    configurationManager.createUserRoleMapping(userRoleMapping, context);
  }

  @Override
  public void updateUserRoleMapping(CUserRoleMapping userRoleMapping)
      throws InvalidConfigurationException, NoSuchRoleMappingException
  {
    checkWriteLock();
    configurationManager.updateUserRoleMapping(userRoleMapping);
  }

  @Override
  public void updateUserRoleMapping(CUserRoleMapping userRoleMapping, SecurityValidationContext context)
      throws InvalidConfigurationException, NoSuchRoleMappingException
  {
    checkWriteLock();
    configurationManager.updateUserRoleMapping(userRoleMapping, context);
  }

  @Override
  public CUserRoleMapping readUserRoleMapping(String userId, String source)
      throws NoSuchRoleMappingException
  {
    readLock.lock();
    try {
      return configurationManager.readUserRoleMapping(userId, source);
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public List<CUserRoleMapping> listUserRoleMappings() {
    readLock.lock();
    try {
      return configurationManager.listUserRoleMappings();
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public void updatePrivilege(CPrivilege privilege)
      throws InvalidConfigurationException, NoSuchPrivilegeException
  {
    checkWriteLock();
    configurationManager.updatePrivilege(privilege);
  }

  @Override
  public void updatePrivilege(CPrivilege privilege, SecurityValidationContext context)
      throws InvalidConfigurationException, NoSuchPrivilegeException
  {
    checkWriteLock();
    configurationManager.updatePrivilege(privilege, context);
  }

  @Override
  public String getPrivilegeProperty(CPrivilege privilege, String key) {
    readLock.lock();
    try {
      return configurationManager.getPrivilegeProperty(privilege, key);
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public SecurityValidationContext initializeContext() {
    readLock.lock();
    try {
      return configurationManager.initializeContext();
    }
    finally {
      readLock.unlock();
    }
  }

  /**
   * Attempt to acquire specified lock
   *
   * @param lock the lock to acquire
   * @throws IllegalStateException if lock could not be acquired
   */
  private void acquireLock(Lock lock) {
    try {
      if (!lock.tryLock(lockTimeout, TimeUnit.SECONDS)) {
        //Unable to acquire lock
        throw new IllegalStateException("Unable to acquire lock");
      }
    }
    catch (InterruptedException e) {
      throw new IllegalStateException("Unable to acquire lock", e);
    }
  }

  /**
   * Release specified lock
   *
   * @param lock lock to unlock
   */
  private void releaseLock(Lock lock) {
    lock.unlock();
  }

  /**
   * Checks that the currently executing thread holds a write lock
   *
   * @throws IllegalStateException if thread does not hold a write lock
   */
  private void checkWriteLock() {
    if (readWriteLock.getWriteHoldCount() == 0) {
      throw new IllegalStateException("Method called without proper locking");
    }
  }
}
