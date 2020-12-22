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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.Date;
import java.util.Set;

import org.sonatype.nexus.scheduling.schedule.Monthly.CalendarDay;
import org.sonatype.nexus.scheduling.schedule.Weekly.Weekday;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit tests for the {@link ScheduleSerializer} and {@link ScheduleDeserializer}
 */
public class ScheduleSerializerDeserializerTest
{
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Test
  public void testMonthlySerializationDeserialization() throws Exception {
    File file = File.createTempFile("monthly", ".json");

    Date date = new Date();
    Set<CalendarDay> daysToRun = CalendarDay.days(1, 2, 3, 10, 11, 12);
    Schedule monthly = new Monthly(date, daysToRun);
    TestSchedule schedule = new TestSchedule();
    schedule.setSchedule(monthly);
    OBJECT_MAPPER.writeValue(file, schedule);
    TestSchedule deserializedSchedule = importObjectFromJson(file);

    assertThat(deserializedSchedule.getSchedule(), instanceOf(Monthly.class));
    Monthly deserializedMonthly = (Monthly) deserializedSchedule.getSchedule();
    assertThat(deserializedMonthly.getStartAt(), is(date));
    assertThat(deserializedMonthly.getDaysToRun(), is(daysToRun));

    FileUtils.deleteQuietly(file);
  }

  @Test
  public void testWeeklySerializationDeserialization() throws Exception {
    File file = File.createTempFile("weekly", ".json");

    Date date = new Date();
    Set<Weekday> daysToRun = ImmutableSet.of(Weekday.MON, Weekday.FRI);
    Schedule weekly = new Weekly(date, daysToRun);
    TestSchedule schedule = new TestSchedule();
    schedule.setSchedule(weekly);
    OBJECT_MAPPER.writeValue(file, schedule);
    TestSchedule deserializedSchedule = importObjectFromJson(file);

    assertThat(deserializedSchedule.getSchedule(), instanceOf(Weekly.class));
    Weekly deserializedWeekly = (Weekly) deserializedSchedule.getSchedule();
    assertThat(deserializedWeekly.getStartAt(), is(date));
    assertThat(deserializedWeekly.getDaysToRun(), is(daysToRun));

    FileUtils.deleteQuietly(file);
  }

  @Test
  public void testDailySerializationDeserialization() throws Exception {
    File file = File.createTempFile("daily", ".json");

    Date date = new Date();
    Schedule daily = new Daily(date);
    TestSchedule schedule = new TestSchedule();
    schedule.setSchedule(daily);
    OBJECT_MAPPER.writeValue(file, schedule);
    TestSchedule deserializedSchedule = importObjectFromJson(file);

    assertThat(deserializedSchedule.getSchedule(), instanceOf(Daily.class));
    Daily deserializedDaily = (Daily) deserializedSchedule.getSchedule();
    assertThat(deserializedDaily.getStartAt(), is(date));

    FileUtils.deleteQuietly(file);
  }

  @Test
  public void testHourlySerializationDeserialization() throws Exception {
    File file = File.createTempFile("hourly", ".json");

    Date date = new Date();
    Schedule hourly = new Hourly(date);
    TestSchedule schedule = new TestSchedule();
    schedule.setSchedule(hourly);
    OBJECT_MAPPER.writeValue(file, schedule);
    TestSchedule deserializedSchedule = importObjectFromJson(file);

    assertThat(deserializedSchedule.getSchedule(), instanceOf(Hourly.class));
    Hourly deserializedHourly = (Hourly) deserializedSchedule.getSchedule();
    assertThat(deserializedHourly.getStartAt(), is(date));

    FileUtils.deleteQuietly(file);
  }

  @Test
  public void testOnceSerializationDeserialization() throws Exception {
    File file = File.createTempFile("once", ".json");

    Date date = new Date();
    Schedule once = new Once(date);
    TestSchedule schedule = new TestSchedule();
    schedule.setSchedule(once);
    OBJECT_MAPPER.writeValue(file, schedule);
    TestSchedule deserializedSchedule = importObjectFromJson(file);

    assertThat(deserializedSchedule.getSchedule(), instanceOf(Once.class));
    Once deserializedOnce = (Once) deserializedSchedule.getSchedule();
    assertThat(deserializedOnce.getStartAt(), is(date));

    FileUtils.deleteQuietly(file);
  }

  @Test
  public void testCronSerializationDeserialization() throws Exception {
    File file = File.createTempFile("cron", ".json");

    Date date = new Date();
    String cronExpression = "0 0 */6 ? * *";
    String zoneId = ZoneId.systemDefault().toString();
    Schedule cron = new Cron(date, cronExpression, zoneId);
    TestSchedule schedule = new TestSchedule();
    schedule.setSchedule(cron);
    OBJECT_MAPPER.writeValue(file, schedule);
    TestSchedule deserializedSchedule = importObjectFromJson(file);

    assertThat(deserializedSchedule.getSchedule(), instanceOf(Cron.class));
    Cron deserializedCron = (Cron) deserializedSchedule.getSchedule();
    assertThat(deserializedCron.getStartAt(), is(date));
    assertThat(deserializedCron.getCronExpression(), is(cronExpression));
    assertThat(deserializedCron.getTimeZone().toZoneId().toString(), is(zoneId));

    FileUtils.deleteQuietly(file);
  }

  @Test
  public void testManualSerializationDeserialization() throws Exception {
    File file = File.createTempFile("manual", ".json");

    Schedule manual = new Manual();
    TestSchedule schedule = new TestSchedule();
    schedule.setSchedule(manual);
    OBJECT_MAPPER.writeValue(file, schedule);
    TestSchedule deserializedSchedule = importObjectFromJson(file);

    assertThat(deserializedSchedule.getSchedule(), instanceOf(Manual.class));

    FileUtils.deleteQuietly(file);
  }

  @Test
  public void testNowSerializationDeserialization() throws Exception {
    File file = File.createTempFile("now", ".json");

    Schedule now = new Now();
    TestSchedule schedule = new TestSchedule();
    schedule.setSchedule(now);
    OBJECT_MAPPER.writeValue(file, schedule);
    TestSchedule deserializedSchedule = importObjectFromJson(file);

    assertThat(deserializedSchedule.getSchedule(), instanceOf(Now.class));

    FileUtils.deleteQuietly(file);
  }

  public TestSchedule importObjectFromJson(final File file) throws IOException {
    try (FileInputStream inputStream = new FileInputStream(file)) {
      String jsonData = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
      return OBJECT_MAPPER.readValue(jsonData, TestSchedule.class);
    }
  }

  private static class TestSchedule
  {
    @JsonSerialize(using = ScheduleSerializer.class)
    @JsonDeserialize(using = ScheduleDeserializer.class)
    private Schedule schedule;

    public Schedule getSchedule() {
      return schedule;
    }

    public void setSchedule(final Schedule schedule) {
      this.schedule = schedule;
    }
  }
}
