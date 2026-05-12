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
package org.sonatype.nexus.self.hosted.blobstore.s3.upgrade;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class ScheduleS3CompactTasksTaskDescriptorTest
{
  private ScheduleS3CompactTasksTaskDescriptor underTest;

  @Test
  public void testNotExposedByDefault() {
    underTest = new ScheduleS3CompactTasksTaskDescriptor(false);

    assertThat("Task should not be exposed by default", underTest.isExposed(), is(false));
    assertThat("Task should not be visible by default", underTest.isVisible(), is(false));
  }

  @Test
  public void testVisibleAndExposedWhenEnabled() {
    underTest = new ScheduleS3CompactTasksTaskDescriptor(true);

    assertThat("Task should be visible when enabled", underTest.isVisible(), is(true));
    assertThat("Task should be exposed when enabled", underTest.isExposed(), is(true));
  }

  @Test
  public void testTaskType() {
    underTest = new ScheduleS3CompactTasksTaskDescriptor(false);

    assertThat("Task type should be ScheduleS3CompactTasksTask",
        underTest.getType(),
        equalTo(ScheduleS3CompactTasksTask.class));
  }

  @Test
  public void testName() {
    underTest = new ScheduleS3CompactTasksTaskDescriptor(false);

    assertThat("Task name should be set", underTest.getName(), notNullValue());
    assertThat("Task name should describe the task",
        underTest.getName(),
        equalTo("Schedule compact blobstore tasks based on expiration policy"));
  }

  @Test
  public void testIsVisibleReturnsFalseWhenNotExposed() {
    underTest = new ScheduleS3CompactTasksTaskDescriptor(false);

    assertThat("Task should not be visible in UI when exposed is false",
        underTest.isVisible(),
        is(false));
  }

  @Test
  public void testIsExposedReturnsFalseWhenNotExposed() {
    underTest = new ScheduleS3CompactTasksTaskDescriptor(false);

    assertThat("Task should not be exposed when exposed is false",
        underTest.isExposed(),
        is(false));
  }
}
