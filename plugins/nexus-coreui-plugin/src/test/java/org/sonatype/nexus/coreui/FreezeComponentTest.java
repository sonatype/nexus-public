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

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.FreezeService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FreezeComponentTest
    extends TestSupport
{

  FreezeComponent underTest;

  @Mock
  FreezeService freezeService;

  @Before
  public void setup() {
    underTest = new FreezeComponent(freezeService);
  }

  @Test
  public void read() throws Exception {
    when(freezeService.isFrozen()).thenReturn(true);
    FreezeStatusXO freezeStatusXO = underTest.read();
    assertThat(freezeStatusXO.isFrozen(), is(true));
  }

  @Test
  public void testUpdateRelease() throws Exception {
    FreezeStatusXO freezeStatusXO = new FreezeStatusXO();
    freezeStatusXO.setFrozen(false);

    underTest.update(freezeStatusXO);

    verify(freezeService).cancelFreeze();
  }

  @Test
  public void testUpdateFreeze() throws Exception {
    FreezeStatusXO freezeStatusXO = new FreezeStatusXO();
    freezeStatusXO.setFrozen(true);

    underTest.update(freezeStatusXO);

    verify(freezeService).requestFreeze(isA(String.class));
  }

}
