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
package org.sonatype.nexus.security.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.event.EventBus;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.security.authz.AuthorizationConfigurationChanged;
import org.sonatype.nexus.security.config.CPrivilege;
import org.sonatype.nexus.security.config.CRole;
import org.sonatype.nexus.security.config.CUser;
import org.sonatype.nexus.security.config.CUserRoleMapping;
import org.sonatype.nexus.security.config.DynamicSecurityConfigurationResource;
import org.sonatype.nexus.security.config.MemorySecurityConfiguration;
import org.sonatype.nexus.security.config.SecurityConfiguration;
import org.sonatype.nexus.security.config.SecurityConfigurationCleaner;
import org.sonatype.nexus.security.config.SecurityConfigurationManager;
import org.sonatype.nexus.security.config.SecurityConfigurationSource;
import org.sonatype.nexus.security.config.StaticSecurityConfigurationResource;
import org.sonatype.nexus.security.privilege.NoSuchPrivilegeException;
import org.sonatype.nexus.security.role.NoSuchRoleException;
import org.sonatype.nexus.security.user.NoSuchRoleMappingException;
import org.sonatype.nexus.security.user.UserManager;
import org.sonatype.nexus.security.user.UserNotFoundException;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.shiro.authc.credential.PasswordService;

import static com.google.common.base.Preconditions.checkState;

/**
 * Default {@link SecurityConfigurationManager}.
 */
@Named("default")
@Singleton
public class SecurityConfigurationManagerImpl
    extends ComponentSupport
    implements SecurityConfigurationManager
{
  private final SecurityConfigurationSource configurationSource;

  private final SecurityConfigurationCleaner configCleaner;

  private final PasswordService passwordService;

  private final EventBus eventBus;

  private final List<StaticSecurityConfigurationResource> staticResources;

  private final List<DynamicSecurityConfigurationResource> dynamicResources;

  private volatile SecurityConfiguration defaultConfiguration;

  private volatile SecurityConfiguration mergedConfiguration;

  @Inject
  public SecurityConfigurationManagerImpl(final SecurityConfigurationSource configurationSource,
                                          final List<StaticSecurityConfigurationResource> staticResources,
                                          final List<DynamicSecurityConfigurationResource> dynamicResources,
                                          final SecurityConfigurationCleaner configCleaner,
                                          final PasswordService passwordService,
                                          final EventBus eventBus)
  {
    this.configurationSource = configurationSource;
    this.dynamicResources = dynamicResources;
    this.staticResources = staticResources;
    this.eventBus = eventBus;
    this.configCleaner = configCleaner;
    this.passwordService = passwordService;
  }

  @Override
  public List<CPrivilege> listPrivileges() {
    List<CPrivilege> privileges = Lists.newArrayList();
    privileges.addAll(getDefaultConfiguration().getPrivileges());
    privileges.addAll(getMergedConfiguration().getPrivileges());
    return Collections.unmodifiableList(privileges);
  }

  @Override
  public List<CRole> listRoles() {
    List<CRole> roles = Lists.newArrayList();
    roles.addAll(getDefaultConfiguration().getRoles());
    roles.addAll(getMergedConfiguration().getRoles());
    return Collections.unmodifiableList(roles);
  }

  @Override
  public List<CUser> listUsers() {
    return Collections.unmodifiableList(getDefaultConfiguration().getUsers());
  }

  @Override
  public List<CUserRoleMapping> listUserRoleMappings() {
    return Collections.unmodifiableList(getDefaultConfiguration().getUserRoleMappings());
  }

  @Override
  public void createPrivilege(CPrivilege privilege) {
    getDefaultConfiguration().addPrivilege(privilege);
  }

  @Override
  public void createRole(CRole role) {
    getDefaultConfiguration().addRole(role);
  }

  @Override
  public void createUser(CUser user, Set<String> roles) {
    createUser(user, null, roles);
  }

  @Override
  public void createUser(CUser user, String password, Set<String> roles) {
    if (!Strings2.isBlank(password)) {
      user.setPassword(passwordService.encryptPassword(password));
    }
    getDefaultConfiguration().addUser(user, roles);
  }

  @Override
  public void deletePrivilege(String id) throws NoSuchPrivilegeException {
    boolean found = getDefaultConfiguration().removePrivilege(id);
    if (!found) {
      throw new NoSuchPrivilegeException(id);
    }
    cleanRemovedPrivilege(id);
  }

  @Override
  public void deleteRole(String id) throws NoSuchRoleException {
    boolean found = getDefaultConfiguration().removeRole(id);
    if (!found) {
      throw new NoSuchRoleException(id);
    }
    cleanRemovedRole(id);
  }

  @Override
  public void deleteUser(String id) throws UserNotFoundException {
    boolean found = getDefaultConfiguration().removeUser(id);

    if (!found) {
      throw new UserNotFoundException(id);
    }
  }

  @Override
  public CPrivilege readPrivilege(String id) throws NoSuchPrivilegeException {
    CPrivilege privilege = getMergedConfiguration().getPrivilege(id);
    if (privilege != null) {
      return privilege;
    }

    privilege = getDefaultConfiguration().getPrivilege(id);
    if (privilege != null) {
      return privilege;
    }

    throw new NoSuchPrivilegeException(id);
  }

  @Override
  public CRole readRole(String id) throws NoSuchRoleException {
    CRole role = getMergedConfiguration().getRole(id);
    if (role != null) {
      return role;
    }

    role = getDefaultConfiguration().getRole(id);
    if (role != null) {
      return role;
    }

    throw new NoSuchRoleException(id);
  }

  @Override
  public CUser readUser(String id) throws UserNotFoundException {
    CUser user = getDefaultConfiguration().getUser(id);

    if (user != null) {
      return user;
    }
    throw new UserNotFoundException(id);
  }

  @Override
  public void updatePrivilege(CPrivilege privilege) throws NoSuchPrivilegeException {
    getDefaultConfiguration().updatePrivilege(privilege);
  }

  @Override
  public void updateRole(CRole role) throws NoSuchRoleException {
    getDefaultConfiguration().updateRole(role);
  }

  @Override
  public void updateUser(CUser user) throws UserNotFoundException {
    Set<String> roles = Collections.emptySet();
    try {
      roles = readUserRoleMapping(user.getId(), UserManager.DEFAULT_SOURCE).getRoles();
    }
    catch (NoSuchRoleMappingException e) {
      log.debug("User: {} has no roles", user.getId());
    }
    updateUser(user, roles);
  }

  @Override
  public void updateUser(CUser user, Set<String> roles) throws UserNotFoundException {
    getDefaultConfiguration().updateUser(user, roles);
  }

  @Override
  public void createUserRoleMapping(CUserRoleMapping userRoleMapping) {
    getDefaultConfiguration().addUserRoleMapping(userRoleMapping);
  }

  @Override
  public CUserRoleMapping readUserRoleMapping(String userId, String source) throws NoSuchRoleMappingException {
    CUserRoleMapping mapping = getDefaultConfiguration().getUserRoleMapping(userId, source);

    if (mapping != null) {
      return mapping;
    }
    else {
      throw new NoSuchRoleMappingException(userId);
    }
  }

  @Override
  public void updateUserRoleMapping(CUserRoleMapping userRoleMapping) throws NoSuchRoleMappingException {
    getDefaultConfiguration().updateUserRoleMapping(userRoleMapping);
  }

  @Override
  public void deleteUserRoleMapping(String userId, String source) throws NoSuchRoleMappingException {
    boolean found = getDefaultConfiguration().removeUserRoleMapping(userId, source);

    if (!found) {
      throw new NoSuchRoleMappingException(userId);
    }
  }

  @Override
  public void cleanRemovedPrivilege(String privilegeId) {
    configCleaner.privilegeRemoved(getDefaultConfiguration(), privilegeId);
  }

  @Override
  public void cleanRemovedRole(String roleId) {
    configCleaner.roleRemoved(getDefaultConfiguration(), roleId);
  }

  private SecurityConfiguration getDefaultConfiguration() {
    // Assign configuration to local variable first, as calls to clearCache can null it out at any time
    SecurityConfiguration configuration = this.defaultConfiguration;
    if (configuration == null) {
      synchronized (this) {
        // double-checked locking of volatile is apparently OK with java5+
        // http://www.cs.umd.edu/~pugh/java/memoryModel/DoubleCheckedLocking.html
        configuration = this.defaultConfiguration;
        if (configuration == null) {
          this.defaultConfiguration = configuration = doGetDefaultConfiguration();
        }
      }
    }
    return configuration;
  }

  private SecurityConfiguration doGetDefaultConfiguration() {
    configurationSource.loadConfiguration();
    return configurationSource.getConfiguration();
  }

  private SecurityConfiguration getMergedConfiguration() {
    // Assign configuration to local variable first, as calls to clearCache can null it out at any time
    SecurityConfiguration configuration = this.mergedConfiguration;
    if (configuration == null || isDirty()) {
      boolean rebuiltConfiguration = false;

      synchronized (this) {
        // double-checked locking of volatile is apparently OK with java5+
        // http://www.cs.umd.edu/~pugh/java/memoryModel/DoubleCheckedLocking.html
        configuration = this.mergedConfiguration;
        if (configuration == null || isDirty()) {
          rebuiltConfiguration = (configuration != null);
          this.mergedConfiguration = configuration = doGetMergedConfiguration();
        }
      }

      if (rebuiltConfiguration) {
        // signal rebuild (outside lock to avoid contention)
        eventBus.post(new AuthorizationConfigurationChanged());
      }
    }
    return configuration;
  }

  @Override
  public boolean isDirty() {
    for (DynamicSecurityConfigurationResource resource : dynamicResources) {
      if (resource.isDirty()) {
        return true;
      }
    }
    return false;
  }

  private MemorySecurityConfiguration doGetMergedConfiguration() {
    final MemorySecurityConfiguration configuration = new MemorySecurityConfiguration();

    for (StaticSecurityConfigurationResource resource : staticResources) {
      SecurityConfiguration resConfig = resource.getConfiguration();

      if (resConfig != null) {
        checkState(
            resConfig.getUsers() == null || resConfig.getUsers().isEmpty(),
            "Static resources cannot have users"
        );
        checkState(
            resConfig.getUserRoleMappings() == null || resConfig.getUserRoleMappings().isEmpty(),
            "Static resources cannot have user/role mappings"
        );
        appendConfig(configuration, resConfig);
      }
    }

    for (DynamicSecurityConfigurationResource resource : dynamicResources) {
      SecurityConfiguration resConfig = resource.getConfiguration();

      if (resConfig != null) {
        checkState(
            resConfig.getUsers() == null || resConfig.getUsers().isEmpty(),
            "Dynamic resources cannot have users"
        );
        checkState(
            resConfig.getUserRoleMappings() == null || resConfig.getUserRoleMappings().isEmpty(),
            "Dynamic resources cannot have user/role mappings"
        );
        appendConfig(configuration, resConfig);
      }
    }

    return configuration;
  }

  private SecurityConfiguration appendConfig(final SecurityConfiguration to, final SecurityConfiguration from) {
    for (CPrivilege privilege : from.getPrivileges()) {
      privilege.setReadOnly(true);
      to.addPrivilege(privilege);
    }

    // number of roles can be significant (>15K), so need to speedup lookup roles by roleId
    final Map<String, CRole> roles = new HashMap<String, CRole>();
    for (CRole role : to.getRoles()) {
      roles.put(role.getId(), role);
    }

    for (CRole role : from.getRoles()) {
      // need to check if we need to merge the static config
      CRole eachRole = roles.get(role.getId());
      if (eachRole != null) {
        role = this.mergeRolesContents(role, eachRole);
        to.removeRole(role.getId());
      }

      role.setReadOnly(true);
      to.addRole(role);
      roles.put(role.getId(), role); // deduplicate config roles
    }

    return to;
  }

  private CRole mergeRolesContents(CRole roleA, CRole roleB) {
    Set<String> roles = new HashSet<>();
    // make sure they are not empty
    if (roleA.getRoles() != null) {
      roles.addAll(roleA.getRoles());
    }
    if (roleB.getRoles() != null) {
      roles.addAll(roleB.getRoles());
    }

    Set<String> privs = new HashSet<>();
    // make sure they are not empty
    if (roleA.getPrivileges() != null) {
      privs.addAll(roleA.getPrivileges());
    }
    if (roleB.getPrivileges() != null) {
      privs.addAll(roleB.getPrivileges());
    }

    CRole newRole = new CRole();
    newRole.setId(roleA.getId());
    newRole.setRoles(Sets.newHashSet(roles));
    newRole.setPrivileges(Sets.newHashSet(privs));

    // now for the name and description
    if (!Strings2.isBlank(roleA.getName())) {
      newRole.setName(roleA.getName());
    }
    else {
      newRole.setName(roleB.getName());
    }

    if (!Strings2.isBlank(roleA.getDescription())) {
      newRole.setDescription(roleA.getDescription());
    }
    else {
      newRole.setDescription(roleB.getDescription());
    }

    return newRole;
  }
}
