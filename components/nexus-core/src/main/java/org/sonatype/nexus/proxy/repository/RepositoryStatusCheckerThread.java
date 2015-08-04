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
package org.sonatype.nexus.proxy.repository;

import org.sonatype.nexus.SystemStatus;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.slf4j.Logger;

public class RepositoryStatusCheckerThread
    extends Thread
{
  private final Logger logger;

  private final SystemStatus systemStatus;

  private final ProxyRepository repository;

  private volatile boolean running;

  public RepositoryStatusCheckerThread(final Logger logger, final SystemStatus systemStatus,
      final ProxyRepository repository)
  {
    super("RepositoryStatusChecker-" + repository.getId());

    this.logger = logger;

    this.systemStatus = systemStatus;

    this.repository = repository;

    this.running = true;
  }

  public ProxyRepository getRepository() {
    return repository;
  }

  public boolean isRunning() {
    return running;
  }

  public void setRunning(boolean val) {
    this.running = val;
  }

  public void run() {
    boolean interrupted = false;

    while (isRunning() && getRepository().getProxyMode() != null) {

      // wait for Nexus to fully start
      if (!systemStatus.isNexusStarted()) {
        try {
          Thread.sleep(5 * 1000);
        }
        catch (InterruptedException e) {
          Thread.yield();
        }
        continue; // check again
      }

      // if interrupted from sleep, since autoBlock happened, do NOT try to unblock it immediately
      // it has to sleep the 1st amount of time repo says, and THEN try to unblock it
      if (!interrupted) {
        LocalStatus repositoryLocalStatus = getRepository().getLocalStatus();

        // check only if repository is in service
        if (repositoryLocalStatus.shouldServiceRequest()) {
          // get status check mode
          RepositoryStatusCheckMode repositoryStatusCheckMode =
              getRepository().getRepositoryStatusCheckMode();

          if (RepositoryStatusCheckMode.ALWAYS.equals(repositoryStatusCheckMode)) {
            // just do it, don't care for proxyMode
            getRepository().getRemoteStatus(new ResourceStoreRequest(RepositoryItemUid.PATH_ROOT), true);
          }
          else if (RepositoryStatusCheckMode.AUTO_BLOCKED_ONLY.equals(repositoryStatusCheckMode)) {
            // do it only if proxyMode , don't care for proxyMode
            ProxyMode repositoryProxyMode = getRepository().getProxyMode();

            if (repositoryProxyMode.shouldAutoUnblock()) {
              getRepository().getRemoteStatus(new ResourceStoreRequest(RepositoryItemUid.PATH_ROOT),
                  true);
            }
          }
          else if (RepositoryStatusCheckMode.NEVER.equals(repositoryStatusCheckMode)) {
            // nothing
          }
        }
      }

      try {
        long sleepTime = getRepository().getNextRemoteStatusRetainTime();

        // say this message only if repository is auto-blocked, regardless of repositoryStatusCheckMode
        if (getRepository().getProxyMode().shouldAutoUnblock()) {
          logger.info(
              "Next attempt to auto-unblock the \"" + getRepository().getName() + "\" (id="
                  + getRepository().getId()
                  + ") repository by checking its remote peer health will occur in "
                  + DurationFormatUtils.formatDurationWords(sleepTime, true, true) + ".");
        }

        Thread.sleep(sleepTime);

        interrupted = false;
      }
      catch (InterruptedException e) {
        // just ignore it, isRunning() will take care.
        interrupted = true;
      }
    }
  }
}
