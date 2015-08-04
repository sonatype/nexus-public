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

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Adds some more details to the TreeNode, some items that are required for index browsing in the UI
 */
@XStreamAlias("indexBrowserTreeNode")
public class IndexBrowserTreeNodeDTO
    extends AbstractNexusTreeNodeDTO<IndexBrowserTreeNodeDTO>
{
  /**
   * The classifier of the artifact.
   */
  private final String classifier;

  /**
   * The file extension of the artifact.
   */
  private final String extension;

  /**
   * The packaging of the artifact.
   */
  private final String packaging;

  /**
   * The URI of the artifact.
   */
  private final String artifactUri;

  /**
   * The URI of the artifact's pom file.
   */
  private final String pomUri;

  public IndexBrowserTreeNodeDTO(String type, boolean leaf, String nodeName, String path,
                                 List<IndexBrowserTreeNodeDTO> children, String groupId, String artifactId,
                                 String version, String repositoryId, boolean locallyAvailable,
                                 long artifactTimestamp, String artifactSha1Checksum, String artifactMd5Checksum,
                                 String initiatorUserId, String initiatorIpAddress, String artifactOriginReason,
                                 String artifactOriginUrl, String classifier, String extension, String packaging,
                                 String artifactUri, String pomUri)
  {
    super(type, leaf, nodeName, path, children, groupId, artifactId, version, repositoryId, locallyAvailable,
        artifactTimestamp, artifactSha1Checksum, artifactMd5Checksum, initiatorUserId, initiatorIpAddress,
        artifactOriginReason, artifactOriginUrl);
    this.classifier = classifier;
    this.extension = extension;
    this.packaging = packaging;
    this.artifactUri = artifactUri;
    this.pomUri = pomUri;
  }

  public String getClassifier() {
    return classifier;
  }

  public String getExtension() {
    return extension;
  }

  public String getPackaging() {
    return packaging;
  }

  public String getArtifactUri() {
    return artifactUri;
  }

  public String getPomUri() {
    return pomUri;
  }
}
