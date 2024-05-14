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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.formfields.StringTextFormField;
import org.sonatype.nexus.scheduling.TaskDescriptor;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

import static org.sonatype.nexus.common.app.FeatureFlags.H2_DATABASE_EXPORT_SCRIPT_TASK_ENABLED;
import static org.sonatype.nexus.formfields.FormField.OPTIONAL;

/**
 * A {@link TaskDescriptor} for exporting the SQL database to a script
 */
@Named
@Singleton
@FeatureFlag(name = H2_DATABASE_EXPORT_SCRIPT_TASK_ENABLED)
public class H2ExportDatabaseScriptTaskDescriptor
    extends TaskDescriptorSupport
{
  static final String TYPE_ID = "database.export.script.h2.task";

  static final String LOCATION = "location";

  @Inject
  public H2ExportDatabaseScriptTaskDescriptor()
  {
    super(TYPE_ID, H2ExportDatabaseScriptTask.class, "Admin - Export SQL database to script", VISIBLE, EXPOSED,
        new StringTextFormField(LOCATION, "Script Location",
        "File system location of the SQL script that will be generated. If not provided, defaults to sonatype-work/nexus3/db",
            OPTIONAL));
  }
}
