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

import org.sonatype.nexus.client.internal.util.Check;

/**
 * Result of resolving a {@link ArtifactMaven}.
 *
 * @since 2.1
 */
public class ResolveRequest
{

  public static final String VERSION_LATEST = "LATEST";

  public static final String VERSION_RELEASE = "RELEASE";

  private final String repositoryId; // mandatory

  private final String groupId; // mandatory

  private final String artifactId; // mandatory

  private final String version; // mandatory

  private final String packaging;

  private final String classifier;

  private final String extension;

  private final Boolean isLocal;

  public ResolveRequest(String repositoryId, String groupId, String artifactId, String version) {
    this(repositoryId, groupId, artifactId, version, null, null, null, null);
  }

  public ResolveRequest(String repositoryId, String groupId, String artifactId, String version, String packaging,
                        String classifier, String extension, Boolean isLocal)
  {
    this.repositoryId = Check.notBlank(repositoryId, "repositoryId");
    this.groupId = Check.notBlank(groupId, "groupId");
    this.artifactId = Check.notBlank(artifactId, "artifactId");
    this.version = Check.notBlank(version, "version");
    this.packaging = packaging;
    this.classifier = classifier;
    this.extension = extension;
    this.isLocal = isLocal;
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

  public String getPackaging() {
    return packaging;
  }

  public String getClassifier() {
    return classifier;
  }

  public String getRepositoryId() {
    return repositoryId;
  }

  public String getExtension() {
    return extension;
  }

  public Boolean getIsLocal() {
    return isLocal;
  }
}
