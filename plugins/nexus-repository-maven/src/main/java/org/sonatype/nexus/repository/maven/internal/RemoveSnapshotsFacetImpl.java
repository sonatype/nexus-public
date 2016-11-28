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
package org.sonatype.nexus.repository.maven.internal;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.orient.entity.AttachedEntityHelper;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.maven.MavenFacet;
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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OResultSet;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.StreamSupport.stream;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_BASE_VERSION;
import static org.sonatype.nexus.repository.maven.internal.MavenFacetUtils.version;
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
   * Find where we have more than X snapshots for a given GAbV. Results are confined to the same repository (bucket).
   */
  private static final String SNAPSHOTS_WITH_MINIMUM_COUNT = "SELECT FROM (SELECT group, name, attributes" +
      ".maven2.baseVersion AS baseVersion, count(*) AS cnt FROM component WHERE bucket=:bucket AND " +
      "attributes.maven2.baseVersion LIKE '%SNAPSHOT' GROUP BY group, name, attributes.maven2.baseVersion) where cnt " +
      ">:minimumCount";

  /**
   * Load components that are candidates for deletion.
   */
  private static final String SNAPSHOTS_FOR_GABV = "SELECT FROM component WHERE bucket=:bucket AND group=:group " +
      "AND name=:name AND attributes.maven2.baseVersion=:baseVersion " +
      "ORDER BY last_updated ASC LIMIT :limit";

  /**
   * Find where we have a release (in any repository) related to a snapshot in a specific repository (bucket),
   * older than X.
   */
  private static final String WITH_RELEASES = "SELECT FROM component " +
      "LET $temp = (SELECT FROM component WHERE format='maven2' AND group = $parent.current.group " +
      " AND name = $parent.current.name " +
      " AND version = $parent.current.attributes.maven2.baseVersion.replace('-SNAPSHOT', '')" +
      ") " +
      "WHERE bucket=:bucket AND format='maven2' AND last_updated < :lastUpdated " +
      "AND attributes.maven2.baseVersion LIKE '%-SNAPSHOT'AND $temp.size() > 0 ";

  private static final Function<Component, GAV> GROUPING_FUNCTION = t -> new GAV(t.group(), t.name(),
      (String) t.attributes().child(Maven2Format.NAME).get(P_BASE_VERSION), -1);

  private final long batchSize;

  private final ComponentEntityAdapter componentEntityAdapter;

  private final Type groupType;

  @Inject
  public RemoveSnapshotsFacetImpl(final ComponentEntityAdapter componentEntityAdapter,
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
    log.info("Removing snapshots on repository {} with configuration: {}", repository.getName(), config);
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
      log.info("Updating metadata on repository: {}", repository.getName());
      for (GAV gav : metadataUpdateRequired) {
        Optional<MavenHostedFacet> mavenHostedFacet = repository.optionalFacet(MavenHostedFacet.class);
        if (mavenHostedFacet.isPresent()) {
          mavenHostedFacet.get().deleteMetadata(gav.group, gav.name, gav.baseVersion);
        }
      }
    }
    else {
      log.info("Skipping metadata updates on proxy repository: {}", repository.getName());
    }
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

    //collect any GAVs we need to refresh metadata for
    Set<GAV> metadataUpdateRequired = new HashSet<>();

    metadataUpdateRequired.addAll(processSnapshots(repository, config, tx));

    // since this spans all maven repos it can be expensive to find deletion candidates, so we do it after deleting 
    // based on other criteria
    if (config.getRemoveIfReleased()) {
      metadataUpdateRequired.addAll(processReleasedSnapshots(repository, config, tx));
    }

    return metadataUpdateRequired;
  }

  /**
   * Delete all snapshots created before the grace period and for which there is an associated release.
   */
  private Collection<GAV> processReleasedSnapshots(final Repository repository, final RemoveSnapshotsConfig config,
                                                   final StorageTx tx)
  {
    Date gracePeriod = DateTime.now().minusDays(Math.max(config.getGracePeriod(), 0)).toDate();
    log.debug("Looking for snapshots with associated releases created before {}", gracePeriod);

    //lookup results and group around the baseVersion
    Iterable<Component> releasedSnapshots = findReleasedSnapshots(tx, repository, gracePeriod);
    Map<GAV, List<Component>> groupedReleasedSnapshots = stream(releasedSnapshots.spliterator(), false)
        .collect(Collectors.groupingBy(GROUPING_FUNCTION));
    log.info("Processing {} GAVs found that have already been released and for which the grace period has elapsed",
        groupedReleasedSnapshots.size());

    long deleted = 0;

    for (Entry<GAV, List<Component>> entry : groupedReleasedSnapshots.entrySet()) {
      GAV key = entry.getKey();
      log.debug("Processing GAV = {}", key);
      for (Component component : entry.getValue()) {
        log.debug("Deleting component: {}", component);
        tx.deleteComponent(component);
        if (++deleted % batchSize == 0) {
          log.debug("Committing batch delete");
          tx.commit();
          tx.begin();
        }
      }
    }
    //commit any potential leftovers that didn't fit in the batch delete window
    log.debug("Committing final batch delete");
    tx.commit();
    tx.begin();
    log.info("Finished processing snapshots with associated releases");
    return groupedReleasedSnapshots.keySet();
  }

  /**
   * Delete all snapshots created before the retention period and potentially preserving a certain number.
   */
  @VisibleForTesting
  Set<GAV> processSnapshots(final Repository repository, final RemoveSnapshotsConfig config, final StorageTx tx)
  {
    if (config.getMinimumRetained() == -1) {
      log.info("Skipping processing of snapshots by age and minimum count due to configuration");
      return new HashSet<>();
    }

    DateTime olderThan = DateTime.now().minusDays(Math.max(config.getSnapshotRetentionDays(), 0));
    Set<GAV> snapshotCandidates = Sets.newHashSet(findSnapshotCandidates(tx, repository, config.getMinimumRetained()));
    log.info("Processing {} GAVs found with more than minimum {} snapshot versions", snapshotCandidates.size(),
        config.getMinimumRetained());

    // only interested in the ones where we actually delete something, otherwise we would needlessly regenerate metadata
    Set<GAV> gavsWithDeletions = new HashSet<>();

    long deleted = 0;
    for (GAV snapshotCandidate : snapshotCandidates) {
      log.debug("Processing GAV = {}", snapshotCandidate);
      List<Component> components = Lists.newArrayList(findSnapshots(tx, repository, snapshotCandidate));
      components.sort((o1, o2) -> version(o2.version()).compareTo(version(o1.version())));

      // always keep this many at least
      components.subList(0, config.getMinimumRetained()).clear();

      // filter out any components that don't meet time criteria
      List<Component> toDelete = components.stream()
          .filter(component -> component.lastUpdated().isBefore(olderThan))
          .collect(Collectors.toList());

      if (!toDelete.isEmpty()) {
        gavsWithDeletions.add(snapshotCandidate);
        for (Component component : toDelete) {
          log.debug("Deleting component: {}", component);
          tx.deleteComponent(component);
          if (++deleted % batchSize == 0) {
            log.debug("Committing batch delete");
            tx.commit();
            tx.begin();
          }
        }
        log.debug("Committing final batch delete");
        tx.commit();
        tx.begin();
      }
    }

    log.info("Finished processing snapshots with more than {} versions created before {}", config.getMinimumRetained(),
        olderThan);
    log.info("Deleted {} components from {} distinct GAVs", deleted, gavsWithDeletions.size());
    return gavsWithDeletions;
  }

  /**
   * Find all snapshots in this repository that have an associated release, taking into account the grace period.
   */
  private Iterable<Component> findReleasedSnapshots(final StorageTx tx, final Repository repository,
                                                    final Date gracePeriod)
  {
    final Bucket bucket = tx.findBucket(repository);
    final OResultSet<ODocument> result = tx.getDb().command(new OSQLSynchQuery<>(WITH_RELEASES))
        .execute(AttachedEntityHelper.id(bucket), gracePeriod);
    return Iterables.transform(result, componentEntityAdapter::readEntity);
  }

  /**
   * Find all snapshots in this repository for the given GAbV.
   */
  @VisibleForTesting
  Iterable<Component> findSnapshots(final StorageTx tx, final Repository repository, final GAV gav)
  {
    final Bucket bucket = tx.findBucket(repository);
    final OResultSet<ODocument> result = tx.getDb().command(new OSQLSynchQuery<>(SNAPSHOTS_FOR_GABV))
        .execute(AttachedEntityHelper.id(bucket), gav.group, gav.name, gav.baseVersion, gav.count);
    return Iterables.transform(result, componentEntityAdapter::readEntity);
  }

  /**
   * Find all GAVs that qualify for deletion.
   */
  @VisibleForTesting
  Iterable<GAV> findSnapshotCandidates(final StorageTx tx, final Repository repository, int minimumCount)
  {
    final Bucket bucket = tx.findBucket(repository);
    final OResultSet<ODocument> result = tx.getDb().command(new OSQLSynchQuery<>(SNAPSHOTS_WITH_MINIMUM_COUNT))
        .execute(AttachedEntityHelper.id(bucket), minimumCount);
    return Iterables.transform(result, (doc) -> {
          String group = doc.field(P_GROUP, String.class);
          String name = doc.field(P_NAME, String.class);
          String baseVersion = doc.field("baseVersion", String.class);
          Integer count = doc.field("cnt", Integer.class);
          return new GAV(group, name, baseVersion, count);
        }
    );
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
