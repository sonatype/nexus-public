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
package org.sonatype.nexus.wonderland.rest

import java.nio.charset.StandardCharsets

import javax.ws.rs.WebApplicationException

import org.sonatype.nexus.util.Tokens
import org.sonatype.nexus.wonderland.AuthTicketService
import org.sonatype.nexus.wonderland.DownloadService
import org.sonatype.sisu.goodies.testsupport.TestSupport

import org.junit.Before
import org.junit.Test
import org.mockito.Mock

import static org.mockito.Mockito.when

/**
 * Test for {@link DownloadResource}.
 */
class DownloadResourceTest
    extends TestSupport
{
  private static final String AUTH_TICKET = "a valid ticket"

  private static final String ZIP_NAME = "foo"

  @Mock
  private DownloadService downloadService

  @Mock
  private AuthTicketService authTickets

  private DownloadResource underTest

  @Before
  void setup() {
    when(authTickets.createTicket()).thenReturn(AUTH_TICKET)
    when(downloadService.get(ZIP_NAME, AUTH_TICKET)).thenReturn(util.createTempFile())
    underTest = new DownloadResource(downloadService, authTickets)
  }

  @Test
  void downloadZipWithAuthTicketParam() {
    underTest.downloadZip(ZIP_NAME, Tokens.encodeBase64String(AUTH_TICKET.getBytes(StandardCharsets.UTF_8)), null)
  }

  @Test
  void downloadZipWithAuthTicketHeader() {
    underTest.downloadZip(ZIP_NAME, null, AUTH_TICKET)
  }

  @Test(expected = WebApplicationException.class)
  void downloadZipNoAuthTicket() {
    underTest.downloadZip(ZIP_NAME, null, null)
  }

  @Test
  void downloadZipNoAuthTicketNoPopUps() {
    underTest.setNoPopUps(true)
    underTest.downloadZip(ZIP_NAME, null, null)
  }
}
