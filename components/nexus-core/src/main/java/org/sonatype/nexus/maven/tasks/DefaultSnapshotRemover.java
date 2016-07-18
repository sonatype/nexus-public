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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.aether.util.version.GenericVersionScheme;
import org.sonatype.aether.version.InvalidVersionSpecificationException;
import org.sonatype.aether.version.Version;
import org.sonatype.aether.version.VersionScheme;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.RequestContext;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.maven.MavenHostedRepository;
import org.sonatype.nexus.proxy.maven.MavenProxyRepository;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.RecreateMavenMetadataWalkerProcessor;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.maven.gav.Gav;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.HostedRepository;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.walker.DefaultWalkerContext;
import org.sonatype.nexus.proxy.walker.DottedStoreWalkerFilter;
import org.sonatype.nexus.proxy.walker.ParentOMatic;
import org.sonatype.nexus.proxy.walker.Walker;
import org.sonatype.nexus.proxy.walker.WalkerContext;
import org.sonatype.nexus.proxy.walker.WalkerContext.TraversalType;
import org.sonatype.nexus.proxy.walker.WalkerException;
import org.sonatype.nexus.proxy.wastebasket.DeleteOperation;
import org.sonatype.nexus.util.PathUtils;
import org.sonatype.scheduling.TaskUtil;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The Class SnapshotRemoverJob. After a successful run, the job guarantees that there will remain at least
 * minCountOfSnapshotsToKeep (but maybe more) snapshots per one snapshot collection by removing all older from
 * removeSnapshotsOlderThanDays. If should remove snaps if their release counterpart exists, the whole GAV will be
 * removed.
 *
 * @author cstamas
 */
@Named
@Singleton
public class DefaultSnapshotRemover
    extends ComponentSupport
    implements SnapshotRemover
{

  private RepositoryRegistry repositoryRegistry;

  private Walker walker;

  private ContentClass maven2ContentClass;

  private VersionScheme versionScheme = new GenericVersionScheme();

  @Inject
  public DefaultSnapshotRemover(final RepositoryRegistry repositoryRegistry,
                                final Walker walker,
                                final @Named("maven2") ContentClass maven2ContentClass)
  {
    this.repositoryRegistry = checkNotNull(repositoryRegistry);
    this.walker = checkNotNull(walker);
    this.maven2ContentClass = checkNotNull(maven2ContentClass);
  }

  protected RepositoryRegistry getRepositoryRegistry() {
    return repositoryRegistry;
  }

  public SnapshotRemovalResult removeSnapshots(SnapshotRemovalRequest request)
      throws NoSuchRepositoryException, IllegalArgumentException
  {
    SnapshotRemovalResult result = new SnapshotRemovalResult();

    logDetails(request);

    if (request.getRepositoryId() != null) {
      Repository repository = getRepositoryRegistry().getRepository(request.getRepositoryId());

      if (!process(request, result, repository)) {
        throw new IllegalArgumentException("The repository with ID=" + repository.getId()
            + " is not valid for Snapshot Removal Task!");
      }
    }
    else {
      for (Repository repository : getRepositoryRegistry().getRepositories()) {
        process(request, result, repository);
      }
    }

    return result;
  }

  private void process(SnapshotRemovalRequest request, SnapshotRemovalResult result, GroupRepository group) {
    for (Repository repository : group.getMemberRepositories()) {
      process(request, result, repository);
    }
  }

  private boolean process(SnapshotRemovalRequest request, SnapshotRemovalResult result, Repository repository) {
    // only from maven repositories, stay silent for others and simply skip
    if (!repository.getRepositoryContentClass().isCompatible(maven2ContentClass)) {
      log.debug("Skipping '" + repository.getId() + "' is not a maven 2 repository");
      return false;
    }

    if (!repository.getLocalStatus().shouldServiceRequest()) {
      log.debug("Skipping '" + repository.getId() + "' the repository is out of service");
      return false;
    }

    if (repository.getRepositoryKind().isFacetAvailable(ProxyRepository.class)) {
      log.debug("Skipping '" + repository.getId() + "' is a proxy repository");
      return false;
    }

    if (repository.getRepositoryKind().isFacetAvailable(GroupRepository.class)) {
      process(request, result, repository.adaptToFacet(GroupRepository.class));
    }
    else if (repository.getRepositoryKind().isFacetAvailable(MavenRepository.class)) {
      result.addResult(removeSnapshotsFromMavenRepository(repository.adaptToFacet(MavenRepository.class),
          request));
    }

    return true;
  }

  /**
   * Removes the snapshots from maven repository.
   *
   * @param repository the repository
   * @throws Exception the exception
   */
  protected SnapshotRemovalRepositoryResult removeSnapshotsFromMavenRepository(MavenRepository repository,
                                                                               SnapshotRemovalRequest request)
  {
    TaskUtil.checkInterruption();

    SnapshotRemovalRepositoryResult result = new SnapshotRemovalRepositoryResult(repository.getId(), 0, 0, true);

    if (!repository.getLocalStatus().shouldServiceRequest()) {
      return result;
    }

    // we are already processed here, so skip repo
    if (request.isProcessedRepo(repository.getId())) {
      return new SnapshotRemovalRepositoryResult(repository.getId(), true);
    }

    request.addProcessedRepo(repository.getId());

    // if this is not snap repo, do nothing
    if (!RepositoryPolicy.SNAPSHOT.equals(repository.getRepositoryPolicy())) {
      return result;
    }

    if (log.isDebugEnabled()) {
      log.debug(
          "Collecting deletable snapshots on repository " + repository.getId() + " from storage directory "
              + repository.getLocalUrl()
      );
    }

    final ParentOMatic parentOMatic = new ParentOMatic();

    // create a walker to collect deletables and let it loose on collections only
    final SnapshotRemoverWalkerProcessor snapshotRemoveProcessor =
        new SnapshotRemoverWalkerProcessor(repository, request, parentOMatic);

    final DefaultWalkerContext ctxMain =
        new DefaultWalkerContext(repository,
            new ResourceStoreRequest("/"),
            new DottedStoreWalkerFilter(),
            TraversalType.BREADTH_FIRST,
            false);
    ctxMain.getContext().put(DeleteOperation.DELETE_OPERATION_CTX_KEY, getDeleteOperation(request));
    ctxMain.getProcessors().add(snapshotRemoveProcessor);
    walker.walk(ctxMain);

    if (ctxMain.getStopCause() != null) {
      result.setSuccessful(false);
    }

    // and collect results
    result.setDeletedSnapshots(snapshotRemoveProcessor.getDeletedSnapshots());
    result.setDeletedFiles(snapshotRemoveProcessor.getDeletedFiles());

    if (log.isInfoEnabled()) {
      log.info("deleted {} snapshot builds containing {} files on repository {}",
          snapshotRemoveProcessor.getDeletedSnapshots(), snapshotRemoveProcessor.getDeletedFiles(), repository.getId());
    }

    // if we are processing a hosted-snapshot repository, we need to rebuild maven metadata
    // without this if below, the walk would happen against proxy repositories too, but doing nothing!
    if (repository.getRepositoryKind().isFacetAvailable(HostedRepository.class)) {
      // expire NFC since we might create new maven metadata files
      repository.expireNotFoundCaches(new ResourceStoreRequest(RepositoryItemUid.PATH_ROOT));

      RecreateMavenMetadataWalkerProcessor metadataRebuildProcessor =
          new RecreateMavenMetadataWalkerProcessor(log, getDeleteOperation(request));

      final List<String> markedPaths = parentOMatic.getMarkedPaths();
      if (log.isInfoEnabled()) {
        log.info("rebuilding metadata for {} paths on repository {}", markedPaths != null ? markedPaths.size() : 0,
            repository.getId());
      }
      for (String path : markedPaths) {
        TaskUtil.checkInterruption();

        DefaultWalkerContext ctxMd =
            new DefaultWalkerContext(repository, new ResourceStoreRequest(path),
                new DottedStoreWalkerFilter());

        ctxMd.getProcessors().add(metadataRebuildProcessor);

        if (log.isTraceEnabled()) {
          log.trace("rebuilding metadata for {} on repository {}", path,
              repository.getId());
        }
        try {
          walker.walk(ctxMd);
        }
        catch (WalkerException e) {
          if (!(e.getCause() instanceof ItemNotFoundException)) {
            // do not ignore it
            throw e;
          }
        }
      }
    }

    return result;
  }

  private DeleteOperation getDeleteOperation(final SnapshotRemovalRequest request) {
    return request.isDeleteImmediately() ? DeleteOperation.DELETE_PERMANENTLY : DeleteOperation.MOVE_TO_TRASH;
  }

  private void logDetails(SnapshotRemovalRequest request) {
    if (request.getRepositoryId() != null) {
      log.info("Removing old SNAPSHOT deployments from " + request.getRepositoryId() + " repository.");
    }
    else {
      log.info("Removing old SNAPSHOT deployments from all repositories.");
    }

    if (log.isDebugEnabled()) {
      log.debug("With parameters: ");
      log.debug("    MinCountOfSnapshotsToKeep: " + request.getMinCountOfSnapshotsToKeep());
      log.debug("    RemoveSnapshotsOlderThanDays: " + request.getRemoveSnapshotsOlderThanDays());
      log.debug("    RemoveIfReleaseExists: " + request.isRemoveIfReleaseExists());
      log.debug("    DeleteImmediately: " + request.isDeleteImmediately());
      log.debug("    UseLastRequestedTimestamp: " + request.shouldUseLastRequestedTimestamp());
    }
  }

  private static class CacheEntry
  {
    private final List<StorageFileItem> items;

    private Long mostRecentItemLastRequested;

    private final Gav gav;

    CacheEntry(final StorageFileItem item, final Gav gav) {
      items = Lists.newArrayList();
      addItem(item);
      this.gav = checkNotNull(gav);
    }

    void addItem(StorageFileItem item) {
      this.items.add(checkNotNull(item));
      if (mostRecentItemLastRequested == null || item.getLastRequested() > mostRecentItemLastRequested) {
        this.mostRecentItemLastRequested = item.getLastRequested();
      }
    }
  }

  private class SnapshotRemoverWalkerProcessor
      extends AbstractFileDeletingWalkerProcessor
  {

    private static final long MILLIS_IN_A_DAY = 86400000L;

    private final MavenRepository repository;

    private final SnapshotRemovalRequest request;

    private final Map<Version, List<StorageFileItem>> toRemainSnapshotsAndFiles = Maps.newHashMap();

    private final Map<Version, List<StorageFileItem>> toDeleteSnapshotsAndFiles = Maps.newHashMap();

    private final ParentOMatic collectionNodes;

    /**
     * Milliseconds representing the oldest date that snapshots should be retained after,
     * if a snapshot last requested date falls after that date.
     */
    private final long dateThreshold;

    private final long startTime;

    private final long gracePeriodInMillis;

    private final List<StorageItem> items;

    private boolean shouldProcessCollection;

    private boolean removeWholeGAV;

    private int deletedSnapshots = 0;

    private int deletedFiles = 0;

    public SnapshotRemoverWalkerProcessor(final MavenRepository repository,
                                          final SnapshotRemovalRequest request,
                                          final ParentOMatic collectionNodes)
    {
      this.repository = repository;
      this.request = request;
      this.collectionNodes = collectionNodes;

      this.startTime = System.currentTimeMillis();

      int days = request.getRemoveSnapshotsOlderThanDays();

      if (days > 0) {
        this.dateThreshold = startTime - (days * MILLIS_IN_A_DAY);
      }
      else {
        this.dateThreshold = -1;
      }

      gracePeriodInMillis = Math.max(0, request.getGraceDaysAfterRelease()) * MILLIS_IN_A_DAY;
      items = Lists.newArrayList();
    }

    protected void addStorageFileItemToMap(Map<Version, List<StorageFileItem>> map, Gav gav, StorageFileItem item) {
      Version key = null;
      try {
        key = versionScheme.parseVersion(gav.getVersion());
      }
      catch (InvalidVersionSpecificationException e) {
        try {
          key = versionScheme.parseVersion("0.0-SNAPSHOT");
        }
        catch (InvalidVersionSpecificationException e1) {
          // nah
        }
      }

      if (!map.containsKey(key)) {
        map.put(key, new ArrayList<StorageFileItem>());
      }

      map.get(key).add(item);
    }

    @Override
    public void onCollectionEnter(WalkerContext context, StorageCollectionItem coll) {
      items.clear();
      toDeleteSnapshotsAndFiles.clear();
      toRemainSnapshotsAndFiles.clear();
      shouldProcessCollection = coll.getPath().endsWith("SNAPSHOT");
    }

    @Override
    public void processItem(WalkerContext context, StorageItem item)
        throws Exception
    {
      if (!shouldProcessCollection) {
        return;
      }
      items.add(item);
    }

    @Override
    public void onCollectionExit(WalkerContext context, StorageCollectionItem coll) {
      if (!shouldProcessCollection) {
        return;
      }
      try {
        doOnCollectionExit(context, coll);
      }
      catch (Exception e) {
        // we always simply log the exception and continue
        log.warn("SnapshotRemover is failed to process path: '" + coll.getPath() + "'.", e);
      }
    }

    public void doOnCollectionExit(WalkerContext context, StorageCollectionItem coll)
        throws Exception
    {
      if (log.isDebugEnabled()) {
        log.debug("doOnCollectionExit() :: " + coll.getRepositoryItemUid().toString());
      }
      removeWholeGAV = false;
      final HashSet<Long> versionsToRemove = Sets.newHashSet();
      boolean checkIfReleaseExists = request.isRemoveIfReleaseExists();
      // track the most recent last requested time for each build in the same GAV <build-number,item-data)
      final Map<Integer, CacheEntry> uniqueBuildsNearestRequestTimes = Maps.newHashMap();

      for (StorageItem item : items) {
        // only process artifacts
        if (!item.isVirtual() && !StorageCollectionItem.class.isAssignableFrom(item.getClass())) {

          final Gav gav =
              ((MavenRepository) coll.getRepositoryItemUid().getRepository()).getGavCalculator().pathToGav(
                  item.getPath());

          if (log.isDebugEnabled()) {
            log.debug(item.getPath());
          }

          // if file does not obey layout, is metadata or other non-artifact, gav is null
          // we will not check these hanging files for removal
          if (gav != null) {

            // when requested, remove all snapshots if release exists for first detected pom
            if (checkIfReleaseExists && !gav.isHash() && !gav.isSignature() && gav.getExtension().equals("pom")) {

              // only check release once per _all_ timestamped GAV pom since it is expensive
              checkIfReleaseExists = false;
              if (releaseExistsForSnapshot(gav, item.getItemContext())) {
                if (log.isDebugEnabled()) {
                  log.debug("release found");
                }

                removeWholeGAV = true;

                // break out and junk whole gav
                break;
              }
              else {
                if (log.isDebugEnabled()) {
                  log.debug("release not found");
                }
              }
            }

            item.getItemContext().put(Gav.class.getName(), gav);

            // do not check timestamp of signature or hash files, only rely on the main storage item
            if (gav.isHash() || gav.isSignature()) {
              if (log.isTraceEnabled()) {
                log.trace("Skipping lastRequested for: {}", item.getPath());
              }
            }
            else {

              if (gav.getSnapshotTimeStamp() != null) {

                long mavenSnapshotTimestamp = gav.getSnapshotTimeStamp().longValue();

                if (log.isDebugEnabled()) {
                  log.debug(
                      "itemTimestamp={} ({}), dateThreshold={} ({})",
                      mavenSnapshotTimestamp, mavenSnapshotTimestamp > 0 ? new Date(mavenSnapshotTimestamp) : "",
                      dateThreshold, dateThreshold > 0 ? new Date(dateThreshold) : ""
                  );
                }

                // always remove snapshot if the number of days for date threshold is not set
                if (-1 == dateThreshold) {
                  versionsToRemove.add(mavenSnapshotTimestamp);
                  addStorageFileItemToMap(toDeleteSnapshotsAndFiles, gav, (StorageFileItem) item);
                }
                // if this timestamped version is already marked to be removed, junk this item as well
                else if (versionsToRemove.contains(mavenSnapshotTimestamp)) {
                  addStorageFileItemToMap(toDeleteSnapshotsAndFiles, gav, (StorageFileItem) item);
                }
                // when requested to use last requested timestamp item attribute, cache the newest time for later check
                else if (request.shouldUseLastRequestedTimestamp()) {
                  // we need gather the most recent (nearest) last requested date of items in the same build number
                  // so in this block we do not yet record an item is to be removed, simply cache for later processing
                  final Integer uniqueSnapKey = gav.getSnapshotBuildNumber();
                  final CacheEntry previouslyCached = uniqueBuildsNearestRequestTimes.get(uniqueSnapKey);

                  if (previouslyCached == null) {
                    uniqueBuildsNearestRequestTimes.put(uniqueSnapKey, new CacheEntry((StorageFileItem) item, gav));
                  }
                  else {
                    previouslyCached.addItem((StorageFileItem) item);
                  }
                }
                // compare maven snapshot timestamp in file name to dateThreshold
                else if (mavenSnapshotTimestamp < dateThreshold) {
                  log.trace("maven timestamp {} earlier than threshold {}", mavenSnapshotTimestamp, dateThreshold);
                  versionsToRemove.add(mavenSnapshotTimestamp);
                  addStorageFileItemToMap(toDeleteSnapshotsAndFiles, gav, (StorageFileItem) item);
                }
                else {
                  //do not delete if dateThreshold not met
                  log.trace("retain date satisfied {}", item.getName());
                  addStorageFileItemToMap(toRemainSnapshotsAndFiles, gav, (StorageFileItem) item);
                }
              }
              else {
                // If no timestamp on gav, then it is a non-unique snapshot and should _not_ be removed
                log.debug("GAV Snapshot timestamp not available, skipping non-unique snapshot");
                addStorageFileItemToMap(toRemainSnapshotsAndFiles, gav, (StorageFileItem) item);
              }
            }
          }
        }
      }

      // and doing the work here
      if (removeWholeGAV) {
        try {
          for (StorageItem item : items) {
            try {
              // preserve possible subdirs
              if (!(item instanceof StorageCollectionItem)) {
                repository.deleteItem(false, createResourceStoreRequest(item, context));
              }
            }
            catch (ItemNotFoundException e) {
              if (log.isDebugEnabled()) {
                log.debug(
                    "Could not delete whole GAV " + coll.getRepositoryItemUid().toString(), e);
              }
            }
          }
        }
        catch (Exception e) {
          log.warn("Could not delete whole GAV " + coll.getRepositoryItemUid().toString(), e);
        }
      }
      else {

        if (!uniqueBuildsNearestRequestTimes.isEmpty()) {
          log.debug("processing {} cached builds by most recently requested date",
              uniqueBuildsNearestRequestTimes.size());
          for (CacheEntry entry : uniqueBuildsNearestRequestTimes.values()) {
            if (entry.mostRecentItemLastRequested < dateThreshold) {
              for (StorageFileItem sfi : entry.items) {
                if (log.isTraceEnabled()) {
                  log.trace("remove: {} most recent lastRequested={} ({}),dateThreshold={} ({})", sfi.getName(),
                      entry.mostRecentItemLastRequested,
                      entry.mostRecentItemLastRequested > 0 ? new Date(entry.mostRecentItemLastRequested) : "",
                      dateThreshold, dateThreshold > 0 ? new Date(dateThreshold) : "");
                }
                addStorageFileItemToMap(toDeleteSnapshotsAndFiles, entry.gav, sfi);
              }
            }
            else {
              for (StorageFileItem sfi : entry.items) {
                if (log.isTraceEnabled()) {
                  log.trace("retain: {} most recent lastRequested={} ({}),dateThreshold={} ({})", sfi.getName(),
                      entry.mostRecentItemLastRequested,
                      entry.mostRecentItemLastRequested > 0 ? new Date(entry.mostRecentItemLastRequested) : "",
                      dateThreshold, dateThreshold > 0 ? new Date(dateThreshold) : "");
                }
                addStorageFileItemToMap(toRemainSnapshotsAndFiles, entry.gav, sfi);
              }
            }
          }
        }

        // and now check some things
        if (toRemainSnapshotsAndFiles.size() < request.getMinCountOfSnapshotsToKeep()) {
          // do something
          if (toRemainSnapshotsAndFiles.size() + toDeleteSnapshotsAndFiles.size() <
              request.getMinCountOfSnapshotsToKeep()) {
            // delete nothing, since there is less snapshots in total as allowed
            toDeleteSnapshotsAndFiles.clear();
          }
          else {
            TreeSet<Version> keys = new TreeSet<Version>(toDeleteSnapshotsAndFiles.keySet());

            while (!keys.isEmpty()
                && toRemainSnapshotsAndFiles.size() < request.getMinCountOfSnapshotsToKeep()) {
              Version keyToMove = keys.last();

              if (toRemainSnapshotsAndFiles.containsKey(keyToMove)) {
                toRemainSnapshotsAndFiles.get(keyToMove).addAll(
                    toDeleteSnapshotsAndFiles.get(keyToMove));
              }
              else {
                toRemainSnapshotsAndFiles.put(keyToMove, toDeleteSnapshotsAndFiles.get(keyToMove));
              }

              toDeleteSnapshotsAndFiles.remove(keyToMove);

              keys.remove(keyToMove);
            }

          }
        }

        // NEXUS-814: is this GAV have remaining artifacts?
        boolean gavHasMoreTimestampedSnapshots = toRemainSnapshotsAndFiles.size() > 0;

        for (Version key : toDeleteSnapshotsAndFiles.keySet()) {

          List<StorageFileItem> files = toDeleteSnapshotsAndFiles.get(key);
          deletedSnapshots++;

          for (StorageFileItem file : files) {
            try {
              // NEXUS-814: mark that we are deleting a TS snapshot, but there are still remaining
              // ones in repository.
              if (gavHasMoreTimestampedSnapshots) {
                file.getItemContext().put(MORE_TS_SNAPSHOTS_EXISTS_FOR_GAV, Boolean.TRUE);
              }
              repository.deleteItemWithChecksums(true, createResourceStoreRequest(file, context));
              deletedFiles++;
            }
            catch (ItemNotFoundException e) {
              // NEXUS-5682 Since checksum files are no longer physically represented on the file system,
              // it is expected that they will generate ItemNotFoundException. Log at trace level only for
              // diagnostic purposes.
              if (log.isTraceEnabled()) {
                log.trace("Could not delete file:", e);
              }

            }
            catch (Exception e) {
              log.info("Could not delete file:", e);
            }
          }
        }
      }

      removeDirectoryIfEmpty(repository, coll);
      updateMetadataIfNecessary(context, coll);
    }

    private void updateMetadataIfNecessary(WalkerContext context, StorageCollectionItem coll)
        throws Exception
    {
      // all snapshot files are deleted
      if (!toDeleteSnapshotsAndFiles.isEmpty() && toRemainSnapshotsAndFiles.isEmpty()) {
        collectionNodes.addAndMarkPath(PathUtils.getParentPath(coll.getPath()));
      }
      else {
        collectionNodes.addAndMarkPath(coll.getPath());
      }
    }

    public boolean releaseExistsForSnapshot(Gav snapshotGav, RequestContext context) {
      long releaseTimestamp = -1;

      for (Repository repository : repositoryRegistry.getRepositories()) {
        // we need to filter for:
        // repository that is MavenRepository and is hosted or proxy
        // repository that has release policy
        if (repository.getRepositoryKind().isFacetAvailable(MavenHostedRepository.class)
            || repository.getRepositoryKind().isFacetAvailable(MavenProxyRepository.class)) {
          // actually, we don't care is it proxy or hosted, we only need to filter out groups and other
          // "composite" reposes like shadows
          MavenRepository mrepository = repository.adaptToFacet(MavenRepository.class);

          // look in release reposes only
          if (mrepository.isUserManaged()
              && RepositoryPolicy.RELEASE.equals(mrepository.getRepositoryPolicy())) {
            try {
              String releaseVersion = null;

              // NEXUS-3148
              if (snapshotGav.getBaseVersion().endsWith("-SNAPSHOT")) {
                // "-SNAPSHOT" :== 9 chars
                releaseVersion =
                    snapshotGav.getBaseVersion().substring(0,
                        snapshotGav.getBaseVersion().length() - 9);
              }
              else {
                // "SNAPSHOT" :== 8 chars
                releaseVersion =
                    snapshotGav.getBaseVersion().substring(0,
                        snapshotGav.getBaseVersion().length() - 8);
              }

              Gav releaseGav =
                  new Gav(snapshotGav.getGroupId(), snapshotGav.getArtifactId(), releaseVersion,
                      snapshotGav.getClassifier(), snapshotGav.getExtension(), null, null, null, false,
                      null, false, null);

              String path = mrepository.getGavCalculator().gavToPath(releaseGav);

              final ResourceStoreRequest req = new ResourceStoreRequest(path, true, false);
              req.getRequestContext().setParentContext(context);

              log.debug("Checking for release counterpart in repository '{}' and path '{}'",
                  mrepository.getId(), req.toString());

              final StorageItem item = mrepository.retrieveItem(false, req);

              releaseTimestamp = item.getCreated();

              break;
            }
            catch (ItemNotFoundException e) {
              // nothing
            }
            catch (Exception e) {
              // nothing
              log.debug("Unexpected exception!", e);
            }
          }
        }
      }

      return releaseTimestamp == 0  // 0 when item creation day is unknown
          || (releaseTimestamp > 0 && startTime > releaseTimestamp + gracePeriodInMillis);
    }

    public int getDeletedSnapshots() {
      return deletedSnapshots;
    }

    public int getDeletedFiles() {
      return deletedFiles;
    }

  }

}
