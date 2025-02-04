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

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.security.SecurityApi;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.anonymous.AnonymousConfiguration;
import org.sonatype.nexus.security.anonymous.AnonymousManager;
import org.sonatype.nexus.security.authz.NoSuchAuthorizationManagerException;
import org.sonatype.nexus.security.role.Role;
import org.sonatype.nexus.security.role.RoleIdentifier;
import org.sonatype.nexus.security.user.NoSuchUserManagerException;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserNotFoundException;
import org.sonatype.nexus.security.user.UserStatus;

import com.google.common.collect.Sets;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.security.user.UserManager.DEFAULT_SOURCE;

/**
 * @since 3.0
 */
@Named
@Singleton
public class SecurityApiImpl
    extends ComponentSupport
    implements SecurityApi
{
  private final AnonymousManager anonymousManager;

  private final SecuritySystem securitySystem;

  @Inject
  public SecurityApiImpl(final AnonymousManager anonymousManager, final SecuritySystem securitySystem) {
    this.anonymousManager = anonymousManager;
    this.securitySystem = securitySystem;
  }

  @Override
  public AnonymousConfiguration setAnonymousAccess(final boolean enabled) {
    AnonymousConfiguration anonymousConfiguration = anonymousManager.getConfiguration();

    if (!anonymousManager.isConfigured() || anonymousConfiguration.isEnabled() != enabled) {
      anonymousConfiguration.setEnabled(enabled);
      anonymousManager.setConfiguration(anonymousConfiguration);
      log.info("Anonymous access configuration updated to: {}", anonymousConfiguration);
    }
    else {
      log.info("Anonymous access configuration unchanged at: {}", anonymousConfiguration);
    }
    return anonymousConfiguration;
  }

  @Override
  public User addUser(
      final String id,
      final String firstName,
      final String lastName,
      final String email,
      final boolean active,
      final String password,
      final List<String> roleIds) throws NoSuchUserManagerException
  {
    User user = new User();
    user.setUserId(checkNotNull(id));
    user.setSource(DEFAULT_SOURCE);
    user.setFirstName(checkNotNull(firstName));
    user.setLastName(checkNotNull(lastName));
    user.setEmailAddress(checkNotNull(email));
    user.setStatus(active ? UserStatus.active : UserStatus.disabled);
    user.setRoles(toIdentifiers(roleIds));

    return securitySystem.addUser(user, password);
  }

  @Override
  public Role addRole(
      final String id,
      final String name,
      final String description,
      final List<String> privileges,
      final List<String> roles) throws NoSuchAuthorizationManagerException
  {

    Role role = new Role();
    role.setRoleId(checkNotNull(id));
    role.setSource(DEFAULT_SOURCE);
    role.setName(checkNotNull(name));
    role.setDescription(description);
    role.setPrivileges(Sets.newHashSet(checkNotNull(privileges)));
    role.setRoles(Sets.newHashSet(checkNotNull(roles)));

    return securitySystem.getAuthorizationManager(DEFAULT_SOURCE).addRole(role);
  }

  @Override
  public User setUserRoles(
      final String userId,
      final List<String> roleIds) throws UserNotFoundException, NoSuchUserManagerException
  {
    User user = securitySystem.getUser(userId, DEFAULT_SOURCE);
    user.setRoles(toIdentifiers(roleIds));
    return securitySystem.updateUser(user);
  }

  private static Set<RoleIdentifier> toIdentifiers(final Collection<String> roleIds) {
    return checkNotNull(roleIds).stream()
        .map(roleId -> new RoleIdentifier(DEFAULT_SOURCE, roleId))
        .collect(Collectors.toSet());
  }
}
