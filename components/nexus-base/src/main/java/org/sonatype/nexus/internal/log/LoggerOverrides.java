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
package org.sonatype.nexus.internal.log;

import java.util.Map;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.log.LoggerLevel;

/**
 * Manages user-configured logger overrides.
 *
 * @since 3.0
 */
public interface LoggerOverrides
    extends Iterable<Map.Entry<String, LoggerLevel>>
{
  /**
   * Load overrides configuration from storage.
   */
  void load();

  /**
   * Save current overrides configuration to storage.
   */
  void save();

  /**
   * Update local logger overrides using datastore and return 'logger name' to 'log level' map
   */
  Map<String, LoggerLevel> syncWithDBAndGet();

  /**
   * Reset overrides configuration to default state.
   */
  void reset();

  /**
   * Set a logger level override.
   */
  void set(String name, LoggerLevel level);

  /**
   * Get the level of an overridden logger.
   */
  @Nullable
  LoggerLevel get(final String name);

  /**
   * Remove override configuration for a logger.
   */
  @Nullable
  LoggerLevel remove(final String name);

  /**
   * Check if a logger has been overridden.
   */
  boolean contains(final String name);
}
