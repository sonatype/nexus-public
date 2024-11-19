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
package com.sonatype.nexus.pax.exam.distribution;

import org.sonatype.nexus.pax.exam.NexusPaxExamSupport;
import org.sonatype.nexus.pax.exam.TestDatabase;
import org.sonatype.nexus.pax.exam.distribution.NexusTestDistribution;

import org.ops4j.pax.exam.Option;

import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFileExtend;
import static org.sonatype.nexus.pax.exam.NexusPaxExamSupport.NEXUS_PROPERTIES_FILE;
import static org.sonatype.nexus.pax.exam.NexusPaxExamSupport.SYSTEM_PROPERTIES_FILE;
import static org.sonatype.nexus.pax.exam.NexusPaxExamSupport.nexusDistribution;
import static org.sonatype.nexus.pax.exam.NexusPaxExamSupport.nexusFeature;
import static org.sonatype.nexus.pax.exam.distribution.NexusTestDistribution.isBase;
import static org.sonatype.nexus.pax.exam.distribution.NexusTestDistribution.isOss;
import static org.sonatype.nexus.pax.exam.distribution.NexusTestDistribution.isPro;

/**
 * Configures Nexus Pro Starter.
 *
 * NOTE: If renaming also update the source_bundle.groovy
 */
public class ProStarterNexusTestDistribution
    implements NexusTestDistribution
{
  @Override
  public int priority(final TestDatabase database, final Distribution dist) {
    switch (dist) {
      case PRO_STARTER:
        return 1;
      default:
        return 0;
    }
  }

  @Override
  public Option[] distribution(final Distribution dist) {
    return NexusPaxExamSupport.options(
        configureNexus(nexusDistribution("com.sonatype.nexus", "nexus-pro-starter", "bundle")),

        editConfigurationFileExtend(SYSTEM_PROPERTIES_FILE, "nexus.loadAsProStarter", "true"),

        // This is used to indicate which nexus test system should be used
        when(isBase(dist))
            .useOptions(editConfigurationFileExtend(NEXUS_PROPERTIES_FILE, "nexus.test.base", "true")),
        when(isOss(dist))
            .useOptions(editConfigurationFileExtend(NEXUS_PROPERTIES_FILE, "nexus.test.internal", "true")),
        when(isPro(dist))
            .useOptions(editConfigurationFileExtend(NEXUS_PROPERTIES_FILE, "nexus.test.pro", "true")),

        // install common test-support features
        nexusFeature("com.sonatype.nexus.testsuite", "nexus-ldap-testsupport"),
        nexusFeature("org.sonatype.nexus.testsuite", "nexus-repository-content-testsupport"),

        // Choose the appropriate repository testsupport
        when(isBase(dist))
            .useOptions(nexusFeature("org.sonatype.nexus.testsuite", "nexus-repository-testsupport")),
        when(!isBase(dist))
            .useOptions(nexusFeature("com.sonatype.nexus.testsuite", "nexus-repository-testsupport-internal"))
    );
  }
}
