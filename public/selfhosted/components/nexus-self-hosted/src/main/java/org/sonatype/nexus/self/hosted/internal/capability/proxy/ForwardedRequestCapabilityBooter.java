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
package org.sonatype.nexus.self.hosted.internal.capability.proxy;

import org.sonatype.nexus.capability.CapabilityBooterSupport;
import org.sonatype.nexus.capability.CapabilityRegistry;

import org.springframework.stereotype.Component;

/**
 * Boots the {@link ForwardedRequestCapability}.
 *
 * <p>
 * This ensures the capability is automatically created on first startup
 * with default settings (enabled).
 * </p>
 */
@Component
public class ForwardedRequestCapabilityBooter
    extends CapabilityBooterSupport
{
  public ForwardedRequestCapabilityBooter() {
    super(ForwardedRequestCapabilityUpgradeStep_2_131.VERSION);
  }

  @Override
  protected void boot(final CapabilityRegistry registry) throws Exception {
    // Create the capability in enabled state by default
    // This ensures backwards compatibility - forwarded headers are processed by default
    maybeAddCapability(registry, ForwardedRequestCapability.TYPE, true, null, null);
  }
}
