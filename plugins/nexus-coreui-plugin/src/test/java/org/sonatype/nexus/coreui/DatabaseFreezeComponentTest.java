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
package org.sonatype.nexus.coreui;

import java.util.Arrays;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.orient.freeze.DatabaseFreezeService;
import org.sonatype.nexus.orient.freeze.FreezeRequest;
import org.sonatype.nexus.orient.freeze.FreezeRequest.InitiatorType;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserNotFoundException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DatabaseFreezeComponentTest
    extends TestSupport
{

  DatabaseFreezeComponent underTest;

  @Mock
  DatabaseFreezeService databaseFreezeService;

  @Mock
  SecuritySystem securitySystem;

  @Before
  public void setup() {
    underTest = new DatabaseFreezeComponent();
    underTest.setDatabaseFreezeService(databaseFreezeService);
    underTest.setSecuritySystem(securitySystem);
  }

  @Test
  public void read() throws Exception {
    when(databaseFreezeService.isFrozen()).thenReturn(true);
    DatabaseFreezeStatusXO databaseFreezeStatusXO = underTest.read();
    assertThat(databaseFreezeStatusXO.getFrozen(), is(true));
  }

  @Test
  public void testUpdateRelease() throws Exception {
    DatabaseFreezeStatusXO databaseFreezeStatusXO = new DatabaseFreezeStatusXO();
    databaseFreezeStatusXO.setFrozen(false);

    underTest.update(databaseFreezeStatusXO);

    verify(databaseFreezeService).releaseUserInitiatedIfPresent();
  }

  @Test
  public void testUpdateReleaseFailsForSystemInitiated() throws Exception {
    DatabaseFreezeStatusXO databaseFreezeStatusXO = new DatabaseFreezeStatusXO();
    databaseFreezeStatusXO.setFrozen(false);

    FreezeRequest request = new FreezeRequest(InitiatorType.SYSTEM, "DatabaseFreezeComponentTest");
    when(databaseFreezeService.getState()).thenReturn(Arrays.asList(request));
    underTest.update(databaseFreezeStatusXO);

    verify(databaseFreezeService, never()).releaseRequest(isA(FreezeRequest.class));
  }

  @Test
  public void testUpdateFreeze() throws UserNotFoundException {
    DatabaseFreezeStatusXO databaseFreezeStatusXO = new DatabaseFreezeStatusXO();
    databaseFreezeStatusXO.setFrozen(true);

    User user = mock(User.class);
    when(user.getUserId()).thenReturn("admin");
    when(securitySystem.currentUser()).thenReturn(user);
    underTest.update(databaseFreezeStatusXO);

    verify(databaseFreezeService).requestFreeze(isA(InitiatorType.class), isA(String.class));
  }

}
