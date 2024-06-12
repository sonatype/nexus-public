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
package org.sonatype.nexus.internal.datastore.task;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.formfields.StringTextFormField;
import org.sonatype.nexus.scheduling.TaskDescriptor;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

import static org.sonatype.nexus.formfields.FormField.MANDATORY;

/**
 * A {@link TaskDescriptor} for backing up an embedded H2 datastore.
 *
 * @since 3.21
 */
@AvailabilityVersion(from = "1.0")
@Named
@Singleton
public class H2BackupTaskDescriptor
    extends TaskDescriptorSupport
{
  static final String TYPE_ID = "h2.backup.task";

  static final String LOCATION = "location";

  public H2BackupTaskDescriptor()
  {
    super(TYPE_ID, H2BackupTask.class, "Admin - Backup H2 Database", VISIBLE, EXPOSED,
        new StringTextFormField(LOCATION, "Location",
            "Specify a directory for the database backup",
            MANDATORY));
  }
}
