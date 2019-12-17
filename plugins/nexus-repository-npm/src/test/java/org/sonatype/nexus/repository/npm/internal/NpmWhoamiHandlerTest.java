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
package org.sonatype.nexus.repository.npm.internal;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.user.User;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

public class NpmWhoamiHandlerTest
    extends TestSupport
{
  @Mock
  private SecuritySystem securitySystem;

  @Mock
  private Context context;

  private NpmWhoamiHandler underTest;

  @Before
  public void setup() {
    underTest = new NpmWhoamiHandler(securitySystem, new ObjectMapper());
  }

  @Test
  public void testHandle() throws Exception {
    currentUser("testuser");
    Response response = underTest.handle(context);

    assertThat(response.getStatus().getCode(), is(200));
    assertThat(IOUtils.toString(response.getPayload().openInputStream()), is("{\"username\":\"testuser\"}"));
  }

  @Test
  public void testHandle_noUser() throws Exception {
    currentUser(null);
    Response response = underTest.handle(context);

    assertThat(response.getStatus().getCode(), is(200));
    assertThat(IOUtils.toString(response.getPayload().openInputStream()), is("{\"username\":\"anonymous\"}"));
  }

  private void currentUser(String userId) throws Exception {
    if (userId != null) {
      User user = new User();
      user.setUserId(userId);
      when(securitySystem.currentUser()).thenReturn(user);
    }
    else {
      when(securitySystem.currentUser()).thenReturn(null);
    }
  }
}
