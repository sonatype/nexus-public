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
package org.sonatype.nexus.tasks;

import org.sonatype.nexus.AbstractMavenRepoContentTests;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.LocalStatus;
import org.sonatype.nexus.scheduling.NexusScheduler;

import org.junit.Test;

public class ExpireCacheTaskTest
    extends AbstractMavenRepoContentTests
{
  NexusScheduler scheduler;

  @Override
  protected void setUp()
      throws Exception
  {
    super.setUp();

    scheduler = lookup(NexusScheduler.class);
  }

  @Override
  protected boolean runWithSecurityDisabled() {
    return true;
  }

  @Test
  public void testBlockRepoInAGroup()
    // NEXUS-3798
      throws Exception
  {
    fillInRepo();

    while (scheduler.getActiveTasks().size() > 0) {
      Thread.sleep(100);
    }

    central.setLocalStatus(LocalStatus.OUT_OF_SERVICE);
    nexusConfiguration().saveConfiguration();

    GroupRepository group = repositoryRegistry.getRepositoryWithFacet("public", GroupRepository.class);
    group.expireCaches(new ResourceStoreRequest("/"));
  }
}
