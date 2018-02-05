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
package org.sonatype.nexus.internal.commands;

import java.util.Arrays;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.internal.commands.DatabaseFreezeAction.Mode;
import org.sonatype.nexus.orient.freeze.DatabaseFreezeService;
import org.sonatype.nexus.orient.freeze.FreezeRequest;
import org.sonatype.nexus.orient.freeze.FreezeRequest.InitiatorType;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DatabaseFreezeActionTest
    extends TestSupport
{

  DatabaseFreezeAction underTest;

  @Mock
  DatabaseFreezeService databaseFreezeService;

  @Before
  public void setUp() throws Exception {
    underTest = new DatabaseFreezeAction(databaseFreezeService);
  }

  @Test
  public void executeFreeze() throws Exception {
    // defaults to 'enable' (read: freeze)
    underTest.execute();

    verify(databaseFreezeService).requestFreeze(isA(InitiatorType.class), isA(String.class));
    verify(databaseFreezeService, never()).releaseRequest(isA(FreezeRequest.class));
  }

  @Test
  public void executeRelease() throws Exception {
    underTest.mode = Mode.release;

    underTest.execute();

    verify(databaseFreezeService, never()).requestFreeze(isA(InitiatorType.class), isA(String.class));
    verify(databaseFreezeService).releaseUserInitiatedIfPresent();
  }

  @Test
  public void executeReleaseOnSystemTaskFails() throws Exception {
    underTest.mode = Mode.release;

    FreezeRequest request = new FreezeRequest(InitiatorType.SYSTEM, "DatabaseFreezeActionTest");
    when(databaseFreezeService.getState()).thenReturn(Arrays.asList(request));
    underTest.execute();

    verify(databaseFreezeService, never()).requestFreeze(isA(InitiatorType.class), isA(String.class));
    verify(databaseFreezeService, never()).releaseRequest(isA(FreezeRequest.class));
  }
}
