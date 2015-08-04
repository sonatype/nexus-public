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
package org.sonatype.nexus.testsuite.group.nexus3832;

import java.io.File;

import org.sonatype.nexus.integrationtests.AbstractNexusProxyIntegrationTest;
import org.sonatype.nexus.test.utils.FileTestingUtils;
import org.sonatype.nexus.test.utils.GavUtil;

import org.apache.maven.index.artifact.Gav;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

@Ignore
public class Nexus3832GroupLocalOverRemoteContentIT
    extends AbstractNexusProxyIntegrationTest
{

  @Test
  public void onLocalCache()
      throws Exception
  {
    Gav gav = GavUtil.newGav("nexus3832", "artifact", "1.0");
    File downloaded = downloadArtifactFromGroup("public", gav, "target/downloads/nexus3832");

    assertTrue(FileTestingUtils.compareFileSHA1s(downloaded, getTestResourceAsFile("projects/p1/artifact.jar")));
  }

  @Test
  public void onlyRemote()
      throws Exception
  {
    Gav gav = GavUtil.newGav("nexus3832", "artifact", "2.0");
    File localFile = getLocalFile("release-proxy-repo-1", gav);

    File downloaded = downloadArtifactFromGroup("public", gav, "target/downloads/nexus3832");

    assertTrue(FileTestingUtils.compareFileSHA1s(downloaded, localFile));
  }

  @Test
  public void onlyLocal()
      throws Exception
  {
    Gav gav = GavUtil.newGav("nexus3832", "artifact", "3.0");
    File localFile = getTestResourceAsFile("projects/p3/artifact.jar");

    File downloaded = downloadArtifactFromGroup("public", gav, "target/downloads/nexus3832");

    assertTrue(FileTestingUtils.compareFileSHA1s(downloaded, localFile));
  }
}
