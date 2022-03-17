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
package org.sonatype.nexus.testsuite.p2.nxcm3938;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

import org.sonatype.nexus.plugins.p2.repository.P2Constants;
import org.sonatype.nexus.testsuite.p2.AbstractNexusProxyP2IT;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.sonatype.sisu.goodies.testsupport.hamcrest.FileMatchers.exists;

public class NXCM3938BlockedMetadataFilesIT
    extends AbstractNexusProxyP2IT
{

  public NXCM3938BlockedMetadataFilesIT() {
    super("nxcm3938");
  }

  /**
   * Tests that only artifacts.xml and content.xml can be downloaded from a p2 proxy and the rest of metadata files
   * are blocked from getting them from remote.
   *
   * @throws Exception - unexpected
   */
  @Test
  public void test()
      throws Exception
  {
    // artifacts.xml/jar and content.xml/jar must exist
    assertThat(downloadFile(P2Constants.ARTIFACTS_XML), exists());
    assertThat(downloadFile(P2Constants.CONTENT_XML), exists());
    assertThat(downloadFile(P2Constants.ARTIFACTS_JAR), exists());
    assertThat(downloadFile(P2Constants.CONTENT_JAR), exists());

    // other metadata files should be blocked
    assertThat(downloadFile(P2Constants.P2_INDEX), not(exists()));
    assertThat(downloadFile(P2Constants.SITE_XML), not(exists()));
    assertThat(downloadFile(P2Constants.COMPOSITE_ARTIFACTS_JAR), not(exists()));
    assertThat(downloadFile(P2Constants.COMPOSITE_ARTIFACTS_XML), not(exists()));
    assertThat(downloadFile(P2Constants.COMPOSITE_CONTENT_JAR), not(exists()));
    assertThat(downloadFile(P2Constants.COMPOSITE_CONTENT_XML), not(exists()));
  }

  private File downloadFile(final String name)
      throws IOException
  {
    final File downloadedFile = new File("target/downloads/nxcm3938" + name);
    if (downloadedFile.exists()) {
      assertThat(downloadedFile.delete(), is(true));
    }

    try {
      downloadFile(
          new URL(getRepositoryUrl(getTestRepositoryId()) + name),
          downloadedFile.getAbsolutePath()
      );
    }
    catch (FileNotFoundException e) {
      // ignore. Check is done later if the downloaded file exists or not
    }

    return downloadedFile;
  }

}
