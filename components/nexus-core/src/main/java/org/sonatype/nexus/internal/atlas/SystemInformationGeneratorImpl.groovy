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
package org.sonatype.nexus.internal.atlas

import java.nio.file.FileSystems

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import org.sonatype.goodies.common.ComponentSupport
import org.sonatype.goodies.common.Iso8601Date
import org.sonatype.nexus.common.app.ApplicationDirectories
import org.sonatype.nexus.common.app.ApplicationLicense
import org.sonatype.nexus.common.app.ApplicationVersion
import org.sonatype.nexus.common.atlas.SystemInformationGenerator
import org.sonatype.nexus.common.node.NodeAccess
import org.sonatype.nexus.common.text.Strings2

import org.apache.karaf.bundle.core.BundleService
import org.eclipse.sisu.Parameters
import org.osgi.framework.BundleContext

import static com.google.common.base.Preconditions.checkNotNull

/**
 * Default {@link SystemInformationGenerator}.
 *
 * @since 2.7
 */
@Named
@Singleton
class SystemInformationGeneratorImpl
    extends ComponentSupport
    implements SystemInformationGenerator
{
  private final ApplicationDirectories applicationDirectories

  private final ApplicationVersion applicationVersion

  private final ApplicationLicense applicationLicense

  private final Map<String, String> parameters

  private final BundleContext bundleContext

  private final BundleService bundleService

  private final NodeAccess nodeAccess

  @Inject
  SystemInformationGeneratorImpl(final ApplicationDirectories applicationDirectories,
                                 final ApplicationVersion applicationVersion,
                                 final ApplicationLicense applicationLicense,
                                 final @Parameters Map<String, String> parameters,
                                 final BundleContext bundleContext,
                                 final BundleService bundleService,
                                 final NodeAccess nodeAccess)
  {
    this.applicationDirectories = checkNotNull(applicationDirectories)
    this.applicationVersion = checkNotNull(applicationVersion)
    this.applicationLicense = checkNotNull(applicationLicense)
    this.parameters = checkNotNull(parameters)
    this.bundleContext = checkNotNull(bundleContext)
    this.bundleService = checkNotNull(bundleService)
    this.nodeAccess = checkNotNull(nodeAccess)
  }

  @Override
  Map report() {
    log.info 'Generating system information report'

    // HACK: provide local references to prevent problems with Groovy BUG accessing private fields
    def applicationDirectories = this.applicationDirectories
    def applicationVersion = this.applicationVersion
    def applicationLicense = this.applicationLicense
    def parameters = this.parameters
    def bundleContext = this.bundleContext
    def bundleService = this.bundleService
    def nodeAccess = this.nodeAccess

    def fileref = {File file ->
      if (file) {
        return file.canonicalPath
      }
      return null
    }

    def reportTime = {
      def now = new Date()
      return [
          'timezone': TimeZone.default.ID,
          'current' : now.time,
          'iso8601' : Iso8601Date.format(now)
      ]
    }

    def reportRuntime = {
      def runtime = Runtime.runtime

      return [
          'availableProcessors': runtime.availableProcessors(),
          'freeMemory'         : runtime.freeMemory(),
          'totalMemory'        : runtime.totalMemory(),
          'maxMemory'          : runtime.maxMemory(),
          'threads'            : Thread.activeCount()
      ]
    }

    def reportFileStores = {
      def data = [:]
      def fs = FileSystems.default
      fs.fileStores.each {store ->
        data[store.name()] = [
            'description'     : store.toString(), // seems to be the only place where mount-point is exposed
            'type'            : store.type(),
            'totalSpace'      : store.totalSpace,
            'usableSpace'     : store.usableSpace,
            'unallocatedSpace': store.unallocatedSpace,
            'readOnly'        : store.readOnly
        ]
      }

      return data
    }

    def reportNetwork = {
      def data = [:]
      NetworkInterface.networkInterfaces.each {intf ->
        data[intf.name] = [
            'displayName': intf.displayName,
            'up'         : intf.up,
            'virtual'    : intf.virtual,
            'multicast'  : intf.supportsMulticast(),
            'loopback'   : intf.loopback,
            'ptp'        : intf.pointToPoint,
            'mtu'        : intf.MTU,
            'addresses'  : intf.inetAddresses.collect {addr ->
              addr.toString()
            }.join(',')
        ]
      }
      return data
    }

    def reportNexusStatus = {
      def data = [
          'version': applicationVersion.version,
          'edition': applicationVersion.edition,
          'buildRevision': applicationVersion.buildRevision,
          'buildTimestamp': applicationVersion.buildTimestamp
      ]

      return data
    }

    def reportNexusNode = {
      def data = [
          'node-id': nodeAccess.id
      ]

      return data
    }

    def reportNexusLicense = {
      def data = [
          'licenseRequired': applicationLicense.required,
          'licenseValid': applicationLicense.valid,
          'licenseInstalled': applicationLicense.installed
      ]

      if (applicationLicense.installed) {
        data += [
            'licenseExpired': applicationLicense.expired,
            'licenseFingerprint': applicationLicense.fingerprint,
        ]

        applicationLicense.attributes.each { key, value ->
          data[key] = value
        }
      }

      return data
    }

    def reportNexusConfiguration = {
      return [
          'installDirectory'  : fileref(applicationDirectories.installDirectory),
          'workingDirectory'  : fileref(applicationDirectories.workDirectory),
          'temporaryDirectory': fileref(applicationDirectories.temporaryDirectory)
      ]
    }

    def reportNexusBundles = {
      def data = [:]
      bundleContext.bundles.each {bundle ->
        def info = bundleService.getInfo(bundle)
        data[info.bundleId] = [
            'bundleId'    : info.bundleId,
            'name'        : info.name,
            'symbolicName': info.symbolicName,
            'location'    : info.updateLocation,
            'version'     : info.version,
            'state'       : info.state.name(),
            'startLevel'  : info.startLevel,
            'fragment'    : info.fragment
        ]
      }
      return data
    }

    // masks the value of any properties that look like passwords
    def reportObfuscatedProperties = {properties ->
      return properties.collectEntries {key, value ->
        if (key.toLowerCase(Locale.US).contains('password')) {
          value = Strings2.mask(value)
        }
        return [key, value]
      }.sort()
    }

    def sections = [
        'system-time'        : reportTime(),
        'system-properties'  : reportObfuscatedProperties(System.properties),
        'system-environment' : System.getenv().sort(),
        'system-runtime'     : reportRuntime(),
        'system-network'     : reportNetwork(),
        'system-filestores'  : reportFileStores(),
        'nexus-status'       : reportNexusStatus(),
        'nexus-node'         : reportNexusNode(),
        'nexus-license'      : reportNexusLicense(),
        'nexus-properties'   : reportObfuscatedProperties(parameters),
        'nexus-configuration': reportNexusConfiguration(),
        'nexus-bundles'      : reportNexusBundles()
    ]

    return sections
  }
}
