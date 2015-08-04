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
package org.sonatype.nexus.testsuite.task.nexus3996;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.rest.model.ScheduledServiceOnceResource;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.tasks.descriptors.EmptyTrashTaskDescriptor;
import org.sonatype.nexus.test.utils.NexusRequestMatchers;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

import com.google.common.collect.Lists;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Nexus would not create tasks for the same day if the client is in a timezone with a larger offset from UTC than
 * the server's timezone.
 * <p/>
 * The date is sent separate from the time as a timestamp for midnight of the selected day. The midnight is calculated
 * on the client based on the local timezone.
 * <p/>
 * The server does not know which timezone the client is in and validates the date based on his timezone, rejecting
 * every midnight that is earlier than local time and not on the same day of the year.
 * <p/>
 * This test simulates a client whose timezone offset is two hours more than the local timezone.
 * <p/>
 * Related issues with timezone problems in the scheduled tasks:
 * <ul>
 * <li> https://issues.sonatype.org/browse/NEXUS-4617 </li>
 * <li> https://issues.sonatype.org/browse/NEXUS-4616 </li>
 * </ul>
 *
 * @since 2.0
 */
public class Nexus3996ScheduledTasksTimezoneDifferentDayValidationIT
    extends AbstractNexusIntegrationTest
{

  @Test
  public void createTask()
      throws IOException
  {

    final ScheduledServiceOnceResource task = new ScheduledServiceOnceResource();
    task.setName("name");
    task.setSchedule("once");
    task.setTypeId(EmptyTrashTaskDescriptor.ID);
    ScheduledServicePropertyResource property = new ScheduledServicePropertyResource();
    property.setKey(EmptyTrashTaskDescriptor.OLDER_THAN_FIELD_ID);
    task.setProperties(Lists.newArrayList(property));

    Calendar cal = Calendar.getInstance();

    // simulating client timezone here: calculate offset from current timezone

    // client is tz+2 -> 3 hours ahead for local tz is one hour ahead for client tz
    cal.add(Calendar.HOUR_OF_DAY, 3);
    task.setStartTime(new SimpleDateFormat("HH:mm").format(cal.getTime()));

    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);

    // client is tz+2 -> midnight for client happens 2 hours before servers midnight
    cal.add(Calendar.HOUR_OF_DAY, -2);
    task.setStartDate(String.valueOf(cal.getTimeInMillis()));

    log.debug("request dates:\nmidnight: {}\ntime offset: {}", cal.getTime(), task.getStartTime());

    assertThat(TaskScheduleUtil.create(task), NexusRequestMatchers.hasStatusCode(201));
  }

}
