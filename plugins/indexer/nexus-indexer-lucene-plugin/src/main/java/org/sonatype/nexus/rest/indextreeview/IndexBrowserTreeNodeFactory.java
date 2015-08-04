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
package org.sonatype.nexus.rest.indextreeview;

import org.sonatype.nexus.index.treeview.DefaultMergedTreeNodeFactory;
import org.sonatype.nexus.proxy.repository.Repository;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.treeview.IndexTreeView;
import org.apache.maven.index.treeview.TreeNode;
import org.apache.maven.index.treeview.TreeViewRequest;

public class IndexBrowserTreeNodeFactory
    extends DefaultMergedTreeNodeFactory
{
  private String baseLinkUrl;

  public IndexBrowserTreeNodeFactory(Repository repository, String baseLinkUrl) {
    super(repository);
    this.baseLinkUrl = baseLinkUrl;
  }

  @Override
  protected TreeNode decorateArtifactNode(IndexTreeView tview, TreeViewRequest req, ArtifactInfo ai, String path,
                                          TreeNode node)
  {
    IndexBrowserTreeNode iNode = (IndexBrowserTreeNode) super.decorateArtifactNode(tview, req, ai, path, node);

    iNode.setClassifier(ai.classifier);
    iNode.setExtension(ai.fextension);
    iNode.setPackaging(ai.packaging);
    iNode.setArtifactUri(buildArtifactUri(iNode));
    iNode.setPomUri(buildPomUri(iNode));

    return iNode;
  }

  @Override
  protected TreeNode instantiateNode(IndexTreeView tview, TreeViewRequest req, String path, boolean leaf,
                                     String nodeName)
  {
    return new IndexBrowserTreeNode(tview, req);
  }

  protected String buildArtifactUri(IndexBrowserTreeNode node) {
    if (StringUtils.isEmpty(node.getPackaging()) || "pom".equals(node.getPackaging())) {
      return "";
    }

    StringBuilder sb = new StringBuilder();
    sb.append("?r=");
    sb.append(node.getRepositoryId());
    sb.append("&g=");
    sb.append(node.getGroupId());
    sb.append("&a=");
    sb.append(node.getArtifactId());
    sb.append("&v=");
    sb.append(node.getVersion());
    sb.append("&p=");
    sb.append(node.getPackaging());

    return this.baseLinkUrl + sb.toString();
  }

  protected String buildPomUri(IndexBrowserTreeNode node) {
    if (StringUtils.isNotEmpty(node.getClassifier())) {
      return "";
    }

    StringBuilder sb = new StringBuilder();
    sb.append("?r=");
    sb.append(node.getRepositoryId());
    sb.append("&g=");
    sb.append(node.getGroupId());
    sb.append("&a=");
    sb.append(node.getArtifactId());
    sb.append("&v=");
    sb.append(node.getVersion());
    sb.append("&p=pom");

    return this.baseLinkUrl + sb.toString();
  }
}
