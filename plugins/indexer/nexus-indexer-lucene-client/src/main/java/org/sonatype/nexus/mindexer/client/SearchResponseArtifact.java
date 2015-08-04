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
package org.sonatype.nexus.mindexer.client;

import org.sonatype.nexus.client.internal.util.Check;

public class SearchResponseArtifact
{

  /**
   * The group id of the artifact.
   */
  private final String groupId;

  /**
   * The artifact id of the artifact.
   */
  private final String artifactId;

  /**
   * The version of the artifact.
   */
  private final String version;

  /**
   * The extension of the artifact.
   */
  private final String extension;

  /**
   * The classifier of the artifact.
   */
  private final String classifier;

  /**
   * The repository where the hit is.
   */
  private final SearchResponseRepository repository;

  /**
   * The latest snapshot version of the artifact.
   */
  private final String latestSnapshot;

  /**
   * The repository of latest snapshot version of the artifact.
   */
  private final String latestSnapshotRepositoryId;

  /**
   * The latest release version of the artifact.
   */
  private final String latestRelease;

  /**
   * The repository of latest release version of the artifact.
   */
  private final String latestReleaseRepositoryId;

  /**
   * A HTML highlighted fragment of the matched hit.
   */
  private final String highlightedFragment;

  public SearchResponseArtifact(final String groupId, final String artifactId, final String version,
                                final String extension, final String classifier,
                                final SearchResponseRepository repository, final String latestSnapshot,
                                final String latestSnapshotRepositoryId, final String latestRelease,
                                final String latestReleaseRepositoryId, final String highlightedFragment)
  {
    this.groupId = Check.notBlank(groupId, "groupId");
    this.artifactId = Check.notBlank(artifactId, "artifactId");
    this.version = Check.notBlank(version, "version");
    this.extension = extension;
    this.classifier = classifier;
    this.repository = Check.notNull(repository, SearchResponseRepository.class);
    this.latestSnapshot = latestSnapshot;
    this.latestSnapshotRepositoryId = latestSnapshotRepositoryId;
    this.latestRelease = latestRelease;
    this.latestReleaseRepositoryId = latestReleaseRepositoryId;
    this.highlightedFragment = highlightedFragment;
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

  public String getExtension() {
    return extension;
  }

  public String getClassifier() {
    return classifier;
  }

  public SearchResponseRepository getRepository() {
    return repository;
  }

  public String getLatestSnapshot() {
    return latestSnapshot;
  }

  public String getLatestSnapshotRepositoryId() {
    return latestSnapshotRepositoryId;
  }

  public String getLatestRelease() {
    return latestRelease;
  }

  public String getLatestReleaseRepositoryId() {
    return latestReleaseRepositoryId;
  }

  public String getHighlightedFragment() {
    return highlightedFragment;
  }
}
