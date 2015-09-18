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
package org.sonatype.nexus.coreui

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import org.sonatype.nexus.extdirect.DirectComponentSupport

import com.softwarementors.extjs.djn.config.annotations.DirectAction
import com.softwarementors.extjs.djn.config.annotations.DirectMethod
import org.apache.karaf.bundle.core.BundleService
import org.apache.shiro.authz.annotation.RequiresPermissions
import org.osgi.framework.BundleContext

/**
 * OSGI bundle component.
 *
 * @since 3.0
 */
@Named
@Singleton
@DirectAction(action = "coreui_Bundle")
class BundleComponent
    extends DirectComponentSupport
{
  @Inject
  BundleContext bundleContext

  @Inject
  BundleService bundleService

  @DirectMethod
  @RequiresPermissions('nexus:bundles:read')
  List<BundleXO> read() {
    def result = []
    bundleContext.bundles.each { bundle ->
      def info = bundleService.getInfo(bundle)
      def entry = new BundleXO(
          id: info.bundleId,
          state: info.state.name(),
          name: info.name,
          symbolicName: info.symbolicName,
          version: info.version,
          location: info.updateLocation,
          startLevel: info.startLevel,
          lastModified: bundle.lastModified,
          fragment: info.fragment,
          fragments: info.fragments.collect { it.bundleId },
          fragmentHosts: info.fragmentHosts.collect { it.bundleId },
      )

      // convert header dict
      entry.headers = [:]
      bundle.headers.with {
        keys().iterator().each { key ->
          def value = get(key)
          entry.headers[key] = value
        }
      }

      result << entry
    }
    return result
  }
}
