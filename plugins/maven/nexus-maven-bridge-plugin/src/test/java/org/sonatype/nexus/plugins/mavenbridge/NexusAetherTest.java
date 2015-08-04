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
package org.sonatype.nexus.plugins.mavenbridge;

import org.sonatype.aether.graph.Dependency;
import org.sonatype.nexus.AbstractMavenRepoContentTests;
import org.sonatype.nexus.proxy.maven.gav.Gav;

import org.junit.Assert;
import org.junit.Test;

public class NexusAetherTest
    extends AbstractMavenRepoContentTests
{

  @Test
  public void testDependency() {
    Gav gav = new Gav("org.apache.maven", "apache-maven", "3.0-beta-1");

    Dependency dep = Utils.createDependencyFromGav(gav, "compile");

    Assert.assertEquals(dep.getArtifact().getGroupId(), gav.getGroupId());
    Assert.assertEquals(dep.getArtifact().getArtifactId(), gav.getArtifactId());
    Assert.assertEquals(dep.getArtifact().getVersion(), gav.getVersion());
    Assert.assertEquals("compile", dep.getScope());
  }

}
