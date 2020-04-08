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

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.content.ContentRepository;
import org.sonatype.nexus.transaction.Transactional;

import com.google.inject.assistedinject.Assisted;

/**
 * {@link ContentRepository} store.
 *
 * @since 3.21
 */
@Named
public class ContentRepositoryStore<T extends ContentRepositoryDAO>
    extends ContentStoreSupport<T>
{
  @Inject
  public ContentRepositoryStore(final DataSessionSupplier sessionSupplier,
                                @Assisted final String contentStoreName,
                                @Assisted final Class<T> daoClass)
  {
    super(sessionSupplier, contentStoreName, daoClass);
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
  public void updateContentRepositoryAttributes(final ContentRepository contentRepository) {
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
