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
package org.sonatype.nexus.testsuite.index.nexus3670;

import java.util.Collections;
import java.util.Set;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.ITGroups.INDEX;
import org.sonatype.nexus.rest.indextreeview.IndexBrowserTreeNodeDTO;
import org.sonatype.nexus.rest.indextreeview.IndexBrowserTreeViewResponseDTO;
import org.sonatype.nexus.rest.model.SearchNGResponse;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

import org.apache.maven.index.treeview.TreeNode;
import org.apache.maven.index.treeview.TreeNode.Type;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.fail;

/**
 * Test Index tree view.
 */
public class Nexus3670IndexTreeViewIT
    extends AbstractNexusIntegrationTest
{
  @Override
  protected void runOnce()
      throws Exception
  {
    super.runOnce();

    // just making sure all tasks are finished
    TaskScheduleUtil.waitForAllTasksToStop();

    getEventInspectorsUtil().waitForCalmPeriod();

    // this is just a "preflight", that all is there what we want, not a real test

    // groupId
    SearchNGResponse results = getSearchMessageUtil().searchNGFor("nexus3670");
    Assert.assertEquals(7, results.getData().size());
    // repoId
    Assert.assertEquals("Where got it deployed?",
        results.getData().get(0).getArtifactHits().get(0).getRepositoryId(), REPO_TEST_HARNESS_REPO);
  }

  /**
   * try to browser the tree, like UI does.
   */
  @Test
  @Category(INDEX.class)
  public void testTreeWithoutHint()
      throws Exception
  {
    IndexBrowserTreeViewResponseDTO response =
        getSearchMessageUtil().indexBrowserTreeView(REPO_TEST_HARNESS_REPO, "/");

    Assert.assertEquals("There is one \"nexus3670\" group!", response.getData().getChildren().size(), 1);

    // this is the G node of the "nexus3670" groupId (note: on G nodes, only the path is filled, but not the GAV!)
    IndexBrowserTreeNodeDTO node = (IndexBrowserTreeNodeDTO) response.getData().getChildren().get(0);

    // check path (note leading and trailing slashes!)
    Assert.assertEquals("The path does not correspond to group!", node.getPath(), "/nexus3670/");

    // get one level deeper
    // but this path is also Group ID, hence response will contain whole tree!
    response = getSearchMessageUtil().indexBrowserTreeView(REPO_TEST_HARNESS_REPO, node.getPath());

    Assert.assertEquals("There are four \"nexus3670\" artifacts in a group!", response.getData().getChildren().size(),
        4);

    // this is group node
    node = getNode(response, "known-artifact-a");

    Assert
        .assertEquals("There is three versions of \"nexus3670:known-artifact-a\" artifact!", node.getChildren().size(),
            3);

    // get one child (V)
    node = node.getChildren().get(0);

    // check path (note leading and trailing slashes!)
    Assert.assertEquals("The path should be V node", TreeNode.Type.V.name(), node.getType());
  }

  private IndexBrowserTreeNodeDTO getNode(IndexBrowserTreeViewResponseDTO response, String artifactId) {
    for (IndexBrowserTreeNodeDTO child : response.getData().getChildren()) {
      if (artifactId.equals(child.getArtifactId())) {
        return child;
      }
    }

    fail("Failed to find artifact " + artifactId);
    return null;
  }

  /**
   * open a tree knowing groupId and artifactId
   */
  @Test
  @Category(INDEX.class)
  public void testTreeWithHint()
      throws Exception
  {
    // here, we will omit the path (it is "root" anyway), but by giving G AND A hints, we actually end up with
    // complete subtree
    IndexBrowserTreeViewResponseDTO response =
        getSearchMessageUtil().indexBrowserTreeView(REPO_TEST_HARNESS_REPO, "nexus3670", "known-artifact-a");

    int artifactCount = countNodes(response.getData(), Collections.singleton(TreeNode.Type.artifact));

    Assert.assertEquals("Total of 3 distinct artifacts here!", artifactCount, 3);
  }

  protected int countNodes(IndexBrowserTreeNodeDTO node, Set<Type> types) {
    int result = types.contains(Type.valueOf(node.getType())) ? 1 : 0;

    if (!node.isLeaf()) {
      for (IndexBrowserTreeNodeDTO child : node.getChildren()) {
        result = result + countNodes(child, types);
      }
    }

    return result;
  }
}
