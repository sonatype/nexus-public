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

import org.sonatype.nexus.testsuite.testsupport.system.NexusTestSystem;
import org.sonatype.nexus.testsuite.testsupport.system.NexusTestSystemSupport;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFileExtend;
import static org.ops4j.pax.exam.options.WrappedUrlProvisionOption.OverwriteMode.MERGE;

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
    return options(
        nexusDistribution("org.sonatype.nexus.assemblies", "nexus-base-template"),

        editConfigurationFileExtend(SYSTEM_PROPERTIES_FILE, "nexus.loadAsOSS", "true"),
        editConfigurationFileExtend(SYSTEM_PROPERTIES_FILE, "nexus.security.randompassword", "false"),
        editConfigurationFileExtend(NEXUS_PROPERTIES_FILE, "nexus.scripts.allowCreation", "true"),
        editConfigurationFileExtend(NEXUS_PROPERTIES_FILE, "nexus.search.event.handler.flushOnCount", "1"),

        // Feature for NexusTestSystem
        editConfigurationFileExtend(NEXUS_PROPERTIES_FILE, "nexus.test.base", "true"),

        // install common test-support features
        nexusFeature("org.sonatype.nexus.testsuite", "nexus-repository-testsupport"),
        wrappedBundle(maven("org.awaitility", "awaitility").versionAsInProject()).overwriteManifest(MERGE)
            .imports("*"));
  }

  @Override
  protected NexusTestSystemSupport<?,?> nexusTestSystem() {
    return nexus;
  }
}
