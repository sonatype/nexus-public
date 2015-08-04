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
package org.sonatype.nexus.testsuite.maven.nexus4970;

import java.io.File;
import java.net.URL;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import static org.sonatype.nexus.test.utils.MavenMetadataHelper.assertEquals;
import static org.sonatype.nexus.test.utils.MavenMetadataHelper.getMetadata;

public class Nexus4970InvalidMetadataMergeOnGroupsIT
    extends AbstractNexusIntegrationTest
{

  /**
   * Test that requesting a maven-metadata.xml via a group will not fail if one of metadata files from member
   * repositories is invalid (e.g. having a wrong group id).
   * <p/>
   * The test will download the file. If metadata merge is not ignoring the invalid metadata from members it will
   * result in an exception (http 500).
   * The downloaded metadata should be equal to metadata found on repository r1.
   *
   * @throws Exception - unexpected
   */
  @Test
  public void invalidMavenMetadataIsIgnoredDuringMerge()
      throws Exception
  {
    final File r1Files = getTestFile("r1");
    final File r2Files = getTestFile("r2");
    final File r1Storage = new File(nexusWorkDir, "storage/r1");
    final File r2Storage = new File(nexusWorkDir, "storage/r2");

    FileUtils.copyDirectory(r1Files, r1Storage);
    FileUtils.copyDirectory(r2Files, r2Storage);

    final File mergedMetadata = downloadFile(
        new URL(nexusBaseUrl + "content/groups/g/itext/itext/maven-metadata.xml"),
        "target/downloads/nexus4970/maven-metadata.xml"
    );

    assertEquals(
        getMetadata(new File(r1Files, "itext/itext/maven-metadata.xml")),
        getMetadata(mergedMetadata)
    );
  }

}
