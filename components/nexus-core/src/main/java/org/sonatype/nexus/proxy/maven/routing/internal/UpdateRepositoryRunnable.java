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
package org.sonatype.nexus.proxy.maven.routing.internal;

import java.io.IOException;

import org.sonatype.nexus.ApplicationStatusSource;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.routing.internal.task.CancelableRunnableSupport;
import org.sonatype.nexus.proxy.maven.routing.internal.task.ProgressListener;

import com.google.common.base.Throwables;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Job that performs prefix file updates and publishing of one single {@link MavenRepository}.
 *
 * @author cstamas
 * @since 2.4
 */
public class UpdateRepositoryRunnable
    extends CancelableRunnableSupport
{
  private final ApplicationStatusSource applicationStatusSource;

  private final ManagerImpl manager;

  private final MavenRepository mavenRepository;

  /**
   * Constructor.
   */
  public UpdateRepositoryRunnable(final ProgressListener progressListener,
                                  final ApplicationStatusSource applicationStatusSource,
                                  final ManagerImpl manager, final MavenRepository mavenRepository)
  {
    super(progressListener, mavenRepository.getId() + " AR-Updater");
    this.applicationStatusSource = checkNotNull(applicationStatusSource);
    this.manager = checkNotNull(manager);
    this.mavenRepository = checkNotNull(mavenRepository);
  }

  @Override
  protected void doRun() {
    if (!applicationStatusSource.getSystemStatus().isNexusStarted()) {
      log.warn("Nexus stopped during background prefix file updates for {}, bailing out.", mavenRepository);
      return;
    }
    try {
      manager.updateAndPublishPrefixFile(mavenRepository);
    }
    catch (Exception e) {
      try {
        manager.unpublish(mavenRepository);
      }
      catch (IOException ioe) {
        // silently
      }
      // propagate original exception
      Throwables.propagate(e);
    }
  }
}