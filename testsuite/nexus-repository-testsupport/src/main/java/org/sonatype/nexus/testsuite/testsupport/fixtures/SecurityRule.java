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
package org.sonatype.nexus.testsuite.testsupport.fixtures;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.security.RepositoryContentSelectorPrivilegeDescriptor;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.anonymous.AnonymousConfiguration;
import org.sonatype.nexus.security.anonymous.AnonymousManager;
import org.sonatype.nexus.security.authz.NoSuchAuthorizationManagerException;
import org.sonatype.nexus.security.privilege.Privilege;
import org.sonatype.nexus.security.realm.RealmManager;
import org.sonatype.nexus.security.role.Role;
import org.sonatype.nexus.security.role.RoleIdentifier;
import org.sonatype.nexus.security.user.NoSuchUserManagerException;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserNotFoundException;
import org.sonatype.nexus.security.user.UserStatus;
import org.sonatype.nexus.selector.CselSelector;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorManager;

import com.google.common.collect.ImmutableMap;
import org.apache.shiro.authz.Permission;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.security.user.UserManager.DEFAULT_SOURCE;

@Named
@Singleton
public class SecurityRule
    extends ExternalResource
{
  private static final Logger log = LoggerFactory.getLogger(SecurityRule.class);

  private final Provider<SecuritySystem> securitySystemProvider;

  private final Provider<SelectorManager> selectorManagerProvider;

  private final Provider<AnonymousManager> anonymousConfigurationProvider;

  private Provider<RealmManager> realmManagerProvider;

  private final Set<Privilege> privileges = new HashSet<>();

  private final Set<String> roles = new HashSet<>();

  private final Set<String> users = new HashSet<>();

  private AnonymousConfiguration originalAnonymousConfiguration = null;

  final Set<String> selectors = new HashSet<>();

  public SecurityRule(
      final Provider<SecuritySystem> securitySystemProvider,
      final Provider<SelectorManager> selectorManagerProvider)
  {
    this(securitySystemProvider, selectorManagerProvider, () -> null);
  }

  public SecurityRule(
      final Provider<SecuritySystem> securitySystemProvider,
      final Provider<SelectorManager> selectorManagerProvider,
      final Provider<AnonymousManager> anonymousConfigurationProvider)
  {
    this.securitySystemProvider = checkNotNull(securitySystemProvider);
    this.selectorManagerProvider = checkNotNull(selectorManagerProvider);
    this.anonymousConfigurationProvider = checkNotNull(anonymousConfigurationProvider);
  }

  @Inject
  public SecurityRule(
      final SecuritySystem securitySystem,
      final SelectorManager selectorManager,
      final AnonymousManager anonymousConfiguration,
      final RealmManager realmManager)
  {
    this.securitySystemProvider = () -> securitySystem;
    this.selectorManagerProvider = () -> selectorManager;
    this.anonymousConfigurationProvider = () -> anonymousConfiguration;
    this.realmManagerProvider = () -> realmManager;
  }

  @Override
  public void after() {
    users.forEach(user -> {
      try {
        securitySystemProvider.get().deleteUser(user, DEFAULT_SOURCE);
      }
      catch (Exception e) { //NOSONAR
        log.debug("Failed to cleanup user: {}", user, e);
      }
    });
    roles.forEach(roleId -> {
      try {
        securitySystemProvider.get().getAuthorizationManager(DEFAULT_SOURCE).deleteRole(roleId);
      }
      catch (Exception e) { //NOSONAR
        log.debug("Failed to cleanup role: {}", roleId, e);
      }
    });
    privileges.forEach(privilege -> {
      try {
        securitySystemProvider.get().getAuthorizationManager(DEFAULT_SOURCE).deletePrivilege(privilege.getId());
      }
      catch (Exception e) { //NOSONAR
        log.debug("Failed to cleanup privilege: {}", privilege.getId(), e);
      }
    });
    selectors.stream()
        .map(selectorManagerProvider.get()::findByName)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .forEach(selectorManagerProvider.get()::delete);

    if (originalAnonymousConfiguration != null) {
      anonymousConfigurationProvider.get().setConfiguration(originalAnonymousConfiguration);
    }
  }

  public Privilege getPrivilege(final String privilegeName) {
    try {
      return securitySystemProvider.get().getAuthorizationManager(DEFAULT_SOURCE).listPrivileges().stream()
          .filter(privilege -> privilege.getName().equals(privilegeName)).findFirst().orElse(null);
    }
    catch (NoSuchAuthorizationManagerException e) {
      log.debug("Failed to get privilege {}", privilegeName, e);
    }
    return null;
  }

  public Role getRole(final String roleId) {
    try {
      return securitySystemProvider.get().getAuthorizationManager(DEFAULT_SOURCE).listRoles().stream()
          .filter(role -> role.getRoleId().equals(roleId)).findFirst().orElse(null);
    }
    catch (NoSuchAuthorizationManagerException e) {
      log.debug("Failed to get role {}", roleId, e);
    }
    return null;
  }

  public User addRole(final String userId, final String... roleIds)
      throws UserNotFoundException, NoSuchUserManagerException
  {
    User user = getUser(userId);
    if (user == null) {
      throw new UserNotFoundException(userId);
    }
    Set<RoleIdentifier> roles = Arrays.stream(roleIds)
        .map(it -> getRole(it))
        .map(it -> new RoleIdentifier(it.getSource(), it.getRoleId()))
        .collect(Collectors.toSet());
    user.setRoles(roles);
    securitySystemProvider.get().updateUser(user);
    return user;
  }

  public User getUser(final String userId) {
    try {
      return securitySystemProvider.get().getUser(userId, DEFAULT_SOURCE);
    }
    catch (UserNotFoundException | NoSuchUserManagerException e) {
      log.debug("Failed to get user {}", userId, e);
    }
    return null;
  }

  public Privilege createContentSelectorPrivilege(final String name, final String selector) {
    return createContentSelectorPrivilege(name, selector, "*", "*");
  }

  public Privilege createContentSelectorPrivilege(final String name, final String selector, final String repository) {
    return createContentSelectorPrivilege(name, selector, repository, "*");
  }

  public Privilege createContentSelectorPrivilege(
      final String name,
      final String selector,
      final String repository,
      final String actions)
  {
    Privilege privilege = new Privilege(name, name, name, RepositoryContentSelectorPrivilegeDescriptor.TYPE,
        ImmutableMap.of("contentSelector", selector, "repository", repository, "actions", actions), false);

    try {
      securitySystemProvider.get().getAuthorizationManager(DEFAULT_SOURCE).addPrivilege(privilege);
      privileges.add(privilege);
      return privilege;
    }
    catch (NoSuchAuthorizationManagerException e) {
      log.debug("Unable to add privilege {}", name, e);
    }
    return null;
  }

  /**
   * Note that the properties should be in multiples of 2 (key/value pairs)
   */
  public Privilege createPrivilege(final String type, final String name, final String... properties) {
    Map<String, String> propMap = new HashMap<>();
    for (int i = 0; i < properties.length; i += 2) {
      propMap.put(properties[i], properties[i + 1]);
    }

    Privilege privilege = new Privilege(name, name, name, type, propMap, false);

    try {
      securitySystemProvider.get().getAuthorizationManager(DEFAULT_SOURCE).addPrivilege(privilege);
      privileges.add(privilege);
      return privilege;
    }
    catch (NoSuchAuthorizationManagerException e) {
      log.debug("Unable to add privilege {}", name, e);
    }
    return null;
  }

  public Role createRole(final String name, final String... privilegeNames) {
    return createRole(name, new String[0], privilegeNames);
  }

  public Role createRole(final String name, final String[] roleIds, final String[] privilegeNames) {
    List<Privilege> privileges =
        Arrays.stream(privilegeNames).map(this::getPrivilege).filter(Objects::nonNull).collect(Collectors.toList());

    if (privileges.size() != privilegeNames.length) {
      String privilegeNamesStr = Arrays.asList(privilegeNames).stream().collect(Collectors.joining(", "));
      throw new IllegalStateException(
          String.format("Missing privileges names: %s privileges: %s", privilegeNamesStr, privileges));
    }

    List<Role> roles = Arrays.stream(roleIds).map(this::getRole).filter(Objects::nonNull).collect(Collectors.toList());

    if (roles.size() != roleIds.length) {
      throw new IllegalStateException("Missing privileges names: ${roleIds} privileges: ${roles}");
    }

    return createRole(name, roles, privileges);
  }

  public Role createRole(final String name, final Privilege... privileges) {
    return createRole(name, new Role[0], privileges);
  }

  public Role createRole(final String name, final List<Role> roles, final List<Privilege> privileges) {
    return createRole(name, roles.toArray(new Role[roles.size()]),
        privileges.toArray(new Privilege[privileges.size()]));
  }

  public Role createRole(final String name, final Role[] containedRoles, final Privilege[] privileges) {
    Role role =
        new Role(name, name, name, DEFAULT_SOURCE, false, Arrays.stream(containedRoles).map(Role::getRoleId).collect(
            Collectors.toSet()), Arrays.stream(privileges).map(Privilege::getId).collect(Collectors.toSet()));

    try {
      securitySystemProvider.get().getAuthorizationManager(DEFAULT_SOURCE).addRole(role);
      roles.add(role.getRoleId());
      return role;
    }
    catch (NoSuchAuthorizationManagerException e) {
      log.debug("Unable to add role {}", name, e);
    }
    return null;
  }

  public Privilege findPrivilege(final Permission permission) {
    return securitySystemProvider.get().listPrivileges().stream()
        .filter(privilege -> privilege.getPermission().equals(permission)).findFirst().orElse(null);
  }

  public Role findRole(final String roleId) {
    try {
      return securitySystemProvider.get().getAuthorizationManager(DEFAULT_SOURCE).listRoles().stream()
          .filter(role -> role.getRoleId().equals(roleId)).findFirst().orElse(null);
    }
    catch (NoSuchAuthorizationManagerException e) {
      log.debug("Unable to find role {}", roleId, e);
    }
    return null;
  }

  /**
   * Create a user with the specified privileges, the username, the password, and the role will be the same.
   *
   * @param prefix a prefix for the generated user & roles.
   * @param privileges the required privileges
   *
   * @return the created user
   */
  public User createUserWithPrivileges(final String prefix, final String... privileges) {
    String roleName = prefix + "-" + UUID.randomUUID().toString();
    Role role = createRole(roleName, privileges);
    return createUser(roleName, roleName, role.getRoleId());
  }

  public User createUser(final String name, final String password, final String... roles) {
    User user = new User();
    user.setUserId(name);
    user.setSource(DEFAULT_SOURCE);
    user.setFirstName(name);
    user.setLastName(name);
    user.setEmailAddress("example@example.com");
    user.setStatus(UserStatus.active);
    user.setRoles(
        Arrays.stream(roles).map(role -> new RoleIdentifier(DEFAULT_SOURCE, role)).collect(Collectors.toSet()));

    try {
      user = securitySystemProvider.get().addUser(user, password);
      users.add(user.getUserId());
      return user;
    }
    catch (NoSuchUserManagerException e) {
      log.debug("Unable to add user {}", name, e);
    }
    return null;
  }

  public SelectorConfiguration createSelector(final String name, final String expression) {
    return createSelector(name, name, CselSelector.TYPE, expression);
  }

  public SelectorConfiguration createSelector(
      final String name,
      final String description,
      final String type,
      final String expression)
  {
    SelectorConfiguration selectorConfiguration = selectorManagerProvider.get()
        .newSelectorConfiguration(name, type, description, ImmutableMap.of("expression", expression));

    selectorManagerProvider.get().create(selectorConfiguration);
    selectors.add(name);
    return selectorConfiguration;
  }

  public void deleteAllSelectors() {
    SelectorManager selectorManager = selectorManagerProvider.get();
    log.debug("Deleting all content selectors.");
    selectorManager.browse().forEach(selectorConfiguration -> {
      try {
        selectorManager.delete(selectorConfiguration);
      }
      catch (Exception e) {
        log.debug("Failed to delete content selector {}", selectorConfiguration.getName(), e);
      }
    });
    selectors.clear();
  }

  /**
   * Sets whether anonymous access should be enabled. Note that the original unset state of Anonymous Access can't be
   * restore as setting the state persists it.
   */
  public void setAnonymousEnabled(final boolean anonymousEnabled) {
    AnonymousManager anonymousManager = anonymousConfigurationProvider.get();
    if (originalAnonymousConfiguration == null) {
      AnonymousConfiguration current = anonymousManager.getConfiguration();
      // The original one can't be modified, technically we can't quite get back the orign
      originalAnonymousConfiguration = anonymousManager.newConfiguration();
      originalAnonymousConfiguration.setEnabled(current.isEnabled());
      originalAnonymousConfiguration.setRealmName(current.getRealmName());
      originalAnonymousConfiguration.setUserId(current.getUserId());
    }

    AnonymousConfiguration configuration = anonymousManager.newConfiguration();
    configuration.setEnabled(anonymousEnabled);
    configuration.setRealmName(originalAnonymousConfiguration.getRealmName());
    configuration.setUserId(originalAnonymousConfiguration.getUserId());

    anonymousManager.setConfiguration(configuration);
  }

  public void manageRole(final String roleId) {
    roles.add(roleId);
  }

  public void manageUser(final String userId) {
    users.add(userId);
  }

  public void unmanageUser(final String userId) {
    users.remove(userId);
  }

  public void unmanageRole(final String roleId) {
    roles.remove(roleId);
  }

  public void enableRoleRealm(final String roleRealmName) {
    realmManagerProvider.get().enableRealm(roleRealmName);
  }
}
