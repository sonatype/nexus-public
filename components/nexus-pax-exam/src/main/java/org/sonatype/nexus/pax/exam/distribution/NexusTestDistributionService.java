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
package org.sonatype.nexus.pax.exam.distribution;

import java.util.ServiceLoader;

import org.sonatype.nexus.pax.exam.NexusPaxExamSupport;
import org.sonatype.nexus.pax.exam.TestDatabase;
import org.sonatype.nexus.pax.exam.distribution.NexusTestDistribution.Distribution;

import org.ops4j.pax.exam.Option;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Service which uses Java SPI to choose a distribution to use for an integration based on the distribution and database
 */
public class NexusTestDistributionService
{
  private static NexusTestDistributionService instance;

  private ServiceLoader<NexusTestDistribution> loader;

  private NexusTestDistributionService() {
    loader = ServiceLoader.load(NexusTestDistribution.class);
  }

  public static NexusTestDistributionService getInstance() {
    if (instance == null) {
      instance = new NexusTestDistributionService();
    }
    return instance;
  }

  /**
   * Retrieve the most appropriate distribution for running the test for the specified database & distribution
   */
  public Option[] getDistribution(final Distribution dist) {
    TestDatabase database = NexusPaxExamSupport.getValidTestDatabase();
    NexusTestDistribution preferred = null;


    for (NexusTestDistribution candidate : loader) {
      if (preferred == null ||
          preferred.priority(database, dist) < candidate.priority(database, dist)) {
        preferred = candidate;
      }
    }
    return checkNotNull(preferred, "Failed to find any services").distribution(dist);
  }
}
