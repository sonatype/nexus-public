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
import org.sonatype.nexus.internal.atlas.customizers.InstallConfigurationCustomizer.SanitizedHazelcastFileSource
import org.sonatype.nexus.internal.atlas.customizers.InstallConfigurationCustomizer.SanitizedJettyFileSource

import org.junit.Test
import org.xmlunit.builder.DiffBuilder
import org.xmlunit.builder.Input

import static org.junit.Assert.assertFalse
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

    def diff = DiffBuilder.compare(Input.fromString(expected))
        .withTest(Input.fromStream(source.content))
        .build()

    assertFalse(diff.toString(), diff.hasDifferences())
  }

  @Test
  void 'SanitizedHazelcastFileSource removes aws credentials and text from known password fields'() {
    File temp = File.createTempFile("test-", ".xml")
    temp.deleteOnExit()

    temp << '''<?xml version="1.0" encoding="UTF-8"?>
<hazelcast xmlns="http://www.hazelcast.com/schema/config"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://www.hazelcast.com/schema/config hazelcast-config-3.6.xsd">
  <network>
    <join>
      <aws enabled="true">
        <access-key>my-access-key</access-key>
        <secret-key>my-secret-key</secret-key>
        <region>us-west-1</region>
        <security-group-name>hazelcast-sg</security-group-name>
        <tag-key>type</tag-key>
        <tag-value>nxrm</tag-value>
      </aws>
    </join>
    <symmetric-encryption enabled="false">
      <algorithm>PBEWithMD5AndDES</algorithm>
      <salt>thesalt</salt>
      <password>thepass</password>
      <iteration-count>19</iteration-count>
    </symmetric-encryption>
  </network>
</hazelcast>
'''

    SanitizedHazelcastFileSource source = new SanitizedHazelcastFileSource(CONFIG, 'test/file', temp, DEFAULT)
    source.prepare()

    final String expected = '''<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<hazelcast xmlns="http://www.hazelcast.com/schema/config" 
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
  xsi:schemaLocation="http://www.hazelcast.com/schema/config hazelcast-config-3.6.xsd">
  <network>
    <join>
      <aws enabled="true">
        <access-key>removed</access-key>
        <secret-key>removed</secret-key>
        <region>us-west-1</region>
        <security-group-name>hazelcast-sg</security-group-name>
        <tag-key>type</tag-key>
        <tag-value>nxrm</tag-value>
      </aws>
    </join>
    <symmetric-encryption enabled="false">
      <algorithm>PBEWithMD5AndDES</algorithm>
      <salt>thesalt</salt>
      <password>removed</password>
      <iteration-count>19</iteration-count>
    </symmetric-encryption>
  </network>
</hazelcast>
'''

    def diff = DiffBuilder.compare(Input.fromString(expected))
      .withTest(Input.fromStream(source.content))
      .build()

    assertFalse(diff.toString(), diff.hasDifferences())
  }

  @Test
  void 'SanitizedHazelcastFileSource removes aws credentials and text from known password fields in discovery strategy'() {
    File temp = File.createTempFile("test-", ".xml")
    temp.deleteOnExit()

    temp << '''<?xml version="1.0" encoding="UTF-8"?>
<hazelcast xmlns="http://www.hazelcast.com/schema/config"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://www.hazelcast.com/schema/config hazelcast-config-3.6.xsd">
  <properties>
    <property name="hazelcast.discovery.enabled">true</property>
  </properties>

  <network>
    <join>
      <multicast enabled="false"/>
      <tcp-ip enabled="false" />
      <aws enabled="false"/>

      <discovery-strategies>
        <discovery-strategy enabled="true" class="com.hazelcast.aws.AwsDiscoveryStrategy">
          <properties>
            <property name="tag-key">Purpose</property>
            <property name="tag-value">Nexus Repository Manager</property>
            <property name="iam-role">EC2_IAM_ROLE_NAME</property>
            <property name="security-group-name">EC2_SECURITY_GROUP_NAME</property>
            <property name="region">us-west-1</property>
            <property name="access-key">my-access-key</property>
            <property name="secret-key">my-secret-key</property>
          </properties>
        </discovery-strategy>
      </discovery-strategies>
    </join>
  </network>
</hazelcast>
'''

    SanitizedHazelcastFileSource source = new SanitizedHazelcastFileSource(CONFIG, 'test/file', temp, DEFAULT)
    source.prepare()

    final String expected = '''<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<hazelcast xmlns="http://www.hazelcast.com/schema/config"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://www.hazelcast.com/schema/config hazelcast-config-3.6.xsd">
  <properties>
    <property name="hazelcast.discovery.enabled">true</property>
  </properties>

  <network>
    <join>
      <multicast enabled="false"/>
      <tcp-ip enabled="false" />
      <aws enabled="false"/>

      <discovery-strategies>
        <discovery-strategy enabled="true" class="com.hazelcast.aws.AwsDiscoveryStrategy">
          <properties>
            <property name="tag-key">Purpose</property>
            <property name="tag-value">Nexus Repository Manager</property>
            <property name="iam-role">EC2_IAM_ROLE_NAME</property>
            <property name="security-group-name">EC2_SECURITY_GROUP_NAME</property>
            <property name="region">us-west-1</property>
            <property name="access-key">removed</property>
            <property name="secret-key">removed</property>
          </properties>
        </discovery-strategy>
      </discovery-strategies>
    </join>
  </network>
</hazelcast>
'''

    def diff = DiffBuilder.compare(Input.fromString(expected))
        .withTest(Input.fromStream(source.content))
        .build()

    assertFalse(diff.toString(), diff.hasDifferences())
  }
}
