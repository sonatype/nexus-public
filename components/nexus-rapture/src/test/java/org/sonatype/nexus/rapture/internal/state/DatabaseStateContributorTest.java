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
package org.sonatype.nexus.rapture.internal.state;

import java.util.Arrays;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.orient.freeze.DatabaseFreezeService;
import org.sonatype.nexus.orient.freeze.FreezeRequest;
import org.sonatype.nexus.orient.freeze.FreezeRequest.InitiatorType;
import org.sonatype.nexus.security.SecurityHelper;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class DatabaseStateContributorTest
    extends TestSupport
{

  DatabaseStateContributor underTest;

  @Mock
  DatabaseFreezeService databaseFreezeService;

  @Mock
  SecurityHelper securityHelper;

  @Before
  public void setup() {
    underTest = new DatabaseStateContributor(databaseFreezeService, securityHelper);
  }

  @Test
  public void testGetState() {
    when(databaseFreezeService.isFrozen()).thenReturn(true);
    assertThat(underTest.getState().get("db"), is(state(true)));

    when(databaseFreezeService.isFrozen()).thenReturn(false);
    assertThat(underTest.getState().get("db"), is(state(false)));
  }

  @Test
  public void testGetStateSystemTask() {
    when(databaseFreezeService.isFrozen()).thenReturn(true);
    when(databaseFreezeService.getState()).thenReturn(
        Arrays.asList(new FreezeRequest(InitiatorType.SYSTEM, "test")));

    // when authorized see the reason
    when(securityHelper.allPermitted(any())).thenReturn(true);
    assertThat(underTest.getState().get("db"), is(state(true, "activated by 1 running system task(s)", true)));

    // when unauthorized reason is blank (but frozen is still true)
    when(securityHelper.allPermitted(any())).thenReturn(false);
    assertThat(underTest.getState().get("db"), is(state(true, "", true)));
  }


  Map<String, Object> state(boolean frozen) {
    return state(frozen, "");
  }

  Map<String, Object> state(boolean frozen, String message) {
    return state(frozen, message, false);
  }

  Map<String, Object> state(boolean frozen, String message, boolean system) {
    return of(
        "dbFrozen", frozen,
        "system", system,
        "reason", message
    );
  }
}
