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
import org.sonatype.nexus.configuration.application.ApplicationConfiguration
import org.sonatype.nexus.configuration.model.io.xpp3.NexusConfigurationXpp3Reader
import org.sonatype.nexus.configuration.model.io.xpp3.NexusConfigurationXpp3Writer
import org.sonatype.sisu.goodies.common.ComponentSupport

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import static com.google.common.base.Preconditions.checkNotNull
import static org.sonatype.nexus.atlas.SupportBundle.ContentSource.Priority
import static org.sonatype.nexus.atlas.SupportBundle.ContentSource.Priority.HIGH
import static org.sonatype.nexus.atlas.SupportBundle.ContentSource.Priority.REQUIRED
import static org.sonatype.nexus.atlas.SupportBundle.ContentSource.Type.CONFIG

/**
 * Adds system configuration files to support bundle.
 *
 * @since 2.7
 */
@Named
@Singleton
class ConfigurationCustomizer
    extends ComponentSupport
    implements SupportBundleCustomizer
{
  private final ApplicationConfiguration applicationConfiguration

  @Inject
  ConfigurationCustomizer(final ApplicationConfiguration applicationConfiguration) {
    this.applicationConfiguration = checkNotNull(applicationConfiguration)
  }

  @Override
  void customize(final SupportBundle supportBundle) {
    // nexus.xml
    supportBundle << new NexusXmlContentSource()

    // helper to include a file
    def maybeIncludeFile = { File file, String prefix, Priority priority = null ->
      if (file.exists()) {
        log.debug 'Including file: {}', file
        supportBundle << new FileContentSourceSupport(CONFIG, "$prefix/${file.name}", file) {
          {
            if (priority) {
              this.priority = priority
            }
          }
        }
      }
      else {
        log.debug 'Skipping non-existent file: {}', file
      }
    }

    // include installation configuration files
    def installDir = applicationConfiguration.installDirectory
    if (installDir) {
      // include all jetty configuration files
      new File(installDir, 'conf').eachFileMatch(~'jetty.*xml') {
        maybeIncludeFile it, 'install/conf', HIGH
      }

      // core properties
      maybeIncludeFile new File(installDir, 'conf/nexus.properties'), 'install/conf', HIGH

      // jsw launcher configuration
      maybeIncludeFile new File(installDir, 'bin/jsw/conf/wrapper.conf'), 'install/bin/jsw/conf', HIGH

      // installer launcher configuration
      maybeIncludeFile new File(installDir, 'bin/nexus.vmoptions'), 'install/bin', HIGH
    }
  }

  /**
   * Source for obfuscated nexus.xml
   */
  private class NexusXmlContentSource
      extends GeneratedContentSourceSupport
  {
    NexusXmlContentSource() {
      super(CONFIG, 'work/conf/nexus.xml')
      this.priority = REQUIRED
    }

    @Override
    protected void generate(final File file) {
      def source = new File(applicationConfiguration.configurationDirectory, 'nexus.xml')
      if (!source.exists()) {
        log.debug 'Skipping non-existent file: {}', source
        return
      }

      log.debug 'Reading: {}', source
      source.withInputStream { input ->
        def model = new NexusConfigurationXpp3Reader().read(input)

        // obfuscate sensitive content
        model.smtpConfiguration?.password = PASSWORD_TOKEN
        model.remoteProxySettings?.httpProxySettings?.authentication?.password = PASSWORD_TOKEN
        model.remoteProxySettings?.httpsProxySettings?.authentication?.password = PASSWORD_TOKEN
        model.repositories?.each { repo ->
          repo.remoteStorage?.authentication?.password = PASSWORD_TOKEN
        }

        file.withOutputStream { output ->
          new NexusConfigurationXpp3Writer().write(output, model)
        }
      }
    }
  }
}