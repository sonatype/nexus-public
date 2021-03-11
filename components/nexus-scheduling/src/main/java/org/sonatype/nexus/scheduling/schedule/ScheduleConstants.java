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

/**
 * Constants for the schedule properties.
 *
 * @since 3.30
 */
public class ScheduleConstants
{
  static final String CLASS_TYPE = "type";

  static final String DAY = "day";

  static final String DAYS_TO_RUN = "daysToRun";

  static final String START_AT = "startAt";

  static final String CRON_EXPRESSION = "cronExpression";

  static final String TIME_ZONE = "timeZone";

  private ScheduleConstants() {}
}
