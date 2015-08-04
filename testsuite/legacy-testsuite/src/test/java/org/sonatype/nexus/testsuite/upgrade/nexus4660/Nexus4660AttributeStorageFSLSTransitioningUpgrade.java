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
package org.sonatype.nexus.testsuite.upgrade.nexus4660;

import java.io.File;
import java.io.IOException;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.RequestFacade;

import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.Test;

/**
 * NEXUS-4660: Testing the upgrade path of NG attributes. We simulate an "upgrade" by providing "old" nexus files
 * in old layout, and during fetches, these attribute files should be upgraded one by one.
 *
 * @author: cstamas
 */
public class Nexus4660AttributeStorageFSLSTransitioningUpgrade
    extends AbstractNexusIntegrationTest
{

  final int filesPresent = 8;

  protected File junkFile;

  protected File nexusBaseDir;

  protected File nexusProxyAttributesDir;

  protected File nexusStorageDir;

  protected File nexusRepoStorageDir;

  protected File nexusRepoNewAttributesDir;

  protected File nexusRepoOldProxyAttributesDir;

  @Override
  protected void copyTestResources()
      throws IOException
  {
    super.copyTestResources();

    FileUtils.copyDirectory(getTestFile("workfolder"), new File(nexusWorkDir));

    nexusBaseDir = new File(nexusWorkDir);

    // prepare some files used across test
    junkFile = new File(nexusBaseDir, "nexus4660-junk");
    junkFile.mkdirs();

    nexusProxyAttributesDir = new File(nexusBaseDir, "proxy/attributes");
    nexusStorageDir = new File(nexusBaseDir, "storage");

    nexusRepoStorageDir = new File(nexusStorageDir, REPO_TEST_HARNESS_REPO);
    nexusRepoNewAttributesDir = new File(nexusRepoStorageDir, ".nexus/attributes");
    nexusRepoOldProxyAttributesDir = new File(nexusProxyAttributesDir, REPO_TEST_HARNESS_REPO);
  }

  @AfterClass
  protected void cleanup()
      throws IOException
  {
    FileUtils.forceDelete(junkFile);
  }

  protected int countFiles(final File directory, final String... extensions) {
    return FileUtils.listFiles(directory, extensions, true).size();
  }

  protected void assertUpgradeStepsByOldNewAttributeCount(final int oldExpected, final int newExpected) {
    final int oldFound = countFiles(nexusRepoOldProxyAttributesDir, "pom", "jar", "sha1");
    final int newFound = countFiles(nexusRepoNewAttributesDir, "pom", "jar", "sha1");

    MatcherAssert.assertThat(oldFound,
        Matchers.equalTo(oldExpected));

    MatcherAssert.assertThat(newFound,
        Matchers.equalTo(newExpected));
    // we don't "loose" files
    MatcherAssert.assertThat(oldFound + newFound,
        Matchers.equalTo(filesPresent));

  }

  /**
   * Here we start with "prepared" old proxy/attributes (and proper content), and start fetching the
   * files one by one. As nexus serves requests, the attributes are getting upgraded. At the end
   * no old attribute remains. Finally, we check that during upgrade, there was no information loss.
   */
  @Test
  public void transitionedUpgrade()
      throws Exception
  {
    assertUpgradeStepsByOldNewAttributeCount(8, 0);

    downloadArtifact("junit", "junit", "3.8.1", "pom", null, junkFile.getAbsolutePath());
    downloadArtifact("junit", "junit", "3.8.1", "pom.sha1", null, junkFile.getAbsolutePath());

    assertUpgradeStepsByOldNewAttributeCount(6, 2);

    downloadArtifact("junit", "junit", "3.8.1", "jar", null, junkFile.getAbsolutePath());
    downloadArtifact("junit", "junit", "3.8.1", "jar.sha1", null, junkFile.getAbsolutePath());

    assertUpgradeStepsByOldNewAttributeCount(4, 4);

    downloadArtifact("classworlds", "classworlds", "1.1", "pom", null, junkFile.getAbsolutePath());
    downloadArtifact("classworlds", "classworlds", "1.1", "pom.sha1", null, junkFile.getAbsolutePath());

    assertUpgradeStepsByOldNewAttributeCount(2, 6);

    downloadArtifact("classworlds", "classworlds", "1.1-alpha-2", "pom", null, junkFile.getAbsolutePath());
    downloadArtifact("classworlds", "classworlds", "1.1-alpha-2", "pom.sha1", null, junkFile.getAbsolutePath());

    assertUpgradeStepsByOldNewAttributeCount(0, 8);

    // oldAttributes count is asserted as 0 here, so we are now upgraded completely
    // now let's check attribute contents
    String describeJsonString;

    // sheck the usual stuff
    describeJsonString = RequestFacade.doGetForText(
        "content/repositories/" + REPO_TEST_HARNESS_REPO + "/junit/junit/3.8.1/junit-3.8.1.pom?describe");
    MatcherAssert.assertThat(describeJsonString,
        Matchers.containsString("digest.sha1=16d74791c801c89b0071b1680ea0bc85c93417bb"));
    describeJsonString = RequestFacade.doGetForText(
        "content/repositories/" + REPO_TEST_HARNESS_REPO + "/junit/junit/3.8.1/junit-3.8.1.jar?describe");
    MatcherAssert.assertThat(describeJsonString,
        Matchers.containsString("digest.sha1=99129f16442844f6a4a11ae22fbbee40b14d774f"));

    // now check a "hidden" extra (like "plugin added" or "user added") attribute, it was present in junit POM
    // it should be transitioned to new attributes too
    // The trick here is we simulate an "extra" attribute, something that core does not know about
    // But despite that fact, it should bring along all the attributes. The checksums above Nexus could
    // recalculate, thus make us believe upgrade happened, but actually all nexus core did is dropped old
    // and created new ones with recalculated checksums. Here, we manually modded junit POM's attributes,
    // and added one extra attribute that core does not know about. If it's here (and way above we already
    // asserted that there is 0 old attributes, so we upgraded), we are cool.
    describeJsonString = RequestFacade.doGetForText(
        "content/repositories/" + REPO_TEST_HARNESS_REPO + "/junit/junit/3.8.1/junit-3.8.1.pom?describe");
    MatcherAssert.assertThat(describeJsonString,
        Matchers.containsString("nexus=badass"));

    // and another "mangled attribute": I use here a specific IP address. The ITs uses 127.0.0.1, so if data loss
    // occurs, and attributes get recreated, this IP address below would be lost/changed.
    describeJsonString = RequestFacade.doGetForText(
        "content/repositories/" + REPO_TEST_HARNESS_REPO + "/junit/junit/3.8.1/junit-3.8.1.jar?describe");
    MatcherAssert.assertThat(describeJsonString,
        Matchers.containsString("request.address=188.36.34.31"));

  }
}
