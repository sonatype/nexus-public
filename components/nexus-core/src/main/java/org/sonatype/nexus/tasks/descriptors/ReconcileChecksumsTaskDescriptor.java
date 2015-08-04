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
package org.sonatype.nexus.tasks.descriptors;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.NumberTextFormField;
import org.sonatype.nexus.formfields.RepoOrGroupComboFormField;
import org.sonatype.nexus.formfields.StringTextFormField;

/**
 * @since 2.11.3
 */
@Named("ReconcileChecksums")
@Singleton
public class ReconcileChecksumsTaskDescriptor
    extends AbstractScheduledTaskDescriptor
{
  public static final String ID = "ReconcileChecksumsTask";

  public static final String REPO_OR_GROUP_FIELD_ID = "repositoryId";

  public static final String RESOURCE_STORE_PATH_FIELD_ID = "resourceStorePath";

  public static final String MODIFIED_SINCE_DATE_ID = "modifiedSinceDate";

  public static final String WALKING_LIMIT_TPS_FIELD_ID = "walkingLimitTps";

  private final RepoOrGroupComboFormField repoField = new RepoOrGroupComboFormField(REPO_OR_GROUP_FIELD_ID,
      FormField.MANDATORY);

  private final StringTextFormField resourceStorePathField = new StringTextFormField(RESOURCE_STORE_PATH_FIELD_ID,
      "Repository path",
      "Enter a repository path to run the task in recursively (ie. \"/\" for root or \"/org/apache\").",
      FormField.OPTIONAL);

  private final StringTextFormField modifiedSinceDateField = new StringTextFormField(MODIFIED_SINCE_DATE_ID,
      "Modified since (yyyy-MM-dd)",
      "Enter a date to limit reconciliation to those checksum attributes modified since the given date.",
      FormField.OPTIONAL).withInitialValue("2015-01-01");

  private final NumberTextFormField walkingLimitTpsField = new NumberTextFormField(WALKING_LIMIT_TPS_FIELD_ID,
      "Walking limit (tps)",
      "Set the walking limit to reduce the overhead of this task at the expense of it taking more time.",
      FormField.OPTIONAL).withInitialValue(100);

  public String getId() {
    return ID;
  }

  public String getName() {
    return "Reconcile Repository Checksums";
  }

  @Override
  public List<FormField> formFields() {
    final List<FormField> fields = new ArrayList<FormField>();

    fields.add(repoField);
    fields.add(resourceStorePathField);
    fields.add(modifiedSinceDateField);
    fields.add(walkingLimitTpsField);

    return fields;
  }

  @Override
  public boolean isExposed() {
    return true;
  }
}
