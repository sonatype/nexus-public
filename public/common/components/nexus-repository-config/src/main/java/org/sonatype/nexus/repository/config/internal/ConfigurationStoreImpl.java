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
package org.sonatype.nexus.repository.config.internal;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.common.app.trash.TrashMigrationHelper;
import org.sonatype.nexus.datastore.ConfigStoreSupport;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationDAO;
import org.sonatype.nexus.repository.config.ConfigurationStore;
import org.sonatype.nexus.transaction.Transactional;

import org.apache.commons.collections4.CollectionUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.sonatype.nexus.common.collect.MoreCollections.toImmutableList;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * MyBatis {@link ConfigurationStore} implementation.
 *
 * <p>
 * Several public methods here deliberately self-invoke {@code @Transactional protected} helpers -- e.g.
 * {@code list()} calls {@code listActive()}/{@code listAll()}, {@code exists()} calls {@code doExists()},
 * and {@code create()}/{@code update()} call {@code persistCreate()}/{@code persistUpdate()} plus a separate
 * {@code refreshPersistedAttributes()} transaction. This is intentional and correct: {@code nexus-transaction}
 * applies transactional advice via AspectJ load-time weaving (not Spring's dynamic proxies), so the advice
 * fires on the real method invocation even when called from within the same class -- self-invocation is
 * intercepted, unlike Spring's proxy-based {@code @Transactional}.
 * </p>
 *
 * @since 3.21
 */
@Component
@Qualifier("mybatis")
public class ConfigurationStoreImpl
    extends ConfigStoreSupport<ConfigurationDAO>
    implements ConfigurationStore
{
  private static final Logger log = LoggerFactory.getLogger(ConfigurationStoreImpl.class);

  @Autowired
  public ConfigurationStoreImpl(final DataSessionSupplier sessionSupplier) {
    super(sessionSupplier);
  }

  @Override
  public Configuration newConfiguration() {
    return new ConfigurationData();
  }

  @Override
  public List<Configuration> list() {
    try {
      return listActive();
    }
    catch (Exception e) {
      if (TrashMigrationHelper.isMissingDeletedOnColumn(e)) {
        log.debug("Trash column 'deleted_on' not yet migrated; falling back to unfiltered browse");
        return listAll();
      }
      throw e;
    }
  }

  @Transactional
  protected List<Configuration> listActive() {
    return toImmutableList(dao().browse());
  }

  @Transactional
  protected List<Configuration> listAll() {
    return toImmutableList(dao().browseAll());
  }

  @Override
  public void create(final Configuration configuration) {
    persistCreate(configuration);
    refreshPersistedAttributes(configuration);
  }

  @Transactional
  protected void persistCreate(final Configuration configuration) {
    postCommitEvent(() -> new ConfigurationCreatedEvent((ConfigurationData) configuration));
    dao().create((ConfigurationData) configuration);
  }

  @Override
  public void update(final Configuration configuration) {
    persistUpdate(configuration);
    refreshPersistedAttributes(configuration);
  }

  @Transactional
  protected void persistUpdate(final Configuration configuration) {
    postCommitEvent(() -> new ConfigurationUpdatedEvent((ConfigurationData) configuration));
    dao().update((ConfigurationData) configuration);
  }

  /**
   * Refreshes the in-memory attributes from the persisted row after a create/update. Runs in its own
   * transaction, separate from the write, with a ZDU fallback: the normal read filters on
   * {@code deleted_on} (which may not exist yet during a rolling upgrade); on that specific failure it
   * retries with a read that ignores trash. Keeping this out of the write transaction is essential
   * on PostgreSQL, where any failed statement aborts the whole transaction — so a read-back that referenced
   * the not-yet-migrated column would otherwise roll back the create/update itself (NEXUS-51733).
   */
  private void refreshPersistedAttributes(final Configuration configuration) {
    try {
      doRefreshPersistedAttributes(configuration);
    }
    catch (Exception e) {
      if (TrashMigrationHelper.isMissingDeletedOnColumn(e)) {
        log.debug(
            "Trash column 'deleted_on' not yet migrated; reading back attributes without trash filter");
        doRefreshPersistedAttributesIgnoringTrash(configuration);
      }
      else {
        throw e;
      }
    }
  }

  @Transactional
  protected void doRefreshPersistedAttributes(final Configuration configuration) {
    dao().read(configuration.getRepositoryId())
        .ifPresent(persisted -> configuration.setAttributes(persisted.getAttributes()));
  }

  @Transactional
  protected void doRefreshPersistedAttributesIgnoringTrash(final Configuration configuration) {
    dao().readIgnoringTrash(configuration.getRepositoryId())
        .ifPresent(persisted -> configuration.setAttributes(persisted.getAttributes()));
  }

  @Transactional
  @Override
  public void delete(final Configuration configuration) {
    postCommitEvent(() -> new ConfigurationDeletedEvent((ConfigurationData) configuration));
    dao().deleteByName(configuration.getRepositoryName());
  }

  @Override
  public Collection<Configuration> readByNames(final Set<String> repositoryNames) {
    try {
      return doReadByNames(repositoryNames);
    }
    catch (Exception e) {
      if (TrashMigrationHelper.isMissingDeletedOnColumn(e)) {
        log.debug(
            "Trash column 'deleted_on' not yet migrated; falling back to name-scoped read ignoring trash for readByNames");
        return doReadByNamesIgnoringTrash(repositoryNames);
      }
      throw e;
    }
  }

  @Transactional
  protected Collection<Configuration> doReadByNames(final Set<String> repositoryNames) {
    return ofNullable(repositoryNames)
        .filter(CollectionUtils::isNotEmpty)
        .map(names -> dao().readByNames(names))
        .orElse(emptyList());
  }

  @Transactional
  protected Collection<Configuration> doReadByNamesIgnoringTrash(final Set<String> repositoryNames) {
    return ofNullable(repositoryNames)
        .filter(CollectionUtils::isNotEmpty)
        .map(names -> dao().readByNamesIgnoringTrash(names))
        .orElse(emptyList());
  }

  @Override
  public Optional<Configuration> readByName(final String name) {
    try {
      return doReadByName(name);
    }
    catch (Exception e) {
      if (TrashMigrationHelper.isMissingDeletedOnColumn(e)) {
        log.debug(
            "Trash column 'deleted_on' not yet migrated; falling back to name-scoped read ignoring trash for readByName");
        return doReadByNameIgnoringTrash(name);
      }
      throw e;
    }
  }

  @Transactional
  protected Optional<Configuration> doReadByName(final String name) {
    return dao().readByName(name).map(Configuration.class::cast);
  }

  @Transactional
  protected Optional<Configuration> doReadByNameIgnoringTrash(final String name) {
    return dao().readByNameIgnoringTrash(name).map(Configuration.class::cast);
  }

  @Override
  public boolean exists(final String repositoryName) {
    try {
      return doExists(repositoryName);
    }
    catch (Exception e) {
      if (TrashMigrationHelper.isMissingDeletedOnColumn(e)) {
        log.debug("Trash column 'deleted_on' not yet migrated; falling back to unfiltered existence check");
        return doExistsIgnoringTrash(repositoryName);
      }
      throw e;
    }
  }

  @Transactional
  protected boolean doExists(final String repositoryName) {
    return dao().readByName(repositoryName).isPresent();
  }

  @Transactional
  protected boolean doExistsIgnoringTrash(final String repositoryName) {
    return dao().readByNameIgnoringTrash(repositoryName).isPresent();
  }

  @Override
  public Collection<Configuration> readByRecipe(final String recipeName) {
    try {
      return doReadByRecipe(recipeName);
    }
    catch (Exception e) {
      if (TrashMigrationHelper.isMissingDeletedOnColumn(e)) {
        log.debug(
            "Trash column 'deleted_on' not yet migrated; falling back to recipe-scoped read ignoring trash for readByRecipe");
        return doReadByRecipeIgnoringTrash(recipeName);
      }
      throw e;
    }
  }

  @Transactional
  protected Collection<Configuration> doReadByRecipe(final String recipeName) {
    return dao().readByRecipe(recipeName);
  }

  @Transactional
  protected Collection<Configuration> doReadByRecipeIgnoringTrash(final String recipeName) {
    return dao().readByRecipeIgnoringTrash(recipeName);
  }

  @Transactional
  @Override
  public void trash(final String repositoryName, final String deletedBy) {
    dao().trash(repositoryName, deletedBy);
    postCommitEvent(() -> new ConfigurationTrashedEvent(repositoryName));
  }

  @Transactional
  @Override
  public void undoTrash(final String repositoryName) {
    dao().undoTrash(repositoryName);
    postCommitEvent(() -> {
      ConfigurationCreatedEvent event = new ConfigurationCreatedEvent();
      event.setRepositoryName(repositoryName);
      return event;
    });
  }

  @Transactional
  @Override
  public List<Configuration> browseDeleted(final int limit, @Nullable final String continuationToken) {
    return toImmutableList(dao().browseDeleted(limit, continuationToken));
  }

  @Transactional
  @Override
  public int countDeleted() {
    return dao().countDeleted();
  }

  @Override
  public Configuration readDeleted(final String repositoryName) {
    try {
      return doReadDeleted(repositoryName);
    }
    catch (Exception e) {
      if (TrashMigrationHelper.isMissingDeletedOnColumn(e)) {
        log.debug("Trash column 'deleted_on' not yet migrated; readDeleted returning null");
        return null;
      }
      throw e;
    }
  }

  @Transactional
  protected Configuration doReadDeleted(final String repositoryName) {
    return dao().readDeleted(repositoryName);
  }

  @Override
  public boolean existsDeletedByNameCaseInsensitive(final String repositoryName) {
    try {
      return doExistsDeletedByNameCaseInsensitive(repositoryName);
    }
    catch (Exception e) {
      if (TrashMigrationHelper.isMissingDeletedOnColumn(e)) {
        log.debug(
            "Trash column 'deleted_on' not yet migrated; existsDeletedByNameCaseInsensitive returning false");
        return false;
      }
      throw e;
    }
  }

  @Transactional
  protected boolean doExistsDeletedByNameCaseInsensitive(final String repositoryName) {
    return !dao().readDeletedByNameCaseInsensitive(repositoryName).isEmpty();
  }

  @Transactional
  @Override
  public void hardDelete(final String repositoryName) {
    dao().hardDelete(repositoryName);
  }

  @Override
  public List<String> readDeletedNamesBefore(final int retentionDays) {
    try {
      return doReadDeletedNamesBefore(retentionDays);
    }
    catch (Exception e) {
      if (TrashMigrationHelper.isMissingDeletedOnColumn(e)) {
        log.debug("Trash column 'deleted_on' not yet migrated; readDeletedNamesBefore returning empty");
        return emptyList();
      }
      throw e;
    }
  }

  @Transactional
  protected List<String> doReadDeletedNamesBefore(final int retentionDays) {
    return dao().readDeletedNamesBefore(retentionDays);
  }

  @Override
  public int countByBlobStore(final String blobStoreName) {
    try {
      return doCountByBlobStore(blobStoreName);
    }
    catch (Exception e) {
      if (TrashMigrationHelper.isMissingDeletedOnColumn(e)) {
        log.debug(
            "Trash column 'deleted_on' not yet migrated; falling back to blob-store count ignoring trash");
        return doCountByBlobStoreIgnoringTrash(blobStoreName);
      }
      throw e;
    }
  }

  @Transactional
  protected int doCountByBlobStore(final String blobStoreName) {
    return dao().countByBlobStore(blobStoreName);
  }

  @Transactional
  protected int doCountByBlobStoreIgnoringTrash(final String blobStoreName) {
    return dao().countByBlobStoreIgnoringTrash(blobStoreName);
  }

  @Override
  @Transactional
  public int countByBlobStoreIncludingTrashed(final String blobStoreName) {
    // Reuses the no-deleted_on-predicate query, which counts live AND trashed repositories, so a trashed
    // (restorable) repository still marks its blob store as in-use (NEXUS-55233). No ZDU fallback needed:
    // the query never references the possibly-not-yet-migrated deleted_on column.
    return dao().countByBlobStoreIgnoringTrash(blobStoreName);
  }
}
