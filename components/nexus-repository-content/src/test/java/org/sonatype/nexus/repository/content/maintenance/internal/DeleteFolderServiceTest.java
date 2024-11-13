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
package org.sonatype.nexus.repository.content.maintenance.internal;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.node.BrowseNode;
import org.sonatype.nexus.repository.browse.node.BrowseNodeConfiguration;
import org.sonatype.nexus.repository.browse.node.BrowseNodeQueryService;
import org.sonatype.nexus.repository.content.browse.BrowseFacet;
import org.sonatype.nexus.repository.content.browse.store.BrowseNodeData;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.maintenance.ContentMaintenanceFacet;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapterManager;
import org.sonatype.nexus.security.SecurityHelper;

import org.apache.shiro.util.ThreadContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DeleteFolderServiceTest
    extends TestSupport
{
  @Mock
  private BrowseNodeQueryService browseNodeQueryService;

  @Mock
  private BrowseNodeConfiguration configuration;

  @Mock
  private ContentPermissionChecker contentPermissionChecker;

  @Mock
  private VariableResolverAdapterManager variableResolverAdapterManager;

  @Mock
  private SecurityHelper securityHelper;

  @Mock
  private DatabaseCheck databaseCheck;

  @Mock
  private Repository repository;

  @Mock
  private ContentFacet contentFacet;

  @Mock
  private ContentMaintenanceFacet contentMaintenance;

  @Mock
  private BrowseFacet browseFacet;

  @Spy
  @InjectMocks
  private DeleteFolderServiceImpl deleteFolderService;

  @Before
  public void setUp() {
    BrowseNodeData node = mock(BrowseNodeData.class);
    when(node.getNodeId()).thenReturn(1L);

    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(repository.facet(ContentMaintenanceFacet.class)).thenReturn(contentMaintenance);
    when(repository.facet(BrowseFacet.class)).thenReturn(browseFacet);
    when(browseFacet.getByRequestPath(any())).thenReturn(Optional.of(node));
    when(configuration.getMaxNodes()).thenReturn(100);
  }

  @After
  public void tearDown() {
    // Unbind SecurityManager
    ThreadContext.unbindSecurityManager();
  }

  @Test
  public void testDeleteFolderForPostgreSql() {
    when(databaseCheck.isPostgresql()).thenReturn(true);
    doReturn(true).when(deleteFolderService).canDeleteComponent(repository);
    when(browseNodeQueryService.getByPath(any(), any(), anyInt())).thenReturn(Collections.emptyList());

    deleteFolderService.deleteFolder(repository, "path", OffsetDateTime.now());

    verify(browseNodeQueryService, times(1)).getByPath(any(), any(), anyInt());
    verify(browseFacet, times(1)).deleteByNodeId(any());
  }

  @Test
  public void testDeleteFolderForNonPostgreSQL() {
    when(databaseCheck.isPostgresql()).thenReturn(false);
    doReturn(true).when(deleteFolderService).canDeleteComponent(repository);
    when(browseNodeQueryService.getByPath(any(), any(), anyInt())).thenReturn(Collections.emptyList());

    deleteFolderService.deleteFolder(repository, "path", OffsetDateTime.now());

    verify(browseNodeQueryService, times(1)).getByPath(any(), any(), anyInt());
    verify(deleteFolderService, never()).deleteFoldersAndBrowseNode(any(), any(), any(), any(), any(), anyInt(),
        anyBoolean());
  }

  @Test
  public void testDeleteFolders() {
    when(browseNodeQueryService.getByPath(any(), any(), anyInt())).thenReturn(Collections.emptyList());

    deleteFolderService.deleteFolders(repository, "path", OffsetDateTime.now(), contentFacet, contentMaintenance, 100,
        true);

    verify(browseNodeQueryService, times(1)).getByPath(any(), any(), anyInt());
  }

  @Test
  public void testDeleteFoldersAndBrowseNode() {
    when(browseNodeQueryService.getByPath(any(), any(), anyInt())).thenReturn(Collections.emptyList());

    deleteFolderService.deleteFoldersAndBrowseNode(repository, "path", OffsetDateTime.now(), contentFacet,
        contentMaintenance, 100, true);

    verify(browseNodeQueryService, times(1)).getByPath(any(), any(), anyInt());
  }

  @Test
  public void testDeleteFolderForPostgreSqlWithChildren() {

    List<BrowseNode> childrenNodes = generateChildrenNodes();
    List<BrowseNode> childrenLeaves = generateChildrenLeaves();
    when(databaseCheck.isPostgresql()).thenReturn(true);
    doReturn(true).when(deleteFolderService).canDeleteComponent(repository);

    when(browseNodeQueryService.getByPath(any(), any(), anyInt()))
        .thenReturn(childrenNodes)
        .thenReturn(childrenLeaves)
        .thenReturn(Collections.emptyList())
        .thenReturn(childrenLeaves)
        .thenReturn(Collections.emptyList());

    deleteFolderService.deleteFolder(repository, "path", OffsetDateTime.now());

    verify(browseNodeQueryService, times(6)).getByPath(any(), any(), anyInt());
    verify(browseFacet, times(7)).deleteByNodeId(any());
    verify(deleteFolderService, never()).deleteFolders(any(), any(), any(), any(), any(), anyInt(), anyBoolean());
    verify(deleteFolderService, times(4)).checkDeleteComponent(any(), any(), any(), anyBoolean(), any());
    verify(deleteFolderService, times(4)).checkDeleteAsset(any(), any(), any(), any(), any());
  }

  private List<BrowseNode> generateChildrenNodes() {
    BrowseNodeData nodeOne = mock(BrowseNodeData.class);
    when(nodeOne.getNodeId()).thenReturn(2L);
    when(nodeOne.getName()).thenReturn("nodeOne");
    when(nodeOne.isLeaf()).thenReturn(false);
    when(nodeOne.getAssetCount()).thenReturn(0L);
    when(nodeOne.getPackageUrl()).thenReturn("/path/nodeOne/");

    BrowseNodeData nodeTwo = mock(BrowseNodeData.class);
    when(nodeTwo.getNodeId()).thenReturn(3L);
    when(nodeTwo.getName()).thenReturn("nodeTwo");
    when(nodeTwo.isLeaf()).thenReturn(false);
    when(nodeTwo.getAssetCount()).thenReturn(0L);
    when(nodeTwo.getPackageUrl()).thenReturn("/path/nodeTwo/");

    return Arrays.asList(nodeOne, nodeTwo);
  }

  private List<BrowseNode> generateChildrenLeaves() {
    BrowseNodeData childOne = mock(BrowseNodeData.class);
    when(childOne.getNodeId()).thenReturn(4L);
    when(childOne.getName()).thenReturn("childOne");
    when(childOne.isLeaf()).thenReturn(true);
    when(childOne.getAssetCount()).thenReturn(0L);
    when(childOne.getPackageUrl()).thenReturn("/path/childOne/");

    BrowseNodeData childTwo = mock(BrowseNodeData.class);
    when(childTwo.getNodeId()).thenReturn(5L);
    when(childTwo.getName()).thenReturn("childTwo");
    when(childTwo.isLeaf()).thenReturn(true);
    when(childTwo.getAssetCount()).thenReturn(0L);
    when(childTwo.getPackageUrl()).thenReturn("/path/childTwo/");

    return Arrays.asList(childOne, childTwo);
  }
}
