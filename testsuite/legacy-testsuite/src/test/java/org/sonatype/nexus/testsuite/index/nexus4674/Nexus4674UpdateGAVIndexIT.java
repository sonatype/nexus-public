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
package org.sonatype.nexus.testsuite.index.nexus4674;

import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.rest.model.NexusArtifact;
import org.sonatype.nexus.test.utils.GavUtil;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.sonatype.sisu.goodies.common.Varargs.$;

/**
 * This test reindex a single version of a GA and check if all versions still present.
 *
 * @author velo
 */
public class Nexus4674UpdateGAVIndexIT
    extends AbstractNexusIntegrationTest
{

  private final static Logger LOGGER = LoggerFactory.getLogger(Nexus4674UpdateGAVIndexIT.class);

  @Test
  public void searchTest()
      throws Exception
  {
    // auto deploy artifacts need time to be indexed
    TaskScheduleUtil.waitForAllTasksToStop();
    getEventInspectorsUtil().waitForCalmPeriod();

    List<NexusArtifact> r = getSearchMessageUtil().searchFor("nexus4674");
    logContent(r);
    assertThat(r, hasSize(5));

    // index GAV
    getSearchMessageUtil().reindexGAV(
        REPO_TEST_HARNESS_REPO, GavUtil.newGav("nexus4674", "artifact", "1")
    );

    r = getSearchMessageUtil().searchFor("nexus4674");
    logContent(r);
    assertThat(r, hasSize(5));

    // index subartifact GAV
    getSearchMessageUtil().reindexGAV(
        REPO_TEST_HARNESS_REPO, GavUtil.newGav("nexus4674.artifact", "subartifact", "1")
    );

    logContent(r);
    r = getSearchMessageUtil().searchFor("nexus4674");
    assertThat(r, hasSize(5));
  }

  private void logContent(final List<NexusArtifact> artifacts) {
    if (artifacts != null && artifacts.size() > 0) {
      for (final NexusArtifact a : artifacts) {
        LOGGER.info("Found artifact: {}:{}:{}", $(a.getGroupId(), a.getArtifactId(), a.getVersion()));
      }
    }
  }
}
