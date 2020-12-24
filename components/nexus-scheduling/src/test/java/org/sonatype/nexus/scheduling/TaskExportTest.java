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
package org.sonatype.nexus.scheduling;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.sonatype.nexus.scheduling.TaskExport.TaskInfoData;
import org.sonatype.nexus.scheduling.schedule.Daily;
import org.sonatype.nexus.scheduling.schedule.Schedule;
import org.sonatype.nexus.supportzip.datastore.JsonExporter;

import com.google.common.collect.ImmutableList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests validity of Serialization/Deserialization {@link TaskInfo} by {@link TaskExport}
 */
public class TaskExportTest
{
  private final JsonExporter jsonExporter = new JsonExporter();

  private File jsonFile;

  @Before
  public void setup() throws IOException {
    jsonFile = File.createTempFile("TaskInfo", ".json");
  }

  @After
  public void tearDown() {
    jsonFile.delete();
  }

  @Test
  public void testExportImportToJson() throws Exception {
    Date date = new Date();
    Schedule daily = new Daily(date);
    List<TaskInfo> tasks = ImmutableList.of(
        createTask("task1", daily),
        createTask("task2", daily));

    TaskScheduler taskScheduler = mock(TaskScheduler.class);
    when(taskScheduler.listsTasks()).thenReturn(tasks);

    TaskExport exporter = new TaskExport(taskScheduler);
    exporter.export(jsonFile);
    List<TaskInfoData> importedData = jsonExporter.importFromJson(jsonFile, TaskInfoData.class);

    assertThat(importedData.size(), is(2));
    for (TaskInfoData importedTaskInfo : importedData) {
      TaskConfiguration configuration = importedTaskInfo.getConfiguration();
      assertThat(configuration.getName(), anyOf(is("task1"), is("task2")));
      assertThat(configuration.getAlertEmail(), is("none@example.com"));
      assertThat(configuration.getMessage(), is("Task message"));
      assertThat(configuration.getTypeId(), is("typeId"));
      assertThat(configuration.getTypeName(), is("Task type"));
      assertThat(configuration.getString("repositoryName"), is("test-repo"));

      assertThat(importedTaskInfo.getSchedule(), instanceOf(Daily.class));
      Daily deserializedDaily = (Daily) importedTaskInfo.getSchedule();
      assertThat(deserializedDaily.getStartAt(), is(date));
    }
  }

  private TaskInfo createTask(final String name, final Schedule schedule) {
    TaskInfoData taskInfo = new TaskInfoData();
    taskInfo.setConfiguration(createTaskConfiguration(name));
    taskInfo.setSchedule(schedule);
    return taskInfo;
  }

  private TaskConfiguration createTaskConfiguration(final String name) {
    TaskConfiguration taskConfiguration = new TaskConfiguration();
    taskConfiguration.setId(UUID.randomUUID().toString());
    taskConfiguration.setName(name);
    taskConfiguration.setAlertEmail("none@example.com");
    taskConfiguration.setMessage("Task message");
    taskConfiguration.setTypeId("typeId");
    taskConfiguration.setTypeName("Task type");
    taskConfiguration.setCreated(new Date());
    taskConfiguration.setUpdated(new Date());
    taskConfiguration.setString("repositoryName", "test-repo");

    return taskConfiguration;
  }
}

