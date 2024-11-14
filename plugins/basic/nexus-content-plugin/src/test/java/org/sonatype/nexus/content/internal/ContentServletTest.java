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
package org.sonatype.nexus.content.internal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.nexus.configuration.application.NexusConfiguration;
import org.sonatype.nexus.content.csp.ContentSecurityPolicy;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.router.RepositoryRouter;
import org.sonatype.nexus.web.WebUtils;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ContentServletTest {
  @Mock
  private NexusConfiguration nexusConfiguration;

  @Mock
  private RepositoryRouter repositoryRouter;

  @Mock
  private ContentRenderer contentRenderer;

  @Mock
  private WebUtils webUtils;

  @Mock
  private HttpServletRequest httpServletRequest;

  @Mock
  private HttpServletResponse httpServletResponse;

  @Mock
  private StorageFileItem storageFileItem;

  @Mock
  private ContentSecurityPolicy contentSecurityPolicy;

  private ContentServlet underTest;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    underTest = new ContentServlet(nexusConfiguration, repositoryRouter, contentRenderer, webUtils, contentSecurityPolicy);
  }

  @Test
  public void testDoGetFile_contentRangeHeader() throws Exception {
    //this is simply to ignore parts of the doGetFile method so i don't have to mock them
    when(storageFileItem.isContentGenerated()).thenReturn(true);
    when(httpServletRequest.getHeader("Range")).thenReturn("bytes=0-499");
    when(storageFileItem.getLength()).thenReturn(1000L);

    underTest.doGetFile(httpServletRequest, httpServletResponse, storageFileItem);

    verify(httpServletResponse).setHeader("Content-Type", null);
    //first time set by file size
    verify(httpServletResponse).setHeader("Content-Length", "1000");
    //then set a 2nd time by range size
    verify(httpServletResponse).setHeader("Content-Length", "500");
    verify(httpServletResponse).setHeader("Content-Range", "bytes 0-499/1000");
  }
}
