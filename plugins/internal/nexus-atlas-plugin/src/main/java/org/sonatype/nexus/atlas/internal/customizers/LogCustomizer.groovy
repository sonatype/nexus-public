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

import org.sonatype.nexus.atlas.FileContentSourceSupport
import org.sonatype.nexus.atlas.GeneratedContentSourceSupport
import org.sonatype.nexus.atlas.SupportBundle
import org.sonatype.nexus.atlas.SupportBundleCustomizer
import org.sonatype.nexus.configuration.application.ApplicationDirectories
import org.sonatype.nexus.log.LogManager
import org.sonatype.sisu.goodies.common.ComponentSupport

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import static com.google.common.base.Preconditions.checkNotNull
import static groovy.io.FileType.FILES
import static org.sonatype.nexus.atlas.SupportBundle.ContentSource.Priority.LOW
import static org.sonatype.nexus.atlas.SupportBundle.ContentSource.Type.CONFIG
import static org.sonatype.nexus.atlas.SupportBundle.ContentSource.Type.LOG

/**
 * Adds nexus.log and logging configuration files to support bundle.
 *
 * @since 2.7
 */
@Named
@Singleton
class LogCustomizer
    extends ComponentSupport
    implements SupportBundleCustomizer
{
  private final LogManager logManager

  private final ApplicationDirectories applicationDirectories

  @Inject
  LogCustomizer(final LogManager logManager,
                final ApplicationDirectories applicationDirectories)
  {
    this.logManager = checkNotNull(logManager)
    this.applicationDirectories = checkNotNull(applicationDirectories)
  }

  @Override
  void customize(final SupportBundle supportBundle) {
    // add source for nexus.log
    supportBundle << new GeneratedContentSourceSupport(LOG, 'nexus.log') {
      {
        this.priority = LOW
      }

      @Override
      protected void generate(final File file) {
        def log = logManager.getApplicationLogAsStream('nexus.log', 0, Long.MAX_VALUE)
        log.inputStream.withStream { input ->
          file.withOutputStream { output ->
            output << input
          }
        }
      }
    }

    // helper to include a file
    def maybeIncludeFile = { SupportBundle.ContentSource.Type type, File file, String prefix ->
      if (file.exists()) {
        log.debug 'Including file: {}', file
        supportBundle << new FileContentSourceSupport(type, "$prefix/${file.name}", file)
      }
      else {
        log.debug 'Skipping non-existent file: {}', file
      }
    }

    // include request.log
    maybeIncludeFile LOG, new File(applicationDirectories.workDirectory, 'logs/request.log'), 'work/logs'

    // include installation configuration
    def installDir = applicationDirectories.installDirectory
    if (installDir) {
      // could be null
      maybeIncludeFile CONFIG, new File(installDir, 'conf/logback.xml'), 'install/conf'
    }

    // include runtime configuration
    def configDir = applicationDirectories.getWorkDirectory('conf')
    assert configDir.exists()
    configDir.eachFileMatch FILES, ~/logback.*/, {
      maybeIncludeFile CONFIG, it, 'work/conf'
    }
  }
}