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
package org.sonatype.nexus.yum.internal.capabilities;

import javax.inject.Named;

import org.sonatype.nexus.plugins.capabilities.CapabilityRegistry;
import org.sonatype.nexus.plugins.capabilities.support.CapabilityBooterSupport;
import org.sonatype.nexus.yum.YumRegistry;

import org.eclipse.sisu.EagerSingleton;

/**
 * Automatically create Yum capability.
 *
 * @since yum 3.0
 */
@Named
@EagerSingleton
public class YumCapabilitiesBooter
    extends CapabilityBooterSupport
{

  @Override
  protected void boot(final CapabilityRegistry registry)
      throws Exception
  {
    maybeAddCapability(
        registry,
        YumCapabilityDescriptor.TYPE,
        true, // enabled
        null, // no notes
        new YumCapabilityConfiguration(YumRegistry.DEFAULT_MAX_NUMBER_PARALLEL_THREADS).asMap()
    );
  }

}
