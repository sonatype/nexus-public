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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.scheduling.schedule.Schedule;
import org.sonatype.nexus.scheduling.schedule.ScheduleDeserializer;
import org.sonatype.nexus.scheduling.schedule.ScheduleSerializer;
import org.sonatype.nexus.supportzip.ExportConfigData;
import org.sonatype.nexus.supportzip.ImportTaskData;
import org.sonatype.nexus.supportzip.datastore.JsonExporter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Write/Read {@link TaskInfo} data to/from a JSON file.
 *
 * @since 3.30
 */
@Named("taskExport")
@Singleton
public class TaskExport
    extends JsonExporter
    implements ExportConfigData, ImportTaskData
{
  private final TaskScheduler taskScheduler;

  @Inject
  public TaskExport(final TaskScheduler taskScheduler) {
    this.taskScheduler = taskScheduler;
  }

  @Override
  public void export(final File file) throws IOException {
    log.debug("Export Tasks data to {}", file);
    List<TaskInfo> tasks = taskScheduler.listsTasks();
    exportToJson(tasks, file);
  }

  @Override
  public void restore(final File file) throws IOException {
    log.debug("Restoring Tasks data from {}", file);

    // remove all tasks by default
    List<TaskInfo> defaultTasks = taskScheduler.listsTasks();
    defaultTasks.forEach(TaskInfo::remove);

    List<TaskInfoData> tasks = importFromJson(file, TaskInfoData.class);
    tasks.forEach(task -> taskScheduler.scheduleTask(task.getConfiguration(), task.getSchedule()));
  }

  // this class is used only for Serialization/Deserialization
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class TaskInfoData
      implements TaskInfo
  {
    private TaskConfiguration configuration;

    @JsonSerialize(using = ScheduleSerializer.class)
    @JsonDeserialize(using = ScheduleDeserializer.class)
    private Schedule schedule;

    private Object lastResult;

    private final Map<String, Object> context = new HashMap<>();

    @Override
    public String getId() {
      return configuration.getId();
    }

    @Override
    public String getName() {
      return configuration.getName();
    }

    @Override
    public String getTypeId() {
      return configuration.getTypeId();
    }

    @Override
    public String getMessage() {
      return configuration.getMessage();
    }

    @Override
    public TaskConfiguration getConfiguration() {
      return configuration;
    }

    public void setConfiguration(final TaskConfiguration configuration) {
      this.configuration = configuration;
    }

    @Override
    public Schedule getSchedule() {
      return schedule;
    }

    public void setSchedule(final Schedule schedule) {
      this.schedule = schedule;
    }

    @Override
    public CurrentState getCurrentState() {
      return new CurrentStateData();
    }

    @Override
    public Object getLastResult() {
      return lastResult;
    }

    @Override
    public LastRunState getLastRunState() {
      return configuration.getLastRunState();
    }

    @Override
    public boolean remove() {
      return true;
    }

    @Override
    public TaskInfo runNow(final String triggerSource) {
      return this;
    }

    @Override
    public String getTriggerSource() {
      return null;
    }

    @Override
    public Map<String, Object> getContext() {
      return context;
    }
  }

  // this class is used only for Serialization/Deserialization
  public static class CurrentStateData
      implements CurrentState
  {
    private TaskState state;

    private Date nextRun;

    @Override
    public TaskState getState() {
      return state;
    }

    @Override
    public Date getNextRun() {
      return nextRun;
    }

    @Override
    public Date getRunStarted() {
      return null;
    }

    @Override
    public TaskState getRunState() {
      return null;
    }

    @Override
    public Future<?> getFuture() {
      return null;
    }
  }
}
