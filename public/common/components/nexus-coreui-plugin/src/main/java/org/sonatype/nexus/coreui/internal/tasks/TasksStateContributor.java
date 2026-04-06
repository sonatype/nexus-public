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
package org.sonatype.nexus.coreui.internal.tasks;

import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.rapture.StateContributor;

import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Singleton
public class TasksStateContributor
    extends ComponentSupport
    implements StateContributor
{
  private final Map<String, Object> state;

  @Inject
  public TasksStateContributor(
      @Value("${nexus.react.tasks:false}") final Boolean featureFlag)
  {
    state = ImmutableMap.of("nexus.react.tasks", featureFlag);
  }

  @Override
  public Map<String, Object> getState() {
    return state;
  }
}
