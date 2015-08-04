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
package org.sonatype.nexus.testsuite.maven.nexus4423;

import java.io.File;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;

import org.junit.Test;

import static org.apache.commons.io.FileUtils.copyDirectoryToDirectory;

/**
 * See NEXUS-4423: in short, Nexus suffers from same problem as Maven2 did: snapshots with classifiers not deployed
 * with
 * latest deploying build (like per-OS artifacts) are not found.
 *
 * @author cstamas
 */
public class Nexus4423Maven3SnapshotMetadataIT
    extends AbstractNexusIntegrationTest
{
  @Test
  public void testMaven3SnapshotMetadata()
      throws Exception
  {
    // copy the "repo" to it's place, we need no index etc so this is fine
    File repo = new File(nexusWorkDir, "storage/" + REPO_TEST_HARNESS_SNAPSHOT_REPO);
    copyDirectoryToDirectory(getTestFile("org"), repo);

    // TODO: resolve? See nexus-proxy:org.sonatype.nexus.proxy.maven.Nexus4423Maven3MetadataTest UT
    // just repeat the same but with /artifact/maven/resolve REST resource and check same asserts (version, buildTs, buildNo)
    // for both queries
  }
}
