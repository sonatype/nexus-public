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
package org.sonatype.nexus.ui;

import java.net.URL;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class UiUtilTest
{
  private static final String TEST_URL = "https://someurl/nexus-frontend-bundle.js";

  @Mock
  private Resource resource;

  @Mock
  private ApplicationContext context;

  @InjectMocks
  private UiUtil undertest;

  @Test
  public void getHashedFilename() throws Exception {
    when(resource.getURL()).thenReturn(new URL(TEST_URL));
    when(context.getResources("classpath*:/static/**/nexus-frontend-bundle.js")).thenReturn(new Resource[]{resource});

    String hashedFilename = undertest.getPathForFile("nexus-frontend-bundle.js");

    assertThat(hashedFilename, is("/nexus-frontend-bundle.js"));
  }
}
