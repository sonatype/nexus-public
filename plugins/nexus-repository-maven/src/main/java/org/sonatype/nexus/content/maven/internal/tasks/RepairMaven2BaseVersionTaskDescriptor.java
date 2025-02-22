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
package org.sonatype.nexus.content.maven.internal.tasks;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.RepositoryCombobox;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

import static org.sonatype.nexus.repository.RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID;

@AvailabilityVersion(from = "1.0")
@Named
@Singleton
public class RepairMaven2BaseVersionTaskDescriptor
    extends TaskDescriptorSupport
{
  public static final String TASK_NAME = "Maven - Repair Base Version";

  public static final String TYPE_ID = "repository.maven.repair-base-version";

  public RepairMaven2BaseVersionTaskDescriptor()
  {
    super(TYPE_ID,
        RepairMaven2BaseVersionTask.class,
        TASK_NAME,
        VISIBLE,
        EXPOSED,
        new RepositoryCombobox(
            REPOSITORY_NAME_FIELD_ID,
            "Repository",
            "Select the repository to repair base version in",
            FormField.MANDATORY
        ).includingAnyOfFormats(Maven2Format.NAME).includingAnyOfTypes(HostedType.NAME)
            .includeAnEntryForAllRepositories()
    );
  }
}
