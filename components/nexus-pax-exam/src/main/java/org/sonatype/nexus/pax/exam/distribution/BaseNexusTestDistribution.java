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

import org.sonatype.nexus.pax.exam.TestDatabase;

import org.ops4j.pax.exam.Option;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFileExtend;
import static org.ops4j.pax.exam.options.WrappedUrlProvisionOption.OverwriteMode.MERGE;
import static org.sonatype.nexus.pax.exam.NexusPaxExamSupport.NEXUS_PROPERTIES_FILE;
import static org.sonatype.nexus.pax.exam.NexusPaxExamSupport.SYSTEM_PROPERTIES_FILE;
import static org.sonatype.nexus.pax.exam.NexusPaxExamSupport.nexusDistribution;
import static org.sonatype.nexus.pax.exam.NexusPaxExamSupport.nexusFeature;
import static org.sonatype.nexus.pax.exam.NexusPaxExamSupport.options;

/**
 * Configures Nexus Base
 *
 * NOTE: If renaming also update the source_bundle.groovy
 */
public class BaseNexusTestDistribution
    implements NexusTestDistribution
{
  @Override
  public int priority(final TestDatabase database, final Distribution dist) {
    switch (dist) {
      case BASE:
        return 1;
      default:
        return 0;
    }
  }

  @Override
  public Option[] distribution(final Distribution dist) {
    return options(
        nexusDistribution("org.sonatype.nexus.assemblies", "nexus-base-template"),

        editConfigurationFileExtend(SYSTEM_PROPERTIES_FILE, "nexus.loadAsOSS", "true"),
        editConfigurationFileExtend(SYSTEM_PROPERTIES_FILE, "nexus.security.randompassword", "false"),
        editConfigurationFileExtend(NEXUS_PROPERTIES_FILE, "nexus.scripts.allowCreation", "true"),
        editConfigurationFileExtend(NEXUS_PROPERTIES_FILE, "nexus.search.event.handler.flushOnCount", "1"),

        // Feature for NexusTestSystem
        editConfigurationFileExtend(NEXUS_PROPERTIES_FILE, "nexus.test.base", "true"),

        // install common test-support features
        nexusFeature("org.sonatype.nexus.testsuite", "nexus-repository-content-testsupport"),

        // install common test-support features
        nexusFeature("org.sonatype.nexus.testsuite", "nexus-repository-testsupport"),
        wrappedBundle(maven("org.awaitility", "awaitility").versionAsInProject()).overwriteManifest(MERGE)
            .imports("*"));
  }
}
