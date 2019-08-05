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
package org.sonatype.nexus.scheduling.schedule;

import java.util.Date;
import java.util.Set;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.scheduling.schedule.Monthly.CalendarDay;
import org.sonatype.nexus.scheduling.schedule.Weekly.Weekday;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Support for {@link ScheduleFactory} implementations.
 *
 * @since 3.0
 */
public class ScheduleFactoryImpl
    extends ComponentSupport
    implements ScheduleFactory
{
  @Override
  public Manual manual() {
    return new Manual();
  }

  @Override
  public Now now() {
    return new Now();
  }

  @Override
  public Once once(final Date startAt) {
    checkNotNull(startAt);
    return new Once(startAt);
  }

  @Override
  public Hourly hourly(final Date startAt) {
    checkNotNull(startAt);
    return new Hourly(startAt);
  }

  @Override
  public Daily daily(final Date startAt) {
    checkNotNull(startAt);
    return new Daily(startAt);
  }

  @Override
  public Weekly weekly(final Date startAt, final Set<Weekday> daysToRun) {
    checkNotNull(startAt);
    checkNotNull(daysToRun);
    checkArgument(!daysToRun.isEmpty(), "No days of week set to run");
    return new Weekly(startAt, daysToRun);
  }

  @Override
  public Monthly monthly(final Date startAt, final Set<CalendarDay> daysToRun) {
    checkNotNull(startAt);
    checkNotNull(daysToRun);
    checkArgument(!daysToRun.isEmpty(), "No days of month set to run");
    return new Monthly(startAt, daysToRun);
  }

  @Override
  public Cron cron(final Date startAt, final String cronExpression) {
    checkNotNull(startAt);
    checkNotNull(cronExpression);
    return new Cron(startAt, cronExpression);
  }

  @Override
  public Cron cron(final Date startAt, final String cronExpression, final String zoneId) {
    checkNotNull(startAt);
    checkNotNull(cronExpression);
    checkNotNull(zoneId);
    return new Cron(startAt, cronExpression, zoneId);
  }
}
