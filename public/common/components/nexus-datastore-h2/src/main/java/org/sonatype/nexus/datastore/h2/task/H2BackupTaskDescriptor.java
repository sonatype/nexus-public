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
package org.sonatype.nexus.datastore.h2.task;

import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.formfields.StringTextFormField;
import org.sonatype.nexus.scheduling.TaskDescriptor;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.formfields.FormField.MANDATORY;

/**
 * A {@link TaskDescriptor} for backing up an embedded H2 datastore.
 *
 * The task descriptor is only visible/exposed when running against H2. On
 * PostgreSQL-backed installations the H2 backup task has no meaningful
 * implementation (the underlying MyBatis data store rejects {@code backup()}
 * with {@code UnsupportedOperationException}), so hiding the descriptor keeps
 * it out of the scheduler UI and REST API.
 */
@AvailabilityVersion(from = "1.0")
@Component
public class H2BackupTaskDescriptor
    extends TaskDescriptorSupport
{
  static final String TYPE_ID = "h2.backup.task";

  static final String LOCATION = "location";

  private final DatabaseCheck databaseCheck;

  @Autowired
  public H2BackupTaskDescriptor(final DatabaseCheck databaseCheck) {
    super(TYPE_ID, H2BackupTask.class, "Admin - Backup H2 Database", VISIBLE, EXPOSED,
        new StringTextFormField(LOCATION, "Location",
            "Specify a directory for the database backup",
            MANDATORY));
    this.databaseCheck = checkNotNull(databaseCheck);
  }

  @Override
  public boolean isVisible() {
    return !databaseCheck.isPostgresql();
  }

  @Override
  public boolean isExposed() {
    return !databaseCheck.isPostgresql();
  }
}
