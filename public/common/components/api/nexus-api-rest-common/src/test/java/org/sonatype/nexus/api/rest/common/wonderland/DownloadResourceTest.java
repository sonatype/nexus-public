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
package org.sonatype.nexus.api.rest.common.wonderland;

import java.io.IOException;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.sonatype.nexus.common.wonderland.AuthTicketService;
import org.sonatype.nexus.common.wonderland.DownloadService;
import org.sonatype.nexus.common.wonderland.DownloadService.Download;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;

import static com.google.common.net.HttpHeaders.CONTENT_DISPOSITION;
import static com.google.common.net.HttpHeaders.CONTENT_LENGTH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(ValidationExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class DownloadResourceTest
{
  @Mock
  private DownloadService downloadService;

  @Mock
  private AuthTicketService authTicketService;

  /**
   * Fix for NEXUS-40992
   */
  @Test
  void downloadZipUsesCorrectFileNameHeader() throws Exception {
    DownloadResource underTest = new DownloadResource(downloadService, authTicketService);
    String fileName = "supportZip-timestamp.zip";
    mockAuthenticatedDownload(fileName);

    Response response = underTest.downloadZip(fileName);

    assertThat(response.getHeaderString(CONTENT_DISPOSITION), is("attachment; filename=\"" + fileName + "\""));
  }

  @Test
  void downloadZipSetsContentLengthHeader() throws Exception {
    DownloadResource underTest = new DownloadResource(downloadService, authTicketService);
    String fileName = "supportZip-timestamp.zip";
    Download mockDownload = mock(Download.class);
    when(authTicketService.createTicket()).thenReturn("ticket");
    when(mockDownload.getLength()).thenReturn(4096L);
    when(downloadService.get(fileName, "ticket")).thenReturn(mockDownload);

    Response response = underTest.downloadZip(fileName);

    assertThat(response.getHeaderString(CONTENT_LENGTH), is("4096"));
  }

  @Test
  void downloadZipMissingAuthTicketReturnsBadRequest() {
    DownloadResource underTest = new DownloadResource(downloadService, authTicketService);
    when(authTicketService.createTicket()).thenReturn(null);

    WebApplicationException e =
        assertThrows(WebApplicationException.class, () -> underTest.downloadZip("supportZip.zip"));
    assertThat(e.getResponse().getStatus(), is(400));
  }

  @Test
  void downloadZipReturnsNotFoundWhenDownloadMissing() throws Exception {
    DownloadResource underTest = new DownloadResource(downloadService, authTicketService);
    when(authTicketService.createTicket()).thenReturn("ticket");
    when(downloadService.get("supportZip.zip", "ticket")).thenReturn(null);

    Response response = underTest.downloadZip("supportZip.zip");

    assertThat(response.getStatus(), is(404));
  }

  @Test
  void downloadZipTranslatesIoExceptionToInternalServerError() throws Exception {
    DownloadResource underTest = new DownloadResource(downloadService, authTicketService);
    when(authTicketService.createTicket()).thenReturn("ticket");
    when(downloadService.get("supportZip.zip", "ticket")).thenThrow(new IOException("boom"));

    WebApplicationException e =
        assertThrows(WebApplicationException.class, () -> underTest.downloadZip("supportZip.zip"));
    assertThat(e.getResponse().getStatus(), is(500));
  }

  private void mockAuthenticatedDownload(final String fileName) throws IOException {
    Download mockDownload = mock(Download.class);
    String fileAuthTicket = fileName + "-authTicket";
    when(authTicketService.createTicket()).thenReturn(fileAuthTicket);
    when(downloadService.get(fileName, fileAuthTicket)).thenReturn(mockDownload);
  }

}
