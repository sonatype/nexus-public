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
package org.sonatype.nexus.wonderland.rest;

import org.sonatype.nexus.util.SystemPropertiesHelper;
import org.sonatype.sisu.goodies.common.ComponentSupport;
import org.sonatype.sisu.siesta.common.Resource;

import com.google.common.annotations.VisibleForTesting;

/**
 * Support for wonderland resources.
 *
 * @since 2.14
 */
public abstract class WonderlandResourceSupport
    extends ComponentSupport
    implements Resource
{
  private boolean noPopUps = SystemPropertiesHelper.getBoolean("nexus.download.noPopUps", false);

  /**
   * Disable authTicket pop-ups?
   */
  protected boolean isNoPopUps() {
    return noPopUps;
  }

  @VisibleForTesting
  void setNoPopUps(final boolean noPopUps) {
    this.noPopUps = noPopUps;
  }
}
