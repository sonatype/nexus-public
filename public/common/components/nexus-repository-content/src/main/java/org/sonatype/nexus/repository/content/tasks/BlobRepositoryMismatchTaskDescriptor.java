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
package org.sonatype.nexus.repository.content.tasks;

import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.RepositoryCombobox;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static org.sonatype.nexus.repository.RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID;

@AvailabilityVersion(from = "1.0")
@Component
public class BlobRepositoryMismatchTaskDescriptor
    extends TaskDescriptorSupport
{
  public static final String TYPE_ID = "repository.blob.mismatch.task";

  @Autowired
  public BlobRepositoryMismatchTaskDescriptor(
      @Value("${repository.blob.mismatch.task:false}") final boolean visibleExposed)
  {
    super(TYPE_ID, BlobRepositoryMismatchTask.class, "Find any blob with a mismatch on Bucket.repo-name and " +
        "fix it in the properties file",
        true, visibleExposed, visibleExposed,
        new RepositoryCombobox(
            REPOSITORY_NAME_FIELD_ID,
            "Repository",
            "Select the repository to fix blob mismatches",
            FormField.MANDATORY).includeAnEntryForAllRepositories()
                .excludingAnyOfTypes(GroupType.NAME)
                .excludingAnyOfTypes(ProxyType.NAME));
  }
}
