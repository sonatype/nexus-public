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
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Schedule that accepts cron expression.
 */
public class Cron
    extends Schedule
{
  private static final Pattern cronPatters = Pattern.compile(
      "^([[0-9]|\\-|,|\\*]+) ([[0-9]|\\-|,|\\*]+) ([[0-9]|\\-|,|\\*]+) (\\?|\\*|[[0-9]|\\-|,|/|L|W]+) (\\*|[JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC|[0-9]|\\-|,]+) (\\*|\\?|[MON|TUE|WED|THU|FRI|SAT|SUN|[0-9]|\\-|,|/|L|W]+)( [[0-9]{4}|,]+)?$");

  public Cron(final Date startAt, final String cronExpression) {
    super("cron");
    checkNotNull(startAt);
    checkNotNull(cronExpression);
    checkArgument(cronPatters.matcher(cronExpression).matches(), "Invalid Cron expression: %s", cronExpression);
    properties.put("schedule.startAt", dateToString(startAt));
    properties.put("schedule.cronExpression", cronExpression);

  }

  public Date getStartAt() {
    return stringToDate(properties.get("schedule.startAt"));
  }

  public String getCronExpression() {
    return properties.get("schedule.cronExpression");
  }
}