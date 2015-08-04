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
package org.sonatype.nexus.testsuite;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Provider;

import org.sonatype.nexus.bundle.launcher.NexusBundle;
import org.sonatype.nexus.bundle.launcher.NexusBundleConfiguration;
import org.sonatype.nexus.client.core.NexusClient;
import org.sonatype.nexus.client.core.subsystem.repository.Repositories;
import org.sonatype.nexus.testsuite.client.Events;
import org.sonatype.nexus.testsuite.client.RemoteLoggerFactory;
import org.sonatype.nexus.testsuite.client.Scheduler;
import org.sonatype.nexus.testsuite.support.NexusParametrizedITSupport;
import org.sonatype.nexus.testsuite.support.NexusStartAndStopStrategy;

import com.google.common.base.Throwables;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.*;

/**
 * Suppot class for case when two nexus instances are needed to be tested how they interact.
 */
@NexusStartAndStopStrategy(NexusStartAndStopStrategy.Strategy.EACH_TEST)
public abstract class TwinNexusITSupport
    extends NexusParametrizedITSupport
{
  private static final Logger LOG = LoggerFactory.getLogger(TwinNexusITSupport.class);

  public TwinNexusITSupport(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  /**
   * Provider used to create Nexus bundles on demand.
   */
  @Inject
  private Provider<NexusBundle> nexusProvider;

  private NexusBundle remote;

  private NexusBundle local;

  private NexusClient remoteClient;

  private NexusClient localClient;

  @Before
  public void beforeTestIsRunning() {
    startNexus(remote());
    assertThat("Remote Nexus was not in running state", remote().isRunning());

    remoteClient = createNexusClientForAdmin(remote());
    logRemoteThatTestIs(remoteRemoteLogger(), "STARTING");

    startNexus(local());
    assertThat("Local Nexus was not in running state", local().isRunning());

    localClient = createNexusClientForAdmin(local());
    logRemoteThatTestIs(localRemoteLogger(), "STARTING");
  }

  @After
  public void afterTestWasRunning() {
    if (remote != null) {
      logRemoteThatTestIs(remoteRemoteLogger(), "FINISHED");

      testIndex.recordAndCopyLink(
          "master wrapper.log", new File(remote.getNexusDirectory(), "logs/wrapper.log")
      );
      testIndex.recordAndCopyLink(
          "master nexus.log", new File(remote.getWorkDirectory(), "logs/nexus.log")
      );
    }
    if (local != null) {
      logRemoteThatTestIs(localRemoteLogger(), "FINISHED");

      testIndex.recordAndCopyLink(
          "slave wrapper.log", new File(local.getNexusDirectory(), "logs/wrapper.log")
      );
      testIndex.recordAndCopyLink(
          "slave nexus.log", new File(local.getWorkDirectory(), "logs/nexus.log")
      );
    }

    stopNexus(local);
    stopNexus(remote);
  }

  protected NexusBundle remote() {
    if (remote == null) {
      remote = nexusProvider.get();
      NexusBundleConfiguration configuration = configureNexus(
          applyDefaultConfiguration(remote).getConfiguration()
      );
      if (configuration != null) {
        configuration.setId("master");
        remote.setConfiguration(configuration);
        configuration = configureRemote(configuration);
        if (configuration != null) {
          remote.setConfiguration(configuration);
        }
      }
    }
    return remote;
  }

  protected NexusBundle local() {
    if (local == null) {
      local = nexusProvider.get();
      NexusBundleConfiguration configuration = configureNexus(
          applyDefaultConfiguration(local).getConfiguration()
      );
      if (configuration != null) {
        configuration.setId("slave");
        local.setConfiguration(configuration);
        configuration = configureLocal(configuration);
        if (configuration != null) {
          local.setConfiguration(configuration);
        }
      }
    }
    return local;
  }

  protected NexusClient remoteClient() {
    return remoteClient;
  }

  protected NexusClient localClient() {
    return localClient;
  }

  protected Repositories remoteRepositories() {
    return remoteClient().getSubsystem(Repositories.class);
  }

  protected Repositories localRepositories() {
    return localClient().getSubsystem(Repositories.class);
  }

  @Override
  protected void logRemoteThatTestIs(final Logger remoteLogger, final String doingWhat) {
    if (remoteLogger != null) {
      super.logRemoteThatTestIs(remoteLogger, doingWhat);
    }
  }

  private NexusBundleConfiguration configureNexus(final NexusBundleConfiguration configuration) {
    return configuration;
  }

  protected NexusBundleConfiguration configureRemote(final NexusBundleConfiguration configuration) {
    return configuration;
  }

  protected NexusBundleConfiguration configureLocal(final NexusBundleConfiguration configuration) {
    return configuration;
  }

  protected static void startNexus(final NexusBundle nexusBundle) {
    if (nexusBundle != null && !nexusBundle.isRunning()) {
      try {
        LOG.info("Starting Nexus ({})", nexusBundle);
        nexusBundle.start();
      }
      catch (Exception e) {
        throw Throwables.propagate(e);
      }
    }
  }

  protected static void stopNexus(final NexusBundle nexusBundle) {
    if (nexusBundle != null && nexusBundle.isRunning()) {
      try {
        LOG.info("Stopping Nexus ({})", nexusBundle);
        nexusBundle.stop();
      }
      catch (Exception e) {
        throw Throwables.propagate(e);
      }
    }
  }

  protected void waitForRemoteToSettleDown() {
    remoteClient().getSubsystem(Scheduler.class).waitForAllTasksToStop();
    remoteClient().getSubsystem(Events.class).waitForCalmPeriod();
  }

  protected void waitForLocalToSettleDown() {
    localClient().getSubsystem(Scheduler.class).waitForAllTasksToStop();
    localClient().getSubsystem(Events.class).waitForCalmPeriod();
  }

  protected Logger remoteRemoteLogger() {
    if (remoteClient() != null) {
      return remoteClient().getSubsystem(RemoteLoggerFactory.class).getLogger(this.getClass().getName());
    }
    return null;
  }

  protected Logger localRemoteLogger() {
    if (localClient() != null) {
      return localClient().getSubsystem(RemoteLoggerFactory.class).getLogger(this.getClass().getName());
    }
    return null;
  }

}
