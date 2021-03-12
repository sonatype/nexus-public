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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.sonatype.nexus.scheduling.schedule.Monthly.CalendarDay;
import org.sonatype.nexus.scheduling.schedule.Weekly.Weekday;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import static org.sonatype.nexus.scheduling.schedule.ScheduleConstants.CLASS_TYPE;
import static org.sonatype.nexus.scheduling.schedule.ScheduleConstants.CRON_EXPRESSION;
import static org.sonatype.nexus.scheduling.schedule.ScheduleConstants.DAY;
import static org.sonatype.nexus.scheduling.schedule.ScheduleConstants.DAYS_TO_RUN;
import static org.sonatype.nexus.scheduling.schedule.ScheduleConstants.START_AT;
import static org.sonatype.nexus.scheduling.schedule.ScheduleConstants.TIME_ZONE;

/**
 * Deserializer for the {@link Schedule} class.
 *
 * @since 3.30
 */
public class ScheduleDeserializer
    extends StdDeserializer<Schedule>
{
  private static final long serialVersionUID = 4245698506536873052L;

  public ScheduleDeserializer() {
    super(Schedule.class);
  }

  @Override
  public Schedule deserialize(JsonParser parser, DeserializationContext context)
      throws IOException
  {
    JsonNode node = parser.readValueAsTree();
    String type = node.get(CLASS_TYPE).textValue();
    if (Monthly.TYPE.equals(type)) {
      Date date = parseDate(node.get(START_AT));
      Set<CalendarDay> daysToRun = parseCalendarDays(node.get(DAYS_TO_RUN));
      return new Monthly(date, daysToRun);
    }
    else if (Weekly.TYPE.equals(type)) {
      Date date = parseDate(node.get(START_AT));
      Set<Weekday> daysToRun = parseDaysToRun(node.get(DAYS_TO_RUN));
      return new Weekly(date, daysToRun);
    }
    else if (Daily.TYPE.equals(type)) {
      Date date = parseDate(node.get(START_AT));
      return new Daily(date);
    }
    else if (Hourly.TYPE.equals(type)) {
      Date date = parseDate(node.get(START_AT));
      return new Hourly(date);
    }
    else if (Once.TYPE.equals(type)) {
      Date date = parseDate(node.get(START_AT));
      return new Once(date);
    }
    else if (Cron.TYPE.equals(type)) {
      Date date = parseDate(node.get(START_AT));
      String cronExpression = node.get(CRON_EXPRESSION).asText();
      String zoneId = node.get(TIME_ZONE).asText();
      return new Cron(date, cronExpression, zoneId);
    }
    else if (Manual.TYPE.equals(type)) {
      return new Manual();
    }
    else if (Now.TYPE.equals(type)) {
      return new Now();
    }
    else {
      throw new UnsupportedEncodingException("Can't determine the class type: " + type);
    }
  }

  private Set<CalendarDay> parseCalendarDays(final JsonNode jsonNode) {
    Set<CalendarDay> calendarDays = new HashSet<>();
    if (jsonNode.isArray()) {
      Iterator<JsonNode> elements = jsonNode.elements();
      while (elements.hasNext()) {
        JsonNode calendarDayElement = elements.next();
        int day = calendarDayElement.get(DAY).asInt();
        calendarDays.add(CalendarDay.day(day));
      }
    }

    return calendarDays;
  }

  private Set<Weekday> parseDaysToRun(final JsonNode jsonNode) {
    Set<Weekday> weekdays = new HashSet<>();
    if (jsonNode.isArray()) {
      Iterator<JsonNode> elements = jsonNode.elements();
      while (elements.hasNext()) {
        String weekday = elements.next().asText();
        weekdays.add(Weekday.valueOf(weekday));
      }
    }

    return weekdays;
  }

  private Date parseDate(final JsonNode jsonNode) throws UnsupportedEncodingException {
    if (jsonNode.isTextual()) {
      String startAt = jsonNode.asText();
      OffsetDateTime odt = OffsetDateTime.parse(startAt);
      return Date.from(odt.toInstant());
    }
    else if (jsonNode.isLong()) {
      long startAt = jsonNode.longValue();
      return new Date(startAt);
    }
    else {
      throw new UnsupportedEncodingException("Can't determine date format");
    }
  }
}
