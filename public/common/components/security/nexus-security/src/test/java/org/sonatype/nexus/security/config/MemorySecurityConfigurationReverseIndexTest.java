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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.sonatype.nexus.security.config.memory.MemoryCRole;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

/**
 * Tests for the reverse index (privilege-to-roles mapping) in MemorySecurityConfiguration.
 * This addresses NEXUS-49996: performance optimization for repository deletion with large role sets.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class MemorySecurityConfigurationReverseIndexTest
{
  private static final Logger log = LoggerFactory.getLogger(MemorySecurityConfigurationReverseIndexTest.class);

  private MemorySecurityConfiguration config;

  @Before
  public void setup() {
    config = new MemorySecurityConfiguration();
  }

  @Test
  public void testGetRolesByPrivilege_returnsCorrectRoles() {
    // Create roles with different privileges
    MemoryCRole role1 = createRole("role1", "priv1", "priv2");
    MemoryCRole role2 = createRole("role2", "priv2", "priv3");
    MemoryCRole role3 = createRole("role3", "priv4");

    config.addRole(role1);
    config.addRole(role2);
    config.addRole(role3);

    // Verify reverse index lookups
    Set<String> rolesWithPriv1 = config.getRolesByPrivilege("priv1");
    assertThat(rolesWithPriv1, containsInAnyOrder("role1"));

    Set<String> rolesWithPriv2 = config.getRolesByPrivilege("priv2");
    assertThat(rolesWithPriv2, containsInAnyOrder("role1", "role2"));

    Set<String> rolesWithPriv3 = config.getRolesByPrivilege("priv3");
    assertThat(rolesWithPriv3, containsInAnyOrder("role2"));

    Set<String> rolesWithPriv4 = config.getRolesByPrivilege("priv4");
    assertThat(rolesWithPriv4, containsInAnyOrder("role3"));
  }

  @Test
  public void testGetRolesByPrivilege_nonExistentPrivilege_returnsEmptySet() {
    MemoryCRole role1 = createRole("role1", "priv1");
    config.addRole(role1);

    Set<String> rolesWithNonExistent = config.getRolesByPrivilege("nonexistent");
    assertThat(rolesWithNonExistent, is(empty()));
  }

  @Test
  public void testAddRole_maintainsReverseIndex() {
    MemoryCRole role = createRole("test-role", "priv1", "priv2", "priv3");
    config.addRole(role);

    // Verify all privileges are indexed
    assertThat(config.getRolesByPrivilege("priv1"), containsInAnyOrder("test-role"));
    assertThat(config.getRolesByPrivilege("priv2"), containsInAnyOrder("test-role"));
    assertThat(config.getRolesByPrivilege("priv3"), containsInAnyOrder("test-role"));
  }

  @Test
  public void testUpdateRole_updatesReverseIndex() {
    // Add role with initial privileges
    MemoryCRole role = createRole("test-role", "priv1", "priv2");
    config.addRole(role);

    // Update role - remove priv1, keep priv2, add priv3
    MemoryCRole updatedRole = createRole("test-role", "priv2", "priv3");
    config.updateRole(updatedRole);

    // Verify reverse index updated correctly
    assertThat(config.getRolesByPrivilege("priv1"), is(empty()));
    assertThat(config.getRolesByPrivilege("priv2"), containsInAnyOrder("test-role"));
    assertThat(config.getRolesByPrivilege("priv3"), containsInAnyOrder("test-role"));
  }

  @Test
  public void testUpdateRole_addPrivileges_updatesReverseIndex() {
    // Add role with one privilege
    MemoryCRole role = createRole("test-role", "priv1");
    config.addRole(role);

    // Update role - add more privileges
    MemoryCRole updatedRole = createRole("test-role", "priv1", "priv2", "priv3");
    config.updateRole(updatedRole);

    // Verify all privileges indexed
    assertThat(config.getRolesByPrivilege("priv1"), containsInAnyOrder("test-role"));
    assertThat(config.getRolesByPrivilege("priv2"), containsInAnyOrder("test-role"));
    assertThat(config.getRolesByPrivilege("priv3"), containsInAnyOrder("test-role"));
  }

  @Test
  public void testUpdateRole_removeAllPrivileges_cleansReverseIndex() {
    // Add role with privileges
    MemoryCRole role = createRole("test-role", "priv1", "priv2");
    config.addRole(role);

    // Update role - remove all privileges
    MemoryCRole updatedRole = createRole("test-role");
    config.updateRole(updatedRole);

    // Verify reverse index cleaned up
    assertThat(config.getRolesByPrivilege("priv1"), is(empty()));
    assertThat(config.getRolesByPrivilege("priv2"), is(empty()));
  }

  @Test
  public void testRemoveRole_cleansReverseIndex() {
    // Add role with privileges
    MemoryCRole role = createRole("test-role", "priv1", "priv2", "priv3");
    config.addRole(role);

    // Remove role
    boolean removed = config.removeRole("test-role");
    assertThat(removed, is(true));

    // Verify reverse index cleaned up
    assertThat(config.getRolesByPrivilege("priv1"), is(empty()));
    assertThat(config.getRolesByPrivilege("priv2"), is(empty()));
    assertThat(config.getRolesByPrivilege("priv3"), is(empty()));
  }

  @Test
  public void testRemoveRole_multipleRolesWithSamePrivilege_onlyRemovesDeletedRole() {
    MemoryCRole role1 = createRole("role1", "shared-priv");
    MemoryCRole role2 = createRole("role2", "shared-priv");
    config.addRole(role1);
    config.addRole(role2);

    // Remove one role
    config.removeRole("role1");

    // Verify other role still indexed
    assertThat(config.getRolesByPrivilege("shared-priv"), containsInAnyOrder("role2"));
  }

  @Test
  public void testClone_deepCopiesReverseIndex() throws CloneNotSupportedException {
    // Add roles to original config
    MemoryCRole role1 = createRole("role1", "priv1", "priv2");
    MemoryCRole role2 = createRole("role2", "priv2", "priv3");
    config.addRole(role1);
    config.addRole(role2);

    // Clone configuration
    MemorySecurityConfiguration clonedConfig = config.clone();

    // Verify cloned config has same reverse index
    assertThat(clonedConfig.getRolesByPrivilege("priv1"), containsInAnyOrder("role1"));
    assertThat(clonedConfig.getRolesByPrivilege("priv2"), containsInAnyOrder("role1", "role2"));
    assertThat(clonedConfig.getRolesByPrivilege("priv3"), containsInAnyOrder("role2"));

    // Modify original config
    MemoryCRole role3 = createRole("role3", "priv1");
    config.addRole(role3);

    // Verify cloned config unchanged
    assertThat(clonedConfig.getRolesByPrivilege("priv1"), containsInAnyOrder("role1"));
    assertThat(clonedConfig.getRolesByPrivilege("priv1").contains("role3"), is(false));
  }

  @Test
  public void testReverseIndex_performanceWithManyRoles() {
    // This simulates the Lloyds Bank scenario: 8,000 roles with repository privileges
    int numRoles = 8000;
    String targetPrivilege = "nx-repository-view-*-*-delete";

    // Create 8,000 roles, ~10% with the target privilege
    for (int i = 0; i < numRoles; i++) {
      MemoryCRole role = new MemoryCRole();
      role.setId("role-" + i);
      role.setName("Test Role " + i);

      if (i % 10 == 0) {
        // Every 10th role has the target privilege
        role.setPrivileges(new HashSet<>(Arrays.asList(targetPrivilege, "other-priv-" + i)));
      }
      else {
        role.setPrivileges(new HashSet<>(Arrays.asList("other-priv-" + i)));
      }

      config.addRole(role);
    }

    // Measure reverse index lookup (should be O(1) instead of O(n))
    long startTime = System.currentTimeMillis();
    Set<String> affectedRoles = config.getRolesByPrivilege(targetPrivilege);
    long endTime = System.currentTimeMillis();

    // Verify correct number of roles found (800 out of 8000)
    assertThat(affectedRoles.size(), is(800));

    // Verify lookup was fast (should be < 100ms even with 8000 roles)
    long duration = endTime - startTime;
    log.info("Reverse index lookup with {} roles took {}ms", numRoles, duration);
    assertThat("Lookup should be sub-second", duration < 1000, is(true));
  }

  private MemoryCRole createRole(String roleId, String... privilegeIds) {
    MemoryCRole role = new MemoryCRole();
    role.setId(roleId);
    role.setName("Test Role: " + roleId);
    role.setPrivileges(new HashSet<>(Arrays.asList(privilegeIds)));
    return role;
  }
}
