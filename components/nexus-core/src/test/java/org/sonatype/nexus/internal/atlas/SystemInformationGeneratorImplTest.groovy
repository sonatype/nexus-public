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

import java.nio.file.FileStore
import java.nio.file.FileSystems

import org.sonatype.nexus.common.app.ApplicationDirectories
import org.sonatype.nexus.common.app.ApplicationLicense
import org.sonatype.nexus.common.app.ApplicationVersion
import org.sonatype.nexus.common.node.NodeAccess

import org.apache.karaf.bundle.core.BundleService
import org.osgi.framework.BundleContext
import spock.lang.Specification

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.text.IsEmptyString.isEmptyString
import static org.hamcrest.core.IsNot.not

/**
 * Unit tests for {@link SystemInformationGeneratorImpl}
 */
class SystemInformationGeneratorImplTest
    extends Specification
{
  def "reportFileStores runs successfully using FileSystem.default"() {
    given:
      def generator = mockSystemInformationGenerator()

    when:
      def data = FileSystems.default.fileStores.collectEntries {
        [ (it.name()): generator.reportFileStore(it) ]
      }

    then:
      data.keySet().size()
      data.each { k, v ->
        assertThat(k, not(isEmptyString()))
        assertThat(v.description, not(isEmptyString()))
        assertThat(v.type, not(isEmptyString()))
        assertThat(v.totalSpace, not(isEmptyString()))
        assertThat(v.usableSpace, not(isEmptyString()))
        assertThat(v.readOnly, not(isEmptyString()))
      }
  }

  def "reportFileStores handles IOException gracefully"() {
    given:
      def generator = mockSystemInformationGenerator()

    when:
      def fs = Mock(FileStore) {
        toString() >> "description"
        type() >> "brokenfstype"
        name() >> "brokenfsname"
        getTotalSpace() >> { throw new IOException("testing") }
      }
      def fsReport = generator.reportFileStore(fs)

    then:
      fsReport == SystemInformationGeneratorImpl.UNAVAILABLE
  }

  def "reportNetwork runs successfully using NetworkInterface.networkInterfaces"() {
    given:
      def generator = mockSystemInformationGenerator()

    when:
      def data = NetworkInterface.networkInterfaces.toList().collectEntries {
        [ (it.name): generator.reportNetworkInterface(it) ]
      }

    then:
      data.keySet().size()
      data.each { k, v ->
        assertThat(k, not(isEmptyString()))
        assertThat(v.displayName, not(isEmptyString()))
        assertThat(v.up, not(isEmptyString()))
        assertThat(v.virtual, not(isEmptyString()))
        assertThat(v.multicast, not(isEmptyString()))
        assertThat(v.loopback, not(isEmptyString()))
        assertThat(v.ptp, not(isEmptyString()))
        assertThat(v.mtu, not(isEmptyString()))
        assertThat(v.addresses, not(isEmptyString()))
      }
  }

  def "reportNetwork handles SocketException gracefully"() {
    given:
      def generator = mockSystemInformationGenerator()

    when:
      def intf = GroovyMock(NetworkInterface) {
        getDisplayName() >> "brokenintf"
        supportsMulticast() >> { throw new SocketException("testing") }
      }
      def report = generator.reportNetworkInterface(intf)

    then:
      report == SystemInformationGeneratorImpl.UNAVAILABLE
  }

  def mockSystemInformationGenerator() {
    return new SystemInformationGeneratorImpl(
        Mock(ApplicationDirectories.class),
        Mock(ApplicationVersion.class),
        Mock(ApplicationLicense.class),
        Collections.emptyMap(),
        Mock(BundleContext.class),
        Mock(BundleService.class),
        Mock(NodeAccess.class))
  }
}
