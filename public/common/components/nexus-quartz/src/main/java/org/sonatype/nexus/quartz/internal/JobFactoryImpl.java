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
package org.sonatype.nexus.quartz.internal;

import javax.annotation.Nullable;

import org.quartz.Job;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spring-aware {@link JobFactory}.
 *
 * @since 3.0
 */
@Component
public class JobFactoryImpl
    implements JobFactory, ApplicationContextAware
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private ApplicationContext applicationContext;

  @Override
  public Job newJob(final TriggerFiredBundle bundle, final Scheduler scheduler) throws SchedulerException {
    Class<? extends Job> type = bundle.getJobDetail().getJobClass();
    log.debug("New job: {}", type);

    ObjectProvider<? extends Job> beanEntry = locate(type);
    if (beanEntry == null) {
      throw new SchedulerException("Missing Job component for type: " + type.getName());
    }

    return beanEntry.getIfUnique(); // to support non-singletons
  }

  @Nullable
  private ObjectProvider<? extends Job> locate(final Class<? extends Job> jobType) {
    return applicationContext.getBeanProvider(jobType);
  }

  @Override
  public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }
}
