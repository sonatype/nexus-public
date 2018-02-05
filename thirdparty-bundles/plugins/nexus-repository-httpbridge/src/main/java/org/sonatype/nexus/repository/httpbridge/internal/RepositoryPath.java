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

import org.sonatype.nexus.repository.BadRequestException;

import org.apache.commons.io.FilenameUtils;

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

  /**
   * Validate and parse the path.
   *
   * @throws BadRequestException if validation fails
   *
   * @return The parsed path
   */
  public static RepositoryPath parse(final String input) {
    String repo = validateAndExtractRepo(input);
    String path = validateAndExtractPath(input);
    return new RepositoryPath(repo, path);
  }

  private static String validateAndExtractRepo(final String input) {
    if (input == null || input.isEmpty()) {
      throw new BadRequestException("Repository path must not be null or empty");
    }

    if (!(input.charAt(0) == '/')) {
      throw new BadRequestException("Repository path must start with '/'");
    }

    int i = input.indexOf('/', 1);
    if (i == -1) {
      throw new BadRequestException("Repository path must have another '/' after initial '/'");
    }

    String repo = input.substring(1, i);
    if (".".equals(repo) || "..".equals(repo)) {
      throw new BadRequestException("Repository path must not contain a relative token");
    }

    return repo;
  }

  private static String validateAndExtractPath(final String input) {
    String path = input.substring(input.indexOf('/', 1), input.length());
    path = FilenameUtils.normalize(path, true); //unixSeparator:true is necessary to make this work on Windows.
    if (path == null) {
      throw new BadRequestException("Repository path must not contain a relative token");
    }
    return path;
  }
}
