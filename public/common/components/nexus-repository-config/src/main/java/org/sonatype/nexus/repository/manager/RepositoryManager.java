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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.lifecycle.Lifecycle;
import org.sonatype.nexus.datastore.DataStoreUsageChecker;
import org.sonatype.nexus.repository.Recipe;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;

/**
 * Repository manager.
 *
 * @since 3.0
 */
public interface RepositoryManager
    extends DataStoreUsageChecker, Lifecycle
{
  Iterable<Repository> browse();

  /**
   * @since 3.6.1
   */
  Iterable<Repository> browseForBlobStore(String blobStoreId);

  boolean exists(String name);

  @Nullable
  Repository get(String name);

  /**
   * Returns a stream of the configurations for repositories registered with the manager. The Configuration objects
   * should not be modified. The associated repositories may not be started if invoked during startup.
   */
  Stream<Configuration> getConfigurations();

  @Nullable
  Repository softGet(String name);

  Optional<Configuration> retrieveConfigurationByName(String name);

  Repository create(Configuration configuration) throws Exception;

  Repository update(Configuration configuration) throws Exception;

  void delete(String name) throws Exception;

  /**
   * Soft-delete a repository: stop it, untrack it, and mark the configuration as deleted.
   * The configuration remains in the database and can be restored within the retention period.
   */
  void softDelete(String name, String deletedBy) throws Exception;

  /**
   * Restore a previously soft-deleted repository: re-instantiate, track, start,
   * and clear the soft-delete markers from the configuration.
   */
  Repository restoreFromSoftDelete(String name) throws Exception;

  /**
   * Hard-delete a soft-deleted repository: run full content cleanup (facet deletion),
   * remove secrets, and permanently delete the configuration.
   */
  void hardDelete(String name) throws Exception;

  boolean isBlobstoreUsed(String blobStoreName);

  long blobstoreUsageCount(String blobStoreName);

  /**
   * Returns the set of blob store names referenced by any repository's {@code storage.blobStoreName}, read
   * authoritatively from the shared configuration store in a single query. Intended for callers that need the
   * in-use state of many blob stores at once (e.g. the blob store list UI) without an {@link #isBlobstoreUsed}
   * call per blob store. See NEXUS-54229.
   *
   * @apiNote This covers repository usage only; it does NOT include blob stores referenced solely as members of
   *          a group blob store (that is {@code blobStoreManager.blobStoreUsageCount()}). Callers deciding whether
   *          a blob store is deletable must combine both, exactly as {@link #isBlobstoreUsed} does.
   */
  Set<String> blobstoresInUse();

  /**
   * Returns a set of group repositories who directly or transitively contain the specified repository
   */
  Set<String> findContainingGroups(String repositoryName);

  Stream<Repository> browseForCleanupPolicy(final String cleanupPolicyName);

  Collection<Recipe> getAllSupportedRecipes();

  /**
   * Create a new {@link Configuration} instance.
   *
   * @since 3.21
   */
  Configuration newConfiguration();

  int count();
}
