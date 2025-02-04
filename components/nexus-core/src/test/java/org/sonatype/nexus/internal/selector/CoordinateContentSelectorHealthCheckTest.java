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
package org.sonatype.nexus.internal.selector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorManager;

import com.codahale.metrics.health.HealthCheck.Result;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.lessThan;
import static org.mockito.Mockito.when;

public class CoordinateContentSelectorHealthCheckTest
    extends TestSupport
{
  @Mock
  SelectorConfiguration noCoordinates1;

  @Mock
  SelectorConfiguration noCoordinates2;

  @Mock
  SelectorConfiguration noCoordinates3;

  @Mock
  SelectorConfiguration coordinates1;

  @Mock
  SelectorConfiguration coordinates2;

  @Mock
  SelectorConfiguration coordinates3;

  @Mock
  SelectorManager selectorManager;

  CoordinateContentSelectorHealthCheck coordinateCheck;

  @Before
  public void setup() {
    when(noCoordinates1.hasCoordinates()).thenReturn(false);
    when(noCoordinates2.hasCoordinates()).thenReturn(false);
    when(noCoordinates3.hasCoordinates()).thenReturn(false);

    when(coordinates1.hasCoordinates()).thenReturn(true);
    when(coordinates2.hasCoordinates()).thenReturn(true);
    when(coordinates3.hasCoordinates()).thenReturn(true);

    when(coordinates1.getName()).thenReturn("coordinates1");
    when(coordinates2.getName()).thenReturn("coordinates2");
    when(coordinates3.getName()).thenReturn("coordinates3");

    coordinateCheck = new CoordinateContentSelectorHealthCheck(selectorManager);
  }

  @Test
  public void noSelectorsIsHealthy() {
    when(selectorManager.browse()).thenReturn(Collections.emptyList());

    assertThat(coordinateCheck.check().isHealthy(), is(true));
  }

  @Test
  public void allSelectorsWithoutCoordinatesIsHealthy() {
    when(selectorManager.browse()).thenReturn(newArrayList(noCoordinates1, noCoordinates2, noCoordinates3));

    assertThat(coordinateCheck.check().isHealthy(), is(true));
  }

  @Test
  public void allSelectorsWitCoordinatesIsUnhealthy() {
    when(selectorManager.browse()).thenReturn(newArrayList(coordinates3, coordinates1, coordinates2));

    Result result = coordinateCheck.check();
    assertThat(result.isHealthy(), is(false));
    assertThat(result.getMessage(), containsString("3"));
    assertThat(result.getMessage(), containsString("coordinates1, coordinates2, coordinates3"));
  }

  @Test
  public void longSelectorNamesAreTruncated() {
    List<SelectorConfiguration> selectorConfigurationList = new ArrayList<>();
    for (int idx = 0; idx < 160; idx++) {
      selectorConfigurationList.add(coordinates3);
      selectorConfigurationList.add(coordinates1);
      selectorConfigurationList.add(coordinates2);
    }
    when(selectorManager.browse()).thenReturn(selectorConfigurationList);

    Result result = coordinateCheck.check();
    assertThat(result.isHealthy(), is(false));
    assertThat(result.getMessage().length(), is(lessThan(300)));
  }

  @Test
  public void someSelectorsWitCoordinatesIsUnhealthy() {
    when(selectorManager.browse()).thenReturn(newArrayList(coordinates3, noCoordinates1, coordinates1, noCoordinates2, noCoordinates3));

    Result result = coordinateCheck.check();
    assertThat(result.isHealthy(), is(false));
    assertThat(result.getMessage(), containsString("2"));
    assertThat(result.getMessage(), containsString("coordinates1, coordinates3"));
  }
}
