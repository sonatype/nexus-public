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
package org.sonatype.nexus.repository.content.tasks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sonatype.nexus.security.config.CPrivilege;
import org.sonatype.nexus.security.config.CRole;
import org.sonatype.nexus.security.config.SecurityConfiguration;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class PrivilegeNxRM2UpgradeTaskTest
{
  @Mock
  private SecurityConfiguration securityConfiguration;

  private PrivilegeNxRM2UpgradeTask underTest;

  @Before
  public void setup() {
    underTest = new PrivilegeNxRM2UpgradeTask(securityConfiguration);
  }

  @Test
  public void testExecute_upgradesNXRM2PrivilegesAndRoles() throws Exception {
    CPrivilege nxrm2Privilege1 = createPrivilege("old-id-1", "Repository View");
    CPrivilege nxrm2Privilege2 = createPrivilege("old-id-2", "User Management");
    CPrivilege nxrm3Privilege = createPrivilege("nxrm3-privilege", "nxrm3-privilege");

    List<CPrivilege> allPrivileges = List.of(nxrm2Privilege1, nxrm2Privilege2, nxrm3Privilege);
    when(securityConfiguration.getPrivileges()).thenReturn(allPrivileges);

    when(securityConfiguration.getPrivilege("Repository_View")).thenReturn(null);
    when(securityConfiguration.getPrivilege("User_Management")).thenReturn(null);

    CPrivilege newPrivilege1 = mock(CPrivilege.class);
    CPrivilege newPrivilege2 = mock(CPrivilege.class);
    when(securityConfiguration.newPrivilege()).thenReturn(newPrivilege1, newPrivilege2);

    CRole role1 = createRole("role-1", Set.of("old-id-1"));
    CRole role2 = createRole("role-2", Set.of("old-id-1", "old-id-2"));
    CRole role3 = createRole("role-3", Set.of("nxrm3-privilege"));

    List<CRole> allRoles = List.of(role1, role2, role3);
    when(securityConfiguration.getRoles()).thenReturn(allRoles);

    underTest.execute();

    verify(securityConfiguration).addPrivilege(newPrivilege1);
    verify(securityConfiguration).addPrivilege(newPrivilege2);
    verify(securityConfiguration).updateRole(role1);
    verify(securityConfiguration, times(2)).updateRole(role2);
    verify(securityConfiguration).removePrivilege("old-id-1");
    verify(securityConfiguration).removePrivilege("old-id-2");
  }

  @Test
  public void testExecute_upgradesNXRM2PrivilegeWithConflictingName() throws Exception {
    CPrivilege nxrm2Privilege = createPrivilege("old-id", "Repository View");
    CPrivilege existingPrivilege0 = createPrivilege("Repository_View", "Repository_View");
    CPrivilege existingPrivilege1 = createPrivilege("Repository_View_1", "Repository_View_1");
    List<CPrivilege> allPrivileges = List.of(nxrm2Privilege, existingPrivilege0, existingPrivilege1);
    when(securityConfiguration.getPrivileges()).thenReturn(allPrivileges);

    when(securityConfiguration.getPrivilege("Repository_View")).thenReturn(existingPrivilege0);
    when(securityConfiguration.getPrivilege("Repository_View_1")).thenReturn(existingPrivilege1);

    CPrivilege newPrivilege = mock(CPrivilege.class);
    when(securityConfiguration.newPrivilege()).thenReturn(newPrivilege);

    CRole role = createRole("role-1", Set.of("old-id"));
    when(securityConfiguration.getRoles()).thenReturn(List.of(role));

    underTest.execute();

    verify(securityConfiguration).addPrivilege(newPrivilege);
    verify(securityConfiguration).getPrivilege("Repository_View");
    verify(securityConfiguration).getPrivilege("Repository_View_1");
    verify(securityConfiguration).getPrivilege("Repository_View_2");
    verify(securityConfiguration).updateRole(role);
    verify(securityConfiguration).removePrivilege("old-id");
  }

  private static CPrivilege createPrivilege(final String id, final String name) {
    CPrivilege privilege = mock(CPrivilege.class);
    when(privilege.getId()).thenReturn(id);
    when(privilege.getName()).thenReturn(name);
    when(privilege.getDescription()).thenReturn("some description");
    when(privilege.getType()).thenReturn("someType");
    when(privilege.getProperties()).thenReturn(new HashMap<>());
    when(privilege.isReadOnly()).thenReturn(false);
    when(privilege.getVersion()).thenReturn(1);
    return privilege;
  }

  private static CRole createRole(final String id, final Set<String> privilegeIds) {
    CRole role = mock(CRole.class);
    when(role.getId()).thenReturn(id);
    Set<String> privileges = new HashSet<>(privilegeIds);
    when(role.getPrivileges()).thenReturn(privileges);
    return role;
  }
}
