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

import org.quartz.JobDetail;
import org.quartz.JobKey;

import static com.google.common.base.Preconditions.checkState;

/**
 * {@link JobDetail} entity.
 *
 * @since 3.0
 */
public class JobDetailEntity
    extends MarshalledEntity<JobDetail>
{
  private String name;

  @Nullable
  private String group;

  private String jobType;

  public JobDetailEntity() {
    // empty
  }

  public JobDetailEntity(final JobDetail value) {
    setValue(value);
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

  public String getJobType() {
    return jobType;
  }

  public void setJobType(final String jobType) {
    this.jobType = jobType;
  }

  /**
   * Populate entity fields used for indexing.
   */
  @Override
  public void setValue(final JobDetail value) {
    super.setValue(value);

    JobKey key = value.getKey();
    checkState(key != null, "Missing key");
    setName(key.getName());
    setGroup(key.getGroup());

    Class clazz = value.getJobClass();
    checkState(clazz != null, "Missing job-class");
    setJobType(clazz.getCanonicalName());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "name='" + name + '\'' +
        ", group='" + group + '\'' +
        ", jobType='" + jobType + '\'' +
        ", value=" + getValue() +
        '}';
  }
}
