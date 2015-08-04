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

import java.util.List;

import org.sonatype.nexus.index.treeview.DefaultMergedTreeNode;

import com.google.common.collect.Lists;
import org.apache.maven.index.treeview.IndexTreeView;
import org.apache.maven.index.treeview.TreeNode;
import org.apache.maven.index.treeview.TreeViewRequest;

/**
 * Adds some more details to the TreeNode, some items that are required for index browsing in the UI
 */
public class IndexBrowserTreeNode
    extends DefaultMergedTreeNode
{
  /**
   * The classifier of the artifact.
   */
  private String classifier;

  /**
   * The file extension of the artifact.
   */
  private String extension;

  /**
   * The packaging of the artifact.
   */
  private String packaging;

  /**
   * The URI of the artifact.
   */
  private String artifactUri;

  /**
   * The URI of the artifact's pom file.
   */
  private String pomUri;

  /**
   * Constructor that takes an IndexTreeView implmentation and a TreeNodeFactory implementation;
   */
  public IndexBrowserTreeNode(IndexTreeView tview, TreeViewRequest request) {
    super(tview, request);
  }

  /**
   * Get the classifier of the artifact.
   *
   * @return String
   */
  public String getClassifier() {
    return classifier;
  }

  /**
   * Set the classifier of the artifact.
   */
  public void setClassifier(String classifier) {
    this.classifier = classifier;
  }

  /**
   * Get the file extension of the artifact.
   *
   * @return String
   */
  public String getExtension() {
    return extension;
  }

  /**
   * Set the file extension of the artifact.
   */
  public void setExtension(String extension) {
    this.extension = extension;
  }

  /**
   * Get the URI of the artifact.
   *
   * @return String
   */
  public String getArtifactUri() {
    return artifactUri;
  }

  /**
   * Set the URI of the artifact.
   */
  public void setArtifactUri(String artifactUri) {
    this.artifactUri = artifactUri;
  }

  /**
   * Get the URI of the artifact's pom file.
   *
   * @return String
   */
  public String getPomUri() {
    return pomUri;
  }

  /**
   * Set the URI of the artifact's pom file.
   */
  public void setPomUri(String pomUri) {
    this.pomUri = pomUri;
  }

  /**
   * Get the packaging of the artifact.
   *
   * @return String
   */
  public String getPackaging() {
    return packaging;
  }

  /**
   * Set the packaging of the artifact.
   */
  public void setPackaging(String packaging) {
    this.packaging = packaging;
  }

  /**
   * Converts this instance into a DTO, ready for wire transmission.
   *
   * @since 2.7.0
   */
  public IndexBrowserTreeNodeDTO toDTO() {
    List<IndexBrowserTreeNodeDTO> dtoChildren = null;
    final List<TreeNode> children = getChildren();
    if (children != null && !children.isEmpty()) {
      dtoChildren = Lists.newArrayList();
      for (TreeNode childNode : children) {
        if (childNode instanceof IndexBrowserTreeNode) {
          dtoChildren.add(((IndexBrowserTreeNode) childNode).toDTO());
        }
      }
    }
    return new IndexBrowserTreeNodeDTO(getType().name(), isLeaf(), getNodeName(), getPath(), dtoChildren,
        getGroupId(), getArtifactId(), getVersion(), getRepositoryId(), isLocallyAvailable(),
        getArtifactTimestamp(), getArtifactSha1Checksum(), getArtifactMd5Checksum(), getInitiatorUserId(),
        getInitiatorIpAddress(), getArtifactOriginReason(), getArtifactOriginUrl(), classifier, extension,
        packaging, artifactUri, pomUri);
  }
}
