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
package org.sonatype.nexus.rest.schedules;

import org.sonatype.nexus.rest.model.ScheduledServiceAdvancedResource;
import org.sonatype.nexus.rest.model.ScheduledServiceBaseResource;
import org.sonatype.nexus.rest.model.ScheduledServiceDailyResource;
import org.sonatype.nexus.rest.model.ScheduledServiceHourlyResource;
import org.sonatype.nexus.rest.model.ScheduledServiceMonthlyResource;
import org.sonatype.nexus.rest.model.ScheduledServiceOnceResource;
import org.sonatype.nexus.rest.model.ScheduledServiceWeeklyResource;
import org.sonatype.plexus.rest.xstream.LookAheadStreamReader;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.AbstractReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.mapper.Mapper;

public class ScheduledServiceBaseResourceConverter
    extends AbstractReflectionConverter
{
  /**
   * Schedule Type Off.
   */
  public static final String SCHEDULE_TYPE_MANUAL = "manual";

  /**
   * Schedule type run now
   */
  public static final String SCHEDULE_TYPE_RUN_NOW = "internal";

  /**
   * Schedule Type Once.
   */
  public static final String SCHEDULE_TYPE_ONCE = "once";

  /**
   * Schedule type Hourly
   */
  public static final String SCHEDULE_TYPE_HOURLY = "hourly";

  /**
   * Schedule Type Daily.
   */
  public static final String SCHEDULE_TYPE_DAILY = "daily";

  /**
   * Schedule Type Weekly.
   */
  public static final String SCHEDULE_TYPE_WEEKLY = "weekly";

  /**
   * Schedule Type Monthly.
   */
  public static final String SCHEDULE_TYPE_MONTHLY = "monthly";

  /**
   * Schedule Type Advanced.
   */
  public static final String SCHEDULE_TYPE_ADVANCED = "advanced";

  public ScheduledServiceBaseResourceConverter(Mapper mapper, ReflectionProvider reflectionProvider) {
    super(mapper, reflectionProvider);
  }

  public boolean canConvert(Class type) {
    return ScheduledServiceBaseResource.class.equals(type);
  }

  protected Object instantiateNewInstance(HierarchicalStreamReader reader, UnmarshallingContext context) {
    if (LookAheadStreamReader.class.isAssignableFrom(reader.getClass())
        || LookAheadStreamReader.class.isAssignableFrom(reader.underlyingReader().getClass())) {
      String schedule = null;

      if (LookAheadStreamReader.class.isAssignableFrom(reader.getClass())) {
        schedule = ((LookAheadStreamReader) reader).getFieldValue("schedule");
      }
      else {
        schedule = ((LookAheadStreamReader) reader.underlyingReader()).getFieldValue("schedule");
      }

      if (schedule == null) {
        return super.instantiateNewInstance(reader, context);
      }
      else if (SCHEDULE_TYPE_MANUAL.equals(schedule)) {
        return new ScheduledServiceBaseResource();
      }
      else if (SCHEDULE_TYPE_ONCE.equals(schedule)) {
        return new ScheduledServiceOnceResource();
      }
      else if (SCHEDULE_TYPE_HOURLY.equals(schedule)) {
        return new ScheduledServiceHourlyResource();
      }
      else if (SCHEDULE_TYPE_DAILY.equals(schedule)) {
        return new ScheduledServiceDailyResource();
      }
      else if (SCHEDULE_TYPE_WEEKLY.equals(schedule)) {
        return new ScheduledServiceWeeklyResource();
      }
      else if (SCHEDULE_TYPE_MONTHLY.equals(schedule)) {
        return new ScheduledServiceMonthlyResource();
      }
      else if (SCHEDULE_TYPE_ADVANCED.equals(schedule)) {
        return new ScheduledServiceAdvancedResource();
      }
      else {
        return new ScheduledServiceBaseResource();
      }
    }
    else {
      return super.instantiateNewInstance(reader, context);
    }
  }
}
