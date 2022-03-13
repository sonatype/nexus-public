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

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.FreezeService;
import org.sonatype.nexus.internal.commands.FreezeAction.Mode;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class FreezeActionTest
    extends TestSupport
{

  FreezeAction underTest;

  @Mock
  FreezeService freezeService;

  @Before
  public void setUp() throws Exception {
    underTest = new FreezeAction(freezeService);
  }

  @Test
  public void executeFreeze() throws Exception {
    // defaults to 'enable' (read: freeze)
    underTest.execute();

    verify(freezeService).requestFreeze(isA(String.class));
    verify(freezeService, never()).cancelFreeze();
  }

  @Test
  public void executeRelease() throws Exception {
    underTest.mode = Mode.release;

    underTest.execute();

    verify(freezeService, never()).requestFreeze(isA(String.class));
    verify(freezeService).cancelFreeze();
  }
}
