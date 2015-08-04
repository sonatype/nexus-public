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
package org.sonatype.nexus.maven.tasks.descriptors;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.NumberTextFormField;
import org.sonatype.nexus.formfields.RepoOrGroupComboFormField;
import org.sonatype.nexus.tasks.descriptors.AbstractScheduledTaskDescriptor;

import com.google.common.collect.Lists;

/**
 * @since 2.7.0
 */
@Named("UnusedSnapshotRemoval")
@Singleton
public class UnusedSnapshotRemovalTaskDescriptor
    extends AbstractScheduledTaskDescriptor
{

  public static final String ID = "UnusedSnapshotRemoverTask";

  public static final String REPO_OR_GROUP_FIELD_ID = "repositoryId";

  public static final String DAYS_SINCE_LAST_REQUESTED_FIELD_ID = "daysSinceLastRequested";

  private final List<FormField> formFields;

  @Inject
  public UnusedSnapshotRemovalTaskDescriptor() {
    formFields = Lists.<FormField>newArrayList(
        new RepoOrGroupComboFormField(
            REPO_OR_GROUP_FIELD_ID,
            FormField.MANDATORY
        ),
        new NumberTextFormField(
            DAYS_SINCE_LAST_REQUESTED_FIELD_ID,
            "Days since last requested",
            "The job will purge all snapshots that were last time requested from Nexus before the specified number of days",
            FormField.MANDATORY
        )
    );
  }

  public String getId() {
    return ID;
  }

  public String getName() {
    return "Remove Unused Snapshots From Repository";
  }

  public List<FormField> formFields() {
    return formFields;
  }

}
