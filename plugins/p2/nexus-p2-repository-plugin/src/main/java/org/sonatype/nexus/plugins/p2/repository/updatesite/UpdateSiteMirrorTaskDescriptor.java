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
package org.sonatype.nexus.plugins.p2.repository.updatesite;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.formfields.CheckboxFormField;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.RepoOrGroupComboFormField;
import org.sonatype.nexus.tasks.descriptors.AbstractScheduledTaskDescriptor;

@Named(UpdateSiteMirrorTask.ROLE_HINT)
@Singleton
public class UpdateSiteMirrorTaskDescriptor
    extends AbstractScheduledTaskDescriptor
{
  public static final String REPO_OR_GROUP_FIELD_ID = "repositoryId";

  public static final String FORCE_MIRROR_FIELD_ID = "ForceMirror";

  private final RepoOrGroupComboFormField repoField = new RepoOrGroupComboFormField(REPO_OR_GROUP_FIELD_ID,
      RepoOrGroupComboFormField.DEFAULT_LABEL, "Select Eclipse Update Site repository to assign to this task.",
      FormField.MANDATORY);

  private final CheckboxFormField forceField = new CheckboxFormField(FORCE_MIRROR_FIELD_ID, "Force mirror",
      "Mirror eclipse update site content even if site.xml did not change.", FormField.OPTIONAL);

  @Override
  public String getId() {
    return UpdateSiteMirrorTask.ROLE_HINT;
  }

  @Override
  public String getName() {
    return "Mirror Eclipse Update Site";
  }

  @Override
  public List<FormField> formFields() {
    final List<FormField> fields = new ArrayList<FormField>();

    fields.add(repoField);
    fields.add(forceField);

    return fields;
  }
}
