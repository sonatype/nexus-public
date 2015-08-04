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
package org.sonatype.nexus.maven.tasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.aether.util.version.GenericVersionScheme;
import org.sonatype.aether.version.InvalidVersionSpecificationException;
import org.sonatype.aether.version.Version;
import org.sonatype.aether.version.VersionScheme;
import org.sonatype.nexus.maven.tasks.descriptors.ReleaseRemovalTaskDescriptor;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.gav.Gav;
import org.sonatype.nexus.proxy.targets.Target;
import org.sonatype.nexus.proxy.targets.TargetStoreWalkerFilter;
import org.sonatype.nexus.proxy.walker.ConjunctionWalkerFilter;
import org.sonatype.nexus.proxy.walker.DefaultWalkerContext;
import org.sonatype.nexus.proxy.walker.DottedStoreWalkerFilter;
import org.sonatype.nexus.proxy.walker.Walker;
import org.sonatype.nexus.proxy.walker.WalkerContext;
import org.sonatype.nexus.proxy.walker.WalkerFilter;
import org.sonatype.nexus.proxy.wastebasket.DeleteOperation;
import org.sonatype.scheduling.TaskUtil;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.maven.tasks.descriptors.ReleaseRemovalTaskDescriptor.ID;

/**
 * Release remover based on {@link Walker} for repository crawling and collecting items to be removed.
 *
 * @since 2.11.3
 */
@Named(ReleaseRemoverBackend.WALKER)
@Singleton
public class WalkerReleaseRemoverBackend
    extends ComponentSupport
    implements ReleaseRemoverBackend
{
  private final Walker walker;

  private final VersionScheme versionScheme;

  @Inject
  public WalkerReleaseRemoverBackend(final Walker walker)
  {
    this.walker = checkNotNull(walker);
    this.versionScheme = new GenericVersionScheme();
  }

  @Override
  public void removeReleases(final ReleaseRemovalRequest request,
                             final ReleaseRemovalResult result,
                             final MavenRepository repository,
                             final @Nullable Target repositoryTarget)
      throws IOException
  {
    TaskUtil.checkInterruption();

    log.debug("Collecting deletable releases on repository '{}' from storage directory {}",
        repository.getId(),
        repository.getLocalUrl());

    DefaultWalkerContext ctxMain =
        new DefaultWalkerContext(repository, new ResourceStoreRequest("/"),
            determineFilter(repositoryTarget));

    ctxMain.getContext().put(DeleteOperation.DELETE_OPERATION_CTX_KEY, DeleteOperation.MOVE_TO_TRASH);

    ctxMain.getProcessors().add(new ReleaseRemovalWalkerProcessor(repository, request, result, repositoryTarget));

    walker.walk(ctxMain);

    if (ctxMain.getStopCause() != null) {
      result.setSuccessful(false);
    }
  }

  /**
   * Create an appropriate filter based on the repositoryTarget
   */
  private WalkerFilter determineFilter(final Target repositoryTarget) {
    if (repositoryTarget == null) {
      return new DottedStoreWalkerFilter();
    }
    return ConjunctionWalkerFilter.satisfiesAllOf(new DottedStoreWalkerFilter(),
        new TargetStoreWalkerFilter(repositoryTarget));
  }

  private class ReleaseRemovalWalkerProcessor
      extends AbstractFileDeletingWalkerProcessor
  {

    private static final String POSSIBLY_EMPTY_COLLECTIONS = "possiblyEmptyCollections";

    private final MavenRepository repository;

    private final ReleaseRemovalRequest request;

    private final Map<Version, List<StorageFileItem>> deletableVersionsAndFiles = Maps.newHashMap();

    private final Map<Gav, Map<Version, List<StorageFileItem>>> groupArtifactToVersions = Maps.newHashMap();

    private final ReleaseRemovalResult result;

    private final Target repositoryTarget;

    private int deletedFiles = 0;

    private ReleaseRemovalWalkerProcessor(final MavenRepository repository,
                                          final ReleaseRemovalRequest request,
                                          final ReleaseRemovalResult result,
                                          final Target repositoryTarget)
    {
      this.repository = repository;
      this.request = request;
      this.result = result;
      this.repositoryTarget = repositoryTarget;
    }

    @Override
    public void processItem(final WalkerContext context, final StorageItem item)
        throws Exception
    {
    }

    @Override
    public void onCollectionExit(final WalkerContext context, final StorageCollectionItem coll)
        throws Exception
    {
      try {
        doOnCollectionExit(context, coll);
      }
      catch (Exception e) {
        // we always simply log the exception and continue
        log.warn("{} failed to process path: '{}'.", ID, coll.getPath(), e);
      }
    }

    private void doOnCollectionExit(final WalkerContext context, final StorageCollectionItem coll)
        throws ItemNotFoundException, StorageException, IllegalOperationException,
               InvalidVersionSpecificationException
    {
      deletableVersionsAndFiles.clear();

      Collection<StorageItem> items = repository.list(false, coll);
      Gav gav = null;
      for (StorageItem item : items) {
        if (!item.isVirtual() && !StorageCollectionItem.class.isAssignableFrom(item.getClass())) {
          gav =
              ((MavenRepository) coll.getRepositoryItemUid().getRepository()).getGavCalculator().pathToGav(
                  item.getPath());
          if (gav != null) {
            addCollectionToContext(context, coll);

            maybeAddStorageFileItemToMap(gav, (StorageFileItem) item);
          }
        }
      }
      // if a gav can be calculated, it should be shared by all files in the collection
      if (null != gav) {
        groupVersions(groupArtifactToVersions, deletableVersionsAndFiles, gav);
      }
    }

    /**
     * Compare the item path to the RepositoryTarget(if it is declared) and only include files that match that
     * pattern.
     * While the walker itself handles GAV paths, this ensures that we also respect patterns with file-level
     * granularity.
     */
    private void maybeAddStorageFileItemToMap(final Gav gav, final StorageFileItem item) {
      if (repositoryTarget != null && !repositoryTarget.isPathContained(
          item.getRepositoryItemUid().getRepository().getRepositoryContentClass(), item
              .getPath())) {
        log.debug("Excluding file: {} from deletion due to repositoryTarget: {}.", item.getName(),
            repositoryTarget.getName());
        return;
      }
      addStorageFileItemToMap(deletableVersionsAndFiles, gav, (StorageFileItem) item);
    }

    /**
     * Store visited collections so we can later determine if we need to delete them.
     */
    private void addCollectionToContext(final WalkerContext context, final StorageCollectionItem coll) {
      if (!context.getContext().containsKey(POSSIBLY_EMPTY_COLLECTIONS)) {
        context.getContext().put(POSSIBLY_EMPTY_COLLECTIONS, Lists.<StorageCollectionItem>newArrayList());
      }
      ((List<StorageCollectionItem>) context.getContext().get(POSSIBLY_EMPTY_COLLECTIONS)).add(coll);
    }

    /**
     * Map Group + Artifact to each version with those GA coordinates
     */
    private void groupVersions(final Map<Gav, Map<Version, List<StorageFileItem>>> groupArtifactToVersions,
                               final Map<Version, List<StorageFileItem>> versionsAndFiles,
                               final Gav gav)
    {
      //ga only coordinates
      Gav ga = new Gav(gav.getGroupId(), gav.getArtifactId(), "");
      if (!groupArtifactToVersions.containsKey(ga)) {
        groupArtifactToVersions.put(ga, Maps.newHashMap(versionsAndFiles));
      }
      groupArtifactToVersions.get(ga).putAll(versionsAndFiles);
    }

    protected void addStorageFileItemToMap(Map<Version, List<StorageFileItem>> map, Gav gav, StorageFileItem item) {
      Version key = null;
      try {
        key = versionScheme.parseVersion(gav.getVersion());
      }
      catch (InvalidVersionSpecificationException e) {
        throw new IllegalStateException("Unable to determine version for " + gav.getVersion() +
            ", cannot proceed with deletion of releases unless"
            + "all version information can be parsed into major.minor.incremental version.");
      }

      if (!map.containsKey(key)) {
        map.put(key, new ArrayList<StorageFileItem>());
      }

      map.get(key).add(item);
    }

    @Override
    public void afterWalk(final WalkerContext context)
        throws Exception
    {
      for (Map.Entry<Gav, Map<Version, List<StorageFileItem>>> gavListEntry : groupArtifactToVersions.entrySet()) {
        Map<Version, List<StorageFileItem>> versions = gavListEntry.getValue();
        if (versions.size() > request.getNumberOfVersionsToKeep()) {
          log.debug("{} will delete {} versions of artifact with g={} a={}",
              ReleaseRemovalTaskDescriptor.ID,
              versions.size() - request.getNumberOfVersionsToKeep(),
              gavListEntry.getKey().getGroupId(), gavListEntry.getKey().getArtifactId());

          List<Version> sortedVersions = Lists.newArrayList(versions.keySet());
          Collections.sort(sortedVersions);
          List<Version> toDelete =
              sortedVersions.subList(0, versions.size() - request.getNumberOfVersionsToKeep());
          log.debug("Will delete these specific versions: {}", toDelete);
          for (Version version : toDelete) {
            for (StorageFileItem storageFileItem : versions.get(version)) {
              repository.deleteItem(createResourceStoreRequest(storageFileItem, context));
              deletedFiles++;
            }
          }
          if (context.getContext().containsKey(POSSIBLY_EMPTY_COLLECTIONS)) {
            for (StorageCollectionItem coll : (List<StorageCollectionItem>) context.getContext().get(
                POSSIBLY_EMPTY_COLLECTIONS)) {
              removeDirectoryIfEmpty(repository, coll);
            }
          }
        }
      }
      result.setDeletedFileCount(deletedFiles);
      result.setSuccessful(true);
    }
  }
}
