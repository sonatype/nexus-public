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

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.internal.atlas.customizers.InstallConfigurationCustomizer.SanitizedJettyFileSource

import org.junit.Test

import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Priority.DEFAULT
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type.CONFIG

/**
 * Tests for {@link InstallConfigurationCustomizer}.
 */
class InstallConfigurationCustomizerTest
    extends TestSupport
{
  @Test
  void 'SanitizedJettyFileSource can correctly apply XSLT to jetty-https to remove text from known password fields'() {

    File temp = File.createTempFile("test-", ".xml")
    temp.deleteOnExit()

    temp << '''<?xml version="1.0"?>
<!DOCTYPE Configure PUBLIC "-//Jetty//Configure//EN" "http://www.eclipse.org/jetty/configure_9_0.dtd">
<Configure id="Server" class="org.eclipse.jetty.server.Server">
  <New id="sslContextFactory" class="org.eclipse.jetty.util.ssl.SslContextFactory">
    <Set name="KeyStorePath">path</Set>
    <Set name="KeyStorePassword">password</Set>
    <Set name="KeyManagerPassword">password</Set>
    <Set name="TrustStorePath">path</Set>
    <Set name="TrustStorePassword">password</Set>
  </New>
</Configure>
'''

    SanitizedJettyFileSource source = new SanitizedJettyFileSource(CONFIG, 'test/file', temp, DEFAULT)
    source.prepare()

    final String expected = '''<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE Configure PUBLIC "-//Jetty//Configure//EN" "http://www.eclipse.org/jetty/configure_9_0.dtd">
<Configure id="Server" class="org.eclipse.jetty.server.Server">
  <New id="sslContextFactory" class="org.eclipse.jetty.util.ssl.SslContextFactory">
    <Set name="KeyStorePath">path</Set>
    <Set name="KeyStorePassword"/>
    <Set name="KeyManagerPassword"/>
    <Set name="TrustStorePath">path</Set>
    <Set name="TrustStorePassword"/>
  </New>
</Configure>
'''

    assert expected == source.content.text.replace('\r\n', '\n')
  }
}
