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
package org.sonatype.nexus.internal.security.secrets.task;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.logging.task.TaskLogType;
import org.sonatype.nexus.logging.task.TaskLogging;
import org.sonatype.nexus.scheduling.Cancelable;
import org.sonatype.nexus.scheduling.TaskSupport;
import org.sonatype.nexus.security.secrets.SecretsMigrator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Task to migrate secrets from external sources to a single source of truth
 */
@Named
@TaskLogging(TaskLogType.TASK_LOG_ONLY)
public class SecretsMigrationTask
    extends TaskSupport
    implements Cancelable
{
  private final List<SecretsMigrator> migrators;

  @Inject
  public SecretsMigrationTask(final List<SecretsMigrator> migrators) {
    this.migrators = checkNotNull(migrators);
  }

  @Override
  public String getMessage() {
    return "Migrate existing secrets into a single source (secrets table).";
  }

  @Override
  protected Object execute() throws Exception {
    for (SecretsMigrator migrator : migrators) {
      migrator.migrate();
    }
    return null;
  }
}
