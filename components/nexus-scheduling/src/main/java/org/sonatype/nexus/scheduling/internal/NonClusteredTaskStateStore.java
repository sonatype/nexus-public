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
package org.sonatype.nexus.scheduling.internal;

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.scheduling.ClusteredTaskState;
import org.sonatype.nexus.scheduling.ClusteredTaskStateStore;
import org.sonatype.nexus.scheduling.TaskInfo;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of {@link ClusteredTaskStateStore} for non-clustered environments.
 * 
 * @since 3.1
 */
@Named
@Singleton
public class NonClusteredTaskStateStore
    implements ClusteredTaskStateStore
{
  @Override
  public void setLocalState(TaskInfo taskInfo) {
    checkNotNull(taskInfo);
  }

  @Override
  public void removeClusteredState(String taskId) {
    checkNotNull(taskId);
  }

  @Override
  public List<ClusteredTaskState> getClusteredState(String taskId) {
    checkNotNull(taskId);
    return null; // NOSONAR
  }
}
