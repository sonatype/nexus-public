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
import java.util.Optional;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.content.ContentRepository;
import org.sonatype.nexus.transaction.Transactional;

/**
 * {@link ContentRepository} store.
 *
 * @since 3.next
 */
public abstract class ContentRepositoryStore<T extends ContentRepositoryDAO>
    extends ContentStoreSupport<T>
{
  public ContentRepositoryStore(final DataSessionSupplier sessionSupplier, final String storeName) {
    super(sessionSupplier, storeName);
  }

  /**
   * Browse all content repositories in the content data store.
   */
  @Transactional
  public Collection<ContentRepository> browseContentRepositories() {
    return dao().browseContentRepositories();
  }

  /**
   * Creates the given content repository in the content data store.
   *
   * @param contentRepository the repository to create
   */
  @Transactional
  public void createContentRepository(final ContentRepositoryData contentRepository) {
    dao().createContentRepository(contentRepository);
  }

  /**
   * Retrieves a content repository from the content data store based on its config identity.
   *
   * @param configRepositoryId the config repository id
   * @return content repository if it was found
   */
  @Transactional
  public Optional<ContentRepository> readContentRepository(final EntityId configRepositoryId) {
    return dao().readContentRepository(configRepositoryId);
  }

  /**
   * Updates the attributes of the given content repository in the content data store.
   *
   * @param contentRepository the content repository to update
   */
  @Transactional
  public void updateContentRepositoryAttributes(final ContentRepositoryData contentRepository) {
    dao().updateContentRepositoryAttributes(contentRepository);
  }

  /**
   * Deletes a content repository from the content data store based on its config identity.
   *
   * @param configRepositoryId the config repository id
   * @return {@code true} if the content repository was deleted
   */
  @Transactional
  public boolean deleteContentRepository(final EntityId configRepositoryId) {
    return dao().deleteContentRepository(configRepositoryId);
  }
}
