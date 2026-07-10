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
package org.sonatype.nexus.scheduling.constraints;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.ConstraintValidatorContext;

import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.schedule.ScheduleFactory;
import org.sonatype.nexus.validation.ConstraintValidatorSupport;

import org.springframework.context.annotation.Lazy;
import static com.google.common.base.Preconditions.checkNotNull;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * {@link CronExpression} validator.
 *
 * @see ScheduleFactory#cron(Date, String)
 * @since 3.0
 */
@Lazy
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CronExpressionValidator
    extends ConstraintValidatorSupport<CronExpression, String>
{
  private final ScheduleFactory scheduleFactory;

  @Autowired
  public CronExpressionValidator(final TaskScheduler taskScheduler) {
    checkNotNull(taskScheduler);
    this.scheduleFactory = taskScheduler.getScheduleFactory();
  }

  @Override
  public void initialize(final CronExpression annotation) {
    // nop
  }

  @Override
  public boolean isValid(final String value, final ConstraintValidatorContext context) {
    try {
      scheduleFactory.cron(new Date(), value);
    }
    catch (IllegalArgumentException e) {
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate(getEscapeHelper().stripJavaEl(e.getMessage()))
          .addConstraintViolation();
      return false;
    }
    return true;
  }
}
