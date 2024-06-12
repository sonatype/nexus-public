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
package org.sonatype.nexus.repository.maven.tasks;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.formfields.CheckboxFormField;
import org.sonatype.nexus.formfields.NumberTextFormField;
import org.sonatype.nexus.formfields.RepositoryCombobox;
import org.sonatype.nexus.repository.maven.RemoveSnapshotsFacet;
import org.sonatype.nexus.repository.maven.VersionPolicy;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

/**
 * Configuration definition for {@link RemoveSnapshotsTask}
 * @since 3.0
 */
@AvailabilityVersion(from = "1.0")
@Named
@Singleton
public class RemoveSnapshotsTaskDescriptor
    extends TaskDescriptorSupport
{
  public static final String TYPE_ID = "repository.maven.remove-snapshots";

  public static final String REPOSITORY_NAME_FIELD_ID = "repositoryName";
  
  public static final String MINIMUM_SNAPSHOT_RETAINED_COUNT = "minimumRetained";
  
  public static final String SNAPSHOT_RETENTION_DAYS = "snapshotRetentionDays";
  
  public static final String REMOVE_IF_RELEASED = "removeIfReleased";
  
  public static final String GRACE_PERIOD = "gracePeriodInDays";
  
  public RemoveSnapshotsTaskDescriptor()
  {
    super(TYPE_ID,
        RemoveSnapshotsTask.class,
        "Maven - Delete SNAPSHOT",
        VISIBLE,
        EXPOSED,
        new RepositoryCombobox(REPOSITORY_NAME_FIELD_ID,
            "Repository",
            "Select the Maven repository or repository group to remove snapshots from.",
            true).includingAnyOfFacets(RemoveSnapshotsFacet.class)
            .excludingAnyOfVersionPolicies(VersionPolicy.RELEASE.name())
            .includeAnEntryForAllRepositories(),
        new NumberTextFormField(MINIMUM_SNAPSHOT_RETAINED_COUNT,
            "Minimum snapshot count",
            "Minimum number of snapshots to keep for one GAV.",
            true).withInitialValue(1).withMinimumValue(-1),
        new NumberTextFormField(SNAPSHOT_RETENTION_DAYS,
            "Snapshot retention (days)",
            "Delete all snapshots older than this, provided we still keep the minimum number specified.",
            true).withInitialValue(30).withMinimumValue(0),
        new CheckboxFormField(REMOVE_IF_RELEASED,
            "Remove if released",
            "Delete all snapshots that have a corresponding release", false),
        new NumberTextFormField(GRACE_PERIOD,
            "Grace period after release (days)",
            "The grace period during which snapshots with an associated release will not be deleted.",
            false).withMinimumValue(0));
  }
}
