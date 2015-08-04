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
package org.sonatype.nexus.atlas.internal.customizers

import groovy.json.JsonOutput
import org.sonatype.nexus.atlas.GeneratedContentSourceSupport
import org.sonatype.nexus.atlas.SupportBundle
import org.sonatype.nexus.atlas.SupportBundleCustomizer
import org.sonatype.nexus.atlas.SystemInformationGenerator
import org.sonatype.sisu.goodies.common.ComponentSupport

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import static com.google.common.base.Preconditions.checkNotNull
import static org.sonatype.nexus.atlas.SupportBundle.ContentSource.Priority.REQUIRED
import static org.sonatype.nexus.atlas.SupportBundle.ContentSource.Type.SYSINFO

/**
 * Adds system information report to support bundle.
 *
 * @since 2.7
 */
@Named
@Singleton
class SystemInformationCustomizer
    extends ComponentSupport
    implements SupportBundleCustomizer
{
  private final SystemInformationGenerator systemInformationGenerator

  @Inject
  SystemInformationCustomizer(final SystemInformationGenerator systemInformationGenerator) {
    this.systemInformationGenerator = checkNotNull(systemInformationGenerator)
  }

  @Override
  void customize(final SupportBundle supportBundle) {
    supportBundle << new GeneratedContentSourceSupport(SYSINFO, 'sysinfo.json') {
      {
        this.priority = REQUIRED
      }

      @Override
      protected void generate(final File file) {
        def report = systemInformationGenerator.report()
        JsonOutput.with {
          file.text = prettyPrint(toJson(report))
        }
      }
    }
  }
}