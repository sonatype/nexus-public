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

import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.LifecycleSupport;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseInstanceNames;
import org.sonatype.nexus.security.config.CPrivilege;
import org.sonatype.nexus.security.config.CRole;
import org.sonatype.nexus.security.config.CUser;
import org.sonatype.nexus.security.config.CUserRoleMapping;
import org.sonatype.nexus.security.config.SecurityConfiguration;
import org.sonatype.nexus.security.config.SecurityConfigurationSource;
import org.sonatype.nexus.security.privilege.NoSuchPrivilegeException;
import org.sonatype.nexus.security.role.NoSuchRoleException;
import org.sonatype.nexus.security.user.NoSuchRoleMappingException;
import org.sonatype.nexus.security.user.UserManager;
import org.sonatype.nexus.security.user.UserNotFoundException;

import com.google.common.collect.ImmutableList;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.orientechnologies.orient.core.record.impl.ODocument;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.valueOf;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SCHEMAS;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTx;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTxRetry;

/**
 * Default {@link SecurityConfigurationSource} implementation using Orient db as store.
 *
 * @since 3.0
 */
@Named
@ManagedLifecycle(phase = SCHEMAS)
@Singleton
public class OrientSecurityConfigurationSource
    extends LifecycleSupport
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

  private final CUserEntityAdapter userEntityAdapter;

  private final CRoleEntityAdapter roleEntityAdapter;

  private final CPrivilegeEntityAdapter privilegeEntityAdapter;

  private final CUserRoleMappingEntityAdapter userRoleMappingEntityAdapter;

  /**
   * The configuration.
   */
  private SecurityConfiguration configuration;

  @Inject
  public OrientSecurityConfigurationSource(@Named(DatabaseInstanceNames.SECURITY) final Provider<DatabaseInstance> databaseInstance,
                                           @Named("static") final SecurityConfigurationSource defaults,
                                           final CUserEntityAdapter userEntityAdapter,
                                           final CRoleEntityAdapter roleEntityAdapter,
                                           final CPrivilegeEntityAdapter privilegeEntityAdapter,
                                           final CUserRoleMappingEntityAdapter userRoleMappingEntityAdapter)
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
              userEntityAdapter.addEntity(db, user);
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
              roleEntityAdapter.addEntity(db, role);
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
              privilegeEntityAdapter.addEntity(db, privilege);
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
              userRoleMappingEntityAdapter.addEntity(db, mapping);
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
    private ConcurrentModificationException concurrentlyModified(final String type, final String value) {
      throw new ConcurrentModificationException(type + " '" + value + "' updated in the meantime");
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
      log.trace("Adding user: {}", user.getId());

      inTxRetry(databaseInstance).run(db -> {
        userEntityAdapter.addEntity(db, user);
        addUserRoleMapping(mapping(user.getId(), roles));
      });
    }

    @Override
    public void updateUser(final CUser user, final Set<String> roles) throws UserNotFoundException {
      checkNotNull(user);
      checkNotNull(user.getId());
      log.trace("Updating user: {}", user.getId());

      try {
        inTxRetry(databaseInstance).throwing(UserNotFoundException.class).run(db -> {
          ODocument document = userEntityAdapter.readDocument(db, user.getId());
          if (document == null) {
            throw new UserNotFoundException(user.getId());
          }
          if (user.getVersion() != null && !Objects.equals(user.getVersion(), valueOf(document.getVersion()))) {
            throw concurrentlyModified("User", user.getId());
          }
          userEntityAdapter.writeEntity(document, user);

          CUserRoleMapping mapping = mapping(user.getId(), roles);
          try {
            updateUserRoleMapping(mapping);
          }
          catch (NoSuchRoleMappingException e) {
            addUserRoleMapping(mapping);
          }
        });
      }
      catch (OConcurrentModificationException e) {
        throw concurrentlyModified("User", user.getId());
      }
    }

    @Override
    public boolean removeUser(final String id) {
      checkNotNull(id);
      log.trace("Removing user: {}", id);

      try {
        return inTxRetry(databaseInstance).call(db -> {
          if (userEntityAdapter.delete(db, id)) {
            removeUserRoleMapping(id, UserManager.DEFAULT_SOURCE);
            return true;
          }
          return false;
        });
      }
      catch (OConcurrentModificationException e) {
        throw concurrentlyModified("User", id);
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
    public void addPrivilege(final CPrivilege privilege) {
      checkNotNull(privilege);
      checkNotNull(privilege.getId());
      log.trace("Adding privilege: {}", privilege.getId());

      inTxRetry(databaseInstance).run(db -> privilegeEntityAdapter.addEntity(db, privilege));
    }

    @Override
    public void updatePrivilege(final CPrivilege privilege) throws NoSuchPrivilegeException {
      checkNotNull(privilege);
      checkNotNull(privilege.getId());
      log.trace("Updating privilege: {}", privilege.getId());

      try {
        inTxRetry(databaseInstance).throwing(NoSuchPrivilegeException.class).run(db -> {
          ODocument document = privilegeEntityAdapter.readDocument(db, privilege.getId());
          if (document == null) {
            throw new NoSuchPrivilegeException(privilege.getId());
          }
          if (privilege.getVersion() != null && !Objects.equals(privilege.getVersion(), valueOf(document.getVersion()))) {
            throw concurrentlyModified("Privilege", privilege.getId());
          }
          privilegeEntityAdapter.writeEntity(document, privilege);
        });
      }
      catch (OConcurrentModificationException e) {
        throw concurrentlyModified("Privilege", privilege.getId());
      }
    }

    @Override
    public boolean removePrivilege(final String id) {
      checkNotNull(id);
      log.trace("Removing privilege: {}", id);

      try {
        return inTxRetry(databaseInstance).call(db -> privilegeEntityAdapter.delete(db, id));
      }
      catch (OConcurrentModificationException e) {
        throw concurrentlyModified("Privilege", id);
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
      log.trace("Adding role: {}", role.getId());

      inTxRetry(databaseInstance).run(db -> roleEntityAdapter.addEntity(db, role));
    }

    @Override
    public void updateRole(final CRole role) throws NoSuchRoleException {
      checkNotNull(role);
      checkNotNull(role.getId());
      log.trace("Updating role: {}", role.getId());

      try {
        inTxRetry(databaseInstance).throwing(NoSuchRoleException.class).run(db -> {
          ODocument document = roleEntityAdapter.readDocument(db, role.getId());
          if (document == null) {
            throw new NoSuchRoleException(role.getId());
          }
          if (role.getVersion() != null && !Objects.equals(role.getVersion(), valueOf(document.getVersion()))) {
            throw concurrentlyModified("Role", role.getId());
          }
          roleEntityAdapter.writeEntity(document, role);
        });
      }
      catch (OConcurrentModificationException e) {
        throw concurrentlyModified("Role", role.getId());
      }
    }

    @Override
    public boolean removeRole(final String id) {
      checkNotNull(id);
      log.trace("Removing role: {}", id);

      try {
        return inTxRetry(databaseInstance).call(db -> roleEntityAdapter.delete(db, id));
      }
      catch (OConcurrentModificationException e) {
        throw concurrentlyModified("Role", id);
      }
    }

    //
    // User-Role Mappings
    //

    private CUserRoleMapping mapping(final String userId, final Set<String> roles) {
      CUserRoleMapping mapping = new CUserRoleMapping();
      mapping.setUserId(userId);
      mapping.setSource(UserManager.DEFAULT_SOURCE);
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
      checkNotNull(mapping.getUserId());
      checkNotNull(mapping.getSource());
      log.trace("Adding user/role mappings for: {}/{}", mapping.getUserId(), mapping.getSource());

      inTxRetry(databaseInstance).run(db -> userRoleMappingEntityAdapter.addEntity(db, mapping));
    }

    @Override
    public void updateUserRoleMapping(final CUserRoleMapping mapping) throws NoSuchRoleMappingException {
      checkNotNull(mapping);
      checkNotNull(mapping.getUserId());
      checkNotNull(mapping.getSource());
      log.trace("Updating user/role mappings for: {}/{}", mapping.getUserId(), mapping.getSource());

      try {
        inTxRetry(databaseInstance).throwing(NoSuchRoleMappingException.class).run(db -> {
          ODocument document = userRoleMappingEntityAdapter.readDocument(db, mapping.getUserId(), mapping.getSource());
          if (document == null) {
            throw new NoSuchRoleMappingException(mapping.getUserId());
          }
          if (mapping.getVersion() != null && !Objects.equals(mapping.getVersion(), valueOf(document.getVersion()))) {
            throw concurrentlyModified("User-role mapping", mapping.getUserId());
          }
          userRoleMappingEntityAdapter.writeEntity(document, mapping);
        });
      }
      catch (OConcurrentModificationException e) {
        throw concurrentlyModified("User-role mapping", mapping.getUserId());
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
        throw concurrentlyModified("User-role mapping", userId);
      }
    }
  }
}
