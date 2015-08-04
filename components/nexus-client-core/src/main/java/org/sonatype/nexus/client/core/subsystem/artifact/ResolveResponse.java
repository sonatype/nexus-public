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
package org.sonatype.nexus.client.core.subsystem.artifact;

/**
 * Request for resolving a {@link ArtifactMaven}.
 *
 * @since 2.1
 */
public class ResolveResponse
{

  /**
   * True if item is present in local storage.
   */
  private final boolean presentLocally;

  /**
   * The groupId of the requested artifact.
   */
  private final String groupId;

  /**
   * The artifactId of the requested artifact.
   */
  private final String artifactId;

  /**
   * The version of the requested artifact.
   */
  private final String version;

  /**
   * The base version of the requested artifact.
   */
  private final String baseVersion;

  /**
   * The classifier of the requested artifact.
   */
  private final String classifier;

  /**
   * The file extension of the requested artifact.
   */
  private final String extension;

  /**
   * Flag that states if requested artifact is a snapshot.
   */
  private final boolean snapshot;

  /**
   * The build number of the snapshot version of the requested artifact.
   */
  private final long snapshotBuildNumber;

  /**
   * The timestamp portion of the snapshot version of the requested artifact.
   */
  private final long snapshotTimeStamp;

  /**
   * The filename of the requested artifact.
   */
  private final String fileName;

  /**
   * The sha1 hash of the requested artifact.
   */
  private final String sha1;

  /**
   * The path in the repository of the requested artifact.
   */
  private final String repositoryPath;

  public ResolveResponse(final boolean presentLocally, final String groupId, final String artifactId,
                         final String version, final String baseVersion, final String classifier,
                         final String extension, final boolean snapshot, final long snapshotBuildNumber,
                         final long snapshotTimeStamp, final String fileName, final String sha1,
                         final String repositoryPath)
  {
    this.presentLocally = presentLocally;
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
    this.baseVersion = baseVersion;
    this.classifier = classifier;
    this.extension = extension;
    this.snapshot = snapshot;
    this.snapshotBuildNumber = snapshotBuildNumber;
    this.snapshotTimeStamp = snapshotTimeStamp;
    this.fileName = fileName;
    this.sha1 = sha1;
    this.repositoryPath = repositoryPath;
  }

  public boolean isPresentLocally() {
    return presentLocally;
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

  public String getBaseVersion() {
    return baseVersion;
  }

  public String getClassifier() {
    return classifier;
  }

  public String getExtension() {
    return extension;
  }

  public boolean isSnapshot() {
    return snapshot;
  }

  public long getSnapshotBuildNumber() {
    return snapshotBuildNumber;
  }

  public long getSnapshotTimeStamp() {
    return snapshotTimeStamp;
  }

  public String getFileName() {
    return fileName;
  }

  public String getSha1() {
    return sha1;
  }

  public String getRepositoryPath() {
    return repositoryPath;
  }
}
