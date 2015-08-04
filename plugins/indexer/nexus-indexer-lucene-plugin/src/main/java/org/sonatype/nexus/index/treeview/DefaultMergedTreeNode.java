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
package org.sonatype.nexus.index.treeview;

import org.apache.maven.index.treeview.DefaultTreeNode;
import org.apache.maven.index.treeview.IndexTreeView;
import org.apache.maven.index.treeview.TreeViewRequest;

/**
 * Enhances the DefaultTreeNode (which is built from index information) to add information that
 * Nexus has stored about the item.
 */
public class DefaultMergedTreeNode
    extends DefaultTreeNode
{
  /**
   * Flag that states whether the node is locally available.
   */
  private boolean locallyAvailable;

  /**
   * The timestamp the artifact was last modified..
   */
  private long artifactTimestamp;

  /**
   * The sha1 checksum of the artifact.
   */
  private String artifactSha1Checksum;

  /**
   * The md5 checksum of the artifact.
   */
  private String artifactMd5Checksum;

  /**
   * The user id that initiated Nexus to store this artifact.
   */
  private String initiatorUserId;

  /**
   * The ip address that initiated Nexus to store this artifact.
   */
  private String initiatorIpAddress;

  /**
   * The reason this artifact is in Nexus (i.e. cached from proxy repository or deployed into hosted repository).
   */
  private String artifactOriginReason;

  /**
   * The remote url that this artifact was retrieved from.
   */
  private String artifactOriginUrl;

  /**
   * Constructor that takes an IndexTreeView implmentation and a TreeNodeFactory implementation;
   */
  public DefaultMergedTreeNode(IndexTreeView tview, TreeViewRequest request) {
    super(tview, request);
  }

  /**
   * Get Flag that states whether the node is locally available.
   *
   * @return boolean
   */
  public boolean isLocallyAvailable() {
    return locallyAvailable;
  }

  /**
   * Set flag that states whether the node is locally available.
   */
  public void setLocallyAvailable(boolean locallyAvailable) {
    this.locallyAvailable = locallyAvailable;
  }

  /**
   * Get the timestamp the artifact was last modified..
   *
   * @return long
   */
  public long getArtifactTimestamp() {
    return artifactTimestamp;
  }

  /**
   * Set the timestamp the artifact was last modified..
   */
  public void setArtifactTimestamp(long artifactTimestamp) {
    this.artifactTimestamp = artifactTimestamp;
  }

  /**
   * Get the sha1 checksum of the artifact.
   *
   * @return String
   */
  public String getArtifactSha1Checksum() {
    return artifactSha1Checksum;
  }

  /**
   * Set the sha1 checksum of the artifact.
   */
  public void setArtifactSha1Checksum(String artifactSha1Checksum) {
    this.artifactSha1Checksum = artifactSha1Checksum;
  }

  /**
   * Get the md5 checksum of the artifact.
   *
   * @return String
   */
  public String getArtifactMd5Checksum() {
    return artifactMd5Checksum;
  }

  /**
   * Set the sha1 checksum of the artifact.
   */
  public void setArtifactMd5Checksum(String artifactMd5Checksum) {
    this.artifactMd5Checksum = artifactMd5Checksum;
  }

  /**
   * @return
   */
  public String getInitiatorUserId() {
    return initiatorUserId;
  }

  /**
   * @param initiatorUserId
   */
  public void setInitiatorUserId(String initiatorUserId) {
    this.initiatorUserId = initiatorUserId;
  }

  /**
   * @return
   */
  public String getInitiatorIpAddress() {
    return initiatorIpAddress;
  }

  /**
   * @param initiatorIpAddress
   */
  public void setInitiatorIpAddress(String initiatorIpAddress) {
    this.initiatorIpAddress = initiatorIpAddress;
  }

  /**
   * @return
   */
  public String getArtifactOriginReason() {
    return artifactOriginReason;
  }

  /**
   * @param artifactOriginReason
   */
  public void setArtifactOriginReason(String artifactOriginReason) {
    this.artifactOriginReason = artifactOriginReason;
  }

  /**
   * @return
   */
  public String getArtifactOriginUrl() {
    return artifactOriginUrl;
  }

  /**
   * @param artifactOriginUrl
   */
  public void setArtifactOriginUrl(String artifactOriginUrl) {
    this.artifactOriginUrl = artifactOriginUrl;
  }

}
