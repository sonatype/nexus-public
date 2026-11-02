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
package org.sonatype.nexus.security.config;

import java.util.List;
import java.util.Set;

import org.sonatype.nexus.security.config.memory.MemoryCPrivilege;
import org.sonatype.nexus.security.config.memory.MemoryCRole;
import org.sonatype.nexus.security.config.memory.MemoryCUser;
import org.sonatype.nexus.security.config.memory.MemoryCUserRoleMapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pins the contract of the list getters of {@link MemorySecurityConfiguration}: values()-based getters return a
 * detached, immutable snapshot (not a live view of the backing map), and every returned list tolerates
 * {@code contains(null)}/{@code indexOf(null)} (as Guava's ImmutableList did) instead of throwing
 * {@link NullPointerException}.
 */
class MemorySecurityConfigurationSnapshotTest
{
  private MemorySecurityConfiguration config;

  @BeforeEach
  void setup() {
    config = new MemorySecurityConfiguration();
  }

  @Test
  void getUsersReturnsSnapshotUnaffectedByLaterChanges() {
    config.withUsers(user("u1"));

    List<CUser> snapshot = config.getUsers();
    assertThat(snapshot, hasSize(1));

    config.addUser(user("u2"), Set.of());
    assertThat(snapshot, hasSize(1));
    assertThat(config.getUsers(), hasSize(2));

    config.removeUser("u1");
    assertThat(snapshot, hasSize(1));
    assertThat(snapshot.get(0).getId(), is("u1"));
    assertThat(config.getUsers(), hasSize(1));
  }

  @Test
  void getUsersToleratesNullQueryAndIsImmutable() {
    config.withUsers(user("u1"));

    List<CUser> users = config.getUsers();
    assertNullQueryTolerantAndImmutable(users, user("u3"));
  }

  @Test
  void getUserRoleMappingsReturnsSnapshotUnaffectedByLaterChanges() {
    config.withUserRoleMappings(mapping("u1", "default"));

    List<CUserRoleMapping> snapshot = config.getUserRoleMappings();
    assertThat(snapshot, hasSize(1));

    config.addUserRoleMapping(mapping("u2", "default"));
    assertThat(snapshot, hasSize(1));
    assertThat(config.getUserRoleMappings(), hasSize(2));

    config.removeUserRoleMapping("u1", "default");
    assertThat(snapshot, hasSize(1));
    assertThat(snapshot.get(0).getUserId(), is("u1"));
    assertThat(config.getUserRoleMappings(), hasSize(1));
  }

  @Test
  void getUserRoleMappingsToleratesNullQueryAndIsImmutable() {
    config.withUserRoleMappings(mapping("u1", "default"));

    assertNullQueryTolerantAndImmutable(config.getUserRoleMappings(), mapping("u3", "default"));
  }

  @Test
  void getPrivilegesReturnsSnapshotUnaffectedByLaterChanges() {
    config.addPrivilege(privilege("p1"));

    List<CPrivilege> snapshot = config.getPrivileges();
    assertThat(snapshot, hasSize(1));

    config.addPrivilege(privilege("p2"));
    assertThat(snapshot, hasSize(1));
    assertThat(config.getPrivileges(), hasSize(2));

    config.removePrivilege("p1");
    assertThat(snapshot, hasSize(1));
    assertThat(snapshot.get(0).getId(), is("p1"));
    assertThat(config.getPrivileges(), hasSize(1));
  }

  @Test
  void getPrivilegesToleratesNullQueryAndIsImmutable() {
    config.addPrivilege(privilege("p1"));

    assertNullQueryTolerantAndImmutable(config.getPrivileges(), privilege("p3"));
  }

  @Test
  void getRolesReturnsSnapshotUnaffectedByLaterChanges() {
    config.addRole(role("r1"));

    List<CRole> snapshot = config.getRoles();
    assertThat(snapshot, hasSize(1));

    config.addRole(role("r2"));
    assertThat(snapshot, hasSize(1));
    assertThat(config.getRoles(), hasSize(2));

    config.removeRole("r1");
    assertThat(snapshot, hasSize(1));
    assertThat(snapshot.get(0).getId(), is("r1"));
    assertThat(config.getRoles(), hasSize(1));
  }

  @Test
  void getRolesToleratesNullQueryAndIsImmutable() {
    config.addRole(role("r1"));

    assertNullQueryTolerantAndImmutable(config.getRoles(), role("r3"));
  }

  @Test
  void getPrivilegesByIdsWithEmptyIdsToleratesNullQuery() {
    List<CPrivilege> result = config.getPrivileges(Set.of());

    assertThat(result.isEmpty(), is(true));
    assertThat(result.contains(null), is(false));
    assertThat(result.indexOf(null), is(-1));
  }

  @Test
  void getPrivilegesByIdsToleratesNullQuery() {
    config.addPrivilege(privilege("p1"));

    List<CPrivilege> found = config.getPrivileges(Set.of("p1", "unknown"));
    assertThat(found, hasSize(1));
    assertThat(found.contains(null), is(false));
    assertThat(found.indexOf(null), is(-1));

    List<CPrivilege> none = config.getPrivileges(Set.of("unknown"));
    assertThat(none.isEmpty(), is(true));
    assertThat(none.contains(null), is(false));
    assertThat(none.indexOf(null), is(-1));
  }

  @Test
  void getRolesByIdsWithEmptyIdsToleratesNullQuery() {
    List<CRole> result = config.getRoles(Set.of());

    assertThat(result.isEmpty(), is(true));
    assertThat(result.contains(null), is(false));
    assertThat(result.indexOf(null), is(-1));
  }

  @Test
  void getRolesByIdsToleratesNullQuery() {
    config.addRole(role("r1"));

    List<CRole> found = config.getRoles(Set.of("r1", "unknown"));
    assertThat(found, hasSize(1));
    assertThat(found.contains(null), is(false));
    assertThat(found.indexOf(null), is(-1));

    List<CRole> none = config.getRoles(Set.of("unknown"));
    assertThat(none.isEmpty(), is(true));
    assertThat(none.contains(null), is(false));
    assertThat(none.indexOf(null), is(-1));
  }

  private static <T> void assertNullQueryTolerantAndImmutable(final List<T> list, final T extra) {
    assertThat(list.contains(null), is(false));
    assertThat(list.indexOf(null), is(-1));
    assertThrows(UnsupportedOperationException.class, () -> list.add(extra));
    assertThrows(UnsupportedOperationException.class, () -> list.remove(0));
  }

  private static CUser user(final String id) {
    return new MemoryCUser().withId(id);
  }

  private static CUserRoleMapping mapping(final String userId, final String source) {
    return new MemoryCUserRoleMapping().withUserId(userId).withSource(source).withRoles("test-role");
  }

  private static CPrivilege privilege(final String id) {
    MemoryCPrivilege privilege = new MemoryCPrivilege();
    privilege.setId(id);
    return privilege;
  }

  private static CRole role(final String id) {
    MemoryCRole role = new MemoryCRole();
    role.setId(id);
    return role;
  }
}
