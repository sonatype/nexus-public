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
package org.sonatype.nexus.quartz.internal.task;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.scheduling.TaskConfiguration;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.scheduling.TaskConfiguration.REMOVE_ATTRIBUTE_MARKER;

public class QuartzTaskUtilsTest
    extends TestSupport
{
  public static final String FOO_KEY = "foo";

  public static final String BAR_KEY = "bar";

  public static final String FOO_VALUE = "fooValue";

  @Mock
  private JobDetail jobDetail;

  @Mock
  private JobDataMap jobDataMap;

  @Mock
  private TaskConfiguration taskConfiguration;

  @Before
  public void setup() {
    when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);
  }

  @Test
  public void shouldRemoveAttribute() {
    when(taskConfiguration.asMap()).thenReturn(ImmutableMap.of(FOO_KEY, FOO_VALUE, BAR_KEY, REMOVE_ATTRIBUTE_MARKER));
    when(jobDataMap.get(FOO_KEY)).thenReturn(FOO_VALUE);

    QuartzTaskUtils.updateJobData(jobDetail, taskConfiguration);

    verify(jobDataMap).remove(BAR_KEY);
    verify(jobDataMap, never()).put(anyString(), anyString());
  }

  @Test
  public void shouldUpdateAttribute() {
    String anotherFooValue = "anotherFooValue";

    when(taskConfiguration.asMap()).thenReturn(ImmutableMap.of(FOO_KEY, FOO_VALUE));
    when(jobDataMap.get(FOO_KEY)).thenReturn(anotherFooValue);

    QuartzTaskUtils.updateJobData(jobDetail, taskConfiguration);

    verify(jobDataMap).put(FOO_KEY, FOO_VALUE);
    verify(jobDataMap, never()).remove(BAR_KEY);
  }
}
