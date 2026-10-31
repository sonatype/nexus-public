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
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.sonatype.nexus.security.authz.AuthorizationConfigurationChanged;
import org.sonatype.nexus.security.config.CPrivilege;
import org.sonatype.nexus.security.config.SecurityConfigurationManager;
import org.sonatype.nexus.security.config.memory.MemoryCPrivilege;
import org.sonatype.nexus.security.config.memory.MemoryCRole;
import org.sonatype.nexus.security.privilege.PrivilegeDescriptor;
import org.sonatype.nexus.security.role.NoSuchRoleException;

import org.apache.shiro.authz.Permission;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link RolePermissionResolverImpl}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class RolePermissionResolverImplTest
{
  private RolePermissionResolverImpl underTest;

  private SecurityConfigurationManager securityConfigurationManager;

  @Before
  public void setUp() throws Exception {
    securityConfigurationManager = mock(SecurityConfigurationManager.class);
    when(securityConfigurationManager.readRole(any())).thenThrow(new NoSuchRoleException("Role not found"));
    underTest = new RolePermissionResolverImpl(securityConfigurationManager, Collections.emptyList(),
        10);
  }

  // ---------------------------------------------------------------------------
  // resolvePermissionsForRoles — batch variant tests
  // ---------------------------------------------------------------------------

  @Test
  public void resolvePermissionsForRoles_nullAndEmptyReturnEmpty() {
    assertThat(underTest.resolvePermissionsForRoles(null), is(empty()));
    assertThat(underTest.resolvePermissionsForRoles(Collections.emptyList()), is(empty()));
    verify(securityConfigurationManager, never()).readRoles(any());
  }

  /**
   * A batch of N role IDs must produce exactly 1 readRoles() call, never N readRole() calls.
   */
  @Test
  @SuppressWarnings("unchecked")
  public void resolvePermissionsForRoles_singleBatchQuery() {
    MemoryCRole r1 = new MemoryCRole().withId("r1");
    MemoryCRole r2 = new MemoryCRole().withId("r2");
    MemoryCRole r3 = new MemoryCRole().withId("r3");
    when(securityConfigurationManager.readRoles(any())).thenReturn(List.of(r1, r2, r3));

    underTest.resolvePermissionsForRoles(List.of("r1", "r2", "r3"));

    ArgumentCaptor<Collection<String>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(securityConfigurationManager, times(1)).readRoles(captor.capture());
    assertThat(captor.getValue(), containsInAnyOrder("r1", "r2", "r3"));
    verify(securityConfigurationManager, never()).readRole(any());
  }

  /**
   * Roles absent from the DB go into the NFC; a repeat call must not hit readRoles again.
   */
  @Test
  public void resolvePermissionsForRoles_missingRolesGoToNfc() {
    // First call: DB returns nothing → both roles go to NFC
    when(securityConfigurationManager.readRoles(any())).thenReturn(Collections.emptyList());
    underTest.resolvePermissionsForRoles(List.of("missing1", "missing2"));
    verify(securityConfigurationManager, times(1)).readRoles(any());

    // Second call: NFC should short-circuit, no further DB access
    underTest.resolvePermissionsForRoles(List.of("missing1", "missing2"));
    verify(securityConfigurationManager, times(1)).readRoles(any());
  }

  /**
   * After a first call, leaf roles are cached. A second call for overlapping roles
   * must not re-query the DB for the already-cached ones.
   */
  @Test
  @SuppressWarnings("unchecked")
  public void resolvePermissionsForRoles_leafRolesCachedAcrossCalls() {
    MemoryCRole r1 = new MemoryCRole().withId("r1"); // leaf
    MemoryCRole r2 = new MemoryCRole().withId("r2"); // leaf
    MemoryCRole r3 = new MemoryCRole().withId("r3"); // leaf — only in second call
    when(securityConfigurationManager.readRoles(any())).thenAnswer(inv -> {
      Collection<String> ids = inv.getArgument(0);
      return ids.stream()
          .map(id -> id.equals("r1") ? r1 : id.equals("r2") ? r2 : r3)
          .collect(java.util.stream.Collectors.toList());
    });

    // First call populates the per-role cache for r1 and r2
    underTest.resolvePermissionsForRoles(List.of("r1", "r2"));
    ArgumentCaptor<Collection<String>> captor1 = ArgumentCaptor.forClass(Collection.class);
    verify(securityConfigurationManager, times(1)).readRoles(captor1.capture());
    assertThat(captor1.getValue(), containsInAnyOrder("r1", "r2"));

    // Second call for r1 (cached) + r3 (new): only r3 should go to DB
    underTest.resolvePermissionsForRoles(List.of("r1", "r3"));
    ArgumentCaptor<Collection<String>> captor2 = ArgumentCaptor.forClass(Collection.class);
    verify(securityConfigurationManager, times(2)).readRoles(captor2.capture());
    assertThat(captor2.getValue(), containsInAnyOrder("r3"));
  }

  /**
   * Roles with nested child roles trigger a second BFS-level batch call.
   */
  @Test
  @SuppressWarnings("unchecked")
  public void resolvePermissionsForRoles_nestedRolesUseBfsBatching() {
    MemoryCRole parent = new MemoryCRole().withId("parent").withRoles("child1", "child2");
    MemoryCRole child1 = new MemoryCRole().withId("child1");
    MemoryCRole child2 = new MemoryCRole().withId("child2");

    when(securityConfigurationManager.readRoles(any())).thenAnswer(inv -> {
      Collection<String> ids = inv.getArgument(0);
      List<MemoryCRole> result = new java.util.ArrayList<>();
      if (ids.contains("parent"))
        result.add(parent);
      if (ids.contains("child1"))
        result.add(child1);
      if (ids.contains("child2"))
        result.add(child2);
      return result;
    });

    underTest.resolvePermissionsForRoles(List.of("parent"));

    // Expect exactly 2 readRoles calls: one for [parent], one for [child1, child2]
    ArgumentCaptor<Collection<String>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(securityConfigurationManager, times(2)).readRoles(captor.capture());
    List<Collection<String>> allCalls = captor.getAllValues();
    assertThat(allCalls.get(0), containsInAnyOrder("parent"));
    assertThat(allCalls.get(1), containsInAnyOrder("child1", "child2"));
    verify(securityConfigurationManager, never()).readRole(any());
  }

  /**
   * Firing AuthorizationConfigurationChanged clears the role cache so a subsequent
   * call re-queries the DB.
   */
  @Test
  public void resolvePermissionsForRoles_cacheInvalidatedOnEvent() {
    MemoryCRole r1 = new MemoryCRole().withId("r1");
    when(securityConfigurationManager.readRoles(any())).thenReturn(List.of(r1));

    underTest.resolvePermissionsForRoles(List.of("r1"));
    verify(securityConfigurationManager, times(1)).readRoles(any());

    // Cache hit — no DB call
    underTest.resolvePermissionsForRoles(List.of("r1"));
    verify(securityConfigurationManager, times(1)).readRoles(any());

    // Invalidate
    underTest.on(new AuthorizationConfigurationChanged());

    underTest.resolvePermissionsForRoles(List.of("r1"));
    verify(securityConfigurationManager, times(2)).readRoles(any());
  }

  /**
   * All privilege IDs referenced by a batch of roles must be fetched in a single
   * readPrivileges() call. The singular readPrivilege() must never be invoked from
   * the batch path (NEXUS-52583).
   */
  @Test
  @SuppressWarnings("unchecked")
  public void resolvePermissionsForRoles_privilegesBatchFetched() {
    PrivilegeDescriptor descriptor = mock(PrivilegeDescriptor.class);
    when(descriptor.getType()).thenReturn("testType");
    Permission perm1 = mock(Permission.class);
    Permission perm2 = mock(Permission.class);

    CPrivilege priv1 = new MemoryCPrivilege.MemoryCPrivilegeBuilder("priv1").name("priv1").type("testType").build();
    CPrivilege priv2 = new MemoryCPrivilege.MemoryCPrivilegeBuilder("priv2").name("priv2").type("testType").build();
    when(descriptor.createPermission(priv1)).thenReturn(perm1);
    when(descriptor.createPermission(priv2)).thenReturn(perm2);

    // Rebuild underTest with a real descriptor so privileges can be resolved
    RolePermissionResolverImpl resolver = new RolePermissionResolverImpl(
        securityConfigurationManager, List.of(descriptor), 10);

    MemoryCRole role = new MemoryCRole().withId("roleA").withPrivileges("priv1", "priv2");
    when(securityConfigurationManager.readRoles(any())).thenReturn(List.of(role));
    when(securityConfigurationManager.readPrivileges(any(Set.class))).thenReturn(List.of(priv1, priv2));

    Collection<Permission> result = resolver.resolvePermissionsForRoles(List.of("roleA"));

    assertThat(result, containsInAnyOrder(perm1, perm2));
    verify(securityConfigurationManager, times(1)).readPrivileges(any());
    verify(securityConfigurationManager, never()).readPrivilege(any());
  }

  /**
   * Privileges already in the cache must not be re-fetched on a second call.
   */
  @Test
  @SuppressWarnings("unchecked")
  public void resolvePermissionsForRoles_cachedPrivilegesSkippedInBatch() {
    PrivilegeDescriptor descriptor = mock(PrivilegeDescriptor.class);
    when(descriptor.getType()).thenReturn("testType");
    when(descriptor.createPermission(any())).thenReturn(mock(Permission.class));

    CPrivilege priv1 = new MemoryCPrivilege.MemoryCPrivilegeBuilder("priv1").name("priv1").type("testType").build();
    CPrivilege priv2 = new MemoryCPrivilege.MemoryCPrivilegeBuilder("priv2").name("priv2").type("testType").build();

    RolePermissionResolverImpl resolver = new RolePermissionResolverImpl(
        securityConfigurationManager, List.of(descriptor), 10);

    MemoryCRole role = new MemoryCRole().withId("roleA").withPrivileges("priv1", "priv2");
    when(securityConfigurationManager.readRoles(any())).thenReturn(List.of(role));
    when(securityConfigurationManager.readPrivileges(any(Set.class))).thenReturn(List.of(priv1, priv2));

    // First call: both privileges fetched from DB and cached
    resolver.resolvePermissionsForRoles(List.of("roleA"));
    verify(securityConfigurationManager, times(1)).readPrivileges(any());

    // Second call: role is a leaf so it is in rolePermissionsCache — no privilege fetch needed
    resolver.resolvePermissionsForRoles(List.of("roleA"));
    verify(securityConfigurationManager, times(1)).readPrivileges(any());
  }

  // ---------------------------------------------------------------------------
  // resolvePermissionsInRole — existing NFC test (unchanged below)
  // ---------------------------------------------------------------------------

  @Test
  public void resolvePermissionsInRole_roleNotFoundCache() throws Exception {
    underTest.resolvePermissionsInRole("role1");
    verify(securityConfigurationManager).readRole(any());

    // just call it 3 more times for fun
    underTest.resolvePermissionsInRole("role1");
    underTest.resolvePermissionsInRole("role1");
    underTest.resolvePermissionsInRole("role1");

    // should still have only been called once
    verify(securityConfigurationManager).readRole(any());

    // simulate event being fired, which clears cache
    underTest.on(new AuthorizationConfigurationChanged());

    underTest.resolvePermissionsInRole("role1");
    verify(securityConfigurationManager, times(2)).readRole(any());

    // and finally make sure we are hitting cache again
    underTest.resolvePermissionsInRole("role1");
    underTest.resolvePermissionsInRole("role1");
    underTest.resolvePermissionsInRole("role1");
    verify(securityConfigurationManager, times(2)).readRole(any());
  }
}
