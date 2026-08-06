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
package org.sonatype.nexus.repository.security.rest;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import jakarta.ws.rs.core.MediaType;

import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.security.RepositoryAdminPrivilegeDescriptor;
import org.sonatype.nexus.repository.security.RepositoryContentSelectorPrivilegeDescriptor;
import org.sonatype.nexus.repository.security.RepositoryViewPrivilegeDescriptor;
import org.sonatype.nexus.rest.WebApplicationMessageException;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.authz.AuthorizationManager;
import org.sonatype.nexus.security.privilege.ApplicationPrivilegeDescriptor;
import org.sonatype.nexus.security.privilege.Privilege;
import org.sonatype.nexus.security.privilege.PrivilegeDescriptor;
import org.sonatype.nexus.security.privilege.WildcardPrivilegeDescriptor;
import org.sonatype.nexus.security.privilege.rest.PrivilegeAction;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorManager;
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
import static org.sonatype.nexus.repository.security.rest.ApiPrivilegeRepositoryContentSelector.CSEL_KEY;
import static org.sonatype.nexus.repository.security.rest.ApiPrivilegeWithRepository.FORMAT_KEY;
import static org.sonatype.nexus.repository.security.rest.ApiPrivilegeWithRepository.REPOSITORY_KEY;
import static org.sonatype.nexus.security.privilege.rest.ApiPrivilegeWithActions.ACTIONS_KEY;

@ExtendWith(MockitoExtension.class)
@ExtendWith(ValidationExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class RepositoryPrivilegeApiResourceTest
{
  @Mock
  private SecuritySystem securitySystem;

  @Mock
  private SecurityHelper securityHelper;

  @Mock
  private AuthorizationManager authorizationManager;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private SelectorManager selectorManager;

  @Mock
  private Format format1;

  @Mock
  private Format format2;

  @Mock
  private Repository repository1;

  @Mock
  private Repository repository2;

  private RepositoryPrivilegeApiResource underTest;

  @BeforeEach
  void setup() throws Exception {
    lenient().when(securitySystem.getAuthorizationManager("default")).thenReturn(authorizationManager);
    lenient().when(securityHelper.allPermitted(any(Permission.class))).thenReturn(true);
    lenient().when(repository1.getFormat()).thenReturn(format1);
    lenient().when(repository2.getFormat()).thenReturn(format2);
    lenient().when(repositoryManager.get("repository1")).thenReturn(repository1);
    lenient().when(repositoryManager.get("repository2")).thenReturn(repository2);
    lenient().when(repositoryManager.get("invalid")).thenReturn(null);
    lenient().when(selectorManager.findByName(any())).thenReturn(Optional.of(mock(SelectorConfiguration.class)));
    lenient().when(selectorManager.findByName("invalid")).thenReturn(Optional.empty());
    lenient().when(format1.getValue()).thenReturn("format1");
    lenient().when(format2.getValue()).thenReturn("format2");

    List<Format> formats = Arrays.asList(format1, format2);
    List<PrivilegeDescriptor> privilegeDescriptors = List.of(new ApplicationPrivilegeDescriptor(false),
        new WildcardPrivilegeDescriptor(),
        new RepositoryAdminPrivilegeDescriptor(repositoryManager, formats, false),
        new RepositoryViewPrivilegeDescriptor(
            repositoryManager, formats, false),
        new RepositoryContentSelectorPrivilegeDescriptor(repositoryManager, selectorManager, formats, false));

    underTest = new RepositoryPrivilegeApiResource(securitySystem, securityHelper, privilegeDescriptors);
  }

  @Test
  void testCreatePrivilege_repositoryView() {
    ApiPrivilegeRepositoryViewRequest apiPrivilege =
        new ApiPrivilegeRepositoryViewRequest("name", "description", "format1",
            "repository1", Arrays
                .asList(PrivilegeAction.BROWSE, PrivilegeAction.READ, PrivilegeAction.DELETE, PrivilegeAction.EDIT,
                    PrivilegeAction.ADD));

    underTest.createPrivilege(apiPrivilege);

    ArgumentCaptor<Privilege> argument = ArgumentCaptor.forClass(Privilege.class);
    verify(authorizationManager).addPrivilege(argument.capture());
    assertPrivilege(argument.getValue(), "name", "description", FORMAT_KEY, "format1", REPOSITORY_KEY,
        "repository1", ACTIONS_KEY, "browse,read,delete,edit,add");
  }

  @Test
  void testCreatePrivilege_repositoryViewInvalidRepository() {
    ApiPrivilegeRepositoryViewRequest apiPrivilege = new ApiPrivilegeRepositoryViewRequest("name",
        "description", "format1", "invalid", Arrays
            .asList(PrivilegeAction.BROWSE, PrivilegeAction.READ, PrivilegeAction.DELETE, PrivilegeAction.EDIT,
                PrivilegeAction.ADD));

    try {
      underTest.createPrivilege(apiPrivilege);
      fail("create should have failed as repository is invalid.");
    }
    catch (WebApplicationMessageException e) {
      assertThat(e.getResponse().getStatus(), is(400));
      assertThat(e.getResponse().getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));
      assertThat(e.getResponse().getEntity().toString(),
          is("ValidationErrorXO{id='*', message='\"Invalid repository 'invalid' supplied.\"'}"));
    }
  }

  @Test
  void testCreatePrivilege_repositoryViewInvalidFormat() {
    ApiPrivilegeRepositoryViewRequest apiPrivilege = new ApiPrivilegeRepositoryViewRequest("name",
        "description", "invalid", "*", Arrays
            .asList(PrivilegeAction.BROWSE, PrivilegeAction.READ, PrivilegeAction.DELETE, PrivilegeAction.EDIT,
                PrivilegeAction.ADD));

    try {
      underTest.createPrivilege(apiPrivilege);
      fail("create should have failed as repository is invalid.");
    }
    catch (WebApplicationMessageException e) {
      assertThat(e.getResponse().getStatus(), is(400));
      assertThat(e.getResponse().getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));
      assertThat(e.getResponse().getEntity().toString(),
          is("ValidationErrorXO{id='*', message='\"Invalid format 'invalid' supplied.\"'}"));
    }
  }

  @Test
  void testCreatePrivilege_repositoryViewMismatchRepositoryAndFormat() {
    ApiPrivilegeRepositoryViewRequest apiPrivilege = new ApiPrivilegeRepositoryViewRequest("name",
        "description", "invalid", "repository1", Arrays
            .asList(PrivilegeAction.BROWSE, PrivilegeAction.READ, PrivilegeAction.DELETE, PrivilegeAction.EDIT,
                PrivilegeAction.ADD));

    try {
      underTest.createPrivilege(apiPrivilege);
      fail("create should have failed as repository is invalid.");
    }
    catch (WebApplicationMessageException e) {
      assertThat(e.getResponse().getStatus(), is(400));
      assertThat(e.getResponse().getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));
      assertThat(e.getResponse().getEntity().toString(),
          is("ValidationErrorXO{id='*', message='\"Invalid format 'invalid' supplied for repository 'repository1'.\"'}"));
    }
  }

  @Test
  void testCreatePrivilege_repositoryAdmin() {
    ApiPrivilegeRepositoryAdminRequest apiPrivilege =
        new ApiPrivilegeRepositoryAdminRequest("name", "description", "format1",
            "repository1", Arrays
                .asList(PrivilegeAction.BROWSE, PrivilegeAction.READ, PrivilegeAction.DELETE, PrivilegeAction.EDIT,
                    PrivilegeAction.ADD));

    underTest.createPrivilege(apiPrivilege);

    ArgumentCaptor<Privilege> argument = ArgumentCaptor.forClass(Privilege.class);
    verify(authorizationManager).addPrivilege(argument.capture());
    assertPrivilege(argument.getValue(), "name", "description", FORMAT_KEY, "format1", REPOSITORY_KEY,
        "repository1", ACTIONS_KEY, "browse,read,delete,edit,add");
  }

  @Test
  void testCreatePrivilege_repositoryAdminInvalidRepository() {
    ApiPrivilegeRepositoryAdminRequest apiPrivilege = new ApiPrivilegeRepositoryAdminRequest("name",
        "description", "format1", "invalid", Arrays
            .asList(PrivilegeAction.BROWSE, PrivilegeAction.READ, PrivilegeAction.DELETE, PrivilegeAction.EDIT,
                PrivilegeAction.ADD));

    try {
      underTest.createPrivilege(apiPrivilege);
      fail("create should have failed as repository is invalid.");
    }
    catch (WebApplicationMessageException e) {
      assertThat(e.getResponse().getStatus(), is(400));
      assertThat(e.getResponse().getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));
      assertThat(e.getResponse().getEntity().toString(),
          is("ValidationErrorXO{id='*', message='\"Invalid repository 'invalid' supplied.\"'}"));
    }
  }

  @Test
  void testCreatePrivilege_repositoryContentSelector() {
    ApiPrivilegeRepositoryContentSelectorRequest apiPrivilege = new ApiPrivilegeRepositoryContentSelectorRequest("name",
        "description", "format1", "repository1", "contentSelector", Arrays
            .asList(PrivilegeAction.BROWSE, PrivilegeAction.READ, PrivilegeAction.DELETE, PrivilegeAction.EDIT,
                PrivilegeAction.ADD));

    underTest.createPrivilege(apiPrivilege);

    ArgumentCaptor<Privilege> argument = ArgumentCaptor.forClass(Privilege.class);
    verify(authorizationManager).addPrivilege(argument.capture());
    assertPrivilege(argument.getValue(), "name", "description", FORMAT_KEY, "format1", REPOSITORY_KEY,
        "repository1", CSEL_KEY, "contentSelector", ACTIONS_KEY, "browse,read,delete,edit,add");
  }

  @Test
  void testCreatePrivilege_repositoryContentSelectorInvalidRepository() {
    ApiPrivilegeRepositoryContentSelectorRequest apiPrivilege = new ApiPrivilegeRepositoryContentSelectorRequest("name",
        "description", "format1", "invalid", "contentSelector", Arrays
            .asList(PrivilegeAction.BROWSE, PrivilegeAction.READ, PrivilegeAction.DELETE, PrivilegeAction.EDIT,
                PrivilegeAction.ADD));

    try {
      underTest.createPrivilege(apiPrivilege);
      fail("create should have failed as repository is invalid.");
    }
    catch (WebApplicationMessageException e) {
      assertThat(e.getResponse().getStatus(), is(400));
      assertThat(e.getResponse().getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));
      assertThat(e.getResponse().getEntity().toString(),
          is("ValidationErrorXO{id='*', message='\"Invalid repository 'invalid' supplied.\"'}"));
    }
  }

  @Test
  void testCreatePrivilege_repositoryContentSelectorInvalidContentSelector() {
    ApiPrivilegeRepositoryContentSelectorRequest apiPrivilege = new ApiPrivilegeRepositoryContentSelectorRequest("repo",
        "description", "format1", "repository1", "invalid", Arrays
            .asList(PrivilegeAction.BROWSE, PrivilegeAction.READ, PrivilegeAction.DELETE, PrivilegeAction.EDIT,
                PrivilegeAction.ADD));

    try {
      underTest.createPrivilege(apiPrivilege);
      fail("create should have failed as content selector is invalid.");
    }
    catch (WebApplicationMessageException e) {
      assertThat(e.getResponse().getStatus(), is(400));
      assertThat(e.getResponse().getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));
      assertThat(e.getResponse().getEntity().toString(),
          is("ValidationErrorXO{id='*', message='\"Invalid selector 'invalid' supplied.\"'}"));
    }
  }

  @Test
  void testUpdatePrivilege_crossType_rejected() {
    Privilege priv = createPrivilege("repository-view", "priv", "privdesc", false, FORMAT_KEY, "format1",
        REPOSITORY_KEY, "repository1", ACTIONS_KEY, "read");
    when(authorizationManager.getPrivilegeByName("priv")).thenReturn(priv);

    ApiPrivilegeRepositoryAdminRequest apiPrivilege = new ApiPrivilegeRepositoryAdminRequest("priv", "newdescription",
        "format1", "repository1", List.of(PrivilegeAction.READ));

    try {
      underTest.updatePrivilege("priv", apiPrivilege);
      fail("cross-type update should have been rejected with 409");
    }
    catch (WebApplicationMessageException e) {
      assertThat(e.getResponse().getStatus(), is(409));
      assertThat(e.getResponse().getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));
      assertThat(e.getResponse().getEntity().toString(),
          is("ValidationErrorXO{id='*', message='\"Privilege 'priv' is of type 'repository-view'"
              + " and cannot be updated via the 'repository-admin' endpoint.\"'}"));
    }
    verify(authorizationManager, never()).updatePrivilegeByName(any());
  }

  @Test
  void testUpdatePrivilege_repositoryView() {
    Privilege priv =
        createPrivilege("repository-view", "priv", "privdesc", false, FORMAT_KEY, "format1", REPOSITORY_KEY,
            "repository1", ACTIONS_KEY, "read,delete");
    when(authorizationManager.getPrivilegeByName("priv")).thenReturn(priv);

    ApiPrivilegeRepositoryViewRequest apiPrivilege = new ApiPrivilegeRepositoryViewRequest("priv", "newdescription",
        "format2", "repository2", List.of(PrivilegeAction.DELETE));

    underTest.updatePrivilege("priv", apiPrivilege);

    ArgumentCaptor<Privilege> argument = ArgumentCaptor.forClass(Privilege.class);
    verify(authorizationManager).updatePrivilegeByName(argument.capture());
    assertPrivilege(argument.getValue(), "priv", "newdescription", FORMAT_KEY, "format2", REPOSITORY_KEY,
        "repository2", ACTIONS_KEY, "delete");
  }

  @Test
  void testUpdatePrivilege_repositoryViewInvalidRepository() {
    Privilege priv =
        createPrivilege("repository-view", "priv", "privdesc", false, FORMAT_KEY, "format1", REPOSITORY_KEY,
            "repository1", ACTIONS_KEY, "read,delete");
    when(authorizationManager.getPrivilegeByName("priv")).thenReturn(priv);

    ApiPrivilegeRepositoryViewRequest apiPrivilege = new ApiPrivilegeRepositoryViewRequest("priv", "newdescription",
        "format2", "invalid", List.of(PrivilegeAction.DELETE));

    try {
      underTest.updatePrivilege("priv", apiPrivilege);
      fail("create should have failed as repository is invalid.");
    }
    catch (WebApplicationMessageException e) {
      assertThat(e.getResponse().getStatus(), is(400));
      assertThat(e.getResponse().getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));
      assertThat(e.getResponse().getEntity().toString(),
          is("ValidationErrorXO{id='*', message='\"Invalid repository 'invalid' supplied.\"'}"));
    }
  }

  @Test
  void testUpdatePrivilege_repositoryAdmin() {
    Privilege priv = createPrivilege("repository-admin", "priv", "privdesc", false, FORMAT_KEY, "format1",
        REPOSITORY_KEY, "repository1", ACTIONS_KEY, "read,delete");
    when(authorizationManager.getPrivilegeByName("priv")).thenReturn(priv);

    ApiPrivilegeRepositoryAdminRequest apiPrivilege = new ApiPrivilegeRepositoryAdminRequest("priv", "newdescription",
        "format2", "repository2", List.of(PrivilegeAction.DELETE));

    underTest.updatePrivilege("priv", apiPrivilege);

    ArgumentCaptor<Privilege> argument = ArgumentCaptor.forClass(Privilege.class);
    verify(authorizationManager).updatePrivilegeByName(argument.capture());
    assertPrivilege(argument.getValue(), "priv", "newdescription", FORMAT_KEY, "format2", REPOSITORY_KEY,
        "repository2", ACTIONS_KEY, "delete");
  }

  @Test
  void testUpdatePrivilege_repositoryAdminInvalidRepository() {
    Privilege priv = createPrivilege("repository-admin", "priv", "privdesc", false);
    when(authorizationManager.getPrivilegeByName("priv")).thenReturn(priv);

    ApiPrivilegeRepositoryAdminRequest apiPrivilege = new ApiPrivilegeRepositoryAdminRequest("priv", "newdescription",
        "format2", "invalid", List.of(PrivilegeAction.DELETE));

    try {
      underTest.updatePrivilege("priv", apiPrivilege);
      fail("create should have failed as repository is invalid.");
    }
    catch (WebApplicationMessageException e) {
      assertThat(e.getResponse().getStatus(), is(400));
      assertThat(e.getResponse().getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));
      assertThat(e.getResponse().getEntity().toString(),
          is("ValidationErrorXO{id='*', message='\"Invalid repository 'invalid' supplied.\"'}"));
    }
  }

  @Test
  void testUpdatePrivilege_repositoryContentSelector() {
    Privilege priv = createPrivilege("repository-content-selector", "priv", "privdesc", false, FORMAT_KEY, "format1",
        REPOSITORY_KEY, "repository1", CSEL_KEY, "contentSelector", ACTIONS_KEY, "read,delete");
    when(authorizationManager.getPrivilegeByName("priv")).thenReturn(priv);

    ApiPrivilegeRepositoryContentSelectorRequest apiPrivilege = new ApiPrivilegeRepositoryContentSelectorRequest("priv",
        "newdescription", "format2", "repository2", "newcontentSelector",
        List.of(PrivilegeAction.DELETE));

    underTest.updatePrivilege("priv", apiPrivilege);

    ArgumentCaptor<Privilege> argument = ArgumentCaptor.forClass(Privilege.class);
    verify(authorizationManager).updatePrivilegeByName(argument.capture());
    assertPrivilege(argument.getValue(), "priv", "newdescription", FORMAT_KEY, "format2", REPOSITORY_KEY,
        "repository2", CSEL_KEY, "newcontentSelector", ACTIONS_KEY, "delete");
  }

  @Test
  void testUpdatePrivilege_repositoryContentSelectorInvalidRepository() {
    Privilege priv = createPrivilege("repository-content-selector", "priv", "privdesc", false);
    when(authorizationManager.getPrivilegeByName("priv")).thenReturn(priv);

    ApiPrivilegeRepositoryContentSelectorRequest apiPrivilege =
        new ApiPrivilegeRepositoryContentSelectorRequest("priv", "newdescription",
            "format2", "invalid", "newContentSelector", List.of(PrivilegeAction.DELETE));

    WebApplicationMessageException e =
        assertThrows(WebApplicationMessageException.class, () -> underTest.updatePrivilege("priv", apiPrivilege));
    assertThat(e.getResponse().getStatus(), is(400));
    assertThat(e.getResponse().getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));
    assertThat(e.getResponse().getEntity().toString(),
        is("ValidationErrorXO{id='*', message='\"Invalid repository 'invalid' supplied.\"'}"));
  }

  @Test
  void testUpdatePrivilege_repositoryContentSelectorInvalidContentSelector() {
    Privilege priv = createPrivilege("repository-content-selector", "priv", "privdesc", false);
    when(authorizationManager.getPrivilegeByName("priv")).thenReturn(priv);

    ApiPrivilegeRepositoryContentSelectorRequest apiPrivilege =
        new ApiPrivilegeRepositoryContentSelectorRequest("priv", "newdescription",
            "format2", "repository2", "invalid", List.of(PrivilegeAction.DELETE));

    WebApplicationMessageException e =
        assertThrows(WebApplicationMessageException.class, () -> underTest.updatePrivilege("priv", apiPrivilege));
    assertThat(e.getResponse().getStatus(), is(400));
    assertThat(e.getResponse().getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));
    assertThat(e.getResponse().getEntity().toString(),
        is("ValidationErrorXO{id='*', message='\"Invalid selector 'invalid' supplied.\"'}"));
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
