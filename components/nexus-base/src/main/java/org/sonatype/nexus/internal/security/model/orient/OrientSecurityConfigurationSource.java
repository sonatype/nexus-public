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
package org.sonatype.nexus.internal.security.model.orient;

import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseInstanceNames;
import org.sonatype.nexus.security.config.CPrivilege;
import org.sonatype.nexus.security.config.CRole;
import org.sonatype.nexus.security.config.CUser;
import org.sonatype.nexus.security.config.CUserRoleMapping;
import org.sonatype.nexus.security.config.SecurityConfiguration;
import org.sonatype.nexus.security.config.SecurityConfigurationSource;
import org.sonatype.nexus.security.privilege.DuplicatePrivilegeException;
import org.sonatype.nexus.security.privilege.NoSuchPrivilegeException;
import org.sonatype.nexus.security.role.DuplicateRoleException;
import org.sonatype.nexus.security.role.NoSuchRoleException;
import org.sonatype.nexus.security.user.NoSuchRoleMappingException;
import org.sonatype.nexus.security.user.UserNotFoundException;

import com.google.common.collect.ImmutableList;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SCHEMAS;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTx;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTxRetry;
import static org.sonatype.nexus.security.user.UserManager.DEFAULT_SOURCE;

/**
 * Default {@link SecurityConfigurationSource} implementation using OrientDB as store.
 *
 * @since 3.0
 */
@Named("orient")
@Priority(Integer.MAX_VALUE)
@ManagedLifecycle(phase = SCHEMAS)
@Singleton
public class OrientSecurityConfigurationSource
    extends StateGuardLifecycleSupport
    implements SecurityConfigurationSource
{
  /**
   * Security database.
   */
  private final Provider<DatabaseInstance> databaseInstance;

  /**
   * The defaults configuration source.
   */
  private final SecurityConfigurationSource securityDefaults;

  private final OrientCUserEntityAdapter userEntityAdapter;

  private final OrientCRoleEntityAdapter roleEntityAdapter;

  private final OrientCPrivilegeEntityAdapter privilegeEntityAdapter;

  private final OrientCUserRoleMappingEntityAdapter userRoleMappingEntityAdapter;

  /**
   * The configuration.
   */
  private SecurityConfiguration configuration;

  @Inject
  public OrientSecurityConfigurationSource(
      @Named(DatabaseInstanceNames.SECURITY) final Provider<DatabaseInstance> databaseInstance,
      @Named("static") final SecurityConfigurationSource defaults,
      final OrientCUserEntityAdapter userEntityAdapter,
      final OrientCRoleEntityAdapter roleEntityAdapter,
      final OrientCPrivilegeEntityAdapter privilegeEntityAdapter,
      final OrientCUserRoleMappingEntityAdapter userRoleMappingEntityAdapter)
  {
    this.databaseInstance = checkNotNull(databaseInstance);
    this.securityDefaults = checkNotNull(defaults);
    this.userEntityAdapter = checkNotNull(userEntityAdapter);
    this.roleEntityAdapter = checkNotNull(roleEntityAdapter);
    this.privilegeEntityAdapter = checkNotNull(privilegeEntityAdapter);
    this.userRoleMappingEntityAdapter = checkNotNull(userRoleMappingEntityAdapter);
  }

  @Override
  protected void doStart() {
    try (ODatabaseDocumentTx db = databaseInstance.get().connect()) {
      userEntityAdapter.register(db, new Runnable()
      {
        @Override
        public void run() {
          List<CUser> users = securityDefaults.getConfiguration().getUsers();
          if (users != null && !users.isEmpty()) {
            log.info("Initializing default users");
            for (CUser user : users) {
              userEntityAdapter.addEntity(db, convert(user));
            }
          }
        }
      });

      roleEntityAdapter.register(db, new Runnable()
      {
        @Override
        public void run() {
          List<CRole> roles = securityDefaults.getConfiguration().getRoles();
          if (roles != null && !roles.isEmpty()) {
            log.info("Initializing default roles");
            for (CRole role : roles) {
              roleEntityAdapter.addEntity(db, convert(role));
            }
          }
        }
      });

      privilegeEntityAdapter.register(db, new Runnable()
      {
        @Override
        public void run() {
          List<CPrivilege> privileges = securityDefaults.getConfiguration().getPrivileges();
          if (privileges != null && !privileges.isEmpty()) {
            log.info("Initializing default privileges");
            for (CPrivilege privilege : privileges) {
              privilegeEntityAdapter.addEntity(db, convert(privilege));
            }
          }
        }
      });

      userRoleMappingEntityAdapter.register(db, new Runnable()
      {
        @Override
        public void run() {
          List<CUserRoleMapping> mappings = securityDefaults.getConfiguration().getUserRoleMappings();
          if (mappings != null && !mappings.isEmpty()) {
            log.info("Initializing default user/role mappings");
            for (CUserRoleMapping mapping : mappings) {
              userRoleMappingEntityAdapter.addEntity(db, convert(mapping));
            }
          }
        }
      });
    }
  }

  @Override
  public SecurityConfiguration getConfiguration() {
    return configuration;
  }

  @Override
  public SecurityConfiguration loadConfiguration() {
    configuration = new OrientSecurityConfiguration();
    return getConfiguration();
  }

  private class OrientSecurityConfiguration
      implements SecurityConfiguration
  {
    private ConcurrentModificationException concurrentlyModified(
        final String type,
        final String value,
        final Exception e)
    {
      throw new ConcurrentModificationException(type + " '" + value + "' updated in the meantime", e);
    }

    //
    // Users
    //

    @Override
    public List<CUser> getUsers() {
      log.trace("Retrieving all users");

      return inTx(databaseInstance).call(db -> ImmutableList.copyOf(userEntityAdapter.browse(db)));
    }

    @Override
    public CUser getUser(final String id) {
      checkNotNull(id);
      log.trace("Retrieving user: {}", id);

      return inTx(databaseInstance).call(db -> userEntityAdapter.read(db, id));
    }

    @Override
    public void addUser(final CUser user, final Set<String> roles) {
      checkNotNull(user);
      checkNotNull(user.getId());
      checkUser(user);
      log.trace("Adding user: {}", user.getId());

      inTxRetry(databaseInstance).run(db -> {
        userEntityAdapter.addEntity(db, (OrientCUser) user);
        addUserRoleMapping(mapping(user.getId(), roles, DEFAULT_SOURCE));
      });
    }

    @Override
    public void addRoleMapping(final String userId, final Set<String> roles, final String source) {
      inTxRetry(databaseInstance).run(db -> addUserRoleMapping(mapping(userId, roles, source)));
    }

    @Override
    public void updateUser(final CUser user) throws UserNotFoundException {
      checkNotNull(user);
      checkNotNull(user.getId());
      checkUser(user);
      log.trace("Updating user: {}", user.getId());

      try {
        inTxRetry(databaseInstance).throwing(UserNotFoundException.class).run(db -> {
          CUser existing = userEntityAdapter.read(db, user.getId());
          if (existing == null) {
            throw new UserNotFoundException(user.getId());
          }
          userEntityAdapter.update(db, (OrientCUser) user);
        });
      }
      catch (OConcurrentModificationException e) {
        throw concurrentlyModified("User", user.getId(), e);
      }
    }

    @Override
    public void updateUser(final CUser user, final Set<String> roles) throws UserNotFoundException {
      try {
        inTxRetry(databaseInstance).throwing(UserNotFoundException.class).run(db -> {
          updateUser(user);

          CUserRoleMapping mapping = userRoleMappingEntityAdapter.read(db, user.getId(), DEFAULT_SOURCE);
          if (mapping == null) {
            addUserRoleMapping(mapping(user.getId(), roles));
          }
          else {
            try {
              mapping.setRoles(roles);
              updateUserRoleMapping(mapping);
            }
            catch (NoSuchRoleMappingException e) {
              throw new RuntimeException(e);
            }
          }
        });
      }
      catch (OConcurrentModificationException e) {
        throw concurrentlyModified("User", user.getId(), e);
      }
    }

    @Override
    public boolean removeUser(final String id) {
      checkNotNull(id);
      log.trace("Removing user: {}", id);

      try {
        return inTxRetry(databaseInstance).call(db -> {
          if (userEntityAdapter.delete(db, id)) {
            removeUserRoleMapping(id, DEFAULT_SOURCE);
            return true;
          }
          return false;
        });
      }
      catch (OConcurrentModificationException e) {
        throw concurrentlyModified("User", id, e);
      }
    }

    //
    // Privileges
    //

    @Override
    public List<CPrivilege> getPrivileges() {
      log.trace("Retrieving all privileges");

      return inTx(databaseInstance).call(db -> ImmutableList.copyOf(privilegeEntityAdapter.browse(db)));
    }

    @Override
    public CPrivilege getPrivilege(final String id) {
      checkNotNull(id);
      log.trace("Retrieving privilege {}", id);

      return inTx(databaseInstance).call(db -> privilegeEntityAdapter.read(db, id));
    }

    @Override
    public CPrivilege addPrivilege(final CPrivilege privilege) {
      checkNotNull(privilege);
      checkNotNull(privilege.getId());
      log.trace("Adding privilege: {}", privilege.getId());

      try {
        OrientCPrivilege persistedPrivilege = convert(privilege);
        inTxRetry(databaseInstance).run(db -> privilegeEntityAdapter.addEntity(db, persistedPrivilege));
        return persistedPrivilege;
      }
      catch (ORecordDuplicatedException e) {
        throw new DuplicatePrivilegeException(privilege.getId(), e);
      }
    }

    @Override
    public void updatePrivilege(final CPrivilege privilege) {
      checkNotNull(privilege);
      checkNotNull(privilege.getId());
      checkPrivilege(privilege);
      log.trace("Updating privilege: {}", privilege.getId());

      try {
        inTxRetry(databaseInstance).run(db -> {
          CPrivilege existing = privilegeEntityAdapter.read(db, privilege.getId());
          if (existing == null) {
            throw new NoSuchPrivilegeException(privilege.getId());
          }
          privilegeEntityAdapter.update(db, (OrientCPrivilege) privilege);
        });
      }
      catch (OConcurrentModificationException e) {
        throw concurrentlyModified("Privilege", privilege.getId(), e);
      }
    }

    @Override
    public boolean removePrivilege(final String id) {
      checkNotNull(id);
      log.trace("Removing privilege: {}", id);

      try {
        return inTxRetry(databaseInstance).call(db -> {
          CPrivilege existing = privilegeEntityAdapter.read(db, id);
          if (existing == null) {
            throw new NoSuchPrivilegeException(id);
          }
          return privilegeEntityAdapter.delete(db, id);
        });
      }
      catch (OConcurrentModificationException e) {
        throw concurrentlyModified("Privilege", id, e);
      }
    }

    //
    // Roles
    //

    @Override
    public List<CRole> getRoles() {
      log.trace("Retrieving all roles");

      return inTx(databaseInstance).call(db -> ImmutableList.copyOf(roleEntityAdapter.browse(db)));
    }

    @Override
    public CRole getRole(final String id) {
      checkNotNull(id);
      log.trace("Retrieving role: {}", id);

      return inTx(databaseInstance).call(db -> roleEntityAdapter.read(db, id));
    }

    @Override
    public void addRole(final CRole role) {
      checkNotNull(role);
      checkNotNull(role.getId());
      checkRole(role);
      log.trace("Adding role: {}", role.getId());

      try {
        inTxRetry(databaseInstance).run(db -> {
          if (roleEntityAdapter.read(db, role.getId()) != null) {
            throw new DuplicateRoleException(role.getId());
          }
          roleEntityAdapter.addEntity(db, (OrientCRole) role);
        });
      }
      catch (ORecordDuplicatedException e) {
        throw new DuplicateRoleException(role.getId(), e);
      }
    }

    @Override
    public void updateRole(final CRole role) {
      checkNotNull(role);
      checkNotNull(role.getId());
      checkRole(role);
      log.trace("Updating role: {}", role.getId());

      try {
        inTxRetry(databaseInstance).run(db -> {
          CRole existing = roleEntityAdapter.read(db, role.getId());
          if (existing == null) {
            throw new NoSuchRoleException(role.getId());
          }
          if (!Objects.equals(role.getVersion(), existing.getVersion())) {
            throw concurrentlyModified("Role", role.getId(), null);
          }
          roleEntityAdapter.update(db, (OrientCRole) role);
        });
      }
      catch (OConcurrentModificationException e) {
        throw concurrentlyModified("Role", role.getId(), e);
      }
    }

    @Override
    public boolean removeRole(final String id) {
      checkNotNull(id);
      log.trace("Removing role: {}", id);

      try {
        return inTxRetry(databaseInstance).call(db -> {
          CRole existing = roleEntityAdapter.read(db, id);
          if (existing == null) {
            throw new NoSuchRoleException(id);
          }
          return roleEntityAdapter.delete(db, id);
        });
      }
      catch (OConcurrentModificationException e) {
        throw concurrentlyModified("Role", id, e);
      }
    }

    //
    // User-Role Mappings
    //

    private OrientCUserRoleMapping mapping(final String userId, final Set<String> roles) {
      return mapping(userId, roles, DEFAULT_SOURCE);
    }

    private OrientCUserRoleMapping mapping(final String userId, final Set<String> roles, String source) {
      OrientCUserRoleMapping mapping = new OrientCUserRoleMapping();
      mapping.setUserId(userId);
      mapping.setSource(source);
      mapping.setRoles(roles);
      return mapping;
    }

    @Override
    public List<CUserRoleMapping> getUserRoleMappings() {
      log.trace("Retrieving all user/role mappings");

      return inTx(databaseInstance).call(db -> ImmutableList.copyOf(userRoleMappingEntityAdapter.browse(db)));
    }

    @Override
    public CUserRoleMapping getUserRoleMapping(final String userId, final String source) {
      checkNotNull(userId);
      checkNotNull(source);
      log.trace("Retrieving user/role mappings of: {}/{}", userId, source);

      return inTx(databaseInstance).call(db -> userRoleMappingEntityAdapter.read(db, userId, source));
    }

    @Override
    public void addUserRoleMapping(final CUserRoleMapping mapping) {
      checkNotNull(mapping);
      checkUserRoleMapping(mapping);
      checkNotNull(mapping.getUserId());
      checkNotNull(mapping.getSource());
      log.trace("Adding user/role mappings for: {}/{}", mapping.getUserId(), mapping.getSource());

      inTxRetry(databaseInstance)
          .run(db -> userRoleMappingEntityAdapter.addEntity(db, (OrientCUserRoleMapping) mapping));
    }

    @Override
    public void updateUserRoleMapping(final CUserRoleMapping mapping) throws NoSuchRoleMappingException {
      checkNotNull(mapping);
      checkUserRoleMapping(mapping);
      checkNotNull(mapping.getUserId());
      checkNotNull(mapping.getSource());
      log.trace("Updating user/role mappings for: {}/{}", mapping.getUserId(), mapping.getSource());

      try {
        inTxRetry(databaseInstance).throwing(NoSuchRoleMappingException.class).run(db -> {
          CUserRoleMapping existing = userRoleMappingEntityAdapter.read(db, mapping.getUserId(), mapping.getSource());
          if (existing == null) {
            throw new NoSuchRoleMappingException(mapping.getUserId());
          }
          if (!Objects.equals(mapping.getVersion(), existing.getVersion())) {
            throw concurrentlyModified("User-role mapping", mapping.getUserId(), null);
          }
          userRoleMappingEntityAdapter.update(db, (OrientCUserRoleMapping) mapping);
        });
      }
      catch (OConcurrentModificationException e) {
        throw concurrentlyModified("User-role mapping", mapping.getUserId(), e);
      }
    }

    @Override
    public boolean removeUserRoleMapping(final String userId, final String source) {
      checkNotNull(userId);
      checkNotNull(source);
      log.trace("Removing user/role mappings for: {}/{}", userId, source);

      try {
        return inTxRetry(databaseInstance).call(db -> userRoleMappingEntityAdapter.delete(db, userId, source));
      }
      catch (OConcurrentModificationException e) {
        throw concurrentlyModified("User-role mapping", userId, e);
      }
    }

    @Override
    public CRole newRole() {
      return roleEntityAdapter.newEntity();
    }

    @Override
    public CUser newUser() {
      return userEntityAdapter.newEntity();
    }

    @Override
    public CUserRoleMapping newUserRoleMapping() {
      return userRoleMappingEntityAdapter.newEntity();
    }

    @Override
    public CPrivilege newPrivilege() {
      return privilegeEntityAdapter.newEntity();
    }
  }

  private OrientCPrivilege convert(final CPrivilege privilege) {
    if (privilege instanceof OrientCPrivilege) {
      return (OrientCPrivilege) privilege;
    }
    OrientCPrivilege orientPrivilege = privilegeEntityAdapter.newEntity();

    orientPrivilege.setDescription(privilege.getDescription());
    orientPrivilege.setId(privilege.getId());
    orientPrivilege.setName(privilege.getName());
    orientPrivilege.setProperties(privilege.getProperties());
    orientPrivilege.setReadOnly(privilege.isReadOnly());
    orientPrivilege.setType(privilege.getType());
    orientPrivilege.setVersion(privilege.getVersion());

    return orientPrivilege;
  }

  private OrientCRole convert(final CRole role) {
    OrientCRole orientRole = roleEntityAdapter.newEntity();

    orientRole.setDescription(role.getDescription());
    orientRole.setId(role.getId());
    orientRole.setName(role.getName());
    orientRole.setPrivileges(role.getPrivileges());
    orientRole.setReadOnly(role.isReadOnly());
    orientRole.setRoles(role.getRoles());
    orientRole.setVersion(role.getVersion());

    return orientRole;
  }

  private OrientCUser convert(final CUser user) {
    OrientCUser orientUser = userEntityAdapter.newEntity();
    orientUser.setEmail(user.getEmail());
    orientUser.setFirstName(user.getFirstName());
    orientUser.setId(user.getId());
    orientUser.setLastName(user.getLastName());
    orientUser.setPassword(user.getPassword());
    orientUser.setStatus(user.getStatus());
    orientUser.setVersion(user.getVersion());

    return orientUser;
  }

  private OrientCUserRoleMapping convert(final CUserRoleMapping userRoleMapping) {
    OrientCUserRoleMapping orientUserRoleMapping = userRoleMappingEntityAdapter.newEntity();

    orientUserRoleMapping.setRoles(userRoleMapping.getRoles());
    orientUserRoleMapping.setSource(userRoleMapping.getSource());
    orientUserRoleMapping.setUserId(userRoleMapping.getUserId());
    orientUserRoleMapping.setVersion(userRoleMapping.getVersion());

    return orientUserRoleMapping;
  }

  private static void checkPrivilege(final CPrivilege privilege) {
    checkArgument(privilege instanceof OrientCPrivilege,
        "Privilege is not an instance of " + OrientCPrivilege.class.getName());
  }

  private static void checkRole(final CRole role) {
    checkArgument(role instanceof OrientCRole, "Role is not an instance of " + OrientCRole.class.getName());
  }

  private static void checkUser(final CUser user) {
    checkArgument(user instanceof OrientCUser, "User is not an instance of " + OrientCUser.class.getName());
  }

  private static void checkUserRoleMapping(final CUserRoleMapping userRoleMapping) {
    checkArgument(userRoleMapping instanceof OrientCUserRoleMapping,
        "UserRoleMapping is not an instance of " + OrientCUserRoleMapping.class.getName());
  }
}
