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
package org.sonatype.nexus.repository.manager;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.sonatype.goodies.lifecycle.Lifecycle;
import org.sonatype.nexus.repository.Recipe;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;

/**
 * Repository manager.
 *
 * @since 3.0
 */
public interface RepositoryManager
  extends Lifecycle
{
  Iterable<Repository> browse();

  /**
   * @since 3.6.1
   */
  Iterable<Repository> browseForBlobStore(String blobStoreId);

  boolean exists(String name);

  @Nullable
  Repository get(String name);

  Repository create(Configuration configuration) throws Exception;

  Repository update(Configuration configuration) throws Exception;

  void delete(String name) throws Exception;

  boolean isBlobstoreUsed(String blobStoreName);

  long blobstoreUsageCount(String blobStoreName);

  List<String> findContainingGroups(String repositoryName);

  Stream<Repository> browseForCleanupPolicy(final String cleanupPolicyName);

  Collection<Recipe> getAllSupportedRecipes();
}
