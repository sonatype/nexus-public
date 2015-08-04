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

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.formfields.CheckboxFormField;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.NumberTextFormField;
import org.sonatype.nexus.formfields.RepoComboFormField;
import org.sonatype.nexus.formfields.RepoTargetComboFormField;
import org.sonatype.nexus.tasks.descriptors.AbstractScheduledTaskDescriptor;

import com.google.common.collect.ImmutableList;

/**
 * @since 2.5
 */
@Named
@Singleton
public class ReleaseRemovalTaskDescriptor
    extends AbstractScheduledTaskDescriptor
{

  public static final String ID = "ReleaseRemoverTask";

  public static final String REPOSITORY_FIELD_ID = "repositoryId";

  public static final String NUMBER_OF_VERSIONS_TO_KEEP_FIELD_ID = "numberOfVersionsToKeep";

  public static final String INDEX_BACKEND = "indexBackend";

  public static final String REPOSITORY_TARGET_FIELD_ID = "repositoryTarget";

  private final List<FormField> formFields = ImmutableList.<FormField>of(
      new RepoComboFormField(REPOSITORY_FIELD_ID, FormField.MANDATORY),
      new NumberTextFormField(
          NUMBER_OF_VERSIONS_TO_KEEP_FIELD_ID, "Number to keep", "The number of versions for each GA to keep",
          FormField.MANDATORY),
      new RepoTargetComboFormField(REPOSITORY_TARGET_FIELD_ID, "Repository Target",
          "Select a repository target to apply", FormField.OPTIONAL),
      new CheckboxFormField(INDEX_BACKEND, "Use index",
          "Use Maven Index to walk the repository? Defaults to crawling if unchecked", FormField.MANDATORY)
  );

  public String getId() {
    return ID;
  }

  public String getName() {
    return "Remove Releases From Repository";
  }

  @Override
  public List<FormField> formFields() {
    return formFields;
  }
}
