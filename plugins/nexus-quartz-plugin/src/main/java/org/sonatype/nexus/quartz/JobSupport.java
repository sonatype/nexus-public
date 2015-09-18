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
package org.sonatype.nexus.quartz;

import org.sonatype.goodies.common.ComponentSupport;

import com.google.common.base.Throwables;
import org.quartz.InterruptableJob;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.MDC;

/**
 * Job support class. If possible, is advisable to implement {@link InterruptableJob} interface too. Implementations
 * must be SISU components as {@link QuartzSupport} will use SISU to create new job instances. They should be
 * non-singletons.
 *
 * @since 3.0
 */
public abstract class JobSupport
    extends ComponentSupport
    implements Job
{
  protected JobExecutionContext context;

  @Override
  public void execute(final JobExecutionContext context)
      throws JobExecutionException
  {
    MDC.put(JobSupport.class.getSimpleName(), getClass().getSimpleName());
    this.context = context;
    try {
      execute();
    }
    catch (Exception e) {
      Throwables.propagateIfInstanceOf(e, JobExecutionException.class);
      throw new JobExecutionException(e);
    }
    finally {
      this.context = null;
      MDC.remove(JobSupport.class.getSimpleName());
    }
  }

  // == Internal

  protected abstract void execute() throws Exception;
}