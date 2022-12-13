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
package org.sonatype.nexus.internal.atlas.customizers;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.Collections;
import java.util.stream.Collectors;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.internal.atlas.customizers.InstallConfigurationCustomizer.SanitizedDataStoreFileSource;
import org.sonatype.nexus.internal.atlas.customizers.InstallConfigurationCustomizer.SanitizedHazelcastFileSource;
import org.sonatype.nexus.internal.atlas.customizers.InstallConfigurationCustomizer.SanitizedJettyFileSource;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Priority.DEFAULT;
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type.CONFIG;

/**
 * Tests for {@link InstallConfigurationCustomizer}.
 */
public class InstallConfigurationCustomizerTest
    extends TestSupport
{
  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  // SanitizedJettyFileSource can correctly apply XSLT to jetty-https to remove text from known password fields
  @Test
  @Ignore("NEXUS-36012")
  public void testSanitizedJettyFileSource() throws Exception {
    File temp = tempFolder.newFile("test-.xml");

    Files.write(temp.toPath(), Collections.singleton("<?xml version='1.0'?>\n" +
        "<!DOCTYPE Configure PUBLIC '-//Jetty//Configure//EN' 'http://www.eclipse.org/jetty/configure_9_0.dtd'>\n" +
        "<Configure id='Server' class='org.eclipse.jetty.server.Server'>\n" +
        "  <New id='sslContextFactory' class='org.eclipse.jetty.util.ssl.SslContextFactory'>\n" +
        "    <Set name='KeyStorePath'>path</Set>\n" +
        "    <Set name='KeyStorePassword'>password</Set>\n" +
        "    <Set name='KeyManagerPassword'>password</Set>\n" +
        "    <Set name='TrustStorePath'>path</Set>\n" +
        "    <Set name='TrustStorePassword'>password</Set>\n" +
        "  </New>\n" +
        "</Configure>"));

    SanitizedJettyFileSource source = new SanitizedJettyFileSource(CONFIG, "test/file", temp, DEFAULT);
    source.prepare();

    final String expected = "<?xml version='1.0' encoding='UTF-8'?>\n" +
        "<!DOCTYPE Configure PUBLIC '-//Jetty//Configure//EN' 'http://www.eclipse.org/jetty/configure_9_0.dtd'>\n" +
        "<Configure id='Server' class='org.eclipse.jetty.server.Server'>\n" +
        "  <New id='sslContextFactory' class='org.eclipse.jetty.util.ssl.SslContextFactory'>\n" +
        "    <Set name='KeyStorePath'>path</Set>\n" +
        "    <Set name='KeyStorePassword'/>\n" +
        "    <Set name='KeyManagerPassword'/>\n" +
        "    <Set name='TrustStorePath'>path</Set>\n" +
        "    <Set name='TrustStorePassword'/>\n" +
        "  </New>\n" +
        "</Configure>";

    Diff diff = DiffBuilder.compare(Input.fromString(expected))
        .withTest(Input.fromStream(source.getContent()))
        .build();

    assertFalse(diff.toString(), diff.hasDifferences());
  }

  //SanitizedHazelcastFileSource removes aws credentials and text from known password fields
  @Test
  public void testSanitizedHazelcastFileSource() throws Exception {
    File temp = tempFolder.newFile("test-.xml");

    Files.write(temp.toPath(), Collections.singleton("<?xml version='1.0' encoding='UTF-8'?>\n" +
        "<hazelcast xmlns='http://www.hazelcast.com/schema/config'\n" +
        "  xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'\n" +
        "  xsi:schemaLocation='http://www.hazelcast.com/schema/config hazelcast-config-3.6.xsd'>\n" +
        "  <network>\n" +
        "    <join>\n" +
        "      <aws enabled='true'>\n" +
        "        <access-key>my-access-key</access-key>\n" +
        "        <secret-key>my-secret-key</secret-key>\n" +
        "        <region>us-west-1</region>\n" +
        "        <security-group-name>hazelcast-sg</security-group-name>\n" +
        "        <tag-key>type</tag-key>\n" +
        "        <tag-value>nxrm</tag-value>\n" +
        "      </aws>\n" +
        "    </join>\n" +
        "    <symmetric-encryption enabled='false'>\n" +
        "      <algorithm>PBEWithMD5AndDES</algorithm>\n" +
        "      <salt>thesalt</salt>\n" +
        "      <password>thepass</password>\n" +
        "      <iteration-count>19</iteration-count>\n" +
        "    </symmetric-encryption>\n" +
        "  </network>\n" +
        "</hazelcast>"));

    SanitizedHazelcastFileSource source = new SanitizedHazelcastFileSource(CONFIG, "test/file", temp, DEFAULT);
    source.prepare();

    final String expected = "<?xml version='1.0' encoding='UTF-8' standalone='no'?>\n" +
        "<hazelcast xmlns='http://www.hazelcast.com/schema/config'\n" +
        "  xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'\n" +
        "  xsi:schemaLocation='http://www.hazelcast.com/schema/config hazelcast-config-3.6.xsd'>\n" +
        "  <network>\n" +
        "    <join>\n" +
        "      <aws enabled='true'>\n" +
        "        <access-key>removed</access-key>\n" +
        "        <secret-key>removed</secret-key>\n" +
        "        <region>us-west-1</region>\n" +
        "        <security-group-name>hazelcast-sg</security-group-name>\n" +
        "        <tag-key>type</tag-key>\n" +
        "        <tag-value>nxrm</tag-value>\n" +
        "      </aws>\n" +
        "    </join>\n" +
        "    <symmetric-encryption enabled='false'>\n" +
        "      <algorithm>PBEWithMD5AndDES</algorithm>\n" +
        "      <salt>thesalt</salt>\n" +
        "      <password>removed</password>\n" +
        "      <iteration-count>19</iteration-count>\n" +
        "    </symmetric-encryption>\n" +
        "  </network>\n" +
        "</hazelcast>";

    Diff diff = DiffBuilder.compare(Input.fromString(expected))
      .withTest(Input.fromStream(source.getContent()))
      .build();

    assertFalse(diff.toString(), diff.hasDifferences());
  }

  // SanitizedHazelcastFileSource removes aws credentials and text from known password fields in discovery strategy
  @Test
  public void testSanitizedHazelcastFileSource_discovery() throws Exception {
    File temp = tempFolder.newFile("test-.xml");

    Files.write(temp.toPath(), Collections.singleton("<?xml version='1.0' encoding='UTF-8'?>\n" +
        "<hazelcast xmlns='http://www.hazelcast.com/schema/config'\n" +
        "  xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'\n" +
        "  xsi:schemaLocation='http://www.hazelcast.com/schema/config hazelcast-config-3.6.xsd'>\n" +
        "  <properties>\n" +
        "    <property name='hazelcast.discovery.enabled'>true</property>\n" +
        "  </properties>\n" +

        "  <network>\n" +
        "    <join>\n" +
        "      <multicast enabled='false'/>\n" +
        "      <tcp-ip enabled='false' />\n" +
        "      <aws enabled='false'/>\n" +

        "      <discovery-strategies>\n" +
        "        <discovery-strategy enabled='true' class='com.hazelcast.aws.AwsDiscoveryStrategy'>\n" +
        "          <properties>\n" +
        "            <property name='tag-key'>Purpose</property>\n" +
        "            <property name='tag-value'>Nexus Repository Manager</property>\n" +
        "            <property name='iam-role'>EC2_IAM_ROLE_NAME</property>\n" +
        "            <property name='security-group-name'>EC2_SECURITY_GROUP_NAME</property>\n" +
        "            <property name='region'>us-west-1</property>\n" +
        "            <property name='access-key'>my-access-key</property>\n" +
        "            <property name='secret-key'>my-secret-key</property>\n" +
        "          </properties>\n" +
        "        </discovery-strategy>\n" +
        "      </discovery-strategies>\n" +
        "    </join>\n" +
        "  </network>\n" +
        "</hazelcast>"));

    SanitizedHazelcastFileSource source = new SanitizedHazelcastFileSource(CONFIG, "test/file", temp, DEFAULT);
    source.prepare();

    final String expected = "<?xml version='1.0' encoding='UTF-8' standalone='no'?>\n" +
        "<hazelcast xmlns='http://www.hazelcast.com/schema/config'\n" +
        "  xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'\n" +
        "  xsi:schemaLocation='http://www.hazelcast.com/schema/config hazelcast-config-3.6.xsd'>\n" +
        "  <properties>\n" +
        "    <property name='hazelcast.discovery.enabled'>true</property>\n" +
        "  </properties>\n" +

        "  <network>\n" +
        "    <join>\n" +
        "      <multicast enabled='false'/>\n" +
        "      <tcp-ip enabled='false' />\n" +
        "      <aws enabled='false'/>\n" +

        "      <discovery-strategies>\n" +
        "        <discovery-strategy enabled='true' class='com.hazelcast.aws.AwsDiscoveryStrategy'>\n" +
        "          <properties>\n" +
        "            <property name='tag-key'>Purpose</property>\n" +
        "            <property name='tag-value'>Nexus Repository Manager</property>\n" +
        "            <property name='iam-role'>EC2_IAM_ROLE_NAME</property>\n" +
        "            <property name='security-group-name'>EC2_SECURITY_GROUP_NAME</property>\n" +
        "            <property name='region'>us-west-1</property>\n" +
        "            <property name='access-key'>removed</property>\n" +
        "            <property name='secret-key'>removed</property>\n" +
        "          </properties>\n" +
        "        </discovery-strategy>\n" +
        "      </discovery-strategies>\n" +
        "    </join>\n" +
        "  </network>\n" +
        "</hazelcast>";

    Diff diff = DiffBuilder.compare(Input.fromString(expected))
        .withTest(Input.fromStream(source.getContent()))
        .build();

    assertFalse(diff.toString(), diff.hasDifferences());
  }

  @Test
  public void testSanitizedDataStoreFileSource() throws Exception {
    File temp = tempFolder.newFile("test-store.properties");

    Files.write(temp.toPath(), Collections.singleton("name=config\n" +
        "password=secret\n" +
        "type=jdbc\n" +
        "jdbcUrl=jdbc\\:postgresql\\://localhost\\:5432/postgres?password=secret&password=secret&pass\n" +
        "username=postgres"));

    SanitizedDataStoreFileSource source = new SanitizedDataStoreFileSource(CONFIG, "test/file", temp, DEFAULT);
    source.prepare();

    final String expected = "password=**REDACTED**\n" +
        "name=config\n" +
        "type=jdbc\n" +
        "jdbcUrl=jdbc\\:postgresql\\://localhost\\:5432/postgres?password\\=**REDACTED**&password\\=**REDACTED**\n" +
        "username=postgres";

    // Skip the timestamp line at the top of the file
    String actual;
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(source.getContent()))) {
      actual = reader.lines().skip(1).collect(Collectors.joining(System.lineSeparator()));
    }

    assertThat(actual, is(expected));
  }
}
