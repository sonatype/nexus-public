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
package org.sonatype.nexus.internal.commands

import javax.inject.Inject
import javax.inject.Named

import org.sonatype.nexus.common.app.ApplicationLicense
import org.sonatype.nexus.common.app.ApplicationVersion

import org.apache.karaf.shell.api.action.Action
import org.apache.karaf.shell.api.action.Command

import static org.apache.karaf.shell.support.ansi.SimpleAnsi.INTENSITY_BOLD
import static org.apache.karaf.shell.support.ansi.SimpleAnsi.INTENSITY_NORMAL

/**
 * Display Nexus system information.
 *
 * @since 3.0
 */
@Named
@Command(name = 'info', scope = 'nexus', description = 'Nexus system information')
class InfoAction
    implements Action
{
  @Inject
  ApplicationVersion applicationVersion

  @Inject
  ApplicationLicense applicationLicense

  @Override
  public Object execute() throws Exception {
    def section = { String name ->
      println INTENSITY_BOLD + name + INTENSITY_NORMAL
    }
    def entry = { String key, Object value ->
      println "  $key: $value"
    }
    def sep = {
      println()
    }

    section 'Application'
    entry 'Version', applicationVersion.version
    entry 'Edition', applicationVersion.edition
    sep()

    // build information
    section 'Build'
    entry 'Revision', applicationVersion.buildRevision
    entry 'Timestamp', applicationVersion.buildTimestamp
    sep()

    // license information
    section 'License'
    entry 'Valid', applicationLicense.valid
    entry 'Required', applicationLicense.required
    entry 'Installed', applicationLicense.installed
    entry 'Expired', applicationLicense.expired
    entry 'Fingerprint', applicationLicense.fingerprint

    if (!applicationLicense.attributes.isEmpty()) {
      sep()
      section 'License Attributes'
      applicationLicense.attributes.each { key, value ->
        entry key, value
      }
    }

    return null
  }
}
