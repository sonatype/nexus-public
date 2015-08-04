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
package org.sonatype.nexus.testsuite.p2.nxcm1871;

import java.io.File;
import java.net.URL;

import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.tasks.descriptors.ExpireCacheTaskDescriptor;
import org.sonatype.nexus.test.utils.FileTestingUtils;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;
import org.sonatype.nexus.testsuite.p2.AbstractNexusProxyP2IT;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.sonatype.sisu.litmus.testsupport.hamcrest.FileMatchers.contains;
import static org.sonatype.sisu.litmus.testsupport.hamcrest.FileMatchers.exists;

public class NXCM1871P2GroupExpireCacheIT
    extends AbstractNexusProxyP2IT
{

  public NXCM1871P2GroupExpireCacheIT() {
    super("nxcm1871");
  }

  @Test
  public void test()
      throws Exception
  {
    // check original content
    final File f1 = downloadFile(
        new URL(getGroupUrl(getTestRepositoryId()) + "/content.xml"),
        "target/downloads/nxcm1871/1/content.xml"
    );
    assertThat(f1, exists());
    assertThat(f1, contains("com.sonatype.nexus.p2.its.feature2.feature.jar"));
    assertThat(f1, not(contains("com.sonatype.nexus.p2.its.feature3.feature.jar")));

    final File repo_nxcm1871_2 = new File(localStorageDir, "nxcm1871-2");

    final File newContentXml = new File(localStorageDir, "nxcm1871-3/content.xml");
    assertThat(newContentXml, exists());
    assertThat(newContentXml, not(contains("com.sonatype.nexus.p2.its.feature2.feature.jar")));
    assertThat(newContentXml, contains("com.sonatype.nexus.p2.its.feature3.feature.jar"));

    FileUtils.copyFileToDirectory(newContentXml, repo_nxcm1871_2, false);

    final File newArtifactsXml = new File(localStorageDir, "nxcm1871-3/artifacts.xml");
    FileUtils.copyFileToDirectory(newArtifactsXml, repo_nxcm1871_2, false);

    final ScheduledServicePropertyResource prop = new ScheduledServicePropertyResource();
    prop.setKey("repositoryId");
    prop.setValue(getTestRepositoryId());

    TaskScheduleUtil.runTask(ExpireCacheTaskDescriptor.ID, prop);

    // make sure nexus has the right content after reindex
    final File f2 = downloadFile(
        new URL(getGroupUrl(getTestRepositoryId()) + "/content.xml"),
        "target/downloads/nxcm1871/2/content.xml"
    );
    assertThat(f2, exists());
    assertThat(f2, not(contains("com.sonatype.nexus.p2.its.feature2.feature.jar")));
    assertThat(f2, contains("com.sonatype.nexus.p2.its.feature3.feature.jar"));

    assertThat(FileTestingUtils.compareFileSHA1s(f1, f2), is(false));
  }

}
