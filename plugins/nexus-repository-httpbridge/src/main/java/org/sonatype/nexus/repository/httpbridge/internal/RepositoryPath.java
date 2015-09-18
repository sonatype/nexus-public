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
package org.sonatype.nexus.repository.httpbridge.internal;

import java.net.URI;

import javax.annotation.Nullable;

/**
 * A utility class for parsing the repository name and remaining path out of a request URI.
 *
 * @since 3.0
 */
class RepositoryPath
{
  private final String repositoryName;

  private final String remainingPath;

  private RepositoryPath(final String repositoryName, final String remainingPath) {
    this.repositoryName = repositoryName;
    this.remainingPath = remainingPath;
  }

  public String getRepositoryName() {
    return repositoryName;
  }

  public String getRemainingPath() {
    return remainingPath;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "repositoryName='" + repositoryName + '\'' +
        ", remainingPath='" + remainingPath + '\'' +
        '}';
  }

  //
  // Parser
  //

  /**
   * @return The parsed path or {@code null}
   */
  @Nullable
  public static RepositoryPath parse(final @Nullable String input) {
    // input not be null or empty
    if (input == null || input.isEmpty()) {
      return null;
    }

    // input must start with '/'
    if (!(input.charAt(0) == '/')) {
      return null;
    }

    // input must have another '/' after initial '/'
    int i = input.indexOf('/', 1);
    if (i == -1) {
      return null;
    }

    // pull off repository-name part
    String repo = input.substring(1, i);

    // repository must not be a relative token
    if (repo.equals(".") || repo.equals("..")) {
      return null;
    }

    // pull off remaining-path part and normalize
    String path = input.substring(i, input.length());
    path = URI.create(path).normalize().toString();

    // path must not contain any relative tokens
    if (path.contains("/..")) {
      return null;
    }

    return new RepositoryPath(repo, path);
  }
}
