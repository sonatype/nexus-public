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

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.event.EventBus;
import org.sonatype.nexus.security.SecurityConfigurationChanged;
import org.sonatype.nexus.security.authz.AuthorizationConfigurationChanged;
import org.sonatype.nexus.security.config.CPrivilege;
import org.sonatype.nexus.security.config.CRole;
import org.sonatype.nexus.security.config.SecurityConfigurationManager;
import org.sonatype.nexus.security.privilege.NoSuchPrivilegeException;
import org.sonatype.nexus.security.privilege.PrivilegeDescriptor;
import org.sonatype.nexus.security.role.NoSuchRoleException;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.RolePermissionResolver;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link RolePermissionResolver}.
 */
@Named("default")
@Singleton
public class RolePermissionResolverImpl
    extends ComponentSupport
    implements RolePermissionResolver
{
  private final SecurityConfigurationManager configuration;

  private final List<PrivilegeDescriptor> privilegeDescriptors;

  /**
   * Privilege-id to permission cache.
   */
  private final Cache<String,Permission> permissionsCache = CacheBuilder.newBuilder().softValues().build();

  /**
   * Role-id to role permissions cache.
   */
  private final Cache<String, Collection<Permission>> rolePermissionsCache = CacheBuilder.newBuilder().softValues().build();

  /**
   * role not found cache.
   */
  private final Cache<String,String> roleNotFoundCache;

  @Inject
  public RolePermissionResolverImpl(final SecurityConfigurationManager configuration,
                                    final List<PrivilegeDescriptor> privilegeDescriptors,
                                    final EventBus eventBus,
                                    @Named("${security.roleNotFoundCacheSize:-100000}") final int roleNotFoundCacheSize)
  {
    this.configuration = checkNotNull(configuration);
    this.privilegeDescriptors = checkNotNull(privilegeDescriptors);
    this.roleNotFoundCache = CacheBuilder.newBuilder().maximumSize(roleNotFoundCacheSize).build();
    eventBus.register(this);
  }

  /**
   * Invalidate caches.
   */
  private void invalidate() {
    permissionsCache.invalidateAll();
    rolePermissionsCache.invalidateAll();
    roleNotFoundCache.invalidateAll();
    log.trace("Cache invalidated");
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final AuthorizationConfigurationChanged event) {
    invalidate();
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final SecurityConfigurationChanged event) {
    invalidate();
  }

  @Override
  public Collection<Permission> resolvePermissionsInRole(final String roleString) {
    checkNotNull(roleString);

    // check memory-sensitive cache; use cached value as long as config is not dirty
    Collection<Permission> cachedPermissions = rolePermissionsCache.getIfPresent(roleString);
    if (cachedPermissions != null && !configuration.isDirty()) {
      return cachedPermissions;
    }

    final Set<Permission> permissions = new LinkedHashSet<>();
    final Deque<String> rolesToProcess = new ArrayDeque<>();
    final Set<String> processedRoleIds = new HashSet<>();

    // initial role
    rolesToProcess.add(roleString);

    while (!rolesToProcess.isEmpty()) {
      final String roleId = rolesToProcess.removeFirst();
      if (processedRoleIds.add(roleId)) {

        if (roleNotFoundCache.getIfPresent(roleId) != null) {
          log.trace("Role {} found in NFC, role check skipped", roleId);
          continue; // use cached results
        }

        try {
          // try to re-use results when resolving the role tree
          cachedPermissions = rolePermissionsCache.getIfPresent(roleId);
          if (cachedPermissions != null && !configuration.isDirty()) {
            permissions.addAll(cachedPermissions);
            continue; // use cached results
          }

          final CRole role = configuration.readRole(roleId);

          // process the roles this role has recursively
          rolesToProcess.addAll(role.getRoles());

          // add the permissions this role has
          for (String privilegeId : role.getPrivileges()) {
            Permission permission = permission(privilegeId);
            if (permission != null) {
              permissions.add(permission);
            }
          }
        }
        catch (NoSuchRoleException e) {
          log.trace("Ignoring missing role: {}", roleId, e);
          roleNotFoundCache.put(roleId, "");
        }
      }
    }

    // cache result of (non-trivial) computation
    rolePermissionsCache.put(roleString, permissions);

    return permissions;
  }

  /**
   * Returns the descriptor for the given privilege-type or {@code null}.
   */
  @Nullable
  private PrivilegeDescriptor descriptor(final String privilegeType) {
    assert privilegeType != null;

    for (PrivilegeDescriptor descriptor : privilegeDescriptors) {
      if (privilegeType.equals(descriptor.getType())) {
        return descriptor;
      }
    }

    log.warn("Missing privilege-descriptor for type: {}", privilegeType);
    return null;
  }

  /**
   * Returns the permission for the given privilege-id or {@code null}.
   */
  @Nullable
  private Permission permission(final String privilegeId) {
    assert privilegeId != null;

    Permission permission = permissionsCache.getIfPresent(privilegeId);
    if (permission == null) {
      try {
        CPrivilege privilege = configuration.readPrivilege(privilegeId);
        PrivilegeDescriptor descriptor = descriptor(privilege.getType());
        if (descriptor != null) {
          permission = descriptor.createPermission(privilege);
          permissionsCache.put(privilegeId, permission);
        }
      }
      catch (NoSuchPrivilegeException e) {
        log.trace("Ignoring missing privilege: {}", privilegeId, e);
      }
    }

    return permission;
  }
}
