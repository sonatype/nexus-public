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

import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.security.config.CPrivilege;
import org.sonatype.nexus.security.config.CRole;
import org.sonatype.nexus.security.config.MemorySecurityConfiguration;
import org.sonatype.nexus.security.config.SecurityConfiguration;
import org.sonatype.nexus.security.config.SecurityConfigurationCleaner;
import org.sonatype.nexus.security.config.SecurityConfigurationSource;
import org.sonatype.nexus.security.config.SecurityContributor;
import org.sonatype.nexus.security.config.memory.MemoryCPrivilege;
import org.sonatype.nexus.security.config.memory.MemoryCRole;
import org.sonatype.nexus.security.privilege.DuplicatePrivilegeException;
import org.sonatype.nexus.security.privilege.NoSuchPrivilegeException;
import org.sonatype.nexus.security.privilege.ReadonlyPrivilegeException;
import org.sonatype.nexus.security.role.DuplicateRoleException;
import org.sonatype.nexus.security.role.NoSuchRoleException;
import org.sonatype.nexus.security.role.ReadonlyRoleException;
import org.sonatype.nexus.security.role.RoleContainsItselfException;

import org.apache.shiro.authc.credential.PasswordService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class SecurityConfigurationManagerImplTest
{
  @Mock
  private SecurityConfigurationSource configSource;

  @Mock
  private SecurityConfigurationCleaner configCleaner;

  @Mock
  private PasswordService passwordService;

  @Mock
  private EventManager eventManager;

  @Mock
  private MemorySecurityConfiguration memorySecurityConfiguration;

  private SecurityConfigurationManagerImpl manager;

  @Before
  public void setUp() {
    when(configSource.loadConfiguration()).thenReturn(memorySecurityConfiguration);
    when(memorySecurityConfiguration.newRole()).thenAnswer(i -> new MemoryCRole());
    manager = new SecurityConfigurationManagerImpl(configSource, configCleaner, passwordService, eventManager);
  }

  @Test
  public void testGetMergedConfiguration_DontLooseMutationsWhileConfigurationIsBeingRebuild() {
    int[] mutableContributorCallCount = new int[1];
    SecurityContributor mutableContributor = new SecurityContributor()
    {
      @Override
      public SecurityConfiguration getContribution() {
        SecurityConfiguration config = new MemorySecurityConfiguration();
        if (mutableContributorCallCount[0]++ > 0) {
          CPrivilege priv = new MemoryCPrivilege();
          priv.setId("test-id");
          priv.setType("test-type");
          config.addPrivilege(priv);
        }
        return config;
      }
    };
    SecurityContributor laterContributor = new SecurityContributor()
    {
      private int callCount;

      @Override
      public SecurityConfiguration getContribution() {
        // the fixture requires laterContributor to get inspected after mutableContributor, double-check sequencing
        assertThat(mutableContributorCallCount[0], is(greaterThan(callCount)));
        if (callCount++ == 0) {
          // this emulates a mutation to mutableContributor after it just had its configuration read
          manager.on(new SecurityContributionChangedEvent());
        }
        return new MemorySecurityConfiguration();
      }
    };
    manager.addContributor(mutableContributor);
    manager.addContributor(laterContributor);
    assertThat(manager.listPrivileges(), hasSize(0));
    assertThat(manager.listPrivileges(), hasSize(1));
    assertThat(mutableContributorCallCount[0], is(2));
  }

  @Test(expected = DuplicatePrivilegeException.class)
  public void testCretePrivilege_duplicateFromOrient() {
    CPrivilege privilege = new MemoryCPrivilege();
    privilege.setId("dup");
    privilege.setName("dup");

    doThrow(new DuplicatePrivilegeException("dup")).when(memorySecurityConfiguration).addPrivilege(privilege);

    manager.createPrivilege(privilege);
  }

  @Test(expected = DuplicatePrivilegeException.class)
  public void testCreatePrivilege_duplicateFromContributors() {
    addSimplePrivilegeContributor("dup");

    CPrivilege privilege = new MemoryCPrivilege();
    privilege.setId("dup");
    privilege.setName("dup");

    manager.createPrivilege(privilege);
  }

  @Test(expected = ReadonlyPrivilegeException.class)
  public void testUpdatePrivilege_readOnly() {
    addSimplePrivilegeContributor("readonly");

    CPrivilege forUpdate = new MemoryCPrivilege();
    forUpdate.setId("readonly");
    forUpdate.setName("readonly");

    manager.updatePrivilege(forUpdate);
  }

  @Test(expected = ReadonlyPrivilegeException.class)
  public void testDeletePrivilege_readOnly() {
    when(memorySecurityConfiguration.removePrivilege("readonly")).thenThrow(new NoSuchPrivilegeException("readonly"));

    addSimplePrivilegeContributor("readonly");

    manager.deletePrivilege("readonly");
  }

  @Test(expected = DuplicateRoleException.class)
  public void testCreateRole_duplicateFromOrient() {
    CRole role = manager.newRole();
    role.setId("dup");
    role.setName("dup");

    doThrow(new DuplicateRoleException("dup")).when(memorySecurityConfiguration).addRole(role);

    manager.createRole(role);
  }

  @Test(expected = DuplicateRoleException.class)
  public void testCreateRole_duplicateFromContributors() {
    addSimpleRoleContributor("dup");

    CRole role = manager.newRole();
    role.setId("dup");
    role.setName("dup");

    manager.createRole(role);
  }

  @Test(expected = NoSuchRoleException.class)
  public void testCreateRole_invalidRole() {
    CRole role = manager.newRole();
    role.setId("new");
    role.setName("new");
    role.addRole("role1");

    manager.createRole(role);
  }

  @Test(expected = NoSuchPrivilegeException.class)
  public void testCreateRole_invalidPrivilege() {
    CRole role = manager.newRole();
    role.setId("new");
    role.setName("new");
    role.addPrivilege("priv1");

    manager.createRole(role);
  }

  @Test
  public void testCreateRole() {
    when(memorySecurityConfiguration.getPrivilege("priv1")).thenReturn(mock(CPrivilege.class));
    when(memorySecurityConfiguration.getRole("role1")).thenReturn(mock(CRole.class));

    CRole role = manager.newRole();
    role.setId("new");
    role.setName("new");
    role.addRole("role1");
    role.addPrivilege("priv1");

    try {
      manager.createRole(role);
    }
    catch (Exception e) {
      fail("expected role creation to succeed");
    }
  }

  @Test(expected = ReadonlyRoleException.class)
  public void testUpdateRole_readOnly() {
    addSimpleRoleContributor("readonly");

    CRole forUpdate = manager.newRole();
    forUpdate.setId("readonly");
    forUpdate.setName("readonly");

    manager.updateRole(forUpdate);
  }

  @Test(expected = ReadonlyRoleException.class)
  public void testDeleteRole_readOnly() {
    when(memorySecurityConfiguration.removeRole("readonly")).thenThrow(NoSuchRoleException.class);
    addSimpleRoleContributor("readonly");

    manager.deleteRole("readonly");
  }

  @Test(expected = RoleContainsItselfException.class)
  public void testUpdateRole_containsItself() {
    CRole role = manager.newRole();
    role.setId("new");
    role.setName("new");
    role.addRole("new");

    when(memorySecurityConfiguration.getRole("new")).thenReturn(role);

    manager.updateRole(role);
  }

  @Test(expected = RoleContainsItselfException.class)
  public void testUpdateRole_containsItselfIndirectly() {
    CRole role = manager.newRole();
    role.setId("new");
    role.setName("new");
    role.addRole("new2");

    CRole role2 = manager.newRole();
    role2.setId("new2");
    role2.setName("new2");
    role2.addRole("new");

    when(memorySecurityConfiguration.getRole("new")).thenReturn(role);
    when(memorySecurityConfiguration.getRole("new2")).thenReturn(role2);

    manager.updateRole(role);
  }

  @Test
  public void testCreateRole_usingPrivilegeNameIfIdNotFound() {
    when(memorySecurityConfiguration.getPrivilege("priv1")).thenReturn(null);
    when(memorySecurityConfiguration.getPrivilegeByName("priv1")).thenReturn(mock(CPrivilege.class));
    when(memorySecurityConfiguration.getRole("role1")).thenReturn(mock(CRole.class));

    CRole role = addSimpleRoleWithPrivilege();
    try {
      manager.createRole(role);
    }
    catch (Exception e) {
      fail("expected role creation to succeed");
    }
  }

  @Test(expected = NoSuchPrivilegeException.class)
  public void failCreateRole_ifPrivilegeIdOrNameDoesntExist() {
    when(memorySecurityConfiguration.getPrivilege("priv1")).thenReturn(null);
    when(memorySecurityConfiguration.getPrivilegeByName("priv1")).thenReturn(null);
    when(memorySecurityConfiguration.getRole("role1")).thenReturn(mock(CRole.class));

    CRole role = addSimpleRoleWithPrivilege();
    manager.createRole(role);
  }

  @Test
  public void testUpdateRole_removesOrphanedPrivileges() {
    // Setup: role exists with a valid privilege and an orphaned privilege
    CRole role = manager.newRole();
    role.setId("test-role");
    role.setName("test-role");
    role.addPrivilege("valid-priv");
    role.addPrivilege("orphaned-priv");

    when(memorySecurityConfiguration.getRole("test-role")).thenReturn(role);
    when(memorySecurityConfiguration.getPrivilege("valid-priv")).thenReturn(mock(CPrivilege.class));
    // orphaned-priv doesn't exist
    when(memorySecurityConfiguration.getPrivilege("orphaned-priv")).thenReturn(null);
    when(memorySecurityConfiguration.getPrivilegeByName("orphaned-priv")).thenReturn(null);

    // Update should succeed (not throw NoSuchPrivilegeException)
    manager.updateRole(role);

    // Verify orphaned privilege was removed
    assertThat(role.getPrivileges(), hasSize(1));
    assertThat(role.getPrivileges().contains("valid-priv"), is(true));
    assertThat(role.getPrivileges().contains("orphaned-priv"), is(false));
  }

  @Test
  public void testUpdateRole_removesAllOrphanedPrivileges_whenAllAreOrphaned() {
    // Setup: role exists with only orphaned privileges
    CRole role = manager.newRole();
    role.setId("test-role");
    role.setName("test-role");
    role.addPrivilege("orphaned-priv-1");
    role.addPrivilege("orphaned-priv-2");

    when(memorySecurityConfiguration.getRole("test-role")).thenReturn(role);
    // Both privileges don't exist
    when(memorySecurityConfiguration.getPrivilege("orphaned-priv-1")).thenReturn(null);
    when(memorySecurityConfiguration.getPrivilegeByName("orphaned-priv-1")).thenReturn(null);
    when(memorySecurityConfiguration.getPrivilege("orphaned-priv-2")).thenReturn(null);
    when(memorySecurityConfiguration.getPrivilegeByName("orphaned-priv-2")).thenReturn(null);

    // Update should succeed
    manager.updateRole(role);

    // Verify all orphaned privileges were removed
    assertThat(role.getPrivileges(), hasSize(0));
  }

  @Test
  public void testUpdateRole_succeeds_withNoPrivileges() {
    // Setup: role exists with no privileges
    CRole role = manager.newRole();
    role.setId("test-role");
    role.setName("test-role");

    when(memorySecurityConfiguration.getRole("test-role")).thenReturn(role);

    // Update should succeed
    manager.updateRole(role);

    // Verify privileges remain empty
    assertThat(role.getPrivileges(), hasSize(0));
  }

  @Test
  public void testUpdateRole_keepsAllPrivileges_whenNoneAreOrphaned() {
    // Setup: role exists with all valid privileges
    CRole role = manager.newRole();
    role.setId("test-role");
    role.setName("test-role");
    role.addPrivilege("valid-priv-1");
    role.addPrivilege("valid-priv-2");
    role.addPrivilege("valid-priv-3");

    when(memorySecurityConfiguration.getRole("test-role")).thenReturn(role);
    when(memorySecurityConfiguration.getPrivilege("valid-priv-1")).thenReturn(mock(CPrivilege.class));
    when(memorySecurityConfiguration.getPrivilege("valid-priv-2")).thenReturn(mock(CPrivilege.class));
    when(memorySecurityConfiguration.getPrivilege("valid-priv-3")).thenReturn(mock(CPrivilege.class));

    // Update should succeed
    manager.updateRole(role);

    // Verify all privileges are kept
    assertThat(role.getPrivileges(), hasSize(3));
    assertThat(role.getPrivileges().contains("valid-priv-1"), is(true));
    assertThat(role.getPrivileges().contains("valid-priv-2"), is(true));
    assertThat(role.getPrivileges().contains("valid-priv-3"), is(true));
  }

  @Test
  public void testUpdateRole_removesMultipleOrphanedPrivileges() {
    // Setup: role exists with mix of valid and multiple orphaned privileges
    CRole role = manager.newRole();
    role.setId("test-role");
    role.setName("test-role");
    role.addPrivilege("valid-priv");
    role.addPrivilege("orphaned-priv-1");
    role.addPrivilege("orphaned-priv-2");
    role.addPrivilege("orphaned-priv-3");

    when(memorySecurityConfiguration.getRole("test-role")).thenReturn(role);
    when(memorySecurityConfiguration.getPrivilege("valid-priv")).thenReturn(mock(CPrivilege.class));
    // Multiple orphaned privileges
    when(memorySecurityConfiguration.getPrivilege("orphaned-priv-1")).thenReturn(null);
    when(memorySecurityConfiguration.getPrivilegeByName("orphaned-priv-1")).thenReturn(null);
    when(memorySecurityConfiguration.getPrivilege("orphaned-priv-2")).thenReturn(null);
    when(memorySecurityConfiguration.getPrivilegeByName("orphaned-priv-2")).thenReturn(null);
    when(memorySecurityConfiguration.getPrivilege("orphaned-priv-3")).thenReturn(null);
    when(memorySecurityConfiguration.getPrivilegeByName("orphaned-priv-3")).thenReturn(null);

    // Update should succeed
    manager.updateRole(role);

    // Verify all orphaned privileges were removed, valid one kept
    assertThat(role.getPrivileges(), hasSize(1));
    assertThat(role.getPrivileges().contains("valid-priv"), is(true));
  }

  @Test
  public void testUpdateRole_keepsPrivilege_whenFoundByName() {
    // Setup: privilege exists by name but not by ID (edge case in existing code)
    CRole role = manager.newRole();
    role.setId("test-role");
    role.setName("test-role");
    role.addPrivilege("priv-by-name");

    when(memorySecurityConfiguration.getRole("test-role")).thenReturn(role);
    // Privilege not found by ID, but found by name
    when(memorySecurityConfiguration.getPrivilege("priv-by-name")).thenReturn(null);
    when(memorySecurityConfiguration.getPrivilegeByName("priv-by-name")).thenReturn(mock(CPrivilege.class));

    // Update should succeed
    manager.updateRole(role);

    // Verify privilege is kept (found by name)
    assertThat(role.getPrivileges(), hasSize(1));
    assertThat(role.getPrivileges().contains("priv-by-name"), is(true));
  }

  @Test(expected = NoSuchRoleException.class)
  public void testUpdateRole_failsForOrphanedNestedRole() {
    // Setup: role contains a nested role that doesn't exist
    // Nested roles should still be strictly validated (not auto-cleaned)
    CRole role = manager.newRole();
    role.setId("test-role");
    role.setName("test-role");
    role.addRole("orphaned-nested-role");

    when(memorySecurityConfiguration.getRole("test-role")).thenReturn(role);
    // Nested role doesn't exist
    when(memorySecurityConfiguration.getRole("orphaned-nested-role")).thenReturn(null);

    // Update should fail with NoSuchRoleException
    manager.updateRole(role);
  }

  @Test
  public void testUpdateRole_handlesValidRolesAndOrphanedPrivileges() {
    // Setup: role has valid nested role but orphaned privilege
    CRole role = manager.newRole();
    role.setId("test-role");
    role.setName("test-role");
    role.addRole("valid-nested-role");
    role.addPrivilege("orphaned-priv");

    CRole nestedRole = manager.newRole();
    nestedRole.setId("valid-nested-role");
    nestedRole.setName("valid-nested-role");

    when(memorySecurityConfiguration.getRole("test-role")).thenReturn(role);
    when(memorySecurityConfiguration.getRole("valid-nested-role")).thenReturn(nestedRole);
    // Privilege doesn't exist
    when(memorySecurityConfiguration.getPrivilege("orphaned-priv")).thenReturn(null);
    when(memorySecurityConfiguration.getPrivilegeByName("orphaned-priv")).thenReturn(null);

    // Update should succeed
    manager.updateRole(role);

    // Verify nested role is kept, orphaned privilege removed
    assertThat(role.getRoles(), hasSize(1));
    assertThat(role.getRoles().contains("valid-nested-role"), is(true));
    assertThat(role.getPrivileges(), hasSize(0));
  }

  private void addSimpleRoleContributor(final String roleName) {
    manager.addContributor(() -> {
      SecurityConfiguration config = new MemorySecurityConfiguration();
      CRole readonlyRole = manager.newRole();
      readonlyRole.setId(roleName);
      readonlyRole.setName(roleName);
      config.addRole(readonlyRole);
      return config;
    });
  }

  private void addSimplePrivilegeContributor(final String privName) {
    manager.addContributor(() -> {
      SecurityConfiguration config = new MemorySecurityConfiguration();
      CPrivilege readonlyPriv = new MemoryCPrivilege();
      readonlyPriv.setId(privName);
      readonlyPriv.setName(privName);
      config.addPrivilege(readonlyPriv);
      return config;
    });
  }

  private CRole addSimpleRoleWithPrivilege() {
    CRole role = manager.newRole();
    role.setId("new");
    role.setName("new");
    role.addRole("role1");
    role.addPrivilege("priv1");
    return role;
  }
}
