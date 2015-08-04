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
package org.sonatype.nexus.testsuite.p2.nxcm3963;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.sonatype.nexus.testsuite.p2.AbstractNexusProxyP2IT;

import org.junit.Test;

public class NXCM3963P2GroupLoadTestIT
    extends AbstractNexusProxyP2IT
{

  public NXCM3963P2GroupLoadTestIT() {
    super("nxcm3963");
  }

  @Test
  public void test()
      throws Exception
  {
    final Thread downloadThread1 = new DownloadThread("1");
    final Thread downloadThread2 = new DownloadThread("2");

    downloadThread1.start();
    downloadThread2.start();

    downloadThread1.join();
    downloadThread2.join();

  }

  private class DownloadThread
      extends Thread
  {

    private final String id;

    DownloadThread(String id) {
      super("DownloadThread-" + id);
      this.id = id;
    }

    @Override
    public void run() {
      for (int i = 1; i < 100; i++) {
        final File file = new File("target/downloads/nxcm3963" + id + "/content-" + i + ".xml");
        try {
          downloadFile(
              new URL(getRepositoryUrl(getTestRepositoryId()) + "content.xml"),
              file.getAbsolutePath()
          );
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

}
