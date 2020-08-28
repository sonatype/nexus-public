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
package org.sonatype.nexus.repository.content.npm.internal.search.legacy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.npm.internal.NpmFacetSupport;
import org.sonatype.nexus.repository.npm.internal.NpmPackageParser;
import org.sonatype.nexus.repository.npm.internal.search.legacy.NpmSearchIndexFacet;
import org.sonatype.nexus.repository.npm.internal.search.legacy.NpmSearchIndexFilter;
import org.sonatype.nexus.repository.npm.orient.internal.search.legacy.NpmSearchIndexInvalidatedEvent;
import org.sonatype.nexus.repository.view.Content;

import org.joda.time.DateTime;

/**
 * npm search index facet for repository types that do their own caching of index document (currently all except
 * proxies).
 *
 * @since 3.0
 * @deprecated No longer actively used by npm upstream, replaced by v1 search api (NEXUS-13150).
 */
@Deprecated
public abstract class NpmSearchIndexFacetCaching
    extends NpmFacetSupport
    implements NpmSearchIndexFacet
{
  private final EventManager eventManager;

  protected NpmSearchIndexFacetCaching(final EventManager eventManager, final NpmPackageParser npmPackageParser) {
    super(npmPackageParser);
    this.eventManager = eventManager;
  }

  /**
   * Fetches the cached index document, or, if not present, builds index document, caches it and sends it as response.
   */
  @Override
  @Nonnull
  public Content searchIndex(@Nullable final DateTime since) throws IOException {
    // NOTE: This has been split up into separate calls to realize different transactional behavior:
    // First attempt restricted to cache, tolerating frozen database ...
    Content searchIndex = getCachedSearchIndex();
    if (searchIndex == null) {
      // ... second attempt allowed to create the index, requiring unfrozen database though
      searchIndex = getSearchIndex();
    }
    return NpmSearchIndexFilter.filterModifiedSince(searchIndex, since);
  }

  @Nullable
  protected Content getCachedSearchIndex() throws IOException {
    return getSearchIndex(true);
  }

  protected Content getSearchIndex() throws IOException {
    return getSearchIndex(false);
  }

  @Nullable
  private Content getSearchIndex(final boolean fromCacheOnly) throws IOException {
    Optional<FluentAsset> packageRootAsset = findRepositoryRootAsset();
    if (packageRootAsset.isPresent()) {
      return packageRootAsset.get().download();
    }

    if (fromCacheOnly) {
      return null;
    }

    log.debug("Building npm index for {}", getRepository().getName());
    final Path path = Files.createTempFile("npm-searchIndex", "json");
    try {
      Content content = buildIndex(path);
      return content().putSearchIndex(content);
    }
    finally {
      Files.delete(path);
    }
  }

  /**
   * Invalidates cached index document, by deleting it.
   */
  @Override
  public void invalidateCachedSearchIndex() {
    deleteAsset();
  }

  protected void deleteAsset() {
    log.debug("Invalidating cached npm index of {}", getRepository().getName());
    findRepositoryRootAsset().ifPresent(asset -> {
      if (asset.delete()) {
        eventManager.post(new NpmSearchIndexInvalidatedEvent(getRepository()));
      }
    });
  }

  /**
   * Builds the full index document of repository.
   */
  @Nonnull
  protected abstract Content buildIndex(final Path path)
      throws IOException;
}
