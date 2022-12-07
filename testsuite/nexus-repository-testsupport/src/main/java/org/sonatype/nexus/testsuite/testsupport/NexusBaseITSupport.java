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
package org.sonatype.nexus.testsuite.testsupport;

import javax.inject.Inject;

import org.sonatype.nexus.pax.exam.distribution.NexusTestDistribution.Distribution;
import org.sonatype.nexus.pax.exam.distribution.NexusTestDistributionService;
import org.sonatype.nexus.testsuite.testsupport.system.NexusTestSystem;
import org.sonatype.nexus.testsuite.testsupport.system.NexusTestSystemSupport;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

/**
 * Base class for Nexus public distribution tests. Tests should be direct subclasses of this, shared code should be
 * extracted into shared helper classes.
 */
public abstract class NexusBaseITSupport
    extends ITSupport
{
  @Inject
  protected NexusTestSystem nexus;

  @Configuration
  public static Option[] configureNexus() {
    return configureNexusBase();
  }

  /**
   * Configure Nexus base with out-of-the box settings (no HTTPS).
   */
  public static Option[] configureNexusBase() {
    return NexusTestDistributionService.getInstance().getDistribution(Distribution.BASE);
  }

  @Override
  protected NexusTestSystemSupport<?,?> nexusTestSystem() {
    return nexus;
  }
}
