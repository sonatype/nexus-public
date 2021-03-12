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
package org.sonatype.nexus.content.maven.internal.recipe;

import java.time.LocalDate;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.content.maven.MavenContentFacet;
import org.sonatype.nexus.content.maven.store.Maven2ComponentStore;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.maven.PurgeUnusedSnapshotsFacet;
import org.sonatype.nexus.repository.maven.internal.group.MavenGroupFacet;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.scheduling.CancelableHelper;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.FacetSupport.State.STARTED;

/**
 * Implementation of {@link PurgeUnusedSnapshotsFacet} for the NewDB. The implementation assumes that this facet will
 * only be used with hosted and group repositories.
 *
 * @since 3.30
 */
@Named
public class PurgeUnusedSnapshotsFacetImpl
    extends FacetSupport
    implements PurgeUnusedSnapshotsFacet
{

  private final Type groupType;

  private final Type hostedType;

  private final int findUnusedLimit;

  @Inject
  public PurgeUnusedSnapshotsFacetImpl(
      @Named(GroupType.NAME) final Type groupType,
      @Named(HostedType.NAME) final Type hostedType,
      @Named("${nexus.tasks.purgeUnusedSnapshots.findUnusedLimit:-100}") int findUnusedLimit)
  {
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
      purgeSnapshotsFromRepository(numberOfDays);
    }
    else {
      log.debug("Skipping repository {}, is not group or hosted", getRepository().getName());
    }
  }

  /**
   * Processes this facet's associated repository as a group repository, iterating over its members.
   */
  private void processAsGroup(final MavenGroupFacet groupFacet, final int numberOfDays) {
    groupFacet.leafMembers().stream()
        .filter(member -> hostedType.equals(member.getType()))
        .forEach(member -> member.facet(PurgeUnusedSnapshotsFacet.class).purgeUnusedSnapshots(numberOfDays));
  }

  /**
   * Purges snapshots from the given repository.
   */
  private void purgeSnapshotsFromRepository(final int numberOfDays) {
    LocalDate olderThan = LocalDate.now().minusDays(numberOfDays);
    deleteUnusedSnapshotComponents(olderThan);
  }

  /**
   * Deletes the unused snapshot components and their associated assets and metadata.
   */
  private void deleteUnusedSnapshotComponents(final LocalDate olderThan) {
    Repository repository = getRepository();
    MavenContentFacetImpl contentFacet = (MavenContentFacetImpl) repository.facet(MavenContentFacet.class);
    Maven2ComponentStore componentStore = (Maven2ComponentStore) contentFacet.stores().componentStore;

    // totalComponents is used just for the reporting process
    long totalComponents = contentFacet.components().count();
    log.info("Found {} total components in repository {} to evaluate for unused snapshots", totalComponents,
        repository.getName());

    while (!isCanceled()) {

      // During every new iteration, first components are already removed, so no offset needed
      int[] componentIds = componentStore
          .selectUnusedSnapshots(contentFacet.contentRepositoryId(), olderThan, findUnusedLimit)
          .stream()
          .mapToInt(id -> id)
          .toArray();

      if (componentIds.length == 0) {
        return;
      }

      contentFacet.deleteComponents(componentIds);
    }
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
