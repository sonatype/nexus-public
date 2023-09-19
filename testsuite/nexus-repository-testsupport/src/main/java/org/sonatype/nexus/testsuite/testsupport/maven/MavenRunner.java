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
package org.sonatype.nexus.testsuite.testsupport.maven;

import java.util.Arrays;
import java.util.function.Supplier;

import org.sonatype.goodies.common.ComponentSupport;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;

/**
 * Exercise maven goals against a project.
 */
public class MavenRunner
  extends ComponentSupport
{
  public void run(final MavenDeployment deployment, final String... goals) throws VerificationException {
    doRun(deployment, goals);
  }

  public void run(final Supplier<Boolean> shouldRetry, final MavenDeployment deployment, final String... goals) throws VerificationException {
    do {
      try {
        doRun(deployment, goals);
        //once successful no need to bother retrying
        return;
      }
      catch (Throwable t) {
        log.error("Maven execution failed", t);
      }
    }
    while (shouldRetry.get());
  }

  private void doRun(final MavenDeployment deployment, final String... goals) throws VerificationException {
    log.debug("Deploying: {}", deployment);
    Verifier verifier = new Verifier(deployment.getProjectDir().getAbsolutePath());
    verifier.addCliOption("-s " + deployment.settingsFile().getAbsolutePath());
    verifier.addCliOption(
        ("-DaltDeploymentRepository=local-nexus-admin::default::"+ deployment.getDeployUrl()).replace("//", "////"));
    verifier.addCliOption("-Dmaven.compiler.source=" + deployment.getJavaVersion());
    verifier.addCliOption("-Dmaven.compiler.target=" + deployment.getJavaVersion());

    log.info("Executing maven goals {}", Arrays.asList(goals));
    verifier.executeGoals(Arrays.asList(goals));

    verifier.verifyErrorFreeLog();
    log.debug("Finished running maven from {}", deployment.getProjectDir());
  }
}
