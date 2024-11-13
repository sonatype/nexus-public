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
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.maven.MavenFacet;
import org.sonatype.nexus.repository.maven.PurgeUnusedSnapshotsFacet;
import org.sonatype.nexus.repository.maven.VersionPolicy;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.scheduling.Cancelable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Task to purge unused snapshots of the given Maven repository.
 *
 * @since 3.0
 */
@Named
public class PurgeMavenUnusedSnapshotsTask
    extends RepositoryTaskSupport
    implements Cancelable
{
  public static final String LAST_USED_FIELD_ID = "lastUsed";

  private static final String MAVEN = "maven";

  private static final String VERSION_POLICY = "versionPolicy";

  private final Type groupType;

  private final Type hostedType;

  private final Format maven2Format;

  @Inject
  public PurgeMavenUnusedSnapshotsTask(
      @Named(GroupType.NAME) final Type groupType,
      @Named(HostedType.NAME) final Type hostedType,
      @Named(Maven2Format.NAME) final Format maven2Format)
  {
    this.groupType = checkNotNull(groupType);
    this.hostedType = checkNotNull(hostedType);
    this.maven2Format = checkNotNull(maven2Format);
  }

  @Override
  protected void execute(final Repository repository) {
    repository.facet(PurgeUnusedSnapshotsFacet.class)
        .purgeUnusedSnapshots(getConfiguration().getInteger(LAST_USED_FIELD_ID, -1));
  }

  @Override
  protected boolean appliesTo(final Repository repository) {
    return hasExpectedFormat(repository) && !isReleaseRepo(repository);
  }

  /**
   * Validates if the passed repository has the expected format to run the task
   *
   * @param repository the repository to be validated
   * @return a {@link Boolean} flag representing if the format is valid or not
   */
  private boolean hasExpectedFormat(final Repository repository) {
    return maven2Format.equals(repository.getFormat())
        && (hostedType.equals(repository.getType()) || groupType.equals(repository.getType()));
  }

  private boolean isReleaseRepo(final Repository repository) {
    VersionPolicy versionPolicy = repository.facet(MavenFacet.class).getVersionPolicy();
    return VersionPolicy.RELEASE.equals(versionPolicy);
  }

  @Override
  public String getMessage() {
    return "Purge unused Maven snapshot versions from " + getRepositoryField();
  }
}
