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
package org.sonatype.nexus.testsuite.index.nexus3638;

import java.net.URL;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;

import org.apache.maven.index.artifact.Gav;

public class Downloader
    extends Thread
{

  private Throwable[] errors;

  private int i;

  private Gav gav;

  private Nexus3638IndexProxiedMavenPluginIT it;

  public Downloader(Nexus3638IndexProxiedMavenPluginIT it, Gav gav, int i, Throwable[] errors) {
    this.gav = gav;
    this.i = i;
    this.errors = errors;
    this.it = it;
  }

  @Override
  public void run() {
    try {
      // it.downloadSnapshotArtifact( "nexus3638", gav, new File( "target/downloads/nexus3638/" + i ) );
      it.downloadFile(
          new URL(AbstractNexusIntegrationTest.nexusBaseUrl
              + AbstractNexusIntegrationTest.REPOSITORY_RELATIVE_URL + "nexus3638"
              +
              "/org/apache/maven/plugins/maven-invoker-plugin/1.6-SNAPSHOT/maven-invoker-plugin-1.6-20100922.124315-3.jar"),
          "target/downloads/nexus3638");

    }
    catch (Throwable t) {
      errors[i] = t;
    }
  }

}
