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
package org.sonatype.nexus.testsuite.p2.p2r01;

import java.io.File;

import org.sonatype.nexus.plugins.p2.repository.internal.tasks.P2MetadataGeneratorTaskDescriptor;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;
import org.sonatype.nexus.testsuite.p2.AbstractNexusP2GeneratorIT;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class P2R0102RunP2MetadataGeneratorTaskIT
    extends AbstractNexusP2GeneratorIT
{

  public P2R0102RunP2MetadataGeneratorTaskIT() {
    super("p2r01");
  }

  /**
   * Scan & build metadata on already deployed bundles.
   */
  @Test
  public void test()
      throws Exception
  {
    deployArtifacts(getTestResourceAsFile("artifacts/jars"));

    createP2MetadataGeneratorCapability();

    final ScheduledServicePropertyResource repo = new ScheduledServicePropertyResource();
    repo.setKey(P2MetadataGeneratorTaskDescriptor.REPO_OR_GROUP_FIELD_ID);
    repo.setValue(getTestRepositoryId());
    TaskScheduleUtil.runTask(P2MetadataGeneratorTaskDescriptor.ID, repo);
    TaskScheduleUtil.waitForAllTasksToStop();

    final File p2Artifacts = downloadP2ArtifactsFor("org.ops4j.base", "ops4j-base-lang", "1.2.3");
    assertThat("p2Artifacts has been downloaded", p2Artifacts, is(notNullValue()));
    assertThat("p2Artifacts exists", p2Artifacts.exists(), is(true));
    // TODO compare downloaded file with an expected one

    final File p2Content = downloadP2ContentFor("org.ops4j.base", "ops4j-base-lang", "1.2.3");
    assertThat("p2Content has been downloaded", p2Content, is(notNullValue()));
    assertThat("p2Content exists", p2Content.exists(), is(true));
    // TODO compare downloaded file with an expected one
  }

}
