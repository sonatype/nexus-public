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
package org.sonatype.nexus.repository.config;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.lifecycle.Lifecycle;

/**
 * {@link Configuration} store.
 *
 * @since 3.0
 */
public interface ConfigurationStore
    extends Lifecycle
{
  List<Configuration> list();

  void create(Configuration configuration);

  void update(Configuration configuration);

  void delete(Configuration configuration);

  /**
   * Create a new {@link Configuration} instance.
   *
   * @since 3.21
   */
  Configuration newConfiguration();

  Collection<Configuration> readByNames(Set<String> repositoryNames);

  /**
   * Reads a single {@link Configuration} by its exact name (repository names are stored lowercased). Unlike
   * {@link #readByNames}, whose SQL uses {@code LIKE}, this is an exact match, so a name containing a LIKE
   * metacharacter (e.g. {@code _}) cannot match a different repository. See NEXUS-54229.
   */
  Optional<Configuration> readByName(String name);

  /**
   * Checks whether the database contains an entry with the provided name.<br>
   *
   * Note: Underlying implementations may return {@code false} if unsupported.
   */
  boolean exists(String repositoryName);

  /**
   * Read all repository configuration by given {@code recipeName}
   * 
   * @param recipeName recipe name for collecting configurations
   * @return configurations filtered by {@code recipeName}
   */
  Collection<Configuration> readByRecipe(String recipeName);

  /**
   * Mark a repository configuration as soft-deleted.
   */
  void softDelete(String repositoryName, String deletedBy);

  /**
   * Restore a soft-deleted repository configuration.
   */
  void undoSoftDelete(String repositoryName);

  /**
   * Browse soft-deleted repository configurations with pagination.
   */
  List<Configuration> browseDeleted(int limit, @Nullable String continuationToken);

  /**
   * @return the count of soft-deleted repository configurations
   */
  int countDeleted();

  /**
   * Read a soft-deleted repository configuration by name.
   */
  Configuration readDeleted(String repositoryName);

  /**
   * Returns {@code true} if a soft-deleted repository configuration exists whose name matches
   * {@code repositoryName} ignoring case. Repository names are unique case-insensitively, so this guards
   * against creating (or restoring) a repository whose name would collide with one still in Trash.
   */
  boolean existsDeletedByNameCaseInsensitive(String repositoryName);

  /**
   * Permanently remove a soft-deleted repository configuration.
   */
  void hardDelete(String repositoryName);

  /**
   * Read the names of all repositories soft-deleted more than {@code retentionDays} ago.
   */
  List<String> readDeletedNamesBefore(int retentionDays);

  /**
   * Counts repositories configured to use the given blob store, read authoritatively from the shared store.
   * <p>
   * Unlike an in-memory repository-cache scan this reflects the committed cross-node state, so it is correct
   * even while a peer node's repository create/delete has not yet been replicated locally. See NEXUS-54229.
   *
   * @param blobStoreName the blob store name to match
   * @return the number of repositories configured to use the blob store
   */
  int countByBlobStore(String blobStoreName);
}
