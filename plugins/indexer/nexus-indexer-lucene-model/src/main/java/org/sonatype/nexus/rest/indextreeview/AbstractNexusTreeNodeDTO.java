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

/**
 * Tree node that carries most of the information Nexus Indexer Lucene has about an indexed item.
 *
 * @author cstamas
 * @since 2.7.0
 */
public abstract class AbstractNexusTreeNodeDTO<T extends AbstractNexusTreeNodeDTO>
{
  /**
   * The type of node.
   */
  private final String type;

  /**
   * Flag that determines if the node is a leaf.
   */
  private final boolean leaf;

  /**
   * The name of the node.
   */
  private final String nodeName;

  /**
   * The path of the node.
   */
  private final String path;

  /**
   * The children of this node.
   */
  private final List<T> children;

  /**
   * The group id of this node.
   */
  private final String groupId;

  /**
   * The artifact id of this node.
   */
  private final String artifactId;

  /**
   * The version of this node.
   */
  private final String version;

  /**
   * The repository id that this node is stored in.
   */
  private final String repositoryId;

  /**
   * Flag that states whether the node is locally available.
   */
  private final boolean locallyAvailable;

  /**
   * The timestamp the artifact was last modified..
   */
  private final long artifactTimestamp;

  /**
   * The sha1 checksum of the artifact.
   */
  private final String artifactSha1Checksum;

  /**
   * The md5 checksum of the artifact.
   */
  private final String artifactMd5Checksum;

  /**
   * The user id that initiated Nexus to store this artifact.
   */
  private final String initiatorUserId;

  /**
   * The ip address that initiated Nexus to store this artifact.
   */
  private final String initiatorIpAddress;

  /**
   * The reason this artifact is in Nexus (i.e. cached from proxy repository or deployed into hosted repository).
   */
  private final String artifactOriginReason;

  /**
   * The remote url that this artifact was retrieved from.
   */
  private final String artifactOriginUrl;

  public AbstractNexusTreeNodeDTO(String type, boolean leaf, String nodeName, String path, List<T> children,
                                  String groupId, String artifactId, String version, String repositoryId,
                                  boolean locallyAvailable, long artifactTimestamp, String artifactSha1Checksum,
                                  String artifactMd5Checksum, String initiatorUserId, String initiatorIpAddress,
                                  String artifactOriginReason, String artifactOriginUrl)
  {
    this.type = type;
    this.leaf = leaf;
    this.nodeName = nodeName;
    this.path = path;
    this.children = children;
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
    this.repositoryId = repositoryId;
    this.locallyAvailable = locallyAvailable;
    this.artifactTimestamp = artifactTimestamp;
    this.artifactSha1Checksum = artifactSha1Checksum;
    this.artifactMd5Checksum = artifactMd5Checksum;
    this.initiatorUserId = initiatorUserId;
    this.initiatorIpAddress = initiatorIpAddress;
    this.artifactOriginReason = artifactOriginReason;
    this.artifactOriginUrl = artifactOriginUrl;
  }

  public String getType() {
    return type;
  }

  public boolean isLeaf() {
    return leaf;
  }

  public String getNodeName() {
    return nodeName;
  }

  public String getPath() {
    return path;
  }

  public List<T> getChildren() {
    return children;
  }

  public String getGroupId() {
    return groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public String getVersion() {
    return version;
  }

  public String getRepositoryId() {
    return repositoryId;
  }

  public boolean isLocallyAvailable() {
    return locallyAvailable;
  }

  public long getArtifactTimestamp() {
    return artifactTimestamp;
  }

  public String getArtifactSha1Checksum() {
    return artifactSha1Checksum;
  }

  public String getArtifactMd5Checksum() {
    return artifactMd5Checksum;
  }

  public String getInitiatorUserId() {
    return initiatorUserId;
  }

  public String getInitiatorIpAddress() {
    return initiatorIpAddress;
  }

  public String getArtifactOriginReason() {
    return artifactOriginReason;
  }

  public String getArtifactOriginUrl() {
    return artifactOriginUrl;
  }
}
