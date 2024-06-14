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

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryCreatedEvent;
import org.sonatype.nexus.repository.manager.RepositoryDeletedEvent;

import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_ENABLED;
import static org.sonatype.nexus.common.app.FeatureFlags.REPOSITORY_SIZE_ENABLED;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

/**
 * Maintain mapping of repository name and id
 */
@Named
@Singleton
@FeatureFlag(name = DATASTORE_ENABLED)
@FeatureFlag(name = REPOSITORY_SIZE_ENABLED, enabledByDefault = true)
public class RepositoryNameIdMappingCache
    extends ComponentSupport
    implements EventAware
{
  private volatile Map<String, Integer> nameRepositoryIdMap;

  private final ContentRepositoryStore<?> contentRepositoryStore;

  private final List<String> formatNames;

  private boolean isAvailable;

  @Inject
  public RepositoryNameIdMappingCache(
      FormatStoreManager formatStoreManager,
      final List<Format> formats,
      final DatabaseCheck databaseCheck)
  {
    this.contentRepositoryStore = checkNotNull(formatStoreManager).contentRepositoryStore(DEFAULT_DATASTORE_NAME);
    this.formatNames = checkNotNull(formats).stream().map(Format::getValue).collect(Collectors.toList());
    this.isAvailable = checkNotNull(databaseCheck).isPostgresql();
  }

  @Subscribe
  public void on(final RepositoryCreatedEvent event) {
    if(!isAvailable){
      return;
    }
    Repository repository = event.getRepository();
    log.debug("Handling repository create event for {}", repository.getName());
    fetchRepositoryId(repository.getName(), repository.getFormat().getValue());
  }

  @Subscribe
  public void on(final RepositoryDeletedEvent event) {
    if(!isAvailable){
      return;
    }
    log.debug("Handling repository deleted event for {}", event.getRepository().getName());
    if (nameRepositoryIdMap != null) {
      nameRepositoryIdMap.remove(event.getRepository().getName());
    }
  }

  public Map<Integer, String> getRepositoryNameIds(List<String> repositoryNames, String format) {
    return repositoryNames.stream()
        .map(name -> new AbstractMap.SimpleEntry<>(fetchRepositoryId(name, format), name))
        .filter(entry -> entry.getKey().isPresent())
        .collect(Collectors.toMap(entry -> entry.getKey().getAsInt(), AbstractMap.SimpleEntry::getValue));
  }

  private Map<String, Integer> getCache() {
    Map<String, Integer> cache = nameRepositoryIdMap;
    if (cache == null) {
      synchronized (this) {
        if (nameRepositoryIdMap == null) {
          nameRepositoryIdMap = populateCache();
        }
        cache = nameRepositoryIdMap;
      }
    }
    return cache;
  }

  private OptionalInt fetchRepositoryId(String name, String format) {
    if (getCache().containsKey(name)) {
      return OptionalInt.of(getCache().get(name));
    }
    Optional<Map<String, Object>> repositoryNameId = contentRepositoryStore.readContentRepositoryId(format, name);
    if (repositoryNameId.isPresent() && repositoryNameId.get().containsValue(name)) {
      int repositoryId = (Integer) repositoryNameId.get().get("repository_id");
      nameRepositoryIdMap.put(name, repositoryId);
      return OptionalInt.of(repositoryId);
    }
    else {
      return OptionalInt.empty();
    }
  }

  private Map<String, Integer> populateCache() {
    Map<String, Integer> cache = new ConcurrentHashMap<>();
    List<Map<String, Object>> ids = contentRepositoryStore.readAllContentRepositoryIds(this.formatNames);
    ids.stream().forEach(id -> cache.put((String) id.get("name"), (Integer) id.get("repository_id")));
    return cache;
  }
}
