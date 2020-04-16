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
package org.sonatype.nexus.testsuite.p2;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.MavenVerifierHelper;
import org.sonatype.nexus.integrationtests.TestContainer;

import org.apache.commons.io.FileUtils;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.sonatype.sisu.litmus.testsupport.hamcrest.FileMatchers.exists;
import static org.sonatype.sisu.litmus.testsupport.hamcrest.FileMatchers.isDirectory;
import static org.sonatype.sisu.litmus.testsupport.hamcrest.FileMatchers.readable;

public abstract class AbstractNexusP2IT
    extends AbstractNexusIntegrationTest
{

  protected AbstractNexusP2IT() {
    super();
  }

  protected AbstractNexusP2IT(final String testRepositoryId) {
    super(testRepositoryId);
  }

  protected void installUsingP2(final String repositoryURL, final String installIU, final String destination)
      throws Exception
  {
    installUsingP2(repositoryURL, installIU, destination, null);
  }

  protected void installUsingP2(final String repositoryURL, final String installIU, final String destination,
                                final Map<String, String> sysProps)
      throws Exception
  {
    boolean wasAnonymousAdministrator = isAnonymousAdministrator();

    try {
      makeAnonymousAdministrator(true);

      FileUtils.deleteDirectory(new File(destination));

      String tempDirPath = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
      File testDir = new File(tempDirPath, getTestId() + "/run-p2/" + System.currentTimeMillis());

      FileUtils.deleteDirectory(testDir);

      final File basedir = ResourceExtractor.extractResourcePath(getClass(), "/run-p2", testDir, false);

      final Verifier verifier = MavenVerifierHelper.newDefaultVerifier(basedir);

      verifier.setLocalRepo(new File(TestContainer.getBasedir(), "target/maven/fake-repo").getAbsolutePath());

      verifier.setSystemProperty("org.eclipse.ecf.provider.filetransfer.retrieve.readTimeout", "30000");
      verifier.setSystemProperty("p2.installIU", installIU);
      verifier.setSystemProperty("p2.destination", destination);
      verifier.setSystemProperty("p2.metadataRepository", repositoryURL);
      verifier.setSystemProperty("p2.artifactRepository", repositoryURL);
      verifier.setSystemProperty("p2.profile", getTestId());

      if (sysProps != null) {
        for (Map.Entry<String, String> entry : sysProps.entrySet()) {
          verifier.setSystemProperty(entry.getKey(), entry.getValue());
        }
      }

      verifier.setLogFileName(getTestId() + "-maven-output.log");
      verifier.addCliOption("-X");
      verifier.executeGoals(Arrays.asList("verify"));
      verifier.verifyErrorFreeLog();
      verifier.resetStreams();

      FileUtils.deleteDirectory(testDir);
    }
    finally {
      makeAnonymousAdministrator(wasAnonymousAdministrator);
    }
  }

  protected void installAndVerifyP2Feature(String repoId)
      throws Exception
  {
    installAndVerifyP2Feature("com.sonatype.nexus.p2.its.feature.feature.group",
        new String[]{"com.sonatype.nexus.p2.its.feature_1.0.0"},
        new String[]{"com.sonatype.nexus.p2.its.bundle_1.0.0.jar"}, repoId);
  }

  protected void installAndVerifyP2Feature()
      throws Exception
  {
    installAndVerifyP2Feature(getTestRepositoryId());
  }

  protected void installAndVerifyP2Feature(final String featureToInstall, final String[] features,
                                           final String[] plugins, String repoId)
      throws Exception
  {
    final File installDir = new File("target/eclipse/" + getTestId());

    installUsingP2(getNexusTestRepoUrl(repoId), featureToInstall, installDir.getCanonicalPath());

    for (final String feature : features) {
      final File featureFile = new File(installDir, "features/" + feature);
      assertThat(featureFile, exists());
      assertThat(featureFile, isDirectory());
    }

    for (final String plugin : plugins) {
      final File pluginFile = new File(installDir, "plugins/" + plugin);
      assertThat(pluginFile, is(readable()));
    }
  }

  protected void installAndVerifyP2Feature(final String featureToInstall, final String[] features,
                                           final String[] plugins)
      throws Exception
  {
    installAndVerifyP2Feature(featureToInstall, features, plugins, getTestRepositoryId());
  }

}
