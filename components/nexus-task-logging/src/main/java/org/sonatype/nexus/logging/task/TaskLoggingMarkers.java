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
package org.sonatype.nexus.logging.task;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * Container for Marker constants
 *
 * @since 3.5
 */
public class TaskLoggingMarkers
{
  /**
   * Slf4j {@link Marker} to indicate the log should ONLY be logged to the nexus.log
   */
  public static final Marker NEXUS_LOG_ONLY = MarkerFactory.getMarker("NEXUS_LOG_ONLY");

  /**
   * Slf4j {@link Marker} to indicate the log should ONLY be logged to the task log
   */
  public static final Marker TASK_LOG_ONLY = MarkerFactory.getMarker("TASK_LOG_ONLY");

  /**
   * Slf4j {@link Marker} to indicate the log should ONLY be logged to the cluster log
   */
  public static final Marker CLUSTER_LOG_ONLY = MarkerFactory.getMarker("CLUSTER_LOG_ONLY");

  /**
   * Slf4j {@link Marker} to indicate the log should be logged with the progress logic (nexus.log only gets an entry
   * every minute, every entry goes to task log)
   */
  public static final Marker PROGRESS = MarkerFactory.getMarker("PROGRESS");

  /**
   * FOR INTERNAL USE ONLY - DO NOT USE IN A TASK
   * Slf4j {@link Marker} used internally by the task logging progress mechanism.
   */
  public static final Marker INTERNAL_PROGRESS = MarkerFactory.getMarker("INTERNAL_PROGRESS");
}
