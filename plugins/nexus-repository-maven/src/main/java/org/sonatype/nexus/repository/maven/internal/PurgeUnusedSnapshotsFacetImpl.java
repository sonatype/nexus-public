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

import java.io.IOException;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.maven.MavenFacet;
import org.sonatype.nexus.repository.maven.PurgeUnusedSnapshotsFacet;
import org.sonatype.nexus.repository.maven.internal.group.MavenGroupFacet;
import org.sonatype.nexus.repository.maven.internal.hosted.metadata.MetadataRebuilder;
import org.sonatype.nexus.repository.maven.internal.hosted.metadata.MetadataUtils;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentEntityAdapter;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalDeleteBlob;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.scheduling.CancelableHelper;
import org.sonatype.nexus.scheduling.TaskInterruptedException;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import com.orientechnologies.orient.core.command.script.OCommandScript;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.record.impl.ODocument;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.sonatype.nexus.logging.task.TaskLoggingMarkers.PROGRESS;
import static org.sonatype.nexus.orient.entity.AttachedEntityHelper.id;
import static org.sonatype.nexus.repository.FacetSupport.State.STARTED;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_ARTIFACT_ID;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_BASE_VERSION;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_GROUP_ID;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_COMPONENT;

/**
 * Implementation of {@link PurgeUnusedSnapshotsFacet}. The implementation assumes that this facet will only be used
 * with hosted and group repositories.
 *
 * @since 3.0
 */
@Named
public class PurgeUnusedSnapshotsFacetImpl
    extends FacetSupport
    implements PurgeUnusedSnapshotsFacet
{
  private static final String UNUSED_QUERY =
      // first part of Orient command query - looping components
      "LET $a = (SELECT FROM component WHERE bucket = %s AND @rid > %s ORDER BY @rid LIMIT %d); " +
      // second part - asset join with GROUP BY for lastdownloaded date
      "LET $b = (SELECT component, max(ifnull(last_downloaded, blob_created)) as lastdownloaded " +
      "FROM asset WHERE (%s) GROUP BY component ORDER BY component); " +
      // third part - further filter out non-snapshots and non-date matches
      "SELECT FROM $b WHERE (" +
      "component.attributes.maven2.baseVersion LIKE '%%SNAPSHOT' AND lastdownloaded < '%s') " +
      // though always include the last record for looping USING 'WHERE @rid >'
      "OR component = $a[%d];";


  private static final String UNUSED_WHERE_QUERY = "(bucket = %s AND component = $a[%d])";

  private final ComponentEntityAdapter componentEntityAdapter;

  private final MetadataRebuilder metadataRebuilder;

  private final Type groupType;

  private final Type hostedType;

  private final int findUnusedLimit;

  private ORID lastComponent;

  @Inject
  public PurgeUnusedSnapshotsFacetImpl(final ComponentEntityAdapter componentEntityAdapter,
                                       final MetadataRebuilder metadataRebuilder,
                                       @Named(GroupType.NAME) final Type groupType,
                                       @Named(HostedType.NAME) final Type hostedType,
                                       @Named("${nexus.tasks.purgeUnusedSnapshots.findUnusedLimit:-50}")
                                           int findUnusedLimit)
  {
    this.componentEntityAdapter = checkNotNull(componentEntityAdapter);
    this.metadataRebuilder = checkNotNull(metadataRebuilder);
    this.groupType = checkNotNull(groupType);
    this.hostedType = checkNotNull(hostedType);
    this.findUnusedLimit = findUnusedLimit;
    checkArgument(findUnusedLimit >= 10 && findUnusedLimit <= 1000,
        "nexus.tasks.purgeUnusedSnapshots.findUnusedLimit must be between 10 and 1000");
  }

  @Override
  @Guarded(by = STARTED)
  public void purgeUnusedSnapshots(final int numberOfDays) {
    checkArgument(numberOfDays > 0, "Number of days must be greater than zero");
    log.info("Purging unused snapshots {} days or older from repository {}", numberOfDays, getRepository().getName());
    if (groupType.equals(getRepository().getType())) {
      processAsGroup(facet(MavenGroupFacet.class), numberOfDays);
    }
    else if (hostedType.equals(getRepository().getType())) {
      processAsHosted(numberOfDays);
    }
    else {
      log.debug("Skipping repository {}, is not group or hosted", getRepository().getName());
    }
  }

  /**
   * Processes this facet's associated repository as a hosted repository.
   */
  private void processAsHosted(final int numberOfDays) {
    Set<String> groups = purgeSnapshotsFromRepository(numberOfDays);
    for (String groupId : groups) {
      metadataRebuilder.rebuild(getRepository(), false, false, groupId, null, null);
    }
  }

  /**
   * Processes this facet's associated repository as a group repository, iterating over its members.
   */
  private void processAsGroup(final MavenGroupFacet groupFacet, final int numberOfDays) {
    groupFacet.members().stream()
        .filter(member -> hostedType.equals(member.getType()) || groupType.equals(member.getType()))
        .forEach(member -> member.facet(PurgeUnusedSnapshotsFacet.class).purgeUnusedSnapshots(numberOfDays));
  }

  /**
   * Purges snapshots from the given repository, returning a set of the affected groups for metadata rebuilding.
   */
  private Set<String> purgeSnapshotsFromRepository(final int numberOfDays) {
    LocalDate olderThan = LocalDate.now().minusDays(numberOfDays);
    UnitOfWork.beginBatch(facet(StorageFacet.class).txSupplier());
    try {
      return deleteUnusedSnapshotComponents(olderThan);
    }
    finally {
      UnitOfWork.end();
    }
  }

  /**
   * Deletes the unused snapshot components and their associated metadata.
   *
   * @return the affected groups
   */
  @TransactionalDeleteBlob
  protected Set<String> deleteUnusedSnapshotComponents(LocalDate olderThan) {
    StorageTx tx = UnitOfWork.currentTx();

    // entire component count is used just for reporting progress
    Repository repo = getRepository();
    long totalComponents = tx.countComponents(Query.builder().build(), singletonList(repo));
    log.info("Found {} total components in repository {} to evaluate for unused snapshots", totalComponents,
        repo.getName());

    ORID bucketId = id(tx.findBucket(getRepository()));
    String unusedWhereTemplate = getUnusedWhere(bucketId);

    Set<String> groups = new HashSet<>();
    long skipCount = 0;
    lastComponent = new ORecordId(-1, -1);
    while (skipCount < totalComponents && !isCanceled()) {
      log.info(PROGRESS,
          format("Processing components [%.2f%%] complete", ((double) skipCount / totalComponents) * 100));

      for (Component component : findNextPageOfUnusedSnapshots(tx, olderThan, bucketId, unusedWhereTemplate)) {
        if (isCanceled()) {
          return groups;
        }

        String groupId = deleteComponent(component);
        groups.add(groupId);
      }

      // commit each loop as well
      tx.commit();
      tx.begin();

      skipCount += findUnusedLimit;
    }

    return groups;
  }

  /**
   * Finds the next page of snapshot components that were last accessed before the specified date. The date when a
   * component was last downloaded is the last time an asset of that snapshot was downloaded.
   *
   * Uses an iterative approach in order to handle large repositories with many hundreds of thousands or millions of
   * assets. Note the current implementation is a bit hacky due to an Orient bug which prevents us from using a GROUP
   * BY with LIMIT. Furthermore, because of the required use of the Orient 'script sql' approach, we must loop through
   * the entire component set. Forward looking to Orient 3, it fixes the GROUP BY approach and this can be much
   * simplified. See NEXUS-13130 for further details.
   */
  private List<Component> findNextPageOfUnusedSnapshots(final StorageTx tx,
                                                        final LocalDate olderThan,
                                                        final ORID bucketId,
                                                        final String unusedWhereTemplate)
  {
    String query = format(UNUSED_QUERY, bucketId, lastComponent, findUnusedLimit, unusedWhereTemplate, olderThan,
        findUnusedLimit - 1);

    List<ODocument> result = tx.getDb().command(new OCommandScript("sql", query)).execute();

    if (isEmpty(result)) {
      return emptyList();
    }

    // get the last component to use in the WHERE for the next page
    ODocument lastDoc = Iterables.getLast(result).field(P_COMPONENT);
    lastComponent = lastDoc.getIdentity();

    Date olderThanDate = java.sql.Date.valueOf(olderThan);

    // filters in this stream are to check the last record as all others are filtered out in the 3rd part of the command query
    return result.stream() // remove entries that don't match on last download date
        .filter(entries -> {
          Date lastDownloaded = entries.field("lastdownloaded");
          return lastDownloaded.compareTo(olderThanDate) < 0;
        })
        .map(doc -> componentEntityAdapter.readEntity(doc.field(P_COMPONENT)))
        // remove entries that are not snapshots
        .filter(
            component -> {
              String baseVersion = (String) component.attributes().child(Maven2Format.NAME).get(P_BASE_VERSION);
              return baseVersion != null && baseVersion.endsWith("SNAPSHOT");
            })
        .collect(Collectors.toList());
  }

  private String deleteComponent(final Component component) {
    log.debug("Deleting unused snapshot component {}", component);
    MavenFacet facet = facet(MavenFacet.class);
    final StorageTx tx = UnitOfWork.currentTx();
    tx.deleteComponent(component);

    NestedAttributesMap attributes = component.formatAttributes();
    String groupId = attributes.get(P_GROUP_ID, String.class);
    String artifactId = attributes.get(P_ARTIFACT_ID, String.class);
    String baseVersion = attributes.get(P_BASE_VERSION, String.class);

    try {
      // We have to delete all metadata through GAV levels and rebuild in the next step, as the MetadataRebuilder
      // isn't meant to remove metadata that has been orphaned by the deletion of a component
      MavenFacetUtils.deleteWithHashes(facet, MetadataUtils.metadataPath(groupId, artifactId, baseVersion));
      MavenFacetUtils.deleteWithHashes(facet, MetadataUtils.metadataPath(groupId, artifactId, null));
      MavenFacetUtils.deleteWithHashes(facet, MetadataUtils.metadataPath(groupId, null, null));
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
    return groupId;
  }

  /**
   * The Orient workaround (see https://www.prjhub.com/#/issues/8724) requires us to explicitly reference each record in
   * the count. This method produces that part of the WHERE clause for the unused snapshots query.
   */
  @VisibleForTesting
  String getUnusedWhere(final ORID bucketId) {
    return IntStream.range(0, findUnusedLimit)
        .mapToObj(i -> format(UNUSED_WHERE_QUERY, bucketId, i))
        .collect(Collectors.joining(" OR "));
  }

  private boolean isCanceled() {
    try {
      CancelableHelper.checkCancellation();
      return false;
    }
    catch (TaskInterruptedException e) { // NOSONAR
      log.warn("Purge unused Maven snapshots job is canceled");
      return true;
    }
  }
}
