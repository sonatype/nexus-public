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
package org.sonatype.nexus.testsuite.p2.nxcm1995;

import java.io.File;
import java.net.URL;

import org.sonatype.nexus.test.utils.FileTestingUtils;
import org.sonatype.nexus.testsuite.p2.AbstractNexusProxyP2IT;

import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.sonatype.sisu.litmus.testsupport.hamcrest.FileMatchers.contains;
import static org.sonatype.sisu.litmus.testsupport.hamcrest.FileMatchers.exists;

public class NXCM1995MetadataCacheIT
    extends AbstractNexusProxyP2IT
{

  public NXCM1995MetadataCacheIT() {
    super("nxcm1995");
  }

  @Test
  @Ignore
  public void test()
      throws Exception
  {
    // check original content
    final File f1 = downloadFile(
        new URL(getNexusTestRepoUrl() + "/content.xml"),
        "target/downloads/nxcm1995/1/content.xml"
    );
    assertThat(f1, exists());
    assertThat(f1, contains("com.adobe.flexbuilder.utils.osnative.win"));
    assertThat(f1, not(contains("com.sonatype.nexus.p2.its.feature2.feature.jar")));

    // check original artifact
    final File a1 = downloadFile(
        new URL(getNexusTestRepoUrl() + "/artifacts.xml"),
        "target/downloads/nxcm1995/1/artifacts.xml"
    );
    assertThat(a1, exists());
    assertThat(a1, contains("com.adobe.flexbuilder.multisdk"));
    assertThat(a1, not(contains("com.sonatype.nexus.p2.its.feature2")));

    final File reponxcm1995 = new File(localStorageDir, "nxcm1995");

    // check new content
    final File newContentXml = new File(localStorageDir, "p2repo2/content.xml");
    assertThat(newContentXml, exists());
    assertThat(newContentXml, not(contains("com.adobe.flexbuilder.utils.osnative.win")));
    assertThat(newContentXml, contains("com.sonatype.nexus.p2.its.feature2.feature.jar"));
    FileUtils.copyFileToDirectory(newContentXml, new File(reponxcm1995, "memberrepo1"), false);
    FileUtils.copyFileToDirectory(newContentXml, new File(reponxcm1995, "memberrepo2"), false);

    final File newArtifactsXml = new File(localStorageDir, "p2repo2/artifacts.xml");
    assertThat(newArtifactsXml, exists());
    FileUtils.copyFileToDirectory(newArtifactsXml, new File(reponxcm1995, "memberrepo1"), false);
    FileUtils.copyFileToDirectory(newArtifactsXml, new File(reponxcm1995, "memberrepo2"), false);

    // metadata cache expires in ONE minute, so let's give it some time to expire
    Thread.yield();
    Thread.sleep(1 * 60 * 1000);
    Thread.yield();
    Thread.sleep(1 * 60 * 1000);
    Thread.yield();

    // ScheduledServicePropertyResource prop = new ScheduledServicePropertyResource();
    // prop.setId( "repositoryId" );
    // prop.setValue( REPO );
    // TaskScheduleUtil.runTask( ExpireCacheTaskDescriptor.ID, prop );
    // TaskScheduleUtil.waitForAllTasksToStop();

    // make sure nexus has the right content after metadata cache expires
    final File f2 = downloadFile(
        new URL(getNexusTestRepoUrl() + "/content.xml"),
        "target/downloads/nxcm1995/2/content.xml"
    );
    assertThat(f2, exists());
    assertThat(f2, not(contains("com.adobe.flexbuilder.utils.osnative.win")));
    assertThat(f2, contains("com.sonatype.nexus.p2.its.feature2.feature.jar"));

    assertThat(FileTestingUtils.compareFileSHA1s(f1, f2), is(false));

    // make sure nexus has the right content after metadata cache expires
    final File a2 = downloadFile(
        new URL(getNexusTestRepoUrl() + "/artifacts.xml"),
        "target/downloads/nxcm1995/2/artifacts.xml"
    );
    assertThat(a2, exists());
    assertThat(a2, not(contains("com.adobe.flexbuilder.multisdk")));
    assertThat(a2, contains("com.sonatype.nexus.p2.its.feature2"));

    assertThat(FileTestingUtils.compareFileSHA1s(a1, a2), is(false));
  }

}
