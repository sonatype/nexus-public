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
import java.util.Collection;
import java.util.Collections;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.systemchecks.NodeSystemCheckResult;
import org.sonatype.nexus.systemchecks.SystemCheckService;

import com.codahale.metrics.health.HealthCheck.Result;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.rapture.internal.state.HealthCheckStateContributor.HC_FAILED_KEY;

@RunWith(Parameterized.class)
public class HealthCheckStateContributorTest
    extends TestSupport
{
  @Parameters
  public static Collection<Object[]> data() {
      return Arrays.asList(new Object[][] {
          {true, Result.unhealthy("Not healthy")},
          {false, Result.healthy()}});
  }

  @Parameter
  public boolean expectedState;

  @Parameter(1)
  public Result result;

  @Mock
  private SystemCheckService systemCheckService;

  @Mock
  private NodeSystemCheckResult nodeA;

  @Mock
  private NodeSystemCheckResult nodeB;

  @InjectMocks
  private HealthCheckStateContributor subject;

  @Test
  public void testIndicator() {
    when(nodeA.getResult()).thenReturn(Collections.emptyMap());
    when(nodeB.getResult()).thenReturn(ImmutableMap.of(
        "a", Result.healthy(),
        "b", result,
        "c", Result.healthy()
    ));
    when(systemCheckService.getResults()).thenReturn(Arrays.asList(nodeA, nodeB).stream());

    assertThat(subject.getState(), hasEntry(HC_FAILED_KEY, expectedState));
  }
}
