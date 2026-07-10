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
package org.sonatype.nexus.coreui.internal.browse;

import java.util.Collections;
import java.util.List;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.coreui.BrowseNodeXO;
import org.sonatype.nexus.coreui.ComponentHelper;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.node.BrowseNode;
import org.sonatype.nexus.repository.browse.node.BrowseNodeConfiguration;
import org.sonatype.nexus.repository.browse.node.BrowseNodeQueryService;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class BrowseResourceTest
{
  private static final String REPOSITORY_NAME = "test-repo";

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private BrowseNodeQueryService browseNodeQueryService;

  @Mock
  private SecurityHelper securityHelper;

  @Mock
  private ComponentHelper componentHelper;

  private final BrowseNodeConfiguration configuration = new BrowseNodeConfiguration();

  private BrowseResource underTest;

  @BeforeEach
  void setUp() {
    underTest = new BrowseResource(repositoryManager, browseNodeQueryService, configuration, securityHelper,
        componentHelper);
  }

  private Repository configureRepository() {
    Format format = mock(Format.class);
    when(format.getValue()).thenReturn("maven2");
    Repository repository = mock(Repository.class);
    when(repository.getName()).thenReturn(REPOSITORY_NAME);
    when(repository.getFormat()).thenReturn(format);
    when(repositoryManager.get(REPOSITORY_NAME)).thenReturn(repository);
    when(securityHelper.allPermitted(any())).thenReturn(true);
    return repository;
  }

  @Test
  void getBrowseNodes_rootPath_returnsNodes() {
    Repository repository = configureRepository();

    EntityId componentId = mock(EntityId.class);
    when(componentId.getValue()).thenReturn("component-1");
    EntityId assetId = mock(EntityId.class);
    when(assetId.getValue()).thenReturn("asset-1");

    BrowseNode folderNode = mockBrowseNode("com", null, null, false, null);
    BrowseNode componentNode = mockBrowseNode("org", componentId, null, false, "pkg:maven/org@1.0");
    BrowseNode assetNode = mockBrowseNode("net", null, assetId, true, null);

    when(browseNodeQueryService.getByPath(eq(repository), eq(Collections.emptyList()), anyInt()))
        .thenReturn(List.of(folderNode, componentNode, assetNode));

    List<BrowseNodeXO> result = underTest.getBrowseNodes(REPOSITORY_NAME, "/");

    assertThat(result, hasSize(3));

    assertThat(result.get(0).getText(), is("com"));
    assertThat(result.get(0).getType(), is("folder"));
    assertThat(result.get(0).getId(), is("com"));
    assertThat(result.get(0).isLeaf(), is(false));

    assertThat(result.get(1).getText(), is("org"));
    assertThat(result.get(1).getType(), is("component"));
    assertThat(result.get(1).getComponentId(), is("component-1"));
    assertThat(result.get(1).getPackageUrl(), is("pkg:maven/org@1.0"));

    assertThat(result.get(2).getText(), is("net"));
    assertThat(result.get(2).getType(), is("asset"));
    assertThat(result.get(2).getAssetId(), is("asset-1"));
    assertThat(result.get(2).isLeaf(), is(true));
  }

  @Test
  void getBrowseNodes_nestedPath_returnsNodesWithCorrectIds() {
    Repository repository = configureRepository();

    EntityId assetId = mock(EntityId.class);
    when(assetId.getValue()).thenReturn("asset-1");
    BrowseNode node = mockBrowseNode("artifact.jar", null, assetId, true, null);

    when(browseNodeQueryService.getByPath(eq(repository), eq(List.of("com", "example")), anyInt()))
        .thenReturn(List.of(node));

    List<BrowseNodeXO> result = underTest.getBrowseNodes(REPOSITORY_NAME, "/com/example");

    assertThat(result, hasSize(1));
    assertThat(result.get(0).getId(), is("/com/example/artifact.jar"));
    assertThat(result.get(0).getText(), is("artifact.jar"));
  }

  @Test
  void getBrowseNodes_nullPath_treatedAsRoot() {
    Repository repository = configureRepository();

    when(browseNodeQueryService.getByPath(eq(repository), eq(Collections.emptyList()), anyInt()))
        .thenReturn(Collections.emptyList());

    List<BrowseNodeXO> result = underTest.getBrowseNodes(REPOSITORY_NAME, null);

    assertThat(result, is(empty()));
    verify(browseNodeQueryService).getByPath(eq(repository), eq(Collections.emptyList()), anyInt());
  }

  @Test
  void getBrowseNodes_nullBrowseNodes_returnsEmptyList() {
    Repository repository = configureRepository();

    when(browseNodeQueryService.getByPath(eq(repository), anyList(), anyInt())).thenReturn(null);

    List<BrowseNodeXO> result = underTest.getBrowseNodes(REPOSITORY_NAME, "/");

    assertThat(result, is(empty()));
  }

  @Test
  void getBrowseNodes_repositoryNotFound_throws404() {
    when(repositoryManager.get("nonexistent")).thenReturn(null);

    WebApplicationException ex = assertThrows(WebApplicationException.class,
        () -> underTest.getBrowseNodes("nonexistent", "/"));

    assertThat(ex.getResponse().getStatus(), is(404));
  }

  @Test
  void getBrowseNodes_insufficientPermissions_throws403() {
    Format format = mock(Format.class);
    when(format.getValue()).thenReturn("maven2");
    Repository repository = mock(Repository.class);
    when(repository.getName()).thenReturn(REPOSITORY_NAME);
    when(repository.getFormat()).thenReturn(format);
    when(repositoryManager.get(REPOSITORY_NAME)).thenReturn(repository);
    when(securityHelper.allPermitted(any())).thenReturn(false);

    WebApplicationException ex = assertThrows(WebApplicationException.class,
        () -> underTest.getBrowseNodes(REPOSITORY_NAME, "/"));

    assertThat(ex.getResponse().getStatus(), is(403));
  }

  @Test
  void getBrowseNodes_encodedPathSegments_decodedCorrectly() {
    Repository repository = configureRepository();

    EntityId assetId = mock(EntityId.class);
    when(assetId.getValue()).thenReturn("asset-1");
    BrowseNode node = mockBrowseNode("file.txt", null, assetId, true, null);

    when(browseNodeQueryService.getByPath(eq(repository), eq(List.of("path with spaces", "dir")), anyInt()))
        .thenReturn(List.of(node));

    List<BrowseNodeXO> result = underTest.getBrowseNodes(REPOSITORY_NAME, "/path+with+spaces/dir");

    assertThat(result, hasSize(1));
  }

  @Test
  void deleteFolder_happyPath_returns204() {
    Repository repository = mock(Repository.class);
    when(repositoryManager.get(REPOSITORY_NAME)).thenReturn(repository);
    when(componentHelper.canDeleteFolder(repository, "/com/example")).thenReturn(true);

    Response response = underTest.deleteFolder(REPOSITORY_NAME, "/com/example");

    assertThat(response.getStatus(), is(204));
    verify(componentHelper).deleteFolder(repository, "/com/example");
  }

  @Test
  void deleteFolder_repositoryNotFound_throws404() {
    when(repositoryManager.get("nonexistent")).thenReturn(null);

    WebApplicationException ex = assertThrows(WebApplicationException.class,
        () -> underTest.deleteFolder("nonexistent", "/com/example"));

    assertThat(ex.getResponse().getStatus(), is(404));
  }

  @Test
  void deleteFolder_blankPath_throws400() {
    WebApplicationException ex = assertThrows(WebApplicationException.class,
        () -> underTest.deleteFolder(REPOSITORY_NAME, ""));

    assertThat(ex.getResponse().getStatus(), is(400));
    verify(componentHelper, never()).deleteFolder(any(), any());
  }

  @Test
  void deleteFolder_nullPath_throws400() {
    WebApplicationException ex = assertThrows(WebApplicationException.class,
        () -> underTest.deleteFolder(REPOSITORY_NAME, null));

    assertThat(ex.getResponse().getStatus(), is(400));
  }

  @Test
  void deleteFolder_insufficientPermissions_throws403() {
    Repository repository = mock(Repository.class);
    when(repositoryManager.get(REPOSITORY_NAME)).thenReturn(repository);
    when(componentHelper.canDeleteFolder(repository, "/com/example")).thenReturn(false);

    WebApplicationException ex = assertThrows(WebApplicationException.class,
        () -> underTest.deleteFolder(REPOSITORY_NAME, "/com/example"));

    assertThat(ex.getResponse().getStatus(), is(403));
    verify(componentHelper, never()).deleteFolder(any(), any());
  }

  private static BrowseNode mockBrowseNode(
      final String name,
      final EntityId componentId,
      final EntityId assetId,
      final boolean leaf,
      final String packageUrl)
  {
    BrowseNode node = mock(BrowseNode.class);
    when(node.getName()).thenReturn(name);
    when(node.getComponentId()).thenReturn(componentId);
    when(node.getAssetId()).thenReturn(assetId);
    when(node.isLeaf()).thenReturn(leaf);
    when(node.getPackageUrl()).thenReturn(packageUrl);
    return node;
  }
}
