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
package org.sonatype.nexus.scheduling.spi;

import java.util.Optional;

import org.sonatype.nexus.scheduling.TaskInfo;

/**
 * Store for persisted Task state
 */
public interface TaskResultStateStore
{
  /**
   * Retrieve a state from the provided {@link TaskInfo}
   */
  Optional<TaskResultState> getState(TaskInfo taskInfo);

  void updateJobDataMap(TaskInfo taskInfo);

  default boolean isSupported() {
    return true;
  }
}
