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
package org.sonatype.nexus.integrationtests;

import java.io.File;
import java.io.IOException;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;

public class AbstractMavenNexusIT
    extends AbstractNexusIntegrationTest
{
  private static final MavenVerifierHelper mavenVerifierHelper = new MavenVerifierHelper();

  public AbstractMavenNexusIT() {
    super();
  }

  public AbstractMavenNexusIT(String testRepositoryId) {
    super(testRepositoryId);
  }

  private static MavenVerifierHelper getStaticMavenVerifierHelper() {
    return mavenVerifierHelper;
  }

  @Deprecated
  public Verifier createVerifier(File mavenProject)
      throws VerificationException, IOException
  {
    return createVerifier(mavenProject, null);
  }

  /**
   * Create a nexus verifier instance
   *
   * @param mavenProject Maven Project folder
   * @param settings     A settings.xml file
   */
  @Deprecated
  public Verifier createVerifier(File mavenProject, File settings)
      throws VerificationException, IOException
  {
    if (settings == null) {
      settings = getOverridableFile("settings.xml");
    }
    return createMavenVerifier(mavenProject, settings, getTestId());
  }

  @Deprecated
  public static Verifier createMavenVerifier(File mavenProject, File settings, String testId)
      throws VerificationException, IOException
  {
    String logname = "logs/maven-execution/" + testId + "/" + mavenProject.getName() + ".log";
    final File logFile = new File(mavenProject, logname);
    logFile.getParentFile().mkdirs();
    final MavenDeployment mavenDeployment = MavenDeployment.defaultDeployment(logname, settings, mavenProject);
    cleanRepository(mavenDeployment.getLocalRepositoryFile(), testId);
    return getStaticMavenVerifierHelper().createMavenVerifier(mavenDeployment);
  }

  /**
   * Remove all artifacts on <code>testId</code> groupId
   */
  @Deprecated
  public static void cleanRepository(File mavenRepo, String testId)
      throws IOException
  {
    getStaticMavenVerifierHelper().cleanRepository(mavenRepo, testId);
  }

  /**
   * Workaround to get some decent logging when tests fail
   */
  @Deprecated
  protected void failTest(Verifier verifier)
      throws IOException
  {
    getStaticMavenVerifierHelper().failTest(verifier);
  }
}