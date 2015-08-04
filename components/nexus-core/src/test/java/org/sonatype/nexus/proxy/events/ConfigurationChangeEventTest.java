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
package org.sonatype.nexus.proxy.events;

import org.sonatype.jettytestsuite.ServletServer;
import org.sonatype.nexus.configuration.ConfigurationCommitEvent;
import org.sonatype.nexus.configuration.ConfigurationPrepareForSaveEvent;
import org.sonatype.nexus.proxy.AbstractProxyTestEnvironment;
import org.sonatype.nexus.proxy.EnvironmentBuilder;
import org.sonatype.nexus.proxy.M2TestsuiteEnvironmentBuilder;
import org.sonatype.nexus.proxy.maven.MavenGroupRepository;
import org.sonatype.nexus.proxy.repository.LocalStatus;
import org.sonatype.nexus.proxy.repository.Repository;

import org.junit.Test;

public class ConfigurationChangeEventTest
    extends AbstractProxyTestEnvironment
{
  @Override
  protected EnvironmentBuilder getEnvironmentBuilder()
      throws Exception
  {
    ServletServer ss = (ServletServer) lookup(ServletServer.ROLE);

    return new M2TestsuiteEnvironmentBuilder(ss);
  }

  @Test
  public void testSimplePull()
      throws Exception
  {
    // flush all potential changes
    eventBus().post(new ConfigurationPrepareForSaveEvent(getApplicationConfiguration()));
    eventBus().post(new ConfigurationCommitEvent(getApplicationConfiguration()));

    // get hold on all registered reposes
    Repository repo1 = getRepositoryRegistry().getRepository("repo1");
    Repository repo2 = getRepositoryRegistry().getRepository("repo2");
    Repository repo3 = getRepositoryRegistry().getRepository("repo3");
    Repository inhouse = getRepositoryRegistry().getRepository("inhouse");
    Repository inhouseSnapshot = getRepositoryRegistry().getRepository("inhouse-snapshot");
    MavenGroupRepository test = getRepositoryRegistry().getRepositoryWithFacet("test", MavenGroupRepository.class);

    // now change some of them
    repo1.setLocalStatus(LocalStatus.OUT_OF_SERVICE);
    repo3.setName("kuku");
    test.setMergeMetadata(false);

    // changes are not applied yet!
    assertEquals("Should not be applied!", LocalStatus.IN_SERVICE, repo1.getLocalStatus());
    assertEquals("Should not be applied!", "repo3", repo3.getName());
    assertEquals("Should not be applied!", true, test.isMergeMetadata());

    // fire prepareForSave event
    ConfigurationPrepareForSaveEvent pevt = new ConfigurationPrepareForSaveEvent(getApplicationConfiguration());
    eventBus().post(pevt);
    assertFalse(pevt.isVetoed());

    eventBus().post(new ConfigurationCommitEvent(getApplicationConfiguration()));

    // changes are now applied!
    assertEquals("Should be applied!", LocalStatus.OUT_OF_SERVICE, repo1.getLocalStatus());
    assertEquals("Should be applied!", "kuku", repo3.getName());
    assertEquals("Should be applied!", false, test.isMergeMetadata());

    // changed reposes should be in event
    assertTrue("Is changed!", pevt.getChanges().contains(repo1));
    assertTrue("Is changed!", pevt.getChanges().contains(repo3));
    assertTrue("Is changed!", pevt.getChanges().contains(test));

    // others are not in event
    assertFalse("Is not changed!", pevt.getChanges().contains(repo2));
    assertFalse("Is not changed!", pevt.getChanges().contains(inhouse));
    assertFalse("Is not changed!", pevt.getChanges().contains(inhouseSnapshot));

  }
}
