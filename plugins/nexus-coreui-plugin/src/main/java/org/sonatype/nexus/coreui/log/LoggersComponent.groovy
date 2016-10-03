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
package org.sonatype.nexus.coreui.log

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import javax.validation.Valid
import javax.validation.constraints.NotNull

import org.sonatype.nexus.common.log.LogManager
import org.sonatype.nexus.extdirect.DirectComponent
import org.sonatype.nexus.extdirect.DirectComponentSupport
import org.sonatype.nexus.validation.Validate

import com.codahale.metrics.annotation.ExceptionMetered
import com.codahale.metrics.annotation.Timed
import com.softwarementors.extjs.djn.config.annotations.DirectAction
import com.softwarementors.extjs.djn.config.annotations.DirectMethod
import groovy.transform.WithReadLock
import groovy.transform.WithWriteLock
import org.apache.shiro.authz.annotation.RequiresAuthentication
import org.apache.shiro.authz.annotation.RequiresPermissions
import org.hibernate.validator.constraints.NotEmpty

import static com.google.common.base.Preconditions.checkArgument

/**
 * Loggers {@link DirectComponent}.
 *
 * @since 3.0
 */
@Named
@Singleton
@DirectAction(action = "logging_Loggers")
class LoggersComponent
extends DirectComponentSupport
{

  private static final String ROOT = 'ROOT'

  @Inject
  LogManager logManager

  /**
   * Retrieves configured loggers.
   * @return a list of configured loggers
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @WithReadLock
  @RequiresPermissions('nexus:logging:read')
  List<LoggerXO> read() {
    return logManager.getLoggers().collect { key, value ->
      new LoggerXO(
          name: key,
          level: value
      )
    }
  }

  /**
   * Sets the level of a logger.
   * @param loggerXO logger name/level (cannot be null)
   * @return updated logger
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @WithWriteLock
  @RequiresPermissions('nexus:logging:update')
  @Validate
  LoggerXO update(final @NotNull @Valid LoggerXO loggerXO) {
    logManager.setLoggerLevel(loggerXO.name, loggerXO.level)
    return new LoggerXO(
        name: loggerXO.name,
        level: logManager.getLoggerEffectiveLevel(loggerXO.name)
    )
  }

  /**
   * Un-sets the level of a logger.
   * @param name logger name
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @WithWriteLock
  @RequiresAuthentication
  @RequiresPermissions('nexus:logging:update')
  @Validate
  void remove(final @NotEmpty String name) {
    checkArgument(name != ROOT, "$ROOT logger cannot be removed")
    logManager.unsetLoggerLevel(name)
  }

  /**
   * Resets all loggers to their default levels.
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @WithWriteLock
  @RequiresAuthentication
  @RequiresPermissions('nexus:logging:update')
  void reset() {
    logManager.resetLoggers()
  }


}
