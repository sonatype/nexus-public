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
package org.sonatype.nexus.coreui

import javax.validation.constraints.Future
import javax.validation.constraints.NotNull
import javax.validation.constraints.Pattern

import org.sonatype.nexus.validation.group.Create
import org.sonatype.nexus.validation.group.Update

import groovy.transform.ToString
import org.hibernate.validator.constraints.NotBlank

/**
 * Task exchange object.
 *
 * @since 3.0
 */
@ToString(includePackage = false, includeNames = true)
class TaskXO
{
  @NotBlank(groups = [Update, Schedule])
  String id

  @NotNull
  Boolean enabled

  @NotBlank(groups = [Create, Update])
  String name

  @NotBlank(groups = Create)
  String typeId

  String typeName
  String status
  String statusDescription

  Date nextRun
  Date lastRun
  String lastRunResult
  Boolean runnable
  Boolean stoppable

  String alertEmail

  Map<String, String> properties

  @NotBlank(groups = Create)
  String schedule

  @NotNull(groups = OnceToMonthlySchedule)
  @Future(groups = OnceSchedule)
  Date startDate
  Integer[] recurringDays

  @NotBlank(groups = AdvancedSchedule)
  @Pattern(
      regexp = '^([[0-9]|\\-|,|\\*]+) ([[0-9]|\\-|,|\\*]+) ([[0-9]|\\-|,|\\*]+) (\\?|\\*|[[0-9]|\\-|,|/|L|W]+) (\\*|[JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC|[0-9]|\\-|,]+) (\\*|\\?|[MON|TUE|WED|THU|FRI|SAT|SUN|[0-9]|\\-|,|/|L|W]+)( [[0-9]{4}|,]+)?$',
      message = 'Invalid CRON expression'
  )
  String cronExpression

  public interface Schedule
  {
  }

  public interface AdvancedSchedule
  {}

  public interface OnceSchedule
  {}

  public interface OnceToMonthlySchedule
  {}
}
