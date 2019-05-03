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
package org.sonatype.nexus.security.internal;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.config.CUser;
import org.sonatype.nexus.security.config.SecurityConfigurationManager;

import org.apache.shiro.authc.credential.PasswordService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

public class UserManagerImplTest
    extends TestSupport
{
  @Mock
  EventManager eventManager;

  @Mock
  SecurityConfigurationManager securityConfigurationManager;

  @Mock
  SecuritySystem securitySystem;

  @Mock
  PasswordService passwordService;

  private UserManagerImpl underTest;

  @Before
  public void setup() {
    underTest = new UserManagerImpl(eventManager, securityConfigurationManager, securitySystem, passwordService);
  }

  @Test
  public void testChangePassword() throws Exception {
    CUser user = new CUser();
    user.setStatus(CUser.STATUS_CHANGE_PASSWORD);
    user.setId("test");

    when(securityConfigurationManager.readUser("test")).thenReturn(user);

    underTest.changePassword("test", "newpass");

    assertThat(user.getStatus(), is(CUser.STATUS_ACTIVE));
  }
}
