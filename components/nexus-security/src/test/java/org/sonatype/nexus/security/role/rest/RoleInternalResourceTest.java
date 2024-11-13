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
package org.sonatype.nexus.security.role.rest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import javax.ws.rs.core.Response.Status;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.rest.WebApplicationMessageException;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.authz.NoSuchAuthorizationManagerException;
import org.sonatype.nexus.security.role.Role;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RoleInternalResourceTest
    extends TestSupport
{
  @Mock
  private SecuritySystem securitySystem;

  private RoleInternalResource underTest;

  @Before
  public void setup() {
    underTest = new RoleInternalResource(securitySystem);
  }

  @Test
  public void testListRolesOnEmptySource() {
    Role r1 = createRole("role1", Arrays.asList("roleA", "roleB"), Arrays.asList("read", "write"));
    Role r2 = createRole("role2", Arrays.asList("roleC", "roleD"), Arrays.asList("read", "delete"));
    when(securitySystem.listRoles()).thenReturn(new HashSet<>(Arrays.asList(r1, r2)));

    List<RoleXOResponse> res = underTest.searchRoles("", "searchParam");

    verify(securitySystem).listRoles();
    assertEquals(2, res.size());
    assertEquals("role1", res.get(0).getName());
    assertEquals("role2", res.get(1).getName());
  }

  @Test
  public void testSearchRoles() throws NoSuchAuthorizationManagerException {
    Role r1 = createRole("customRole1", Arrays.asList("roleA", "roleB"), Arrays.asList("read", "write"));
    Role r2 = createRole("customRole2", Arrays.asList("roleC", "roleD"), Arrays.asList("read", "delete"));
    Role r3 = createRole("customRole3", Arrays.asList("roleX", "roleY"), Arrays.asList("read", "delete"));
    when(securitySystem.searchRoles(anyString(), anyString())).thenReturn(new HashSet<>(Arrays.asList(r1, r2, r3)));

    List<RoleXOResponse> res = underTest.searchRoles("customSource", "searchParam");

    verify(securitySystem, never()).listRoles();
    verify(securitySystem).searchRoles("customSource", "searchParam");
    assertEquals(3, res.size());
    assertEquals("customRole1", res.get(0).getName());
    assertEquals("customRole2", res.get(1).getName());
    assertEquals("customRole3", res.get(2).getName());
  }

  @Test
  public void testExceptionThrown() throws NoSuchAuthorizationManagerException {
    when(securitySystem.searchRoles(anyString(), anyString())).thenThrow(
        new NoSuchAuthorizationManagerException("bad source"));
    WebApplicationMessageException ex =
        assertThrows(WebApplicationMessageException.class, () -> underTest.searchRoles("authSource", "searchParam"));
    assertEquals(Status.NOT_FOUND.getStatusCode(), ex.getResponse().getStatus());
  }

  private Role createRole(String roleName, List<String> roles, List<String> privileges) {
    return new Role(roleName + "Id", roleName, roleName, "internal", true,
        new HashSet<>(roles), new HashSet<>(privileges));
  }
}
