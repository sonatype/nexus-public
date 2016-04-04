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

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.maven.MavenFacet;
import org.sonatype.nexus.repository.maven.RemoveSnapshotsFacet;
import org.sonatype.nexus.repository.maven.VersionPolicy;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.scheduling.TaskConfiguration;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.maven.tasks.RemoveSnapshotsTaskDescriptor.GRACE_PERIOD;
import static org.sonatype.nexus.repository.maven.tasks.RemoveSnapshotsTaskDescriptor.MINIMUM_SNAPSHOT_RETAINED_COUNT;
import static org.sonatype.nexus.repository.maven.tasks.RemoveSnapshotsTaskDescriptor.REMOVE_IF_RELEASED;
import static org.sonatype.nexus.repository.maven.tasks.RemoveSnapshotsTaskDescriptor.SNAPSHOT_RETENTION_DAYS;

/**
 * Task to remove snapshots from a Maven repository.
 * @since 3.0
 */
@Named
public class RemoveSnapshotsTask
    extends RepositoryTaskSupport
{
  private final Format maven2Format;
  
  @Inject
  public RemoveSnapshotsTask(@Named(Maven2Format.NAME) final Format maven2Format)
  {
    this.maven2Format = checkNotNull(maven2Format);
  }
  
  @Override
  protected void execute(final Repository repository) {
    log.info("Executing removal of snapshots on repository: {}", repository);
    TaskConfiguration config = getConfiguration();
    RemoveSnapshotsConfig removeSnapshotsConfig = new RemoveSnapshotsConfig(
        config.getInteger(MINIMUM_SNAPSHOT_RETAINED_COUNT, 1),
        config.getInteger(SNAPSHOT_RETENTION_DAYS, 30),
        config.getBoolean(REMOVE_IF_RELEASED, false),
        config.getInteger(GRACE_PERIOD, -1));
    repository.facet(RemoveSnapshotsFacet.class).removeSnapshots(removeSnapshotsConfig);
  }

  @Override
  protected boolean appliesTo(final Repository repository) {
    return maven2Format.equals(repository.getFormat())
        && repository.optionalFacet(RemoveSnapshotsFacet.class).isPresent()
        && repository.facet(MavenFacet.class).getVersionPolicy() != VersionPolicy.RELEASE;
  }

  @Override
  public String getMessage() {
    return "Remove Maven snapshots from " + getRepositoryField();
  }
}
