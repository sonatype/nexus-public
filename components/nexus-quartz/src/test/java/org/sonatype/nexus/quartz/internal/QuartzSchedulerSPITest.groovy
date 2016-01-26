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
package org.sonatype.nexus.quartz.internal

import javax.inject.Provider

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.common.event.EventBus
import org.sonatype.nexus.common.node.LocalNodeAccess
import org.sonatype.nexus.scheduling.TaskConfiguration
import org.sonatype.nexus.scheduling.schedule.Hourly

import org.joda.time.DateTime
import org.joda.time.Duration
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.quartz.JobDetail
import org.quartz.spi.JobStore
import org.quartz.spi.OperableTrigger

import static org.mockito.Matchers.any
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

/**
 * {@link QuartzSchedulerSPI} tests.
 *
 * @since 3.0
 */
class QuartzSchedulerSPITest
    extends TestSupport
{
  @Test
  void 'Scheduling in the past should fire trigger in next hour'() {
    def localNodeAccess = mock(LocalNodeAccess)
    when(localNodeAccess.id).thenReturn("test");
    def provider = mock(Provider)
    def jobStore = mock(JobStore)
    when(provider.get()).thenReturn(jobStore)

    def underTest = new QuartzSchedulerSPI(
        mock(EventBus), localNodeAccess, provider, mock(JobFactoryImpl), 1
    );
    underTest.start();

    def now = DateTime.now()
    def startAt = DateTime.parse("2010-06-30T01:20");
    underTest.scheduleTask(new TaskConfiguration(id: 'test', typeId: 'test'), new Hourly(startAt.toDate()))

    def triggerRecorder = ArgumentCaptor.forClass(OperableTrigger)
    verify(jobStore).storeJobAndTrigger(any(JobDetail), triggerRecorder.capture())


    def triggerTime = new DateTime(triggerRecorder.getValue().startTime)
    // trigger should be in the future
    assert triggerTime.isAfter(now)
    // trigger should have the same minute as start time
    assert triggerTime.minuteOfHour() == startAt.minuteOfHour()
    // trigger should fire in the next hour
    assert new Duration(now, triggerTime).standardMinutes < 60
  }
}
