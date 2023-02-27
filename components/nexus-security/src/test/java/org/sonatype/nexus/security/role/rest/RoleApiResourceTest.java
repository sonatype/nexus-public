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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.rest.WebApplicationMessageException;
import org.sonatype.nexus.security.ErrorMessageUtil;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.authz.AuthorizationManager;
import org.sonatype.nexus.security.authz.NoSuchAuthorizationManagerException;
import org.sonatype.nexus.security.role.DuplicateRoleException;
import org.sonatype.nexus.security.role.NoSuchRoleException;
import org.sonatype.nexus.security.role.ReadonlyRoleException;
import org.sonatype.nexus.security.role.Role;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RoleApiResourceTest
    extends TestSupport
{
  @Mock
  private SecuritySystem securitySystem;

  @Mock
  private AuthorizationManager authorizationManager;

  private RoleApiResource underTest;

  @Before
  public void setup() throws Exception {
    when(securitySystem.getAuthorizationManager("default")).thenReturn(authorizationManager);
    when(securitySystem.listSources()).thenReturn(Arrays.asList("default", "LDAP"));

    underTest = new RoleApiResource(securitySystem);
  }

  @Test
  public void testGetRoles() throws Exception {
    Role role1 = createRole("default", "id1", "role1", "role1", Arrays.asList("role1", "role2"),
        Arrays.asList("priv1", "priv2"));
    Role role2 = createRole("default", "id2", "role2", "role2", Arrays.asList("role2", "role3"),
        Arrays.asList("priv2", "priv3"));

    when(securitySystem.listRoles("default")).thenReturn(new LinkedHashSet<>(Arrays.asList(role2, role1)));

    List<RoleXOResponse> apiRoles = underTest.getRoles("default");

    assertThat(apiRoles.size(), is(2));

    assertApiRole(apiRoles.get(0), "default", "id1", "role1", "role1", Arrays.asList("role1", "role2"),
        Arrays.asList("priv1", "priv2"));
    assertApiRole(apiRoles.get(1), "default", "id2", "role2", "role2", Arrays.asList("role2", "role3"),
        Arrays.asList("priv2", "priv3"));
  }

  @Test
  public void testGetRoles_allSources() throws Exception {
    Role role1 = createRole("default", "id1", "role1", "role1", Arrays.asList("role1", "role2"),
        Arrays.asList("priv1", "priv2"));
    Role role2 = createRole("another", "id2", "role2", "role2", Arrays.asList("role2", "role3"),
        Arrays.asList("priv2", "priv3"));

    when(securitySystem.listRoles()).thenReturn(new LinkedHashSet<>(Arrays.asList(role2, role1)));

    List<RoleXOResponse> apiRoles = underTest.getRoles(null);

    assertThat(apiRoles.size(), is(2));

    assertApiRole(apiRoles.get(0), "default", "id1", "role1", "role1", Arrays.asList("role1", "role2"),
        Arrays.asList("priv1", "priv2"));
    assertApiRole(apiRoles.get(1), "another", "id2", "role2", "role2", Arrays.asList("role2", "role3"),
        Arrays.asList("priv2", "priv3"));
  }

  @Test
  public void testGetRoles_noRoles() throws Exception {
    when(securitySystem.listRoles("default")).thenReturn(new HashSet<>());

    List<RoleXOResponse> apiRoles = underTest.getRoles("default");

    assertThat(apiRoles.size(), is(0));
  }

  @Test
  public void testGetRole() {
    Role role = createRole("default", "id1", "role1", "role1", Arrays.asList("role1", "role2"),
        Arrays.asList("priv1", "priv2"));
    when(authorizationManager.getRole("roleId")).thenReturn(role);

    RoleXOResponse roleXo = underTest.getRole("default", "roleId");

    assertApiRole(roleXo, "default", "id1", "role1", "role1", Arrays.asList("role1", "role2"),
        Arrays.asList("priv1", "priv2"));
  }

  @Test
  public void testGetRole_notFound() {
    when(authorizationManager.getRole("roleId")).thenThrow(NoSuchRoleException.class);

    try {
      underTest.getRole("default", "roleId");
      fail("exception should have been thrown for missing role");
    }
    catch (WebApplicationMessageException e) {
      assertThat(e.getResponse().getStatus(), is(404));
      assertThat(e.getResponse().getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));
      assertThat(e.getResponse().getEntity().toString(), is(ErrorMessageUtil.getFormattedMessage(
          "\"Role 'roleId' not found.\"")));
    }
  }

  @Test
  public void testGetRole_sourceNotFound() throws Exception {
    when(securitySystem.getAuthorizationManager("bad")).thenThrow(NoSuchAuthorizationManagerException.class);

    try {
      underTest.getRole("bad", "roleId");
      fail("exception should have been thrown for missing source");
    }
    catch (WebApplicationMessageException e) {
      assertThat(e.getResponse().getStatus(), is(400));
      assertThat(e.getResponse().getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));
      assertThat(e.getResponse().getEntity().toString(), is(ErrorMessageUtil.getFormattedMessage(
          "\"Source 'bad' not found.\"")));
    }
  }

  @Test
  public void testCreateRole() throws Exception {
    RoleXORequest roleXo = createApiRole("roleId", "roleName", "description", Collections.singleton("childRole"),
        Collections.singleton("priv"));

    Role createdRole = new Role();
    createdRole.setRoleId("roleId");
    createdRole.setSource("default");
    createdRole.setName("roleName");
    createdRole.setDescription("description");
    createdRole.setReadOnly(false);
    createdRole.setRoles(Collections.singleton("childRole"));
    createdRole.setPrivileges(Collections.singleton("priv"));

    when(authorizationManager.addRole(any())).thenReturn(createdRole);

    RoleXOResponse result = underTest.create(roleXo);

    assertApiRole(result, "default", "roleId", "roleName", "description", Collections.singleton("childRole"),
        Collections.singleton("priv"));
  }

  @Test
  public void testCreateRole_alreadyExists() throws Exception {
    when(authorizationManager.addRole(any())).thenThrow(DuplicateRoleException.class);

    RoleXORequest roleXo = createApiRole("roleId", "roleName", "description", Collections.emptySet(),
        Collections.emptySet());

    try {
      underTest.create(roleXo);
      fail("create should have failed as role exists.");
    }
    catch (WebApplicationMessageException e) {
      assertThat(e.getResponse().getStatus(), is(400));
      assertThat(e.getResponse().getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));
      assertThat(e.getResponse().getEntity().toString(), is(ErrorMessageUtil.getFormattedMessage(
          "\"Role 'roleId' already exists, use a unique roleId.\"")));
    }
  }

  @Test
  public void testDelete() throws Exception {
    underTest.delete("roleId");

    verify(authorizationManager).deleteRole("roleId");
  }

  @Test
  public void testDelete_notFound() throws Exception {
    doThrow(NoSuchRoleException.class).when(authorizationManager).deleteRole("roleId");

    try {
      underTest.delete("roleId");
      fail("exception should have been thrown for missing role");
    }
    catch (WebApplicationMessageException e) {
      assertThat(e.getResponse().getStatus(), is(404));
      assertThat(e.getResponse().getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));
      assertThat(e.getResponse().getEntity().toString(), is(ErrorMessageUtil.getFormattedMessage(
          "\"Role 'roleId' not found.\"")));
    }
  }

  @Test
  public void testDeleteRole_readOnly() {
    doThrow(ReadonlyRoleException.class).when(authorizationManager).deleteRole("roleId");

    try {
      underTest.delete("roleId");
      fail("exception should have been thrown for internal role");
    }
    catch (WebApplicationMessageException e) {
      assertThat(e.getResponse().getStatus(), is(400));
      assertThat(e.getResponse().getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));
      assertThat(e.getResponse().getEntity().toString(),
          is(ErrorMessageUtil.getFormattedMessage(
              "\"Role 'roleId' is internal and cannot be modified or deleted.\"")));
    }
  }

  @Test
  public void testUpdateRole() {
    Role role = createRole("default", "id1", "role1", "role1", Arrays.asList("role1", "role2"),
        Arrays.asList("priv1", "priv2"));
    when(authorizationManager.getRole("id1")).thenReturn(role);

    RoleXORequest roleXo = createApiRole("id1", "role2", "role2", Arrays.asList("role3", "role4"),
        Arrays.asList("priv3", "priv4"));

    underTest.update("id1", roleXo);

    ArgumentCaptor<Role> argument = ArgumentCaptor.forClass(Role.class);
    verify(authorizationManager).updateRole(argument.capture());
    assertRole(argument.getValue(), "default", "id1", "role2", "role2", Arrays.asList("role3", "role4"),
        Arrays.asList("priv3", "priv4"));
  }

  @Test
  public void testUpdateRole_notFound() {
    when(authorizationManager.getRole(any())).thenThrow(new NoSuchRoleException("id1"));
    RoleXORequest roleXo = createApiRole("id1", "role1", "role1", Arrays.asList("role1", "role2"),
        Arrays.asList("priv1", "priv2"));

    try {
      underTest.update("id1", roleXo);
      fail("Should have failed update because of missing role.");
    }
    catch (WebApplicationMessageException e) {
      assertThat(e.getResponse().getStatus(), is(404));
      assertThat(e.getResponse().getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));
      assertThat(e.getResponse().getEntity().toString(), is(ErrorMessageUtil.getFormattedMessage(
          "\"Role 'id1' not found.\"")));
    }
  }

  @Test
  public void testUpdateRole_readOnly() {
    Role role = createRole("default", "id", "name", "description", Collections.singleton("role1"),
        Collections.singleton("priv1"));

    when(authorizationManager.getRole("id")).thenReturn(role);
    when(authorizationManager.updateRole(role)).thenThrow(ReadonlyRoleException.class);

    RoleXORequest roleXo = createApiRole("id", "name", "description", Collections.singleton("role1"),
        Collections.singleton("priv1"));

    try {
      underTest.update("id", roleXo);
      fail("exception should have been thrown for internal role");
    }
    catch (WebApplicationMessageException e) {
      assertThat(e.getResponse().getStatus(), is(400));
      assertThat(e.getResponse().getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));
      assertThat(e.getResponse().getEntity().toString(),
          is(ErrorMessageUtil.getFormattedMessage(
              "\"Role 'id' is internal and cannot be modified or deleted.\"")));
    }
  }

  @Test
  public void testUpdateRole_nameConflict() {
    RoleXORequest roleXo = createApiRole("id", "name", "description", Collections.singleton("role1"),
        Collections.singleton("priv1"));

    try {
      underTest.update("bad", roleXo);
      fail("exception should have been thrown for mismatched ids");
    }
    catch (WebApplicationMessageException e) {
      assertThat(e.getResponse().getStatus(), is(409));
      assertThat(e.getResponse().getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));
      assertThat(e.getResponse().getEntity().toString(),
          is(ErrorMessageUtil.getFormattedMessage(
              "\"The Role id 'id' does not match the id used in the path 'bad'.\"")));
    }
  }

  private Role createRole(final String source,
                          final String id,
                          final String name,
                          final String description,
                          final Collection<String> roles,
                          final Collection<String> privileges)
  {
    Role role = new Role();
    role.setRoleId(id);
    role.setName(name);
    role.setDescription(description);
    role.setSource(source);
    roles.forEach(role::addRole);
    privileges.forEach(role::addPrivilege);

    return role;
  }

  private RoleXORequest createApiRole(final String id,
                                      final String name,
                                      final String description,
                                      final Collection<String> roles,
                                      final Collection<String> privileges)
  {
    RoleXORequest roleXo = new RoleXORequest();
    roleXo.setId(id);
    roleXo.setName(name);
    roleXo.setDescription(description);
    roleXo.setRoles(new HashSet<>(roles));
    roleXo.setPrivileges(new HashSet<>(privileges));

    return roleXo;
  }

  private void assertRole(final Role role,
                          final String source,
                          final String id,
                          final String name,
                          final String description,
                          final Collection<String> roles,
                          final Collection<String> privileges)
  {
    assertThat(role.getSource(), is(source));
    assertThat(role.getRoleId(), is(id));
    assertThat(role.getName(), is(name));
    assertThat(role.getDescription(), is(description));
    if (roles.isEmpty()) {
      assertThat(role.getRoles(), empty());
    }
    else {
      assertThat(role.getRoles(), containsInAnyOrder(roles.toArray(new String[] {})));
    }
    if (privileges.isEmpty()) {
      assertThat(role.getPrivileges(), empty());
    }
    else {
      assertThat(role.getPrivileges(), containsInAnyOrder(privileges.toArray(new String[] {})));
    }
  }

  private void assertApiRole(final RoleXOResponse roleXo,
                             final String source,
                             final String id,
                             final String name,
                             final String description,
                             final Collection<String> roles,
                             final Collection<String> privileges)
  {
    assertThat(roleXo.getSource(), is(source));
    assertThat(roleXo.getId(), is(id));
    assertThat(roleXo.getName(), is(name));
    assertThat(roleXo.getDescription(), is(description));
    if (roles.isEmpty()) {
      assertThat(roleXo.getRoles(), empty());
    }
    else {
      assertThat(roleXo.getRoles(), containsInAnyOrder(roles.toArray(new String[] {})));
    }
    if (privileges.isEmpty()) {
      assertThat(roleXo.getPrivileges(), empty());
    }
    else {
      assertThat(roleXo.getPrivileges(), containsInAnyOrder(privileges.toArray(new String[] {})));
    }
  }
}
