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
import org.sonatype.nexus.supportzip.SanitizedXmlSourceSupport
import org.sonatype.nexus.supportzip.SupportBundle
import org.sonatype.nexus.supportzip.SupportBundleCustomizer
import org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type

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
      if (file.isFile()) {
        log.debug 'Including file: {}', file
        if (file.name == 'jetty-https.xml') {
          supportBundle << new SanitizedJettyFileSource(CONFIG, "$prefix/${file.name}", file, priority)
        }
        else if (file.name == 'hazelcast.xml' || file.name == 'hazelcast-network.xml') {
          supportBundle << new SanitizedHazelcastFileSource(CONFIG, "$prefix/${file.name}", file, priority)
        }
        else {
          supportBundle << new FileContentSourceSupport(CONFIG, "$prefix/${file.name}", file, priority)
        }
      }
      else {
        log.warn 'Skipping: {}', file
      }
    }

    // helper to include a directory
    def maybeIncludeDir = { File dir, String prefix, Priority priority = DEFAULT ->
      if (dir.isDirectory()) {
        log.debug 'Including dir: {}', dir
        dir.eachFile {
          maybeIncludeFile it, "$prefix/${dir.name}", priority
        }
      }
      else {
        log.warn 'Skipping: {}', dir
      }
    }

    def installDir = applicationDirectories.installDirectory
    if (installDir) {
      def etcDir = new File(installDir, 'etc')

      maybeIncludeFile new File(etcDir, 'nexus-default.properties'), 'install/etc', HIGH
      maybeIncludeDir new File(etcDir, 'fabric'), 'install/etc', HIGH
      maybeIncludeDir new File(etcDir, 'jetty'), 'install/etc', HIGH
      maybeIncludeDir new File(etcDir, 'karaf'), 'install/etc', HIGH
      maybeIncludeDir new File(etcDir, 'logback'), 'install/etc', HIGH
    }

    def workDir = applicationDirectories.workDirectory
    if (workDir) {
      def etcDir = new File(workDir, 'etc')

      maybeIncludeFile new File(etcDir, 'nexus.properties'), 'install/etc', HIGH
      maybeIncludeFile new File(etcDir, 'fabric/hazelcast-network.xml'), 'install/etc/fabric', HIGH
      maybeIncludeDir new File(etcDir, 'logback'), 'install/etc', HIGH
    }
  }

  /**
   * Ad-hoc subclass that encapsulates the XSLT transformation of a Jetty configuration file, removing passwords.
   */
  static class SanitizedJettyFileSource
      extends SanitizedXmlSourceSupport
  {
    static final STYLESHEET = '''
      <xsl:stylesheet version="1.0"
       xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
       <xsl:output omit-xml-declaration="no"
                   indent="yes"
                   doctype-public="-//Jetty//Configure//EN"
                   doctype-system="http://www.eclipse.org/jetty/configure_9_0.dtd"/>

       <xsl:template match="node()|@*">
        <xsl:copy>
         <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
       </xsl:template>

       <xsl:template match="Set[@name='KeyStorePassword']/text()">
        <xsl:text></xsl:text>
       </xsl:template>
       <xsl:template match="Set[@name='KeyManagerPassword']/text()">
        <xsl:text></xsl:text>
       </xsl:template>
       <xsl:template match="Set[@name='TrustStorePassword']/text()">
        <xsl:text></xsl:text>
       </xsl:template>
      </xsl:stylesheet>'''.stripMargin()

    /**
     * Constructor.
     */
    SanitizedJettyFileSource(final Type type, final String path, final File file, final Priority priority) {
      super(type, path, file, priority, STYLESHEET)
    }
  }

  /**
   * Removes AWS credentials from hazelcast.xml, if present.
   */
  static class SanitizedHazelcastFileSource
    extends SanitizedXmlSourceSupport {

    static final STYLESHEET = '''
      <xsl:stylesheet version="1.0"
       xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
       xmlns:hz="http://www.hazelcast.com/schema/config">
       <xsl:output omit-xml-declaration="no" standalone="no"
                   indent="yes"/>

       <xsl:template match="node()|@*">
        <xsl:copy>
         <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
       </xsl:template>

       <xsl:template match="hz:access-key/text()">
        <xsl:text>removed</xsl:text>
       </xsl:template>
       <xsl:template match="hz:secret-key/text()">
        <xsl:text>removed</xsl:text>
       </xsl:template>
       <xsl:template match="hz:password/text()">
        <xsl:text>removed</xsl:text>
       </xsl:template>
       <xsl:template match="hz:property[@name='access-key']/text()">
        <xsl:text>removed</xsl:text>
       </xsl:template>
       <xsl:template match="hz:property[@name='secret-key']/text()">
        <xsl:text>removed</xsl:text>
       </xsl:template>
      </xsl:stylesheet>'''.stripMargin()

    SanitizedHazelcastFileSource(final Type type, final String path, final File file, final Priority priority) {
      super(type, path, file, priority, STYLESHEET)
    }
  }
}
