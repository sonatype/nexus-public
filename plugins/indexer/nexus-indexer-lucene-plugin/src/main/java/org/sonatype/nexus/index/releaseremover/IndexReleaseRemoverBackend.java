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
package org.sonatype.nexus.index.releaseremover;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.aether.util.version.GenericVersionScheme;
import org.sonatype.aether.version.InvalidVersionSpecificationException;
import org.sonatype.aether.version.Version;
import org.sonatype.aether.version.VersionScheme;
import org.sonatype.nexus.index.DefaultIndexerManager;
import org.sonatype.nexus.index.DefaultIndexerManager.Runnable;
import org.sonatype.nexus.index.IndexerManager;
import org.sonatype.nexus.maven.tasks.ReleaseRemovalRequest;
import org.sonatype.nexus.maven.tasks.ReleaseRemovalResult;
import org.sonatype.nexus.maven.tasks.ReleaseRemoverBackend;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.gav.Gav;
import org.sonatype.nexus.proxy.targets.Target;
import org.sonatype.nexus.util.PathUtils;
import org.sonatype.scheduling.TaskUtil;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.IteratorSearchResponse;
import org.apache.maven.index.MAVEN;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.expr.SourcedSearchExpression;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Release remover based on {@link IndexerManager} and Maven Indexer for collecting items to be inspected for removal.
 * Hence, depends on index state, and items not on index (for any reason), will not be considered for removal, nor will
 * participate in number of versions to keep calculations.
 *
 * @since 2.11.3
 */
@Named(ReleaseRemoverBackend.INDEX)
@Singleton
public class IndexReleaseRemoverBackend
    extends ComponentSupport
    implements ReleaseRemoverBackend
{
  private final DefaultIndexerManager indexerManager;

  private final VersionScheme versionScheme;

  @Inject
  public IndexReleaseRemoverBackend(final DefaultIndexerManager indexerManager)
  {
    this.indexerManager = checkNotNull(indexerManager);
    this.versionScheme = new GenericVersionScheme();
  }

  @Override
  public void removeReleases(final ReleaseRemovalRequest request,
                             final ReleaseRemovalResult result,
                             final MavenRepository repository,
                             final Target target)
      throws IOException
  {
    TaskUtil.checkInterruption();

    int deletedFiles = 0;
    boolean successful = true; // be optimistic
    try {
      // collect all known groupIds
      final Set<String> groupIds = Sets.newHashSet();
      indexerManager.shared(repository, new Runnable()
      {
        @Override
        public void run(final IndexingContext context) throws IOException {
          groupIds.addAll(context.getAllGroups());
        }
      });

      // iterator groupIds
      for (final String groupId : groupIds) {
        TaskUtil.checkInterruption();
        // collect all artifactIds for groupId
        final Set<String> artifactIds = Sets.newHashSet();
        final Query groupIdQ =
            indexerManager.constructQuery(MAVEN.GROUP_ID, new SourcedSearchExpression(groupId));
        try (IteratorSearchResponse searchResponse = indexerManager
            .searchQueryIterator(groupIdQ, repository.getId(), null, null, null, false, null)) {
          for (ArtifactInfo ai : searchResponse) {
            artifactIds.add(ai.artifactId);
          }
        }

        // iterate all artifactIds
        for (final String artifactId : artifactIds) {
          TaskUtil.checkInterruption();
          // collect all versions for GA
          final Set<Version> versions = Sets.newHashSet();
          final Query artifactIdQ =
              indexerManager.constructQuery(MAVEN.ARTIFACT_ID, new SourcedSearchExpression(artifactId));
          final BooleanQuery gaQ = new BooleanQuery();
          gaQ.add(groupIdQ, Occur.MUST);
          gaQ.add(artifactIdQ, Occur.MUST);
          try (IteratorSearchResponse searchResponse = indexerManager
              .searchQueryIterator(gaQ, repository.getId(), null, null, null, false, null)) {
            for (ArtifactInfo ai : searchResponse) {
              try {
                versions.add(versionScheme.parseVersion(ai.version));
              }
              catch (InvalidVersionSpecificationException e) {
                // this is never thrown, look at GenericVersion imple
              }
            }
          }

          // All Vs for GA collected, now do the math
          if (versions.size() > request.getNumberOfVersionsToKeep()) {
            final List<Version> sortedVersions = Lists.newArrayList(versions);
            Collections.sort(sortedVersions);
            final List<Version> toDelete =
                sortedVersions.subList(0, versions.size() - request.getNumberOfVersionsToKeep());
            log.debug("Will delete {}:{} versions: {}", groupId, artifactId, toDelete);
            for (Version version : toDelete) {
              TaskUtil.checkInterruption();
              // we need the "version directory" of this GAV
              final String gavPath = repository.getGavCalculator()
                  .gavToPath(new Gav(groupId, artifactId, version.toString()));
              final String vDirectory = PathUtils.getParentPath(gavPath);
              try {
                int deleted = mayDeleteVersion(target, repository, vDirectory);
                log.debug("Deleted {} files from {}:{}:{} in {}", deleted, groupId, artifactId, version, repository);
                deletedFiles += deleted;
              }
              catch (Exception e) {
                successful = false;
                log.warn("Could not delete {}:{}:{} in {}, skipping it", groupId, artifactId, version, repository, e);
              }
            }
          }
        }
      }
    }
    catch (NoSuchRepositoryException e) {
      // repository removed/put out of service during run? abort
      log.warn("Repository {} unavailable, bailing out", repository, e);
    }
    result.setDeletedFileCount(deletedFiles);
    result.setSuccessful(successful);
  }

  /**
   * Deletes a complete V directory or it's contents selectively, obeying passed in target, if any. Returns the count
   * of deleted files.
   */
  private int mayDeleteVersion(final Target target,
                               final MavenRepository repository,
                               final String vDirectory) throws Exception
  {
    // delete only file items that matched target, if any
    try {
      final Collection<StorageItem> potentiallyDeletable = repository.list(new ResourceStoreRequest(vDirectory));
      final List<StorageItem> mustNotBeDeleted = Lists.newArrayList();
      final List<StorageItem> mustBeDeleted = Lists.newArrayList();
      for (StorageItem item : potentiallyDeletable) {
        if (item instanceof StorageCollectionItem) {
          mustNotBeDeleted.add(item);
          continue;
        }
        if (target != null && !target.isPathContained(repository.getRepositoryContentClass(), item.getPath())) {
          mustNotBeDeleted.add(item);
          continue;
        }
        mustBeDeleted.add(item);
      }
      if (mustNotBeDeleted.isEmpty()) {
        // delete vDirectory
        repository.deleteItem(new ResourceStoreRequest(vDirectory));
        return potentiallyDeletable.size();
      }
      else {
        int deleted = 0;
        // delete each item one by one
        for (StorageItem item : mustBeDeleted) {
          try {
            repository.deleteItemWithChecksums(new ResourceStoreRequest(item.getResourceStoreRequest()));
            deleted = deleted + 1;
          }
          catch (ItemNotFoundException e) {
            // ignore and continue with rest
          }
        }
        return deleted;
      }
    }
    catch (ItemNotFoundException e) {
      // ignore this, user might deleted vDirectory since run started
      return 0;
    }
  }

}
