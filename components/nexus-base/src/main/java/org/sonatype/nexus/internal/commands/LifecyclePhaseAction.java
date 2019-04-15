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
package org.sonatype.nexus.internal.commands;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.app.ManagedLifecycle.Phase;
import org.sonatype.nexus.common.app.ManagedLifecycleManager;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Command to manage the Nexus application lifecycle.
 *
 * @since 3.16
 */
@Named
@Command(name = "lifecyclePhase", scope = "nexus", description = "Move the Nexus application lifecycle to the given phase")
public class LifecyclePhaseAction
    implements Action
{
  private final ManagedLifecycleManager lifecycleManager;

  @Argument(description = "The phase to move to")
  Phase phase;

  @Inject
  public LifecyclePhaseAction(final ManagedLifecycleManager lifecycleManager) {
    this.lifecycleManager = checkNotNull(lifecycleManager);
  }

  @Override
  public Object execute() throws Exception {
    if (phase != null) {
      lifecycleManager.to(phase);
    }
    return lifecycleManager.getCurrentPhase().name();
  }
}
