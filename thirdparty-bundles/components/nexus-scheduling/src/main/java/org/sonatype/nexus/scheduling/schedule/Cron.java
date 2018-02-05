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

/**
 * Schedule that accepts cron expression.
 *
 * @see ScheduleFactory#cron(Date, String)
 */
public class Cron
    extends Schedule
{
  public static final String TYPE = "cron";

  public static final String SCHEDULE_CRON_EXPRESSION = "schedule.cronExpression";

  public Cron(final Date startAt, final String cronExpression) {
    super(TYPE);
    set(SCHEDULE_START_AT, dateToString(startAt));
    set(SCHEDULE_CRON_EXPRESSION, cronExpression);
  }

  public Date getStartAt() {
    return stringToDate(get(SCHEDULE_START_AT));
  }

  public String getCronExpression() {
    return get(SCHEDULE_CRON_EXPRESSION);
  }
}