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
package org.sonatype.nexus.logging;

import java.util.Collection;

import org.sonatype.nexus.logging.model.LevelXO;
import org.sonatype.nexus.logging.model.LoggerXO;

/**
 * Dynamic loggers configuration.
 *
 * @since 2.7
 */
public interface LoggingConfigurator
{

  /**
   * Returns the list of current configured loggers (never null).
   */
  Collection<LoggerXO> getLoggers();

  /**
   * Sets logging level for specified logger. If level is  equal to {@link LevelXO#DEFAULT} the level will be set to
   * effective level.
   *
   * @param name  of logger to set the level of
   * @param level to be set
   * @return passed in level or teh effective level if passed in level is equal to {@link LevelXO#DEFAULT}
   */
  LevelXO setLevel(String name, LevelXO level);

  /**
   * Removes specified logger.
   *
   * @param name of logger to be removed
   */
  void remove(String name);

  /**
   * Resets all loggers to their default levels.
   */
  void reset();

}
