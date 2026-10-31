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
package org.sonatype.nexus.security.privilege.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import jakarta.ws.rs.core.MediaType;

import org.sonatype.nexus.rest.WebApplicationMessageException;
import org.sonatype.nexus.security.ErrorMessageUtil;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.authz.AuthorizationManager;
import org.sonatype.nexus.security.privilege.ApplicationPrivilegeDescriptor;
import org.sonatype.nexus.security.privilege.DuplicatePrivilegeException;
import org.sonatype.nexus.security.privilege.NoSuchPrivilegeException;
import org.sonatype.nexus.security.privilege.Privilege;
import org.sonatype.nexus.security.privilege.PrivilegeDescriptor;
import org.sonatype.nexus.security.privilege.ReadonlyPrivilegeException;
import org.sonatype.nexus.security.privilege.WildcardPrivilegeDescriptor;
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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.security.privilege.rest.ApiPrivilegeApplication.DOMAIN_KEY;
import static org.sonatype.nexus.security.privilege.rest.ApiPrivilegeWildcard.PATTERN_KEY;
import static org.sonatype.nexus.security.privilege.rest.ApiPrivilegeWithActions.ACTIONS_KEY;

@ExtendWith(MockitoExtension.class)
@ExtendWith(ValidationExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class PrivilegeApiResourceTest
{
  @Mock
  private SecuritySystem securitySystem;

  @Mock
  private SecurityHelper securityHelper;

  @Mock
  private AuthorizationManager authorizationManager;

  private PrivilegeApiResource underTest;

  @BeforeEach
  void setup() throws Exception {
    lenient().when(securitySystem.getAuthorizationManager("default")).thenReturn(authorizationManager);
    lenient().when(securityHelper.allPermitted(any(Permission.class))).thenReturn(true);

    List<PrivilegeDescriptor> privilegeDescriptors = new ArrayList<>();
    privilegeDescriptors.add(new ApplicationPrivilegeDescriptor(false));
    privilegeDescriptors.add(new WildcardPrivilegeDescriptor());

    underTest = new PrivilegeApiResource(securitySystem, securityHelper, privilegeDescriptors);
  }

  @Test
  void testGetPrivileges_eachPrivilegeType_HappyPath() {
    Privilege priv1 = createPrivilege("application", "priv1", "priv1desc", false, DOMAIN_KEY, "testDomain", ACTIONS_KEY,
        "create,read,update,delete");
    Privilege priv2 = createPrivilege("wildcard", "priv2", "priv2desc", true, PATTERN_KEY, "a:pattern");

    when(securitySystem.listPrivileges()).thenReturn(new LinkedHashSet<>(Arrays.asList(priv2, priv1)));

    List<ApiPrivilege> apiPrivileges = new ArrayList<>(underTest.getPrivileges());

    assertThat(apiPrivileges.size(), is(2));

    assertApiPrivilegeApplication(apiPrivileges.get(0), "priv1", "priv1desc", false, "testDomain", PrivilegeAction.ADD,
        PrivilegeAction.READ, PrivilegeAction.EDIT, PrivilegeAction.DELETE);
    assertApiPrivilegeWildcard(apiPrivileges.get(1), "priv2", "priv2desc", true, "a:pattern");
  }

  @Test
  void testGetPrivileges_noPrivileges() {
    when(securitySystem.listPrivileges()).thenReturn(Collections.emptySet());

    List<ApiPrivilege> apiPrivileges = new ArrayList<>(underTest.getPrivileges());

    assertThat(apiPrivileges.size(), is(0));
  }

  @Test
  void testGetPrivileges_skipsUnknownPrivilegeTypes() {
    Privilege priv1 = createPrivilege("application", "priv1", "priv1desc", false, DOMAIN_KEY, "testDomain", ACTIONS_KEY,
        "create,read");
    Privilege unknownPriv = createPrivilege("unknown-type", "unknownPriv", "unknown privilege type", false);
    Privilege priv2 = createPrivilege("wildcard", "priv2", "priv2desc", false, PATTERN_KEY, "a:pattern");

    when(securitySystem.listPrivileges()).thenReturn(new LinkedHashSet<>(Arrays.asList(priv1, unknownPriv, priv2)));

    List<ApiPrivilege> apiPrivileges = new ArrayList<>(underTest.getPrivileges());

    assertThat(apiPrivileges.size(), is(2));
    assertApiPrivilegeApplication(apiPrivileges.get(0), "priv1", "priv1desc", false, "testDomain", PrivilegeAction.ADD,
        PrivilegeAction.READ);
    assertApiPrivilegeWildcard(apiPrivileges.get(1), "priv2", "priv2desc", false, "a:pattern");
  }

  @Test
  void testGetPrivilege_application() {
    Privilege priv = createPrivilege("application", "priv", "privdesc", true, DOMAIN_KEY, "testDomain", ACTIONS_KEY,
        "create,update");

    when(authorizationManager.getPrivilegeByName("priv")).thenReturn(priv);

    ApiPrivilege apiPrivilege = underTest.getPrivilege("priv");

    assertApiPrivilegeApplication(apiPrivilege, "priv", "privdesc", true, "testDomain", PrivilegeAction.ADD,
        PrivilegeAction.EDIT);
  }

  @Test
  void testGetPrivilege_wildcard() {
    Privilege priv = createPrivilege("wildcard", "priv", "privdesc", false, PATTERN_KEY, "a:pattern");

    when(authorizationManager.getPrivilegeByName("priv")).thenReturn(priv);

    ApiPrivilege apiPrivilege = underTest.getPrivilege("priv");

    assertApiPrivilegeWildcard(apiPrivilege, "priv", "privdesc", false, "a:pattern");
  }

  @Test
  void testGetPrivilege_notFound() {
    when(authorizationManager.getPrivilegeByName("priv")).thenThrow(new NoSuchPrivilegeException("priv"));

    try {
      underTest.getPrivilege("priv");
      fail("exception should have been thrown for missing privilege");
    }
    catch (WebApplicationMessageException e) {
      assertThat(e.getResponse().getStatus(), is(404));
      assertThat(e.getResponse().getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));
      assertThat(e.getResponse().getEntity().toString(),
          is(ErrorMessageUtil.getFormattedMessage("\"Privilege 'priv' not found.\"")));
    }
  }

  @Test
  void testDeletePrivilege() {
    underTest.deletePrivilege("priv");

    verify(authorizationManager).deletePrivilegeByName("priv");
  }

  @Test
  void testDeletePrivilege_readOnly() {
    doThrow(new ReadonlyPrivilegeException("priv")).when(authorizationManager).deletePrivilegeByName("priv");

    try {
      underTest.deletePrivilege("priv");
      fail("exception should have been thrown for internal privilege");
    }
    catch (WebApplicationMessageException e) {
      assertThat(e.getResponse().getStatus(), is(400));
      assertThat(e.getResponse().getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));
      assertThat(e.getResponse().getEntity().toString(),
          is(ErrorMessageUtil.getFormattedMessage(
              "\"Privilege 'priv' is internal and cannot be modified or deleted.\"")));
    }
  }

  @Test
  void testDeletePrivilege_notFound() {
    doThrow(new NoSuchPrivilegeException("priv")).when(authorizationManager).deletePrivilegeByName("priv");

    try {
      underTest.deletePrivilege("priv");
      fail("exception should have been thrown for missing privilege");
    }
    catch (WebApplicationMessageException e) {
      assertThat(e.getResponse().getStatus(), is(404));
      assertThat(e.getResponse().getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));
      assertThat(e.getResponse().getEntity().toString(),
          is(ErrorMessageUtil.getFormattedMessage("\"Privilege 'priv' not found.\"")));
    }
  }

  @Test
  void testCreatePrivilege_application() {
    ApiPrivilegeApplicationRequest apiPrivilege = new ApiPrivilegeApplicationRequest("name", "description", "domain",
        Arrays.asList(PrivilegeAction.ADD, PrivilegeAction.EDIT, PrivilegeAction.DELETE, PrivilegeAction.READ,
            PrivilegeAction.ASSOCIATE, PrivilegeAction.DISASSOCIATE));

    underTest.createPrivilege(apiPrivilege);

    ArgumentCaptor<Privilege> argument = ArgumentCaptor.forClass(Privilege.class);
    verify(authorizationManager).addPrivilege(argument.capture());
    assertPrivilege(argument.getValue(), "name", "description", DOMAIN_KEY, "domain", ACTIONS_KEY,
        "create,update,delete,read,associate,disassociate");
  }

  @Test
  void testCreatePrivilege_applicationWithAllAction() {
    ApiPrivilegeApplicationRequest apiPrivilege = new ApiPrivilegeApplicationRequest("name", "description", "domain",
        Collections.singleton(PrivilegeAction.ALL));

    underTest.createPrivilege(apiPrivilege);

    ArgumentCaptor<Privilege> argument = ArgumentCaptor.forClass(Privilege.class);
    verify(authorizationManager).addPrivilege(argument.capture());
    assertPrivilege(argument.getValue(), "name", "description", DOMAIN_KEY, "domain", ACTIONS_KEY, "*");
  }

  @Test
  void testCreatePrivilege_wildcard() {
    ApiPrivilegeWildcardRequest apiPrivilege = new ApiPrivilegeWildcardRequest("name", "description", "pattern");

    underTest.createPrivilege(apiPrivilege);

    ArgumentCaptor<Privilege> argument = ArgumentCaptor.forClass(Privilege.class);
    verify(authorizationManager).addPrivilege(argument.capture());
    assertPrivilege(argument.getValue(), "name", "description", PATTERN_KEY, "pattern");
  }

  @Test
  void testCreatePrivilege_alreadyExists() {
    when(authorizationManager.addPrivilege(any())).thenThrow(new DuplicatePrivilegeException("name"));

    ApiPrivilegeWildcardRequest apiPrivilege = new ApiPrivilegeWildcardRequest("name", "description", "pattern");

    try {
      underTest.createPrivilege(apiPrivilege);
      fail("create should have failed as privilege exists.");
    }
    catch (WebApplicationMessageException e) {
      assertThat(e.getResponse().getStatus(), is(400));
      assertThat(e.getResponse().getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));
      assertThat(e.getResponse().getEntity().toString(),
          is(ErrorMessageUtil.getFormattedMessage("\"Privilege 'name' already exists, use a unique name.\"")));
    }
  }

  @Test
  void testCreatePrivilege_runActionWithNonScriptPrivilege() {
    ApiPrivilegeApplicationRequest apiPrivilege = new ApiPrivilegeApplicationRequest("name", "description", "domain",
        Arrays.asList(PrivilegeAction.ADD, PrivilegeAction.RUN));

    try {
      underTest.createPrivilege(apiPrivilege);
      fail("create should have failed as privilege is using invalid action.");
    }
    catch (WebApplicationMessageException e) {
      assertThat(e.getResponse().getStatus(), is(400));
      assertThat(e.getResponse().getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));
      assertThat(e.getResponse().getEntity().toString(),
          is(ErrorMessageUtil.getFormattedMessage(
              "\"Privilege of type 'application' cannot use action(s) of type 'RUN'.\"")));
    }
  }

  @Test
  void testCreatePrivilege_wildcard_blankPattern_rejected() {
    ApiPrivilegeWildcardRequest apiPrivilege = new ApiPrivilegeWildcardRequest("name", "description", "");

    try {
      underTest.createPrivilege(apiPrivilege);
      fail("create should have failed due to blank pattern");
    }
    catch (WebApplicationMessageException e) {
      assertThat(e.getResponse().getStatus(), is(400));
      assertThat(e.getResponse().getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));
      assertThat(e.getResponse().getEntity().toString(),
          is(ErrorMessageUtil.getFormattedMessage("\"Wildcard privilege pattern must not be blank.\"")));
    }
  }

  @Test
  void testUpdatePrivilege_application() {
    Privilege priv = createPrivilege("application", "priv", "privdesc", false, DOMAIN_KEY, "testDomain", ACTIONS_KEY,
        "*");
    when(authorizationManager.getPrivilegeByName("priv")).thenReturn(priv);

    ApiPrivilegeApplicationRequest apiPrivilege = new ApiPrivilegeApplicationRequest("priv", "newdescription",
        "newdomain", Arrays
            .asList(PrivilegeAction.ADD, PrivilegeAction.EDIT, PrivilegeAction.READ, PrivilegeAction.DELETE,
                PrivilegeAction.ASSOCIATE, PrivilegeAction.DISASSOCIATE));

    underTest.updatePrivilege("priv", apiPrivilege);

    ArgumentCaptor<Privilege> argument = ArgumentCaptor.forClass(Privilege.class);
    verify(authorizationManager).updatePrivilegeByName(argument.capture());
    assertPrivilege(argument.getValue(), "priv", "newdescription", DOMAIN_KEY, "newdomain", ACTIONS_KEY,
        "create,update,read,delete,associate,disassociate");
  }

  @Test
  void testUpdatePrivilege_wildcard() {
    Privilege priv = createPrivilege("wildcard", "priv", "privdesc", false, PATTERN_KEY, "a:pattern");
    when(authorizationManager.getPrivilegeByName("priv")).thenReturn(priv);

    ApiPrivilegeWildcardRequest apiPrivilege =
        new ApiPrivilegeWildcardRequest("priv", "newdescription", "a:new:pattern");

    underTest.updatePrivilege("priv", apiPrivilege);

    ArgumentCaptor<Privilege> argument = ArgumentCaptor.forClass(Privilege.class);
    verify(authorizationManager).updatePrivilegeByName(argument.capture());
    assertPrivilege(argument.getValue(), "priv", "newdescription", PATTERN_KEY, "a:new:pattern");
  }

  @Test
  void testUpdatePrivilege_notFound() {
    when(authorizationManager.getPrivilegeByName("priv")).thenThrow(new NoSuchPrivilegeException("priv"));

    ApiPrivilegeWildcardRequest apiPrivilege = new ApiPrivilegeWildcardRequest("priv", "description", "pattern");

    try {
      underTest.updatePrivilege("priv", apiPrivilege);
      fail("Should have failed update because of missing privilege.");
    }
    catch (WebApplicationMessageException e) {
      assertThat(e.getResponse().getStatus(), is(404));
      assertThat(e.getResponse().getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));
      assertThat(e.getResponse().getEntity().toString(),
          is(ErrorMessageUtil.getFormattedMessage("\"Privilege 'priv' not found.\"")));
    }
  }

  @Test
  void testUpdatePrivilege_readOnly() {
    Privilege priv = createPrivilege("wildcard", "priv", "privdesc", true, PATTERN_KEY, "a:pattern");
    when(authorizationManager.getPrivilegeByName("priv")).thenReturn(priv);

    when(authorizationManager.updatePrivilegeByName(priv)).thenThrow(new ReadonlyPrivilegeException("priv"));

    ApiPrivilegeWildcardRequest apiPrivilege = new ApiPrivilegeWildcardRequest("priv", "privdesc", "new_pattern");

    try {
      underTest.updatePrivilege("priv", apiPrivilege);
      fail("exception should have been thrown for internal privilege");
    }
    catch (WebApplicationMessageException e) {
      assertThat(e.getResponse().getStatus(), is(400));
      assertThat(e.getResponse().getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));
      assertThat(e.getResponse().getEntity().toString(),
          is(ErrorMessageUtil.getFormattedMessage(
              "\"Privilege 'priv' is internal and cannot be modified or deleted.\"")));
    }
  }

  @Test
  void testUpdatePrivilege_crossType_rejected() {
    Privilege priv =
        createPrivilege("application", "custom-search", "desc", false, DOMAIN_KEY, "search", ACTIONS_KEY, "read");
    when(authorizationManager.getPrivilegeByName("custom-search")).thenReturn(priv);

    ApiPrivilegeWildcardRequest apiPrivilege = new ApiPrivilegeWildcardRequest("custom-search", "desc", "nexus:*:*");

    try {
      underTest.updatePrivilege("custom-search", apiPrivilege);
      fail("cross-type update should have been rejected with 409");
    }
    catch (WebApplicationMessageException e) {
      assertThat(e.getResponse().getStatus(), is(409));
      assertThat(e.getResponse().getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));
      assertThat(e.getResponse().getEntity().toString(),
          is(ErrorMessageUtil.getFormattedMessage(
              "\"Privilege 'custom-search' is of type 'application' and cannot be updated via the 'wildcard' endpoint.\"")));
    }
    verify(authorizationManager, never()).updatePrivilegeByName(any());
  }

  @Test
  void testUpdatePrivilege_wildcardOnWildcard_succeeds() {
    Privilege priv = createPrivilege("wildcard", "my-wildcard", "desc", false, PATTERN_KEY, "old:pattern");
    when(authorizationManager.getPrivilegeByName("my-wildcard")).thenReturn(priv);

    ApiPrivilegeWildcardRequest apiPrivilege = new ApiPrivilegeWildcardRequest("my-wildcard", "desc", "new:pattern");

    underTest.updatePrivilege("my-wildcard", apiPrivilege);

    ArgumentCaptor<Privilege> argument = ArgumentCaptor.forClass(Privilege.class);
    verify(authorizationManager).updatePrivilegeByName(argument.capture());
    assertPrivilege(argument.getValue(), "my-wildcard", "desc", PATTERN_KEY, "new:pattern");
  }

  @Test
  void testUpdatePrivilege_nameConflict() {
    ApiPrivilegeWildcardRequest apiPrivilege =
        new ApiPrivilegeWildcardRequest("privnotmatching", "privdesc", "new_pattern");

    try {
      underTest.updatePrivilege("priv", apiPrivilege);
      fail("exception should have been thrown for internal privilege");
    }
    catch (WebApplicationMessageException e) {
      assertThat(e.getResponse().getStatus(), is(409));
      assertThat(e.getResponse().getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));
      assertThat(e.getResponse().getEntity().toString(),
          is(ErrorMessageUtil.getFormattedMessage("\"The privilege name 'privnotmatching'"
              + " does not match the name used in the path 'priv'.\"")));
    }
  }

  @Test
  void testUpdatePrivilege_wildcard_forbiddenWhenCallerLacksTheNewPattern() {
    when(authorizationManager.getPrivilegeByName("priv"))
        .thenReturn(createPrivilege("wildcard", "priv", "privdesc", false, PATTERN_KEY, "nexus:metrics:read"));
    when(securityHelper.allPermitted(any(Permission.class))).thenReturn(false);

    ApiPrivilegeWildcardRequest apiPrivilege = new ApiPrivilegeWildcardRequest("priv", "privdesc", "nexus:*");

    try {
      underTest.updatePrivilege("priv", apiPrivilege);
      fail("update should have been forbidden because caller cannot grant the new pattern");
    }
    catch (WebApplicationMessageException e) {
      assertThat(e.getResponse().getStatus(), is(403));
      assertThat(e.getResponse().getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));
      assertThat(e.getResponse().getEntity().toString(),
          is(ErrorMessageUtil.getFormattedMessage(
              "\"The current user is not permitted to grant the effective permission of privilege 'priv'.\"")));
    }
    verify(authorizationManager, never()).updatePrivilegeByName(any(Privilege.class));
  }

  @Test
  void testCreatePrivilege_wildcard_forbiddenWhenCallerLacksThePattern() {
    when(securityHelper.allPermitted(any(Permission.class))).thenReturn(false);

    ApiPrivilegeWildcardRequest apiPrivilege = new ApiPrivilegeWildcardRequest("priv", "privdesc", "nexus:*");

    try {
      underTest.createPrivilege(apiPrivilege);
      fail("create should have been forbidden because caller cannot grant the new pattern");
    }
    catch (WebApplicationMessageException e) {
      assertThat(e.getResponse().getStatus(), is(403));
      assertThat(e.getResponse().getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));
    }
    verify(authorizationManager, never()).addPrivilege(any(Privilege.class));
  }

  @Test
  void testUpdatePrivilege_wildcard_allowedWhenCallerAlreadyHoldsTheNewPattern() {
    Privilege priv = createPrivilege("wildcard", "priv", "privdesc", false, PATTERN_KEY, "nexus:metrics:read");
    when(authorizationManager.getPrivilegeByName("priv")).thenReturn(priv);
    when(securityHelper.allPermitted(any(Permission.class))).thenReturn(true);

    ApiPrivilegeWildcardRequest apiPrivilege = new ApiPrivilegeWildcardRequest("priv", "privdesc", "nexus:health:read");

    underTest.updatePrivilege("priv", apiPrivilege);

    ArgumentCaptor<Privilege> argument = ArgumentCaptor.forClass(Privilege.class);
    verify(authorizationManager).updatePrivilegeByName(argument.capture());
    assertPrivilege(argument.getValue(), "priv", "privdesc", PATTERN_KEY, "nexus:health:read");
  }

  @Test
  void testCreatePrivilege_application_forbiddenWhenCallerLacksTheNewPermission() {
    when(securityHelper.allPermitted(any(Permission.class))).thenReturn(false);

    ApiPrivilegeApplicationRequest apiPrivilege = new ApiPrivilegeApplicationRequest("name", "description", "*",
        Collections.singleton(PrivilegeAction.ALL));

    try {
      underTest.createPrivilege(apiPrivilege);
      fail("create should have been forbidden because caller cannot grant the new permission");
    }
    catch (WebApplicationMessageException e) {
      assertThat(e.getResponse().getStatus(), is(403));
    }
    verify(authorizationManager, never()).addPrivilege(any(Privilege.class));
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

  private static void assertApiPrivilegeApplication(
      final ApiPrivilege apiPrivilege,
      final String name,
      final String description,
      final boolean readOnly,
      final String domain,
      final PrivilegeAction... actions)
  {
    assertApiPrivilege(apiPrivilege, ApplicationPrivilegeDescriptor.TYPE, name, description, readOnly);
    assertThat(((ApiPrivilegeApplication) apiPrivilege).getDomain(), is(domain));
    assertThat(((ApiPrivilegeApplication) apiPrivilege).getActions(), containsInAnyOrder(actions));
  }

  private static void assertApiPrivilegeWildcard(
      final ApiPrivilege apiPrivilege,
      final String name,
      final String description,
      final boolean readOnly,
      final String pattern)
  {
    assertApiPrivilege(apiPrivilege, WildcardPrivilegeDescriptor.TYPE, name, description, readOnly);
    assertThat(((ApiPrivilegeWildcard) apiPrivilege).getPattern(), is(pattern));
  }

  private static void assertApiPrivilege(
      final ApiPrivilege apiPrivilege,
      final String type,
      final String name,
      final String description,
      final boolean readOnly)
  {
    assertThat(apiPrivilege, notNullValue());
    assertThat(apiPrivilege.getType(), is(type));
    assertThat(apiPrivilege.getName(), is(name));
    assertThat(apiPrivilege.getDescription(), is(description));
    assertThat(apiPrivilege.isReadOnly(), is(readOnly));
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
