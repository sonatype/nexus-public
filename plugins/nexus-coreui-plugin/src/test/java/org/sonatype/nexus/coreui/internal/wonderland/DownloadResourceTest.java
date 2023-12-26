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
package org.sonatype.nexus.coreui.internal.wonderland;

import java.io.IOException;

import javax.ws.rs.core.Response;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.wonderland.AuthTicketService;
import org.sonatype.nexus.common.wonderland.DownloadService;
import org.sonatype.nexus.common.wonderland.DownloadService.Download;

import org.junit.Test;
import org.mockito.Mock;

import static com.google.common.net.HttpHeaders.CONTENT_DISPOSITION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DownloadResourceTest
    extends TestSupport
{
  @Mock
  private DownloadService downloadService;

  @Mock
  private AuthTicketService authTicketService;

  /**
   * Fix for NEXUS-40992
   */
  @Test
  public void downloadZipUsesCorrectFileNameHeader() throws IOException {
    DownloadResource underTest = new DownloadResource(downloadService, authTicketService);
    String fileName = "supportZip-timestamp.zip";
    mockAuthenticatedDownload(fileName);

    Response response = underTest.downloadZip(fileName);

    assertThat(response.getHeaderString(CONTENT_DISPOSITION), is("attachment; filename=\"" + fileName + "\""));
  }

  private void mockAuthenticatedDownload(String fileName) {
    Download mockDownload = mock(Download.class);
    String fileAuthTicket = fileName + "-authTicket";
    when(authTicketService.createTicket()).thenReturn(fileAuthTicket);
    try {
      when(downloadService.get(fileName, fileAuthTicket)).thenReturn(mockDownload);
    } catch (IOException e) {
      // swallow exception that will never happen
    }
  }

}
