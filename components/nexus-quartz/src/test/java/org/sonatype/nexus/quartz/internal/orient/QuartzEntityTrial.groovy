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
package org.sonatype.nexus.quartz.internal.orient

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.quartz.internal.task.QuartzTaskJob

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.junit.Before
import org.junit.Test
import org.quartz.JobDataMap
import org.quartz.impl.JobDetailImpl
import org.quartz.impl.calendar.CronCalendar
import org.quartz.impl.triggers.CronTriggerImpl

/**
 * Trial of various Quartz entity handling.
 */
class QuartzEntityTrial
    extends TestSupport
{
  private ObjectMapper mapper

  @Before
  void setUp() {
    mapper = new FieldObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT)
  }

  private static final TypeReference<Map<String, Object>> MAP_STRING_OBJECT =
      new TypeReference<Map<String, Object>>() {}

  // NOTE: testing what json marshaled bits for various quartz persistence objects look like

  void chew(Object record) {
    Map<String, Object> fields = mapper.convertValue(record, MAP_STRING_OBJECT)
    println fields
    println mapper.writeValueAsString(fields)
  }

  @Test
  void 'marshall JobDetailImpl'() {
    def record = new JobDetailImpl(
        name: 'test-name',
        group: 'test-group',
        description: 'test-description',
        durability: true,
        requestsRecovery: false,
        jobClass: QuartzTaskJob.class,
        jobDataMap: new JobDataMap([
            'foo': 1
        ])
    )
    chew(record)
  }

  @Test
  void 'marshall CronCalendar'() {
    def record = new CronCalendar("0 0 10 * * ?")
    chew(record)
  }

  @Test
  void 'marshall CronTriggerImpl'() {
    def record = new CronTriggerImpl("test-name", "test-group", "0 0 10 * * ?")
    chew(record)
  }
}
