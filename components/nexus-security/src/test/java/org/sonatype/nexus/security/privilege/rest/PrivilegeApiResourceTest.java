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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.rest.WebApplicationMessageException;
import org.sonatype.nexus.security.ErrorMessageUtil;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.authz.AuthorizationManager;
import org.sonatype.nexus.security.privilege.ApplicationPrivilegeDescriptor;
import org.sonatype.nexus.security.privilege.DuplicatePrivilegeException;
import org.sonatype.nexus.security.privilege.NoSuchPrivilegeException;
import org.sonatype.nexus.security.privilege.Privilege;
import org.sonatype.nexus.security.privilege.PrivilegeDescriptor;
import org.sonatype.nexus.security.privilege.ReadonlyPrivilegeException;
import org.sonatype.nexus.security.privilege.WildcardPrivilegeDescriptor;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.security.privilege.rest.ApiPrivilegeApplication.DOMAIN_KEY;
import static org.sonatype.nexus.security.privilege.rest.ApiPrivilegeWildcard.PATTERN_KEY;
import static org.sonatype.nexus.security.privilege.rest.ApiPrivilegeWithActions.ACTIONS_KEY;

public class PrivilegeApiResourceTest
    extends TestSupport
{
  @Mock
  private SecuritySystem securitySystem;

  @Mock
  private AuthorizationManager authorizationManager;

  private PrivilegeApiResource underTest;

  @Before
  public void setup() throws Exception {
    when(securitySystem.getAuthorizationManager("default")).thenReturn(authorizationManager);

    Map<String, PrivilegeDescriptor> privilegeDescriptors = new HashMap<>();
    privilegeDescriptors.put(ApplicationPrivilegeDescriptor.TYPE, new ApplicationPrivilegeDescriptor());
    privilegeDescriptors.put(WildcardPrivilegeDescriptor.TYPE, new WildcardPrivilegeDescriptor());

    underTest = new PrivilegeApiResource(securitySystem, privilegeDescriptors);
  }

  @Test
  public void testGetPrivileges_eachPrivilegeType_HappyPath() {
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
  public void testGetPrivileges_noPrivileges() {
    when(securitySystem.listPrivileges()).thenReturn(Collections.emptySet());

    List<ApiPrivilege> apiPrivileges = new ArrayList<>(underTest.getPrivileges());

    assertThat(apiPrivileges.size(), is(0));
  }

  @Test
  public void testGetPrivilege_application() {
    Privilege priv = createPrivilege("application", "priv", "privdesc", true, DOMAIN_KEY, "testDomain", ACTIONS_KEY,
        "create,update");

    when(authorizationManager.getPrivilegeByName("priv")).thenReturn(priv);

    ApiPrivilege apiPrivilege = underTest.getPrivilege("priv");

    assertApiPrivilegeApplication(apiPrivilege, "priv", "privdesc", true, "testDomain", PrivilegeAction.ADD,
        PrivilegeAction.EDIT);
  }

  @Test
  public void testGetPrivilege_wildcard() {
    Privilege priv = createPrivilege("wildcard", "priv", "privdesc", false, PATTERN_KEY, "a:pattern");

    when(authorizationManager.getPrivilegeByName("priv")).thenReturn(priv);

    ApiPrivilege apiPrivilege = underTest.getPrivilege("priv");

    assertApiPrivilegeWildcard(apiPrivilege, "priv", "privdesc", false, "a:pattern");
  }

  @Test
  public void testGetPrivilege_notFound() {
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
  public void testDeletePrivilege() {
    Privilege priv = createPrivilege("wildcard", "priv", "privdesc", false, PATTERN_KEY, "a:pattern");
    when(authorizationManager.getPrivilegeByName("priv")).thenReturn(priv);

    underTest.deletePrivilege("priv");

    verify(authorizationManager).deletePrivilegeByName("priv");
  }

  @Test
  public void testDeletePrivilege_readOnly() {
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
  public void testDeletePrivilege_notFound() {
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
  public void testCreatePrivilege_application() {
    when(authorizationManager.getPrivilege("name")).thenThrow(new NoSuchPrivilegeException("name"));

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
  public void testCreatePrivilege_applicationWithAllAction() {
    when(authorizationManager.getPrivilege("name")).thenThrow(new NoSuchPrivilegeException("name"));

    ApiPrivilegeApplicationRequest apiPrivilege = new ApiPrivilegeApplicationRequest("name", "description", "domain",
        Collections.singleton(PrivilegeAction.ALL));

    underTest.createPrivilege(apiPrivilege);

    ArgumentCaptor<Privilege> argument = ArgumentCaptor.forClass(Privilege.class);
    verify(authorizationManager).addPrivilege(argument.capture());
    assertPrivilege(argument.getValue(), "name", "description", DOMAIN_KEY, "domain", ACTIONS_KEY, "*");
  }

  @Test
  public void testCreatePrivilege_wildcard() {
    when(authorizationManager.getPrivilege("name")).thenThrow(new NoSuchPrivilegeException("name"));

    ApiPrivilegeWildcardRequest apiPrivilege = new ApiPrivilegeWildcardRequest("name", "description", "pattern");

    underTest.createPrivilege(apiPrivilege);

    ArgumentCaptor<Privilege> argument = ArgumentCaptor.forClass(Privilege.class);
    verify(authorizationManager).addPrivilege(argument.capture());
    assertPrivilege(argument.getValue(), "name", "description", PATTERN_KEY, "pattern");
  }

  @Test
  public void testCreatePrivilege_alreadyExists() {
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
  public void testCreatePrivilege_runActionWithNonScriptPrivilege() {
    when(authorizationManager.getPrivilege("name")).thenThrow(new NoSuchPrivilegeException("name"));

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
  public void testUpdatePrivilege_application() {
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
  public void testUpdatePrivilege_wildcard() {
    Privilege priv = createPrivilege("wildcard", "priv", "privdesc", false, PATTERN_KEY, "a:pattern");
    when(authorizationManager.getPrivilegeByName("priv")).thenReturn(priv);

    ApiPrivilegeWildcardRequest apiPrivilege = new ApiPrivilegeWildcardRequest("priv", "newdescription", "a:new:pattern");

    underTest.updatePrivilege("priv", apiPrivilege);

    ArgumentCaptor<Privilege> argument = ArgumentCaptor.forClass(Privilege.class);
    verify(authorizationManager).updatePrivilegeByName(argument.capture());
    assertPrivilege(argument.getValue(), "priv", "newdescription", PATTERN_KEY, "a:new:pattern");
  }

  @Test
  public void testUpdatePrivilege_notFound() {
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
  public void testUpdatePrivilege_readOnly() {
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
  public void testUpdatePrivilege_nameConflict() {
    Privilege priv = createPrivilege("wildcard", "priv", "privdesc", true, PATTERN_KEY, "a:pattern");
    when(authorizationManager.getPrivilege("priv")).thenReturn(priv);

    ApiPrivilegeWildcardRequest apiPrivilege = new ApiPrivilegeWildcardRequest("privnotmatching", "privdesc", "new_pattern");

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

  private void assertPrivilege(Privilege privilege,
                               String name,
                               String description,
                               String... properties)
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

  private void assertApiPrivilegeApplication(ApiPrivilege apiPrivilege,
                                             String name,
                                             String description,
                                             boolean readOnly,
                                             String domain,
                                             PrivilegeAction... actions)
  {
    assertApiPrivilege(apiPrivilege, ApplicationPrivilegeDescriptor.TYPE, name, description, readOnly);
    assertThat(((ApiPrivilegeApplication) apiPrivilege).getDomain(), is(domain));
    assertThat(((ApiPrivilegeApplication) apiPrivilege).getActions(), containsInAnyOrder(actions));
  }

  private void assertApiPrivilegeWildcard(ApiPrivilege apiPrivilege,
                                          String name,
                                          String description,
                                          boolean readOnly,
                                          String pattern)
  {
    assertApiPrivilege(apiPrivilege, WildcardPrivilegeDescriptor.TYPE, name, description, readOnly);
    assertThat(((ApiPrivilegeWildcard) apiPrivilege).getPattern(), is(pattern));
  }

  private void assertApiPrivilege(ApiPrivilege apiPrivilege,
                                  String type,
                                  String name,
                                  String description,
                                  boolean readOnly)
  {
    assertThat(apiPrivilege, notNullValue());
    assertThat(apiPrivilege.getType(), is(type));
    assertThat(apiPrivilege.getName(), is(name));
    assertThat(apiPrivilege.getDescription(), is(description));
    assertThat(apiPrivilege.isReadOnly(), is(readOnly));
  }

  private Privilege createPrivilege(String type,
                                    String name,
                                    String description,
                                    boolean readOnly,
                                    String... properties)
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
