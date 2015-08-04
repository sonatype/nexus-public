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
package org.sonatype.nexus.client.internal.rest.jersey.subsystem.config;

import org.sonatype.nexus.client.core.subsystem.config.OptionalSegment;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;

public abstract class JerseyOptionalSegmentSupport<ME extends OptionalSegment, S>
    extends JerseySegmentSupport<ME, S>
    implements OptionalSegment<ME, S>
{

  public JerseyOptionalSegmentSupport(final JerseyNexusClient nexusClient) {
    super(nexusClient);
  }

  @Override
  public ME disable() {
    onDisable(settings());
    save();
    return me();
  }

  protected abstract void onDisable(final S settings);

}
