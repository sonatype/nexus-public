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
package org.sonatype.nexus.testsuite.p2.p2r03;

import java.io.File;
import java.net.URL;

import org.sonatype.nexus.plugins.p2.repository.internal.tasks.P2RepositoryAggregatorTaskDescriptor;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;
import org.sonatype.nexus.testsuite.p2.AbstractNexusP2GeneratorIT;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.sonatype.sisu.litmus.testsupport.hamcrest.FileMatchers.readable;

public class P2R0303RunP2RepositoryAggregatorTaskIT
    extends AbstractNexusP2GeneratorIT
{

  public P2R0303RunP2RepositoryAggregatorTaskIT() {
    super("p2r03");
  }

  /**
   * When a bundle is deployed p2Artifacts/p2Content gets created and added to the top generated p2 repository.
   */
  @Test
  public void test()
      throws Exception
  {
    createP2MetadataGeneratorCapability();

    deployArtifacts(getTestResourceAsFile("artifacts/jars"));

    createP2RepositoryAggregatorCapability();

    final ScheduledServicePropertyResource repo = new ScheduledServicePropertyResource();
    repo.setKey(P2RepositoryAggregatorTaskDescriptor.REPO_OR_GROUP_FIELD_ID);
    repo.setValue(getTestRepositoryId());
    TaskScheduleUtil.runTask(P2RepositoryAggregatorTaskDescriptor.ID, repo);
    TaskScheduleUtil.waitForAllTasksToStop();

    // ensure link created
    final File file = downloadFile(
        new URL(getNexusTestRepoUrl() + "/.meta/p2/plugins/org.ops4j.base.lang_1.2.3.jar"),
        new File(
            "target/downloads/" + this.getClass().getSimpleName() + "/org.ops4j.base.lang_1.2.3.jar"
        ).getCanonicalPath()
    );
    assertThat(file, is(readable()));

    // ensure repositories are valid
    final File installDir = new File("target/eclipse/p2r0303");

    installUsingP2(getNexusTestRepoUrl() + "/.meta/p2", "org.ops4j.base.lang", installDir.getCanonicalPath());

    final File bundle = new File(installDir, "plugins/org.ops4j.base.lang_1.2.3.jar");
    assertThat(bundle, is(readable()));
  }

}
