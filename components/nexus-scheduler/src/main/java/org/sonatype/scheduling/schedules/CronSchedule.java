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
package org.sonatype.scheduling.schedules;

import java.text.ParseException;
import java.util.Date;

import org.sonatype.scheduling.iterators.CronIterator;
import org.sonatype.scheduling.iterators.SchedulerIterator;
import org.sonatype.scheduling.iterators.cron.CronExpression;

public class CronSchedule
    extends AbstractSchedule
{
  private final String cronString;

  private final CronExpression cronExpression;

  public CronSchedule(String cronExpression)
      throws ParseException
  {
    super(new Date(), null);

    this.cronString = cronExpression;

    this.cronExpression = new CronExpression(cronString);
  }

  public String getCronString() {
    return cronString;
  }

  protected SchedulerIterator createIterator() {
    return new CronIterator(cronExpression);
  }

}
