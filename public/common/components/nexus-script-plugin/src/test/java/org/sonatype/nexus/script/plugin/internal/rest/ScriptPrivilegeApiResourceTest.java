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
package org.sonatype.nexus.script.plugin.internal.rest;

import java.util.LinkedList;
import java.util.List;

import jakarta.ws.rs.core.MediaType;

import org.sonatype.nexus.rest.WebApplicationMessageException;
import org.sonatype.nexus.script.Script;
import org.sonatype.nexus.script.ScriptManager;
import org.sonatype.nexus.script.plugin.internal.security.ScriptPrivilegeDescriptor;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.authz.AuthorizationManager;
import org.sonatype.nexus.security.privilege.ApplicationPrivilegeDescriptor;
import org.sonatype.nexus.security.privilege.Privilege;
import org.sonatype.nexus.security.privilege.PrivilegeDescriptor;
import org.sonatype.nexus.security.privilege.WildcardPrivilegeDescriptor;
import org.sonatype.nexus.security.privilege.rest.PrivilegeAction;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;

import org.apache.shiro.authz.Permission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.script.plugin.internal.rest.ApiPrivilegeScript.SCRIPT_KEY;
import static org.sonatype.nexus.security.privilege.rest.ApiPrivilegeWithActions.ACTIONS_KEY;

@ExtendWith(MockitoExtension.class)
@ExtendWith(ValidationExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class ScriptPrivilegeApiResourceTest
{
  @Mock
  private SecuritySystem securitySystem;

  @Mock
  private SecurityHelper securityHelper;

  @Mock
  private AuthorizationManager authorizationManager;

  @Mock
  private ScriptManager scriptManager;

  private ScriptPrivilegeApiResource underTest;

  @BeforeEach
  void setup() throws Exception {
    lenient().when(securitySystem.getAuthorizationManager("default")).thenReturn(authorizationManager);
    lenient().when(securityHelper.allPermitted(any(Permission.class))).thenReturn(true);
    when(scriptManager.get(any())).thenReturn(mock(Script.class));
    lenient().when(scriptManager.get("invalid")).thenReturn(null);

    List<PrivilegeDescriptor> privilegeDescriptors = new LinkedList<>();
    privilegeDescriptors.add(new ApplicationPrivilegeDescriptor(false));
    privilegeDescriptors.add(new WildcardPrivilegeDescriptor());
    privilegeDescriptors.add(new ScriptPrivilegeDescriptor(scriptManager, false));
    underTest = new ScriptPrivilegeApiResource(securitySystem, securityHelper, privilegeDescriptors);
  }

  @Test
  void testCreatePrivilege_script() {
    ApiPrivilegeScriptRequest apiPrivilege = new ApiPrivilegeScriptRequest("name", "description", "scriptName",
        List.of(PrivilegeAction.BROWSE, PrivilegeAction.READ, PrivilegeAction.DELETE, PrivilegeAction.EDIT,
            PrivilegeAction.ADD, PrivilegeAction.RUN));

    underTest.createPrivilege(apiPrivilege);

    ArgumentCaptor<Privilege> argument = ArgumentCaptor.forClass(Privilege.class);
    verify(authorizationManager).addPrivilege(argument.capture());
    assertPrivilege(argument.getValue(), "name", "description", SCRIPT_KEY, "scriptName", ACTIONS_KEY,
        "browse,read,delete,edit,add,run");
  }

  @Test
  void testCreatePrivilege_scriptWithAllAction() {
    ApiPrivilegeScriptRequest apiPrivilege = new ApiPrivilegeScriptRequest("name", "description", "scriptName",
        List.of(PrivilegeAction.ALL));

    underTest.createPrivilege(apiPrivilege);

    ArgumentCaptor<Privilege> argument = ArgumentCaptor.forClass(Privilege.class);
    verify(authorizationManager).addPrivilege(argument.capture());
    assertPrivilege(argument.getValue(), "name", "description", SCRIPT_KEY, "scriptName", ACTIONS_KEY, "*");
  }

  @Test
  void testCreatePrivilege_invalidScript() {
    ApiPrivilegeScriptRequest apiPrivilege = new ApiPrivilegeScriptRequest("name", "description", "invalid",
        List.of(PrivilegeAction.ALL));

    WebApplicationMessageException e =
        assertThrows(WebApplicationMessageException.class, () -> underTest.createPrivilege(apiPrivilege));
    assertThat(e.getResponse().getStatus(), is(400));
    assertThat(e.getResponse().getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));
    assertThat(e.getResponse().getEntity().toString(),
        is("ValidationErrorXO{id='*', message='\"Invalid script 'invalid' supplied.\"'}"));
  }

  @Test
  void testUpdatePrivilege_crossType_rejected() {
    Privilege priv = createPrivilege("wildcard", "priv", "privdesc", false);
    when(authorizationManager.getPrivilegeByName("priv")).thenReturn(priv);

    ApiPrivilegeScriptRequest apiPrivilege = new ApiPrivilegeScriptRequest("priv", "newdescription", "scriptName",
        List.of(PrivilegeAction.RUN));

    try {
      underTest.updatePrivilege("priv", apiPrivilege);
      fail("cross-type update should have been rejected with 409");
    }
    catch (WebApplicationMessageException e) {
      assertThat(e.getResponse().getStatus(), is(409));
      assertThat(e.getResponse().getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));
      assertThat(e.getResponse().getEntity().toString(),
          is("ValidationErrorXO{id='*', message='\"Privilege 'priv' is of type 'wildcard'"
              + " and cannot be updated via the 'script' endpoint.\"'}"));
    }
    verify(authorizationManager, never()).updatePrivilegeByName(any());
  }

  @Test
  void testUpdatePrivilege_script() {
    Privilege priv = createPrivilege("script", "priv", "privdesc", false, SCRIPT_KEY, "scriptName", ACTIONS_KEY,
        "read,run");
    when(authorizationManager.getPrivilegeByName("priv")).thenReturn(priv);

    ApiPrivilegeScriptRequest apiPrivilege = new ApiPrivilegeScriptRequest("priv", "newdescription", "newScriptName",
        List.of(PrivilegeAction.RUN));

    underTest.updatePrivilege("priv", apiPrivilege);

    ArgumentCaptor<Privilege> argument = ArgumentCaptor.forClass(Privilege.class);
    verify(authorizationManager).updatePrivilegeByName(argument.capture());
    assertPrivilege(argument.getValue(), "priv", "newdescription", SCRIPT_KEY, "newScriptName", ACTIONS_KEY,
        "run");
  }

  private static void assertPrivilege(
      final Privilege privilege,
      final String name,
      final String description,
      final String... properties)
  {
    assertThat(privilege, notNullValue());
    assertThat(privilege.getName(), is(name));
    assertThat(privilege.getId(), is(name));
    assertThat(privilege.getDescription(), is(description));
    assertThat(privilege.isReadOnly(), is(false));

    for (int i = 0; i < properties.length; i += 2) {
      assertThat(privilege.getPrivilegeProperty(properties[i]), is(properties[i + 1]));
    }
  }

  private static Privilege createPrivilege(
      final String type,
      final String name,
      final String description,
      final boolean readOnly,
      final String... properties)
  {
    Privilege privilege = new Privilege();
    privilege.setType(type);
    privilege.setId(name);
    privilege.setName(name);
    privilege.setDescription(description);
    privilege.setReadOnly(readOnly);

    for (int i = 0; i < properties.length; i += 2) {
      privilege.addProperty(properties[i], properties[i + 1]);
    }

    return privilege;
  }
}
