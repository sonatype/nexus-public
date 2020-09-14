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
package org.sonatype.nexus.repository.p2.upgrade.tasks;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.LifecycleSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.repository.p2.upgrade.P2Upgrade_1_2;
import org.sonatype.nexus.scheduling.TaskScheduler;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.TASKS;
import static org.sonatype.nexus.repository.p2.upgrade.P2Upgrade_1_2.MARKER_FILE;
import static org.sonatype.nexus.repository.p2.upgrade.tasks.P2RewriteCompositeMetadataTaskDescriptor.TYPE_ID;

/**
 * Instantiates rewriting composite metadata sites to use site hashes based on the existence of a marker
 * file created by the upgrade {@link P2Upgrade_1_2}.
 *
 * @since 1.1
 */
@Named
@Singleton
@ManagedLifecycle(phase = TASKS)
public class P2RewriteCompositeMetadataService
    extends LifecycleSupport
{
  private final Path markerFile;

  private final TaskScheduler taskScheduler;

  @Inject
  public P2RewriteCompositeMetadataService(
      final ApplicationDirectories directories,
      final TaskScheduler taskScheduler)
  {
    markerFile = new File(directories.getWorkDirectory("db"), MARKER_FILE).toPath();
    this.taskScheduler = checkNotNull(taskScheduler);
  }

  @Override
  protected void doStart() throws Exception {
    if (Files.exists(markerFile)) {
      log.info("Scheduling task: {}", TYPE_ID);
      taskScheduler.submit(taskScheduler.createTaskConfigurationInstance(TYPE_ID));
    }
  }
}
