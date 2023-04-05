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
package org.sonatype.nexus.repository.content.kv;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.datastore.api.ContentDataAccess;
import org.sonatype.nexus.datastore.api.Expects;
import org.sonatype.nexus.datastore.api.SchemaTemplate;
import org.sonatype.nexus.repository.content.store.ContentRepositoryDAO;

import org.apache.ibatis.annotations.Param;

/**
 * Used to store arbitrary key-value types in a category for a specific repository
 */
@Expects({ ContentRepositoryDAO.class })
@SchemaTemplate("format")
public interface KeyValueDAO
    extends ContentDataAccess
{
  /**
   * Get the value for a specified key
   *
   * @param repositoryId the repository for the scope
   * @param category the storage category
   * @param key the key of the data
   * @return the result if it exists
   */
  Optional<String> get(
      @Param("repositoryId") int repositoryId,
      @Param("category") String category,
      @Param("key") String key);

  /**
   * Browse the key-value pairs in the specified repository with the provided category.
   *
   * @param repositoryId the repository for the scope
   * @param category the storage category
   * @param limit the page size
   * @param continuationToken the continuation token
   *
   * @return the page of results
   */
  Continuation<KeyValue> browse(@Param("repositoryId") int repositoryId,
      @Param("category") String category,
      @Param("limit") int limit,
      @Nullable @Param("continuationToken") String continuationToken);

  /**
   * Browse the categories available for the repository.
   *
   * @param repositoryId the repositoryId
   *
   * @return a distinct list of categories.
   */
  List<String> browseCategories(@Param("repositoryId") int repositoryId);

  /**
   * Count the number of key-value pairs in the specified repository in the category.
   *
   * @param repositoryId  the repository for the scope
   * @param category the category to count the entries for
   * @return
   */
  int count(@Param("repositoryId") int repositoryId, @Param("category") String category);

  /**
   * Find categories which contain the provided key.
   *
   * @param key the key to find
   *
   * @return a list of categories
   */
  List<String> findCategories(@Param("repositoryId") final int repositoryId, @Param("key") String key);

  List<KeyValue> findByCategoryAndKeyLike(
      @Param("repositoryId") int repositoryId,
      @Nullable @Param("category") String category,
      @Param("keyLike") String keyLike
  );

  /**
   * Set the value for a specific key in the provided category for the repository.
   *
   * @param repositoryId the repository for the scope
   * @param category the storage category
   * @param key the key for the data
   * @param value the data to store
   */
  void set(
      @Param("repositoryId") int repositoryId,
      @Param("category") String category,
      @Param("key") String key,
      @Param("value") String value);

  /**
   * Remove the data associated with the specific category and key.
   *
   * @param repositoryId the repository for the scope
   * @param category the storage category
   * @param key the key for the data
   */
  void remove(@Param("repositoryId") int repositoryId, @Param("category") String category, @Param("key") String key);

  /**
   * Remove all keys of the specified category associated with the repository.
   *
   * @param repositoryId the repository to scope the changes for
   * @param category the storage category
   * @param limit the maximum number of records to delete
   *
   * @return the number of records removed
   */
  int removeAll(@Param("repositoryId") int repositoryId, @Nullable @Param("category") String category, @Param("limit") int limit);

  /**
   * Remove all data associated with a repository
   *
   * @param repositoryId the repository to remove content for
   * @param limit the maximum number of records to delete
   *
   * @return the number of records removed
   */
  int removeRepository(@Param("repositoryId") int repositoryId, @Param("limit") int limit);
}
