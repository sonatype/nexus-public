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

import java.util.Arrays;
import java.util.HashSet;

import org.sonatype.nexus.security.config.CRole;
import org.sonatype.nexus.security.config.CUserRoleMapping;
import org.sonatype.nexus.security.config.MemorySecurityConfiguration;
import org.sonatype.nexus.security.config.memory.MemoryCRole;
import org.sonatype.nexus.security.config.memory.MemoryCUserRoleMapping;
import org.sonatype.nexus.security.user.UserManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Tests for SecurityConfigurationCleanerImpl with reverse index optimization.
 * This addresses NEXUS-49996: performance optimization for repository deletion with large role sets.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class SecurityConfigurationCleanerImplTest
{
  private static final Logger log = LoggerFactory.getLogger(SecurityConfigurationCleanerImplTest.class);

  private SecurityConfigurationCleanerImpl cleaner;

  private MemorySecurityConfiguration config;

  @Before
  public void setup() {
    cleaner = new SecurityConfigurationCleanerImpl();
    config = new MemorySecurityConfiguration();
  }

  @Test
  public void testPrivilegeRemoved_usesReverseIndex() {
    // Create 3 roles, only 2 have the target privilege
    MemoryCRole role1 = createRole("role1", "target-priv", "other-priv1");
    MemoryCRole role2 = createRole("role2", "other-priv2");
    MemoryCRole role3 = createRole("role3", "target-priv", "other-priv3");

    config.addRole(role1);
    config.addRole(role2);
    config.addRole(role3);

    // Remove the privilege
    cleaner.privilegeRemoved(config, "target-priv");

    // Verify only affected roles were updated
    CRole updatedRole1 = config.getRole("role1");
    assertThat(updatedRole1.getPrivileges(), containsInAnyOrder("other-priv1"));
    assertThat(updatedRole1.getPrivileges(), not(containsInAnyOrder("target-priv")));

    CRole updatedRole2 = config.getRole("role2");
    assertThat(updatedRole2.getPrivileges(), containsInAnyOrder("other-priv2"));

    CRole updatedRole3 = config.getRole("role3");
    assertThat(updatedRole3.getPrivileges(), containsInAnyOrder("other-priv3"));
    assertThat(updatedRole3.getPrivileges(), not(containsInAnyOrder("target-priv")));
  }

  @Test
  public void testPrivilegeRemoved_noRolesAffected() {
    // Create roles without the target privilege
    MemoryCRole role1 = createRole("role1", "priv1");
    MemoryCRole role2 = createRole("role2", "priv2");

    config.addRole(role1);
    config.addRole(role2);

    // Remove non-existent privilege
    cleaner.privilegeRemoved(config, "nonexistent-priv");

    // Verify roles unchanged
    assertThat(config.getRole("role1").getPrivileges(), containsInAnyOrder("priv1"));
    assertThat(config.getRole("role2").getPrivileges(), containsInAnyOrder("priv2"));
  }

  @Test
  public void testPrivilegeRemoved_performanceWith8000Roles() {
    // Simulate Lloyds Bank scenario: 8,000 roles, 6 privilege deletions per repository
    int numRoles = 8000;
    String targetPrivilege = "nx-repository-view-maven2-test-repo-delete";

    // Create 8,000 roles, ~10% with repository privileges
    for (int i = 0; i < numRoles; i++) {
      MemoryCRole role = new MemoryCRole();
      role.setId("role-" + i);
      role.setName("Test Role " + i);

      if (i % 10 == 0) {
        // Every 10th role has repository privileges
        role.setPrivileges(new HashSet<>(Arrays.asList(
            targetPrivilege,
            "nx-repository-view-maven2-test-repo-read",
            "nx-repository-view-maven2-test-repo-browse",
            "other-priv-" + i)));
      }
      else {
        role.setPrivileges(new HashSet<>(Arrays.asList("other-priv-" + i)));
      }

      config.addRole(role);
    }

    // Measure cleanup performance (simulates deleting 1 privilege out of 6)
    long startTime = System.currentTimeMillis();
    cleaner.privilegeRemoved(config, targetPrivilege);
    long endTime = System.currentTimeMillis();

    long duration = endTime - startTime;
    log.info("Privilege cleanup with {} roles took {}ms", numRoles, duration);

    // Verify privilege removed from affected roles
    for (int i = 0; i < numRoles; i += 10) {
      CRole role = config.getRole("role-" + i);
      assertThat("Role should not contain deleted privilege",
          role.getPrivileges().contains(targetPrivilege), is(false));
    }

    // With reverse index, cleanup should be sub-second even with 8,000 roles
    assertThat("Cleanup should be fast with reverse index", duration < 1000, is(true));
  }

  @Test
  public void testRoleRemoved_cleansRolesAndUsers() {
    // Create role hierarchy: role1 contains role2
    MemoryCRole role1 = createRole("role1", "priv1");
    role1.setRoles(new HashSet<>(Arrays.asList("role2")));

    MemoryCRole role2 = createRole("role2", "priv2");

    config.addRole(role1);
    config.addRole(role2);

    // Create user with role2
    MemoryCUserRoleMapping userMapping = new MemoryCUserRoleMapping();
    userMapping.setUserId("test-user");
    userMapping.setSource(UserManager.DEFAULT_SOURCE);
    userMapping.setRoles(new HashSet<>(Arrays.asList("role2", "other-role")));
    config.addUserRoleMapping(userMapping);

    // Remove role2
    cleaner.roleRemoved(config, "role2");

    // Verify role1 no longer contains role2
    CRole updatedRole1 = config.getRole("role1");
    assertThat(updatedRole1.getRoles(), not(containsInAnyOrder("role2")));

    // Verify user no longer has role2
    CUserRoleMapping updatedMapping = config.getUserRoleMapping("test-user", UserManager.DEFAULT_SOURCE);
    assertThat(updatedMapping.getRoles(), containsInAnyOrder("other-role"));
    assertThat(updatedMapping.getRoles(), not(containsInAnyOrder("role2")));
  }

  @Test
  public void testRoleRemoved_multipleRolesAndUsers() {
    // Create 3 roles that all contain target role
    MemoryCRole role1 = createRole("role1", "priv1");
    role1.setRoles(new HashSet<>(Arrays.asList("target-role")));

    MemoryCRole role2 = createRole("role2", "priv2");
    role2.setRoles(new HashSet<>(Arrays.asList("target-role", "other-role")));

    MemoryCRole role3 = createRole("role3", "priv3");
    role3.setRoles(new HashSet<>(Arrays.asList("other-role")));

    MemoryCRole targetRole = createRole("target-role", "target-priv");

    config.addRole(role1);
    config.addRole(role2);
    config.addRole(role3);
    config.addRole(targetRole);

    // Create 2 users with target role
    MemoryCUserRoleMapping user1 = new MemoryCUserRoleMapping();
    user1.setUserId("user1");
    user1.setSource(UserManager.DEFAULT_SOURCE);
    user1.setRoles(new HashSet<>(Arrays.asList("target-role", "role1")));

    MemoryCUserRoleMapping user2 = new MemoryCUserRoleMapping();
    user2.setUserId("user2");
    user2.setSource(UserManager.DEFAULT_SOURCE);
    user2.setRoles(new HashSet<>(Arrays.asList("target-role")));

    config.addUserRoleMapping(user1);
    config.addUserRoleMapping(user2);

    // Remove target role
    cleaner.roleRemoved(config, "target-role");

    // Verify all roles cleaned
    assertThat(config.getRole("role1").getRoles().contains("target-role"), is(false));
    assertThat(config.getRole("role2").getRoles().contains("target-role"), is(false));
    assertThat(config.getRole("role2").getRoles(), containsInAnyOrder("other-role"));
    assertThat(config.getRole("role3").getRoles(), containsInAnyOrder("other-role"));

    // Verify all users cleaned
    assertThat(config.getUserRoleMapping("user1", UserManager.DEFAULT_SOURCE).getRoles(),
        containsInAnyOrder("role1"));
    assertThat(config.getUserRoleMapping("user2", UserManager.DEFAULT_SOURCE).getRoles().isEmpty(), is(true));
  }

  private MemoryCRole createRole(String roleId, String... privilegeIds) {
    MemoryCRole role = new MemoryCRole();
    role.setId(roleId);
    role.setName("Test Role: " + roleId);
    role.setPrivileges(new HashSet<>(Arrays.asList(privilegeIds)));
    return role;
  }
}
