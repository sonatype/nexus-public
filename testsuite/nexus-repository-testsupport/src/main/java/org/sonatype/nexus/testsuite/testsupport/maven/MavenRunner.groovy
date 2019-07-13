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
package org.sonatype.nexus.testsuite.testsupport.maven

import groovy.util.logging.Slf4j
import org.apache.maven.it.Verifier

/**
 * Exercise maven goals against a project.
 * @since 3.2
 */
@Slf4j
class MavenRunner
{
  void run(MavenDeployment deployment, String... goals) {
    log.debug "Deploying: $deployment"
    Verifier verifier = new Verifier(deployment.projectDir.absolutePath)
    verifier.addCliOption("-s ${deployment.settingsFile().absolutePath}")
    verifier.addCliOption(
        "-DaltDeploymentRepository=local-nexus-admin::default::${deployment.deployUrl}".replace('//', '////'))
    
    log.info("Executing maven goals {}", Arrays.asList(goals))
    verifier.executeGoals(Arrays.asList(goals))
    
    verifier.verifyErrorFreeLog()
    log.debug("Finished running maven from ${deployment.projectDir}")
  }
}
