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
package org.sonatype.nexus.repository.content.store;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.datastore.api.ContentDataAccess;
import org.sonatype.nexus.datastore.api.SchemaTemplate;
import org.sonatype.nexus.repository.content.ContentRepository;

import org.apache.ibatis.annotations.Param;

/**
 * Content repository {@link ContentDataAccess}.
 *
 * @since 3.20
 */
@SchemaTemplate("format")
public interface ContentRepositoryDAO
    extends ContentDataAccess
{
  /**
   * Browse all content repositories in the content data store.
   */
  Collection<ContentRepository> browseContentRepositories();

  /**
   * Creates the given content repository in the content data store.
   *
   * @param contentRepository the repository to create
   */
  void createContentRepository(ContentRepositoryData contentRepository);

  /**
   * Retrieves a content repository from the content data store based on its config identity.
   *
   * @param configRepositoryId the config repository id
   * @return content repository if it was found
   */
  Optional<ContentRepository> readContentRepository(@Param("configRepositoryId") EntityId configRepositoryId);

  /**
   * Retrieves the latest attributes of the given content repository in the content data store.
   *
   * @param contentRepository the content repository to read
   * @return repository attributes if found
   */
  Optional<NestedAttributesMap> readContentRepositoryAttributes(ContentRepository contentRepository);

  /**
   * Updates the attributes of the given content repository in the content data store.
   *
   * @param contentRepository the content repository to update
   */
  void updateContentRepositoryAttributes(ContentRepository contentRepository);

  /**
   * Deletes a content repository from the content data store.
   *
   * @param contentRepository the content repository to delete
   * @return {@code true} if the content repository was deleted
   */
  boolean deleteContentRepository(ContentRepository contentRepository);

  /**
   * Retrieves the repository name and content repository id for the given name.
   *
   * @param repositoryFormat the format to retrieve
   * @return repository attributes if found
   */
  Optional<Map<String, Object>> readContentRepositoryId(
      @Param("repositoryFormat") String repositoryFormat,
      @Param("repositoryName") String repositoryName);

  /**
   * Retrieves the repository names and content repository ids.
   *
   * @param repositoryFormats the list formats to retrieve
   * @return repository attributes if found
   */
  List<Map<String, Object>> readAllContentRepositoryIds(@Param("repositoryFormats") List<String> repositoryFormats);
}
