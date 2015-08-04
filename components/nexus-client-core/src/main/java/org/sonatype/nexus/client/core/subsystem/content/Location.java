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
package org.sonatype.nexus.client.core.subsystem.content;

import org.sonatype.nexus.client.internal.util.Check;

/**
 * @since 2.1
 */
public class Location
{

  private final String contentType;

  private final String repositoryId;

  private final String repositoryPath;

  public Location(final String contentType, final String repositoryId, final String repositoryPath) {
    this.contentType = Check.notBlank(contentType, "contentType");
    this.repositoryId = Check.notBlank(repositoryId, "repositoryId");
    String repoPath = Check.notBlank(repositoryPath, "repositoryPath");
    while (repoPath.startsWith("/")) {
      repoPath = repoPath.substring(1);
    }
    this.repositoryPath = repoPath;
  }

  public Location(final String repositoryId, final String repositoryPath) {
    this("repositories", repositoryId, repositoryPath);
  }

  public String toContentPath() {
    return String.format("%s/%s/%s", contentType, repositoryId, repositoryPath);
  }

  // --

  @Override
  public String toString() {
    return toContentPath();
  }

  public static Location repositoryLocation(final String repositoryId, final String path) {
    return new Location("repositories", repositoryId, path);
  }

  public static Location groupLocation(final String groupId, final String path) {
    return new Location("groups", groupId, path);
  }

  public static Location siteLocation(final String siteId, final String path) {
    return new Location("sites", siteId, path);
  }

}
