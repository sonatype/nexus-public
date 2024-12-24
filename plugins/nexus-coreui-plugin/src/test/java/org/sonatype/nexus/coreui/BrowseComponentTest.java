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
package org.sonatype.nexus.coreui;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.node.BrowseNode;
import org.sonatype.nexus.repository.browse.node.BrowseNodeConfiguration;
import org.sonatype.nexus.repository.browse.node.BrowseNodeQueryService;
import org.sonatype.nexus.repository.manager.RepositoryManager;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BrowseComponentTest
    extends TestSupport
{
  private static final String REPOSITORY_NAME = "repositoryName";

  private static final String ROOT = "/";

  private final BrowseNodeConfiguration configuration = new BrowseNodeConfiguration();

  @Mock
  private BrowseNodeQueryService browseNodeQueryService;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private EntityId assetId;

  @Mock
  private EntityId componentId;

  @Mock
  private Repository repository;

  private BrowseComponent underTest;

  @Before
  public void setUp() {
    when(repository.getName()).thenReturn(REPOSITORY_NAME);
    when(componentId.getValue()).thenReturn("componentId");
    when(assetId.getValue()).thenReturn("assetId");
    underTest = new BrowseComponent(configuration, browseNodeQueryService, repositoryManager);
  }

  @Test
  public void testRootNodeListQuery() {
    BrowseNode browseNode1 = mock(BrowseNode.class);
    when(browseNode1.getName()).thenReturn("com");

    BrowseNode browseNode2 = mock(BrowseNode.class);
    when(browseNode2.getName()).thenReturn("org");
    when(browseNode2.getComponentId()).thenReturn(componentId);

    BrowseNode browseNode3 = mock(BrowseNode.class);
    when(browseNode3.getName()).thenReturn("net");
    when(browseNode3.getAssetId()).thenReturn(assetId);
    when(browseNode3.isLeaf()).thenReturn(true);

    List<BrowseNode> browseNodes = List.of(browseNode1, browseNode2, browseNode3);

    TreeStoreLoadParameters treeStoreLoadParameters = new TreeStoreLoadParameters();
    treeStoreLoadParameters.setRepositoryName(REPOSITORY_NAME);
    treeStoreLoadParameters.setNode(ROOT);

    when(repositoryManager.get(REPOSITORY_NAME)).thenReturn(repository);
    when(browseNodeQueryService.getByPath(repository, Collections.emptyList(),
        configuration.getMaxHtmlNodes())).thenReturn(browseNodes);

    List<BrowseNodeXO> xos = underTest.read(treeStoreLoadParameters);

    assertThat(xos, hasSize(3));
    assertThat(xos.get(0).getText(), is("com"));
    assertThat(xos.get(1).getText(), is("org"));
    assertThat(xos.get(2).getText(), is("net"));
    assertThat(xos.get(0).getId(), is("com"));
    assertThat(xos.get(1).getId(), is("org"));
    assertThat(xos.get(2).getId(), is("net"));
    assertThat(xos.get(0).getType(), is(BrowseComponent.FOLDER));
    assertThat(xos.get(1).getType(), is(BrowseComponent.COMPONENT));
    assertThat(xos.get(2).getType(), is(BrowseComponent.ASSET));
    assertFalse(xos.get(0).isLeaf());
    assertFalse(xos.get(1).isLeaf());
    assertTrue(xos.get(2).isLeaf());
  }

  @Test
  public void testNonRootListQuery() {
    BrowseNode browseNode1 = mock(BrowseNode.class);
    when(browseNode1.getName()).thenReturn("com");

    BrowseNode browseNode2 = mock(BrowseNode.class);
    when(browseNode2.getName()).thenReturn("org");
    when(browseNode2.getComponentId()).thenReturn(componentId);

    BrowseNode browseNode3 = mock(BrowseNode.class);
    when(browseNode3.getName()).thenReturn("net");
    when(browseNode3.getAssetId()).thenReturn(assetId);
    when(browseNode3.isLeaf()).thenReturn(true);

    List<BrowseNode> browseNodes = List.of(browseNode1, browseNode2, browseNode3);

    TreeStoreLoadParameters treeStoreLoadParameters = new TreeStoreLoadParameters();
    treeStoreLoadParameters.setRepositoryName(REPOSITORY_NAME);
    treeStoreLoadParameters.setNode("com/boogie/down");

    when(repositoryManager.get(REPOSITORY_NAME)).thenReturn(repository);
    when(browseNodeQueryService.getByPath(repository, List.of("com", "boogie", "down"),
        configuration.getMaxHtmlNodes())).thenReturn(browseNodes);

    List<BrowseNodeXO> xos = underTest.read(treeStoreLoadParameters);

    assertThat(xos, hasSize(3));
    assertThat(xos.get(0).getText(), is("com"));
    assertThat(xos.get(1).getText(), is("org"));
    assertThat(xos.get(2).getText(), is("net"));
    assertThat(xos.get(0).getId(), is("com/boogie/down/com"));
    assertThat(xos.get(1).getId(), is("com/boogie/down/org"));
    assertThat(xos.get(2).getId(), is("com/boogie/down/net"));
    assertThat(xos.get(0).getType(), is(BrowseComponent.FOLDER));
    assertThat(xos.get(1).getType(), is(BrowseComponent.COMPONENT));
    assertThat(xos.get(2).getType(), is(BrowseComponent.ASSET));
    assertFalse(xos.get(0).isLeaf());
    assertFalse(xos.get(1).isLeaf());
    assertTrue(xos.get(2).isLeaf());
  }

  @Test
  public void testValidateEncodedSegments() {
    BrowseNode browseNode1 = mock(BrowseNode.class);
    when(browseNode1.getName()).thenReturn("com");

    BrowseNode browseNode2 = mock(BrowseNode.class);
    when(browseNode2.getName()).thenReturn("org");
    when(browseNode2.getComponentId()).thenReturn(componentId);

    BrowseNode browseNode3 = mock(BrowseNode.class);
    when(browseNode3.getName()).thenReturn("n/e/t");
    when(browseNode3.getAssetId()).thenReturn(assetId);
    when(browseNode3.isLeaf()).thenReturn(true);

    List<BrowseNode> browseNodes = List.of(browseNode1, browseNode2, browseNode3);

    TreeStoreLoadParameters treeStoreLoadParameters = new TreeStoreLoadParameters();
    treeStoreLoadParameters.setRepositoryName(REPOSITORY_NAME);
    treeStoreLoadParameters.setNode("com/boo%2Fgie/down");

    when(repositoryManager.get(REPOSITORY_NAME)).thenReturn(repository);
    when(browseNodeQueryService.getByPath(repository, List.of("com", "boo/gie", "down"),
        configuration.getMaxHtmlNodes())).thenReturn(browseNodes);

    List<BrowseNodeXO> xos = underTest.read(treeStoreLoadParameters);

    assertThat(xos, hasSize(3));
    assertThat(xos.get(0).getText(), is("com"));
    assertThat(xos.get(1).getText(), is("org"));
    assertThat(xos.get(2).getText(), is("n/e/t"));
    assertThat(xos.get(0).getId(), is("com/boo%2Fgie/down/com"));
    assertThat(xos.get(1).getId(), is("com/boo%2Fgie/down/org"));
    assertThat(xos.get(2).getId(), is("com/boo%2Fgie/down/n%2Fe%2Ft"));
    assertThat(xos.get(0).getType(), is(BrowseComponent.FOLDER));
    assertThat(xos.get(1).getType(), is(BrowseComponent.COMPONENT));
    assertThat(xos.get(2).getType(), is(BrowseComponent.ASSET));
    assertFalse(xos.get(0).isLeaf());
    assertFalse(xos.get(1).isLeaf());
    assertTrue(xos.get(2).isLeaf());
  }
}
