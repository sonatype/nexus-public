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
package org.sonatype.nexus.internal.atlas.customizers

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import org.sonatype.goodies.common.ComponentSupport
import org.sonatype.nexus.common.app.ApplicationDirectories
import org.sonatype.nexus.supportzip.FileContentSourceSupport
import org.sonatype.nexus.supportzip.SupportBundle
import org.sonatype.nexus.supportzip.SupportBundleCustomizer

import static com.google.common.base.Preconditions.checkNotNull
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Priority
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Priority.DEFAULT
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Priority.HIGH
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type.CONFIG

/**
 * Adds installation directory configuration files to support bundle.
 *
 * @since 3.0
 */
@Named
@Singleton
class InstallConfigurationCustomizer
    extends ComponentSupport
    implements SupportBundleCustomizer
{
  private final ApplicationDirectories applicationDirectories

  @Inject
  InstallConfigurationCustomizer(final ApplicationDirectories applicationDirectories) {
    this.applicationDirectories = checkNotNull(applicationDirectories)
  }

  @Override
  void customize(final SupportBundle supportBundle) {
    // helper to include a file
    def maybeIncludeFile = { File file, String prefix, Priority priority = DEFAULT ->
      if (file.exists()) {
        log.debug 'Including file: {}', file
        supportBundle << new FileContentSourceSupport(CONFIG, "$prefix/${file.name}", file, priority)
      }
      else {
        log.debug 'Skipping non-existent file: {}', file
      }
    }

    def installDir = applicationDirectories.installDirectory
    if (installDir) {
      // FIXME: should handle keys.properties and users.properties separably as security config
      new File(installDir, 'etc').eachFile {
        maybeIncludeFile it, 'install/etc', HIGH
      }
    }
  }
}
