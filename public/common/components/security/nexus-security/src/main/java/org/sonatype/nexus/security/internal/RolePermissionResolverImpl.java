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
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventHelper;
import org.sonatype.nexus.distributed.event.service.api.common.AuthorizationChangedDistributedEvent;
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
import org.sonatype.nexus.security.authz.BatchRolePermissionResolver;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.RolePermissionResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link RolePermissionResolver}.
 */
@Primary
@Component
@Qualifier("default")
public class RolePermissionResolverImpl
    implements BatchRolePermissionResolver, EventAware
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final SecurityConfigurationManager configuration;

  private final List<PrivilegeDescriptor> privilegeDescriptors;

  /**
   * Privilege-id to permission cache.
   */
  private final Cache<String, Permission> permissionsCache = CacheBuilder.newBuilder().softValues().build();

  /**
   * Role-id to role permissions cache.
   */
  private final Cache<String, Collection<Permission>> rolePermissionsCache =
      CacheBuilder.newBuilder().softValues().build();

  /**
   * role not found cache.
   */
  private final Cache<String, String> roleNotFoundCache;

  @Autowired
  public RolePermissionResolverImpl(
      final SecurityConfigurationManager configuration,
      final List<PrivilegeDescriptor> privilegeDescriptors,
      @Value("${security.roleNotFoundCacheSize:100000}") final int roleNotFoundCacheSize)
  {
    this.configuration = checkNotNull(configuration);
    this.privilegeDescriptors = checkNotNull(privilegeDescriptors);
    this.roleNotFoundCache = CacheBuilder.newBuilder().maximumSize(roleNotFoundCacheSize).build();
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
  public void on(final SecurityContributionChangedEvent event) {
    invalidate();
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final AuthorizationChangedDistributedEvent event) {
    if (EventHelper.isReplicating()) {
      invalidate();
    }
  }

  @Override
  public Collection<Permission> resolvePermissionsInRole(final String roleString) {
    checkNotNull(roleString);

    // check memory-sensitive cache; use cached value as long as config is not dirty
    Collection<Permission> cachedPermissions = rolePermissionsCache.getIfPresent(roleString);
    if (cachedPermissions != null) {
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
          if (cachedPermissions != null) {
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
   * Batch variant of {@link #resolvePermissionsInRole} that fetches an entire set of role IDs
   * in a single DB round-trip per BFS level instead of one query per role.
   *
   * With a flat role hierarchy (no nested roles) the 400 individual readRole() calls that
   * previously drove the 20-second cold-login path become a single readRoles() batch call
   * (NEXUS-52583).
   */
  @Override
  public Collection<Permission> resolvePermissionsForRoles(final Collection<String> roleIds) {
    if (roleIds == null || roleIds.isEmpty()) {
      return Collections.emptySet();
    }

    final Set<Permission> allPermissions = new LinkedHashSet<>();
    Set<String> currentLevel = new HashSet<>(roleIds);
    final Set<String> processedRoleIds = new HashSet<>();

    while (!currentLevel.isEmpty()) {
      Set<String> toFetch = new HashSet<>();
      Set<String> nextLevel = new HashSet<>();

      for (String roleId : currentLevel) {
        if (!processedRoleIds.add(roleId)) {
          continue;
        }
        if (roleNotFoundCache.getIfPresent(roleId) != null) {
          log.trace("Role {} found in NFC, role check skipped", roleId);
          continue;
        }
        Collection<Permission> cached = rolePermissionsCache.getIfPresent(roleId);
        if (cached != null) {
          allPermissions.addAll(cached);
        }
        else {
          toFetch.add(roleId);
        }
      }

      if (toFetch.isEmpty()) {
        break;
      }

      // One DB round-trip for all roles at this BFS level
      List<CRole> roles = configuration.readRoles(toFetch);

      // One DB round-trip for all privileges referenced by the fetched roles.
      // Warms permissionsCache so the per-role permission() calls below are cache hits.
      warmPrivilegeCacheForRoles(roles);

      Set<String> foundIds = new HashSet<>();

      for (CRole role : roles) {
        foundIds.add(role.getId());
        Set<String> childRoles = role.getRoles();
        nextLevel.addAll(childRoles);
        Set<Permission> rolePerms = new LinkedHashSet<>();
        for (String privilegeId : role.getPrivileges()) {
          Permission perm = permission(privilegeId);
          if (perm != null) {
            rolePerms.add(perm);
          }
        }
        allPermissions.addAll(rolePerms);
        // Leaf roles have no children so their full permission set is known after this
        // iteration. Caching them lets other principals sharing the same roles skip DB
        // round-trips on their next cold-login path.
        if (childRoles.isEmpty()) {
          rolePermissionsCache.put(role.getId(), Collections.unmodifiableSet(rolePerms));
        }
      }

      for (String id : toFetch) {
        if (!foundIds.contains(id)) {
          roleNotFoundCache.put(id, "");
          log.trace("Caching missing role in NFC: {}", id);
        }
      }

      currentLevel = nextLevel;
    }

    return allPermissions;
  }

  /**
   * Pre-warms {@link #permissionsCache} for every privilege ID referenced by the given roles
   * using a single {@code readPrivileges} batch call instead of one {@code readPrivilege} call
   * per ID. Privilege IDs already present in the cache are excluded from the batch request.
   * After this method returns, subsequent {@link #permission(String)} calls for those IDs are
   * guaranteed cache hits (NEXUS-52583).
   */
  private void warmPrivilegeCacheForRoles(final List<CRole> roles) {
    Set<String> uncached = new HashSet<>();
    for (CRole role : roles) {
      for (String privId : role.getPrivileges()) {
        if (permissionsCache.getIfPresent(privId) == null) {
          uncached.add(privId);
        }
      }
    }
    if (uncached.isEmpty()) {
      return;
    }
    for (CPrivilege privilege : configuration.readPrivileges(uncached)) {
      PrivilegeDescriptor desc = descriptor(privilege.getType());
      if (desc != null) {
        permissionsCache.put(privilege.getId(), desc.createPermission(privilege));
      }
    }
  }

  /**
   * Returns the descriptor for the given privilege-type or {@code null}.
   */
  @Nullable
  private PrivilegeDescriptor descriptor(final String privilegeType) {
    checkNotNull(privilegeType);

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
    checkNotNull(privilegeId);

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
