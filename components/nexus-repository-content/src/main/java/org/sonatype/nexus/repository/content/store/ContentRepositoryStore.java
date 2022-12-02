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
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.content.AttributeOperation;
import org.sonatype.nexus.repository.content.ContentRepository;
import org.sonatype.nexus.repository.content.event.repository.ContentRepositoryAttributesDesEvent;
import org.sonatype.nexus.repository.content.event.repository.ContentRepositoryAttributesEvent;
import org.sonatype.nexus.repository.content.event.repository.ContentRepositoryCreatedEvent;
import org.sonatype.nexus.repository.content.event.repository.ContentRepositoryDeletedEvent;
import org.sonatype.nexus.repository.content.event.repository.ContentRepositoryPreDeleteEvent;
import org.sonatype.nexus.repository.content.facet.ContentFacetFinder;
import org.sonatype.nexus.transaction.Transactional;

import com.google.inject.assistedinject.Assisted;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.content.AttributesHelper.applyAttributeChange;

/**
 * {@link ContentRepository} store.
 *
 * @since 3.21
 */
@Named
public class ContentRepositoryStore<T extends ContentRepositoryDAO>
    extends ContentStoreEventSupport<T>
{
  private ContentFacetFinder contentFacetFinder;

  @Inject
  public ContentRepositoryStore(
      final DataSessionSupplier sessionSupplier,
      final ContentFacetFinder contentFacetFinder,
      @Assisted final String contentStoreName,
      @Assisted final Class<T> daoClass)
  {
    super(sessionSupplier, contentStoreName, daoClass);
    this.contentFacetFinder = checkNotNull(contentFacetFinder);
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

    postCommitEvent(() -> new ContentRepositoryCreatedEvent(contentRepository));
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
  public void updateContentRepositoryAttributes(
      final ContentRepository contentRepository,
      final AttributeOperation change,
      final String key,
      final @Nullable Object value)
  {
    // reload latest attributes, apply change, then update database if necessary
    dao().readContentRepositoryAttributes(contentRepository).ifPresent(attributes -> {
      ((ContentRepositoryData) contentRepository).setAttributes(attributes);

      if (applyAttributeChange(attributes, change, key, value)) {
        dao().updateContentRepositoryAttributes(contentRepository);

        postCommitEvent(() -> new ContentRepositoryAttributesEvent(contentRepository, change, key, value));
        contentFacetFinder.findRepository(this.format, contentRepository)
            .ifPresent(repository -> {
              String repoName = repository.getName();
              postCommitEvent(() -> new ContentRepositoryAttributesDesEvent(repoName));
            });
      }
    });
  }

  /**
   * Deletes a content repository from the content data store.
   *
   * @param contentRepository the content repository to delete
   * @return {@code true} if the content repository was deleted
   */
  @Transactional
  public boolean deleteContentRepository(final ContentRepository contentRepository) {
    preCommitEvent(() -> new ContentRepositoryPreDeleteEvent(contentRepository));

    boolean deleted = dao().deleteContentRepository(contentRepository);
    if (deleted) {
      postCommitEvent(() -> new ContentRepositoryDeletedEvent(contentRepository, this.format));
    }
    return deleted;
  }

  /**
   * Deletes a content repository from the content data store based on its config identity.
   *
   * @param configRepositoryId the config repository id
   * @return {@code true} if the content repository was deleted
   */
  @Transactional
  public boolean deleteContentRepository(final EntityId configRepositoryId) {
    return dao().readContentRepository(configRepositoryId)
        .map(this::deleteContentRepository)
        .orElse(false);
  }
}
