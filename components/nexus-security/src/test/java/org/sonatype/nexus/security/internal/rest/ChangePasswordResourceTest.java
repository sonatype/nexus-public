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
package org.sonatype.nexus.security.internal.rest;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.rest.WebApplicationMessageException;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.user.UserNotFoundException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class ChangePasswordResourceTest
    extends TestSupport
{
  @Mock
  SecuritySystem securitySystem;

  private ChangePasswordResource underTest;

  @Before
  public void setup() {
    underTest = new ChangePasswordResource(securitySystem);
  }

  @Test
  public void testChangePassword() throws Exception {
    underTest.changePassword("test", "test");

    verify(securitySystem).changePassword("test", "test");
  }

  @Test
  public void testChangePassword_invalidUser() throws Exception {
    doThrow(new UserNotFoundException("test")).when(securitySystem).changePassword("test", "test");

    try {
      underTest.changePassword("test", "test");
      fail("WebApplicationMessageException should have been thrown");
    }
    catch (WebApplicationMessageException e) {
      assertThat(e.getResponse().getStatus(), is(404));
      assertThat(e.getResponse().getEntity(), is("User 'test' not found."));
    }
  }

  @Test
  public void testChangePassword_missingPassword() throws Exception {
    try {
      underTest.changePassword("test", null);
      fail("WebApplicationMessageException should have been thrown");
    }
    catch (WebApplicationMessageException e) {
      assertThat(e.getResponse().getStatus(), is(400));
      assertThat(e.getResponse().getEntity(), is("Password must be supplied."));
      verify(securitySystem, never()).changePassword(any(), any());
    }
  }

  @Test
  public void testChangePassword_emptyPassword() throws Exception {
    try {
      underTest.changePassword("test", "");
      fail("WebApplicationMessageException should have been thrown");
    }
    catch (WebApplicationMessageException e) {
      assertThat(e.getResponse().getStatus(), is(400));
      assertThat(e.getResponse().getEntity(), is("Password must be supplied."));
      verify(securitySystem, never()).changePassword(any(), any());
    }
  }
}
