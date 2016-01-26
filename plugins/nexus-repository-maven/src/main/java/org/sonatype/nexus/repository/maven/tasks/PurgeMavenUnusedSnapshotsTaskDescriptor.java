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

import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.NumberTextFormField;
import org.sonatype.nexus.formfields.RepositoryCombobox;
import org.sonatype.nexus.repository.maven.PurgeUnusedSnapshotsFacet;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

import static org.sonatype.nexus.repository.RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID;
import static org.sonatype.nexus.repository.maven.tasks.PurgeMavenUnusedSnapshotsTask.LAST_USED_FIELD_ID;

/**
 * Task descriptor for {@link PurgeMavenUnusedSnapshotsTask}.
 *
 * @since 3.0
 */
@Named
@Singleton
public class PurgeMavenUnusedSnapshotsTaskDescriptor
    extends TaskDescriptorSupport
{
  public static final String TYPE_ID = "repository.maven.purge-unused-snapshots";

  public PurgeMavenUnusedSnapshotsTaskDescriptor() {
    super(TYPE_ID,
        PurgeMavenUnusedSnapshotsTask.class,
        "Purge unused Maven snapshot versions",
        VISIBLE,
        EXPOSED,
        new RepositoryCombobox(
            REPOSITORY_NAME_FIELD_ID,
            "Repository",
            "Select the repository to purge unused snapshot versions from",
            FormField.MANDATORY
        ).includingAnyOfFacets(PurgeUnusedSnapshotsFacet.class).includeAnEntryForAllRepositories(),
        new NumberTextFormField(
            LAST_USED_FIELD_ID,
            "Last used in days",
            "Purge all snapshots that were last used before given number of days",
            FormField.MANDATORY
        )
    );
  }
}
