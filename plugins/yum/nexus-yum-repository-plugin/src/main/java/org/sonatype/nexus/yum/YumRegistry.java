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
package org.sonatype.nexus.yum;

import java.io.File;

import javax.annotation.Nullable;

import org.sonatype.nexus.proxy.maven.MavenRepository;

/**
 * @since yum 3.0
 */
public interface YumRegistry
{

  static final int DEFAULT_MAX_NUMBER_PARALLEL_THREADS = 10;

  static final String DEFAULT_CREATEREPO_PATH = "createrepo";

  static final String DEFAULT_MERGEREPO_PATH = "mergerepo";

  Yum register(MavenRepository repository);

  Yum unregister(String repositoryId);

  Yum get(String repositoryId);

  boolean isRegistered(String repositoryId);

  YumRegistry setMaxNumberOfParallelThreads(int maxNumberOfParallelThreads);

  int maxNumberOfParallelThreads();

  /**
   * @since 2.11
   */
  String getCreaterepoPath();

  /**
   * @param path path to "createrepo" (if null value will be reset to "createrepo")
   * @since 2.11
   */
  void setCreaterepoPath(final @Nullable String path);

  /**
   * @since 2.11
   */
  String getMergerepoPath();

  /**
   * @param path path to "mergerepo" (if null value will be reset to "mergerepo")
   * @since 2.11
   */
  void setMergerepoPath(final @Nullable String path);

  File getTemporaryDirectory();

}
