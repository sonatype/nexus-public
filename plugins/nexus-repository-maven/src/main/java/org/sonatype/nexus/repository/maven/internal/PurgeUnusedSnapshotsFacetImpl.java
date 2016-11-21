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
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.orient.entity.AttachedEntityHelper;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.maven.MavenFacet;
import org.sonatype.nexus.repository.maven.PurgeUnusedSnapshotsFacet;
import org.sonatype.nexus.repository.maven.internal.group.MavenGroupFacet;
import org.sonatype.nexus.repository.maven.internal.hosted.metadata.MetadataRebuilder;
import org.sonatype.nexus.repository.maven.internal.hosted.metadata.MetadataUtils;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentEntityAdapter;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalDeleteBlob;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.FacetSupport.State.STARTED;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_ARTIFACT_ID;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_BASE_VERSION;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_GROUP_ID;
import static org.sonatype.nexus.repository.maven.internal.Constants.SNAPSHOT_VERSION_SUFFIX;
import static org.sonatype.nexus.repository.maven.internal.Maven2Format.NAME;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_COMPONENT;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_LAST_ACCESSED;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_ATTRIBUTES;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_BUCKET;

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

  private static final String FIND_SNAPSHOTS_SQL = String.format(
      "SELECT FROM (SELECT %s, MAX(%s) AS lastAccessed FROM asset WHERE %s=:bucket AND %s IS NOT NULL GROUP BY %s) WHERE lastAccessed < :olderThan AND %s.%s['%s']['%s'] LIKE '%%%s'",
      P_COMPONENT, P_LAST_ACCESSED, P_BUCKET, P_COMPONENT, P_COMPONENT, P_COMPONENT,
      P_ATTRIBUTES, NAME, P_BASE_VERSION, SNAPSHOT_VERSION_SUFFIX
  );

  private final ComponentEntityAdapter componentEntityAdapter;

  private final MetadataRebuilder metadataRebuilder;

  private final Type groupType;

  private final Type hostedType;

  @Inject
  public PurgeUnusedSnapshotsFacetImpl(final ComponentEntityAdapter componentEntityAdapter,
                                       final MetadataRebuilder metadataRebuilder,
                                       @Named(GroupType.NAME) final Type groupType,
                                       @Named(HostedType.NAME) final Type hostedType)
  {
    this.componentEntityAdapter = checkNotNull(componentEntityAdapter);
    this.metadataRebuilder = checkNotNull(metadataRebuilder);
    this.groupType = checkNotNull(groupType);
    this.hostedType = checkNotNull(hostedType);
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
        .forEach(member -> {
          member.facet(PurgeUnusedSnapshotsFacet.class).purgeUnusedSnapshots(numberOfDays);
        });
  }

  /**
   * Purges snapshots from the given repository, returning a set of the affected groups for metadata rebuilding.
   */
  private Set<String> purgeSnapshotsFromRepository(final int numberOfDays) {
    Date olderThan = DateTime.now().minusDays(numberOfDays).withTimeAtStartOfDay().toDate();
    Set<String> groups = Collections.emptySet();
    UnitOfWork.beginBatch(facet(StorageFacet.class).txSupplier());
    try {
      groups = deleteUnusedSnapshotComponents(olderThan);
    }
    finally {
      UnitOfWork.end();
    }
    return groups;
  }

  /**
   * Deletes the unused snapshot components and their associated metadata.
   *
   * @return the affected groups
   */
  @TransactionalDeleteBlob
  protected Set<String> deleteUnusedSnapshotComponents(Date olderThan) {
    MavenFacet facet = facet(MavenFacet.class);
    StorageTx tx = UnitOfWork.currentTx();

    Set<String> groups = new HashSet<>();
    for (Component component : findUnusedSnapshots(tx, olderThan)) {
      log.debug("Deleting unused snapshot component {}", component);
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
        Throwables.propagate(e);
      }

      groups.add(groupId);
    }
    return groups;
  }

  /**
   * Finds all snapshot components that were last accessed before the specified date. The date when a component was
   * last accessed is the last time an asset of that snapshot was last accessed.
   */
  private Iterable<Component> findUnusedSnapshots(final StorageTx tx, final Date olderThan) {
    final Bucket bucket = tx.findBucket(getRepository());
    Map<String, Object> sqlParams = ImmutableMap.of(
        "bucket", AttachedEntityHelper.id(bucket),
        "olderThan", olderThan
    );
    return Iterables.transform(tx.browse(FIND_SNAPSHOTS_SQL, sqlParams),
        (doc) -> componentEntityAdapter.readEntity(doc.field(P_COMPONENT)));
  }
}
