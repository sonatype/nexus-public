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

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.security.config.CPrivilege;
import org.sonatype.nexus.security.config.CRole;
import org.sonatype.nexus.security.config.MemorySecurityConfiguration;
import org.sonatype.nexus.security.config.SecurityConfiguration;
import org.sonatype.nexus.security.config.SecurityConfigurationCleaner;
import org.sonatype.nexus.security.config.SecurityConfigurationSource;
import org.sonatype.nexus.security.config.SecurityContributor;
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
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SecurityConfigurationManagerImplTest
    extends TestSupport
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
          CPrivilege priv = new CPrivilege();
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
    CPrivilege privilege = new CPrivilege();
    privilege.setId("dup");
    privilege.setName("dup");

    doThrow(new DuplicatePrivilegeException("dup")).when(memorySecurityConfiguration).addPrivilege(privilege);

    manager.createPrivilege(privilege);
  }

  @Test(expected = DuplicatePrivilegeException.class)
  public void testCreatePrivilege_duplicateFromContributors() {
    addSimplePrivilegeContributor("dup");

    CPrivilege privilege = new CPrivilege();
    privilege.setId("dup");
    privilege.setName("dup");

    manager.createPrivilege(privilege);
  }

  @Test(expected = ReadonlyPrivilegeException.class)
  public void testUpdatePrivilege_readOnly() {
    addSimplePrivilegeContributor("readonly");

    CPrivilege forUpdate = new CPrivilege();
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
    CRole role = new CRole();
    role.setId("dup");
    role.setName("dup");

    doThrow(new DuplicateRoleException("dup")).when(memorySecurityConfiguration).addRole(role);

    manager.createRole(role);
  }

  @Test(expected = DuplicateRoleException.class)
  public void testCreateRole_duplicateFromContributors() {
    addSimpleRoleContributor("dup");

    CRole role = new CRole();
    role.setId("dup");
    role.setName("dup");

    manager.createRole(role);
  }

  @Test(expected = NoSuchRoleException.class)
  public void testCreateRole_invalidRole() {
    CRole role = new CRole();
    role.setId("new");
    role.setName("new");
    role.addRole("role1");

    manager.createRole(role);
  }

  @Test(expected = NoSuchPrivilegeException.class)
  public void testCreateRole_invalidPrivilege() {
    CRole role = new CRole();
    role.setId("new");
    role.setName("new");
    role.addPrivilege("priv1");

    manager.createRole(role);
  }

  @Test
  public void testCreateRole() {
    when(memorySecurityConfiguration.getPrivilege("priv1")).thenReturn(mock(CPrivilege.class));
    when(memorySecurityConfiguration.getRole("role1")).thenReturn(mock(CRole.class));

    CRole role = new CRole();
    role.setId("new");
    role.setName("new");
    role.addRole("role1");
    role.addPrivilege("priv1");

    manager.createRole(role);
  }

  @Test(expected = ReadonlyRoleException.class)
  public void testUpdateRole_readOnly() {
    addSimpleRoleContributor("readonly");

    CRole forUpdate = new CRole();
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
    CRole role = new CRole();
    role.setId("new");
    role.setName("new");
    role.addRole("new");

    when(memorySecurityConfiguration.getRole("new")).thenReturn(role);

    manager.updateRole(role);
  }

  @Test(expected = RoleContainsItselfException.class)
  public void testUpdateRole_containsItselfIndirectly() {
    CRole role = new CRole();
    role.setId("new");
    role.setName("new");
    role.addRole("new2");

    CRole role2 = new CRole();
    role2.setId("new2");
    role2.setName("new2");
    role2.addRole("new");

    when(memorySecurityConfiguration.getRole("new")).thenReturn(role);
    when(memorySecurityConfiguration.getRole("new2")).thenReturn(role2);

    manager.updateRole(role);
  }

  private void addSimpleRoleContributor(final String roleName) {
    manager.addContributor(() -> {
      SecurityConfiguration config = new MemorySecurityConfiguration();
      CRole readonlyRole = new CRole();
      readonlyRole.setId(roleName);
      readonlyRole.setName(roleName);
      config.addRole(readonlyRole);
      return config;
    });
  }

  private void addSimplePrivilegeContributor(final String privName) {
    manager.addContributor(() -> {
      SecurityConfiguration config = new MemorySecurityConfiguration();
      CPrivilege readonlyPriv = new CPrivilege();
      readonlyPriv.setId(privName);
      readonlyPriv.setName(privName);
      config.addPrivilege(readonlyPriv);
      return config;
    });
  }
}
