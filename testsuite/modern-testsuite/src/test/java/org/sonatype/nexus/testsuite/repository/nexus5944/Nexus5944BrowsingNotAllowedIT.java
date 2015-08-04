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
package org.sonatype.nexus.testsuite.repository.nexus5944;

import org.sonatype.nexus.client.core.subsystem.repository.maven.MavenHostedRepository;
import org.sonatype.nexus.client.core.subsystem.repository.maven.MavenProxyRepository;
import org.sonatype.nexus.testsuite.TwinNexusITSupport;
import org.sonatype.nexus.testsuite.support.NexusStartAndStopStrategy;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * IT related to issue NEXUS-5944: Remote is Nexus, but browsing of proxied repo
 * is not allowed. Local Nexus' proxy repo should not auto block, it should work.
 */
@NexusStartAndStopStrategy(NexusStartAndStopStrategy.Strategy.EACH_TEST)
public class Nexus5944BrowsingNotAllowedIT
    extends TwinNexusITSupport
{
  public static final String REMOTE_REPOSITORY_ID = "releases";

  public static final String LOCAL_REPOSITORY_ID = "remote-releases";

  public Nexus5944BrowsingNotAllowedIT(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  @Test
  public void remoteBrowsingNotAllowed() {
    // disable browsing on remote/central
    final MavenHostedRepository remoteRepository = remoteRepositories().get(MavenHostedRepository.class,
        REMOTE_REPOSITORY_ID);
    remoteRepository.disableBrowsing().save();

    // create local/central proxying remote/central
    final MavenProxyRepository localRepository = localRepositories()
        .create(MavenProxyRepository.class, LOCAL_REPOSITORY_ID).asProxyOf(remoteRepository.contentUri())
        .doNotDownloadRemoteIndexes().withRepoPolicy("RELEASE").save();
    waitForRemoteToSettleDown();
    waitForLocalToSettleDown();

    assertThat(LOCAL_REPOSITORY_ID + " should not be autoblocked",
        !localRepositories().get(MavenProxyRepository.class, LOCAL_REPOSITORY_ID).status().isAutoBlocked());

    localRepository.refresh();

    assertThat(LOCAL_REPOSITORY_ID + " should not be autoblocked",
        !localRepositories().get(MavenProxyRepository.class, LOCAL_REPOSITORY_ID).status().isAutoBlocked());
  }

}
