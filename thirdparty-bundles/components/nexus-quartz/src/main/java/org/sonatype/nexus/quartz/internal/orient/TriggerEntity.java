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
package org.sonatype.nexus.quartz.internal.orient;

import javax.annotation.Nullable;

import org.quartz.JobKey;
import org.quartz.Trigger.TriggerState;
import org.quartz.TriggerKey;
import org.quartz.spi.OperableTrigger;

import static com.google.common.base.Preconditions.checkState;

/**
 * {@link OperableTrigger} entity.
 *
 * @since 3.0
 */
public class TriggerEntity
    extends MarshalledEntity<OperableTrigger>
{
  private String name;

  @Nullable
  private String group;

  private String jobName;

  @Nullable
  private String jobGroup;

  @Nullable
  private String calendarName;

  /**
   * Trigger entity state, which adds a few more bits to cope with trigger acquisition and blocked pausing states.
   */
  public enum State
  {
    /**
     * Maps to {@link TriggerState#NORMAL}.
     */
    WAITING,

    /**
     * @see JobStoreImpl#acquireNextTriggers
     */
    ACQUIRED,

    /**
     * Maps to {@link TriggerState#COMPLETE}.
     */
    COMPLETE,

    /**
     * Maps to {@link TriggerState#PAUSED}.
     */
    PAUSED,

    /**
     * Maps to {@link TriggerState#BLOCKED}.
     */
    BLOCKED,

    /**
     * Maps to {@link TriggerState#BLOCKED} when trigger is blocked and paused.
     */
    PAUSED_BLOCKED,

    /**
     * Maps to {@link TriggerState#ERROR}.
     */
    ERROR
  }

  private State state;

  public TriggerEntity() {
    // empty
  }

  public TriggerEntity(final OperableTrigger value, final State state) {
    setValue(value);
    setState(state);
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  @Nullable
  public String getGroup() {
    return group;
  }

  public void setGroup(@Nullable final String group) {
    this.group = group;
  }

  public String getJobName() {
    return jobName;
  }

  public void setJobName(final String jobName) {
    this.jobName = jobName;
  }

  @Nullable
  public String getJobGroup() {
    return jobGroup;
  }

  public void setJobGroup(@Nullable final String jobGroup) {
    this.jobGroup = jobGroup;
  }

  @Nullable
  public String getCalendarName() {
    return calendarName;
  }

  public void setCalendarName(@Nullable final String calendarName) {
    this.calendarName = calendarName;
  }

  public State getState() {
    return state;
  }

  public void setState(final State state) {
    this.state = state;
  }

  /**
   * Populate entity fields used for indexing.
   */
  @Override
  public void setValue(final OperableTrigger value) {
    super.setValue(value);

    TriggerKey key = value.getKey();
    checkState(key != null, "Missing key");
    setName(key.getName());
    setGroup(key.getGroup());

    JobKey jobKey = value.getJobKey();
    checkState(jobKey != null, "Missing job-key");
    setJobName(jobKey.getName());
    setJobGroup(jobKey.getGroup());

    setCalendarName(value.getCalendarName());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "name='" + name + '\'' +
        ", group='" + group + '\'' +
        ", jobName='" + jobName + '\'' +
        ", jobGroup='" + jobGroup + '\'' +
        ", calendarName='" + calendarName + '\'' +
        ", state=" + state +
        ", value=" + getValue() +
        '}';
  }
}
