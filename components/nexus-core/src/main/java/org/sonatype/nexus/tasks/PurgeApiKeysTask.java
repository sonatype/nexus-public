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
package org.sonatype.nexus.tasks;

import javax.inject.Named;

import org.sonatype.nexus.scheduling.AbstractNexusTask;
import org.sonatype.nexus.tasks.descriptors.PurgeApiKeysTaskDescriptor;
import org.sonatype.security.events.AuthorizationConfigurationChanged;

@Named(PurgeApiKeysTaskDescriptor.ID)
public class PurgeApiKeysTask
    extends AbstractNexusTask<Void>
{
  /**
   * System event action: purge API keys
   */
  public static final String ACTION = "PURGE_API_KEYS";

  @Override
  protected Void doRun() {
    // triggers the expiry of any orphaned cached user principals
    notifyEventListeners(new AuthorizationConfigurationChanged());
    return null;
  }

  @Override
  protected String getAction() {
    return ACTION;
  }

  @Override
  protected String getMessage() {
    return "Purging Orphaned API Keys.";
  }

}
