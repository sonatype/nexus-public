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
package org.sonatype.nexus.repository.maven.internal.orient;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.logging.task.ProgressLogIntervalHelper;
import org.sonatype.nexus.orient.entity.AttachedEntityHelper;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.orient.maven.MavenFacet;
import org.sonatype.nexus.repository.maven.MavenHostedFacet;
import org.sonatype.nexus.repository.maven.RemoveSnapshotsFacet;
import org.sonatype.nexus.repository.maven.VersionPolicy;
import org.sonatype.nexus.repository.maven.internal.group.MavenGroupFacet;
import org.sonatype.nexus.repository.maven.tasks.RemoveSnapshotsConfig;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentEntityAdapter;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalDeleteBlob;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.collect.Sets;
import com.orientechnologies.orient.core.command.script.OCommandScript;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OResultSet;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.logging.task.TaskLoggingMarkers.PROGRESS;
import static org.sonatype.nexus.repository.maven.internal.orient.MavenFacetUtils.COMPONENT_VERSION_COMPARATOR;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_GROUP;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

/**
 * @since 3.0
 */
@Named
public class RemoveSnapshotsFacetImpl
    extends FacetSupport
    implements RemoveSnapshotsFacet
{
  /**
   * Load all GAVs that contain a snapshot in a repository
   */
  private static final String GAVS_WITH_SNAPSHOTS =
      "SELECT group, name, attributes.maven2.baseVersion AS baseVersion, " +
          "count(*) AS cnt " +
      "FROM component WHERE bucket=:bucket " +
      "AND attributes.maven2.baseVersion LIKE '%-SNAPSHOT' " +
      "GROUP BY group, name, attributes.maven2.baseVersion";

  /**
   * Load components for a GAV which are either snapshots that exist in the repository/bucket being processed,
   * or a release across all repositories/buckets
   */
  private static final String COMPONENTS_FOR_GABV =
      "LET $records = (SELECT FROM component WHERE group = ? AND name=?);" +
      "SELECT FROM $records WHERE ( " +
      "   (bucket=? AND attributes.maven2.baseVersion = ?) " +
      "   OR (attributes.maven2.baseVersion = ?)" +
      ");";

  private final long batchSize;

  private final ComponentEntityAdapter componentEntityAdapter;

  private final Type groupType;

  @Inject
  public RemoveSnapshotsFacetImpl(
      final ComponentEntityAdapter componentEntityAdapter,
      @Named(GroupType.NAME) final Type groupType,
      @Named("${nexus.removeSnapshots.batchSize:-500}") long batchSize)
  {
    this.componentEntityAdapter = checkNotNull(componentEntityAdapter);
    this.groupType = checkNotNull(groupType);
    this.batchSize = batchSize;
  }

  @Override
  @Guarded(by = STARTED)
  public void removeSnapshots(RemoveSnapshotsConfig config)
  {
    Repository repository = getRepository();
    String repositoryName = repository.getName();
    log.info("Beginning snapshot removal on repository '{}' with configuration: {}", repositoryName, config);
    UnitOfWork.beginBatch(facet(StorageFacet.class).txSupplier());
    Set<GAV> metadataUpdateRequired = new HashSet<>();
    try {
      if (groupType.equals(repository.getType())) {
        processGroup(repository.facet(MavenGroupFacet.class), config);
      }
      else {
        metadataUpdateRequired.addAll(processRepository(repository, config));
      }
    }
    finally {
      UnitOfWork.end();
    }

    //only update metadata for non-proxy repos
    if (!repository.optionalFacet(ProxyFacet.class).isPresent()) {
      log.info("Updating metadata on repository '{}'", repositoryName);
      ProgressLogIntervalHelper intervalLogger = new ProgressLogIntervalHelper(log, 60);

      int processed = 0;
      for (GAV gav : metadataUpdateRequired) {
        Optional<MavenHostedFacet> mavenHostedFacet = repository.optionalFacet(MavenHostedFacet.class);
        if (mavenHostedFacet.isPresent()) {
          try {
            mavenHostedFacet.get().deleteMetadata(gav.group, gav.name, gav.baseVersion);
            intervalLogger
                .info("Elapsed time: {}, updated metadata for {} GAVs", intervalLogger.getElapsed(), ++processed);
          }
          catch (Exception e) {
            log.warn("Unable to delete/rebuild {} {} {}", gav.group, gav.name, gav.baseVersion,
                log.isDebugEnabled() ? e : null);
          }
        }
      }

      intervalLogger.flush();
    }
    else {
      log.info("Skipping metadata updates on proxy repository '{}'", repositoryName);
    }

    log.info("Completed snapshot removal on repository '{}'", repositoryName);
  }

  /**
   * Iterate over group members which may contain snapshots and recursively apply the snapshot removal.
   */
  private void processGroup(final MavenGroupFacet groupFacet, final RemoveSnapshotsConfig config) {
    groupFacet.members().stream()
        .filter(member -> isSnapshotRepo(member) || groupType.equals(member.getType()))
        .forEach(member -> member.facet(RemoveSnapshotsFacet.class).removeSnapshots(config));
  }

  /**
   * Examine all snapshots in the given repo, delete those that match our configuration criteria and flag which GAVs
   * require a metadata update.
   */
  @TransactionalDeleteBlob
  protected Collection<GAV> processRepository(final Repository repository, final RemoveSnapshotsConfig config) {
    StorageTx tx = UnitOfWork.currentTx();

    log.info("Begin processing snapshots in repository '{}'", repository.getName());

    Set<GAV> snapshotCandidates = Sets.newHashSet(findSnapshotCandidates(tx, repository));
    log.info("Found {} snapshot GAVs to analyze", snapshotCandidates.size());

    // only interested in the ones where we actually delete something, otherwise we would needlessly regenerate metadata
    Set<GAV> gavsWithDeletions = new HashSet<>();

    ProgressLogIntervalHelper intervalLogger = new ProgressLogIntervalHelper(log, 60);
    long deleted = 0;
    long processed = 0;
    for (GAV snapshotCandidate : snapshotCandidates) {
      log.debug("Processing GAV = {}", snapshotCandidate);
      Set<Component> components = Sets.newHashSet(findComponentsForGav(tx, repository, snapshotCandidate));

      if (components.isEmpty()) {
        continue;
      }

      Set<Component> toDelete = getSnapshotsToDelete(config, components);

      if (!toDelete.isEmpty()) {
        log.debug("Found {} components to remove for GAV = {}", toDelete.size(), snapshotCandidate);

        gavsWithDeletions.add(snapshotCandidate);
        for (Component component : toDelete) {
          log.debug("Deleting component: {}", component);
          tx.deleteComponent(component);

          if (maybeCommit(tx, ++deleted)) {
            intervalLogger.info("Elapsed time: {}, GAVs processed: {}, snapshots deleted: {}",
                intervalLogger.getElapsed(), processed, deleted);
          }
        }
      }

      processed++;
    }
    log.debug("Committing final batch delete");
    tx.commit();
    tx.begin();
    intervalLogger.flush();

    DateTime olderThan = DateTime.now().minusDays(Math.max(config.getSnapshotRetentionDays(), 0));
    log.info("Elapsed time: {}, deleted {} components from {} distinct GAVs", intervalLogger.getElapsed(), deleted,
        gavsWithDeletions.size());
    log.info("Finished processing snapshots with more than {} versions created before {}", config.getMinimumRetained(),
        olderThan);
    return gavsWithDeletions;
  }

  /**
   * Given a list of all components (snapshot & release) for a GAV, determine which ones to delete based on the config
   */
  @VisibleForTesting
  Set<Component> getSnapshotsToDelete(final RemoveSnapshotsConfig config, final Set<Component> components) {
    // get all the snapshot components
    Supplier<Stream<Component>> streamSupplier = () -> components.stream()
            .filter(MavenFacetUtils::isSnapshot)
            .sorted(COMPONENT_VERSION_COMPARATOR.reversed()); // sort by version desc (newest first)

    // filter out components that we want to keep
    DateTime olderThan = DateTime.now().minusDays(Math.max(config.getSnapshotRetentionDays(), 0));
    AtomicInteger keep = new AtomicInteger();
    Set<Component> snapshotsToDelete = (config.getMinimumRetained() == -1)
        ? new HashSet<>()
        : streamSupplier.get()
        .filter(component -> keep.incrementAndGet() > config.getMinimumRetained()) // retention based on desired minimum
        .filter(component -> olderThan.isAfter(component.lastUpdated())) // retention based on date
        .collect(Collectors.toSet());

    // additional processing if 'remove if released' is enabled
    if (config.getRemoveIfReleased() &&
        // Note: its possible to have multiple release artifacts across repositories. We just need to know if one exists.
        components.stream().anyMatch(MavenFacetUtils::isRelease)) {
        DateTime gracePeriod = DateTime.now().minusDays(Math.max(config.getGracePeriod(), 0));
        Set<Component> releasedSnapshotsToDelete = streamSupplier.get()
            .filter(component -> gracePeriod.isAfter(component.lastUpdated()))
            .collect(Collectors.toSet());
        snapshotsToDelete.addAll(releasedSnapshotsToDelete);
    }

    return snapshotsToDelete;
  }

  private boolean maybeCommit(StorageTx tx, long deleted) {
    if (deleted % batchSize == 0) {
      tx.commit();
      tx.begin();
      return true;
    }

    return false;
  }

  /**
   * Find all components (snapshot *OR* release) for a given GAV
   */
  @VisibleForTesting
  Iterable<Component> findComponentsForGav(final StorageTx tx, final Repository repository, final GAV gav)
  {
    final Bucket bucket = tx.findBucket(repository);
    final ORID bucketId = AttachedEntityHelper.id(bucket);

    // the version to use for a release version search. E.g. gav.baseVersion is 1.1-SNAPSHOT, we need to search for 1.1
    String releaseVersion = gav.baseVersion.replace("-SNAPSHOT", "");

    final OResultSet<ODocument> result = tx.getDb().command(new OCommandScript("sql", COMPONENTS_FOR_GABV))
        .execute(gav.group, gav.name, bucketId, gav.baseVersion, releaseVersion);
    return result.stream().map(componentEntityAdapter::readEntity).collect(Collectors.toList());
  }

  /**
   * Find all GAVs that qualify for deletion.
   */
  @VisibleForTesting
  Set<GAV> findSnapshotCandidates(final StorageTx tx, final Repository repository)
  {
    log.info(PROGRESS, "Searching for GAVS with snapshots that qualify for deletion on repository '{}'",
        repository.getName());

    final Bucket bucket = tx.findBucket(repository);
    final OResultSet<ODocument> result = tx.getDb().command(new OSQLSynchQuery<>(GAVS_WITH_SNAPSHOTS))
        .execute(AttachedEntityHelper.id(bucket));
    return result.stream().map((doc) -> {
      String group = doc.field(P_GROUP, String.class);
      String name = doc.field(P_NAME, String.class);
      String baseVersion = doc.field("baseVersion", String.class);
      Integer count = doc.field("cnt", Integer.class);
      return new GAV(group, name, baseVersion, count);
    }).collect(Collectors.toSet());
  }

  /**
   * Determine whether or not the given repo could contain snapshots.
   */
  private static boolean isSnapshotRepo(final Repository member) {
    return member.facet(MavenFacet.class).getVersionPolicy() != VersionPolicy.RELEASE;
  }

  /**
   * Struct to track GAV we need to request metadata rebuild due to deletion.
   */
  static final class GAV
  {
    final String group;

    final String name;

    final String baseVersion;

    final int count;

    public GAV(final String group, final String name, final String baseVersion, final int count) {
      this.group = group;
      this.name = name;
      this.baseVersion = baseVersion;
      this.count = count;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      GAV gav = (GAV) o;
      return count == gav.count &&
          Objects.equal(group, gav.group) &&
          Objects.equal(name, gav.name) &&
          Objects.equal(baseVersion, gav.baseVersion);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(group, name, baseVersion, count);
    }

    @Override
    public String toString() {
      return "GAV{" +
          "group='" + group + '\'' +
          ", name='" + name + '\'' +
          ", baseVersion='" + baseVersion + '\'' +
          ", count=" + count +
          '}';
    }
  }
}
