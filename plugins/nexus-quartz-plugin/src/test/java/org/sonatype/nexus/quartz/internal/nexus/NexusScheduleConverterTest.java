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
package org.sonatype.nexus.quartz.internal.nexus;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.scheduling.schedule.Hourly;
import org.sonatype.nexus.scheduling.schedule.Monthly;
import org.sonatype.nexus.scheduling.schedule.Monthly.CalendarDay;
import org.sonatype.nexus.scheduling.schedule.Weekly;
import org.sonatype.nexus.scheduling.schedule.Weekly.Weekday;

import org.junit.Test;
import org.mockito.internal.util.collections.Sets;
import org.quartz.CronTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.triggers.SimpleTriggerImpl;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * UT of {@link NexusScheduleConverter}
 */
public class NexusScheduleConverterTest
    extends TestSupport
{
  final NexusScheduleConverter converter = new NexusScheduleConverter();

  @Test
  public void cronTimeParts() {
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.HOUR_OF_DAY, 7);
    cal.set(Calendar.MINUTE, 15);
    assertThat(converter.cronTimeParts(cal.getTime()), equalTo("0 15 7"));
    cal.set(Calendar.HOUR_OF_DAY, 22);
    cal.set(Calendar.MINUTE, 59);
    assertThat(converter.cronTimeParts(cal.getTime()), equalTo("0 59 22"));
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    assertThat(converter.cronTimeParts(cal.getTime()), equalTo("0 0 0"));
  }

  @Test
  public void hourly() {
    final Date now = new Date();
    final Hourly hourly = new Hourly(now);
    final TriggerBuilder triggerBuilder = converter.toTrigger(hourly);
    final Trigger trigger = triggerBuilder.build();
    assertThat(trigger, instanceOf(SimpleTriggerImpl.class));
    assertThat(trigger.getFireTimeAfter(new Date(now.getTime())),
        equalTo(new Date(now.getTime() + TimeUnit.HOURS.toMillis(1L))));
  }

  @Test
  public void weekly1() {
    final Weekly weekly = new Weekly(new Date(), Sets.newSet(Weekday.SAT));
    final TriggerBuilder triggerBuilder = converter.toTrigger(weekly);
    final Trigger trigger = triggerBuilder.build();
    assertThat(trigger, instanceOf(CronTrigger.class));
    final String cronExpression = ((CronTrigger) trigger).getCronExpression();
    assertThat(cronExpression, equalTo(converter.cronTimeParts(weekly.getStartAt()) + " ? * SAT"));
  }

  @Test
  public void weekly2() {
    final Weekly weekly = new Weekly(new Date(), Sets.newSet(Weekday.SAT, Weekday.FRI));
    final TriggerBuilder triggerBuilder = converter.toTrigger(weekly);
    final Trigger trigger = triggerBuilder.build();
    assertThat(trigger, instanceOf(CronTrigger.class));
    final String cronExpression = ((CronTrigger) trigger).getCronExpression();
    assertThat(cronExpression, equalTo(converter.cronTimeParts(weekly.getStartAt()) + " ? * FRI,SAT"));
  }

  @Test
  public void montlhy1() {
    final Monthly monthly = new Monthly(new Date(), CalendarDay.days(2));
    final TriggerBuilder triggerBuilder = converter.toTrigger(monthly);
    final Trigger trigger = triggerBuilder.build();
    assertThat(trigger, instanceOf(CronTrigger.class));
    final String cronExpression = ((CronTrigger) trigger).getCronExpression();
    assertThat(cronExpression, equalTo(converter.cronTimeParts(monthly.getStartAt()) + " 2 * ?"));
  }

  @Test
  public void montlhy2() {
    final Monthly monthly = new Monthly(new Date(), CalendarDay.days(1, 2, 3, 10, 11, 12));
    final TriggerBuilder triggerBuilder = converter.toTrigger(monthly);
    final Trigger trigger = triggerBuilder.build();
    assertThat(trigger, instanceOf(CronTrigger.class));
    final String cronExpression = ((CronTrigger) trigger).getCronExpression();
    assertThat(cronExpression, equalTo(converter.cronTimeParts(monthly.getStartAt()) + " 1,2,3,10,11,12 * ?"));
  }

  @Test
  public void montlhy3() {
    final Monthly monthly = new Monthly(new Date(), Sets.newSet(CalendarDay.lastDay()));
    final TriggerBuilder triggerBuilder = converter.toTrigger(monthly);
    final Trigger trigger = triggerBuilder.build();
    assertThat(trigger, instanceOf(CronTrigger.class));
    final String cronExpression = ((CronTrigger) trigger).getCronExpression();
    assertThat(cronExpression, equalTo(converter.cronTimeParts(monthly.getStartAt()) + " L * ?"));
  }
}
