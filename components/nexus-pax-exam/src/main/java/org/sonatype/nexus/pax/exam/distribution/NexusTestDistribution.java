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

import org.sonatype.nexus.pax.exam.NexusPaxExamSupport;
import org.sonatype.nexus.pax.exam.TestDatabase;

import org.ops4j.pax.exam.Option;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFileExtend;
import static org.ops4j.pax.exam.options.WrappedUrlProvisionOption.OverwriteMode.MERGE;
import static org.sonatype.nexus.pax.exam.NexusPaxExamSupport.NEXUS_PROPERTIES_FILE;
import static org.sonatype.nexus.pax.exam.NexusPaxExamSupport.SYSTEM_PROPERTIES_FILE;
import static org.sonatype.nexus.pax.exam.NexusPaxExamSupport.javaVMCompositeOption;
import static org.sonatype.nexus.pax.exam.NexusPaxExamSupport.nexusFeature;

public interface NexusTestDistribution
{
  static final String GROOVY_GROUP_ID = "org.codehaus.groovy";

  String ORG_OW2_ASM_GROUP_ID = "org.ow2.asm";

  enum Distribution
  {
    BASE,
    OSS,
    PRO
  }

  int priority(TestDatabase database, Distribution dist);

  Option[] distribution(Distribution dist);

  default Option[] configureNexus(final Option distribution) {
    return NexusPaxExamSupport.options(
        distribution,

        editConfigurationFileExtend(SYSTEM_PROPERTIES_FILE, "nexus.security.randompassword", "false"),
        editConfigurationFileExtend(NEXUS_PROPERTIES_FILE, "nexus.scripts.allowCreation", "true"),
        editConfigurationFileExtend(NEXUS_PROPERTIES_FILE, "nexus.search.event.handler.flushOnCount", "1"),
        mavenBundle("org.apache.aries.spifly", "org.apache.aries.spifly.dynamic.bundle").versionAsInProject(),
        mavenBundle(ORG_OW2_ASM_GROUP_ID, "asm").versionAsInProject(),
        mavenBundle(ORG_OW2_ASM_GROUP_ID, "asm-commons").versionAsInProject(),
        mavenBundle(ORG_OW2_ASM_GROUP_ID, "asm-util").versionAsInProject(),
        mavenBundle(ORG_OW2_ASM_GROUP_ID, "asm-tree").versionAsInProject(),
        mavenBundle(ORG_OW2_ASM_GROUP_ID, "asm-analysis").versionAsInProject(),
        mavenBundle(GROOVY_GROUP_ID, "groovy").versionAsInProject(),
        mavenBundle(GROOVY_GROUP_ID, "groovy-ant").versionAsInProject(),
        mavenBundle(GROOVY_GROUP_ID, "groovy-json").versionAsInProject(),
        mavenBundle(GROOVY_GROUP_ID, "groovy-jsr223").versionAsInProject(),
        mavenBundle(GROOVY_GROUP_ID, "groovy-xml").versionAsInProject(),
        mavenBundle(GROOVY_GROUP_ID, "groovy-cli-commons").versionAsInProject(),
        // install common test-support features
        nexusFeature("org.sonatype.nexus.testsuite", "nexus-repository-testsupport"),
        wrappedBundle(maven("org.awaitility", "awaitility").versionAsInProject()).overwriteManifest(MERGE).imports("*"),
        javaVMCompositeOption());
  }

  static boolean isBase(final Distribution dist) {
    return Distribution.BASE.equals(dist);
  }

  static boolean isOss(final Distribution dist) {
    return Distribution.OSS.equals(dist);
  }

  static boolean isPro(final Distribution dist) {
    return Distribution.PRO.equals(dist);
  }
}
