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

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.datastore.api.IterableDataAccess;
import org.sonatype.nexus.repository.config.internal.ConfigurationData;

import org.apache.ibatis.annotations.Param;

/**
 * {@link ConfigurationData} access.
 *
 * @since 3.21
 */
public interface ConfigurationDAO
    extends IterableDataAccess.WithName<ConfigurationData>
{
  /**
   * Browse all repositories including soft-deleted ones.
   * Used as a fallback when the soft-delete columns have not yet been migrated.
   */
  Collection<ConfigurationData> browseAll();

  Collection<Configuration> readByNames(Set<String> repositoryNames);

  Collection<ConfigurationData> readByIds(Collection<EntityId> repositoryIds);

  Collection<Configuration> readByRecipe(String recipeName);

  /*
   * ZDU fallbacks: read variants that do NOT filter on {@code deleted_on}, for the pre-migration window
   * when the soft-delete columns may not yet exist. Pre-migration no repository can be soft-deleted, so
   * omitting the predicate is logically equivalent while avoiding a reference to the not-yet-created
   * column. These keep the id/name/recipe predicate (unlike a blind {@code browseAll()}), so callers that
   * fall back on {@link SoftDeleteMigrationHelper#isMissingDeletedOnColumn} still get correctly scoped
   * results rather than every repository.
   */

  /**
   * Read a repository by id, ignoring soft-delete (ZDU fallback for the create/update read-back).
   */
  Optional<ConfigurationData> readIgnoringSoftDelete(EntityId repositoryId);

  /**
   * Read a repository by exact name, ignoring soft-delete (ZDU fallback for the existence check).
   */
  Optional<ConfigurationData> readByNameIgnoringSoftDelete(String repositoryName);

  /**
   * Read repositories by name, ignoring soft-delete (ZDU fallback for {@code readByNames}).
   */
  Collection<Configuration> readByNamesIgnoringSoftDelete(Set<String> repositoryNames);

  /**
   * Read repositories by recipe, ignoring soft-delete (ZDU fallback for {@code readByRecipe}).
   */
  Collection<Configuration> readByRecipeIgnoringSoftDelete(String recipeName);

  /**
   * Read repositories by id, ignoring soft-delete (ZDU fallback for {@code readByIds}).
   */
  Collection<ConfigurationData> readByIdsIgnoringSoftDelete(Collection<EntityId> repositoryIds);

  /**
   * Mark a repository as soft-deleted.
   */
  void softDelete(
      @Param("name") String name,
      @Param("deletedBy") String deletedBy);

  /**
   * Undo soft-delete (restore) a repository by name.
   */
  void undoSoftDelete(@Param("name") String name);

  /**
   * Browse soft-deleted repositories with pagination.
   */
  Continuation<ConfigurationData> browseDeleted(
      @Param("limit") int limit,
      @Param("continuationToken") @Nullable String continuationToken);

  /**
   * Count all soft-deleted repositories.
   */
  int countDeleted();

  /**
   * Read a soft-deleted repository by name.
   */
  ConfigurationData readDeleted(@Param("name") String name);

  /**
   * Read soft-deleted repositories whose name matches the given name ignoring case.
   *
   * Repository names are unique case-insensitively, so this supports blocking creation (and restore) of a
   * repository whose name would collide, ignoring case, with a soft-deleted repository still in Trash.
   *
   * @param name the repository name to match ignoring case
   * @return matching soft-deleted repositories (empty if none)
   */
  List<ConfigurationData> readDeletedByNameCaseInsensitive(@Param("name") String name);

  /**
   * Permanently delete a soft-deleted repository.
   */
  void hardDelete(@Param("name") String name);

  /**
   * Read the names of all repositories soft-deleted before a given number of days ago.
   */
  List<String> readDeletedNamesBefore(@Param("retentionDays") int retentionDays);

  /**
   * Counts repositories whose {@code storage.blobStoreName} attribute matches the given blob store name.
   * <p>
   * Reads the shared configuration store directly, so the result is authoritative across HA nodes -- unlike an
   * in-memory repository-cache scan, which can lag a peer node's create/delete and produce a false "in use"
   * (blocking a legitimate blob store delete) or a false "not used" (allowing deletion of a blob store still
   * referenced by a not-yet-materialized repository). See NEXUS-54229.
   *
   * @param blobStoreName the blob store name to match
   * @return the number of repositories configured to use the blob store
   */
  int countByBlobStore(String blobStoreName);

  /**
   * ZDU fallback for {@link #countByBlobStore}: counts without the {@code deleted_on} predicate, used only
   * when the soft-delete columns are not yet migrated. Pre-migration no repository can be soft-deleted, so
   * this matches {@link #countByBlobStore} while remaining safe against the not-yet-added column.
   */
  int countByBlobStoreIgnoringSoftDelete(String blobStoreName);
}
