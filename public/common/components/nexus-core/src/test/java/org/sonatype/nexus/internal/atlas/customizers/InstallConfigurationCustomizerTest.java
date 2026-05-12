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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.sonatype.nexus.supportzip.SupportBundle;
import org.sonatype.nexus.bootstrap.entrypoint.configuration.ApplicationDirectories;
import org.sonatype.nexus.internal.atlas.customizers.InstallConfigurationCustomizer.SanitizedJettyFileSource;

import org.junit.Rule;
import org.junit.Test;
import org.junit.Before;
import org.junit.rules.TemporaryFolder;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Priority.DEFAULT;
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type.CONFIG;

/**
 * Tests for {@link InstallConfigurationCustomizer}.
 */
public class InstallConfigurationCustomizerTest
{
  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  private File installDir;

  private File workDir;

  private File etcDir;

  @Before
  public void setUp() throws Exception {
    installDir = tempFolder.newFolder("install");
    workDir = tempFolder.newFolder("work");
    etcDir = new File(installDir, "etc");
    etcDir.mkdir();
  }

  @Test
  public void testJettyHttpsFipsFileIncluded() throws Exception {
    File temp = tempFolder.newFile("jetty-https-fips.xml");

    // Create a temporary jetty-https-fips.xml file
    Files.write(temp.toPath(), Collections.singleton("<?xml version='1.0' encoding='UTF-8'?>\n" +
        "<!DOCTYPE Configure PUBLIC '-//Jetty//Configure//EN' 'http://www.eclipse.org/jetty/configure_9_0.dtd'>\n" +
        "<Configure id='Server' class='org.eclipse.jetty.server.Server'>\n" +
        "  <New id=\"sslContextFactory\" class=\"org.eclipse.jetty.util.ssl.SslContextFactory$Server\">\n" +
        "    <Set name=\"KeyStorePath\"><Property name=\"ssl.etc\"/>/keystore.jks</Set>\n" +
        "    <Set name=\"KeyStorePassword\">password</Set>\n" +
        "    <Set name=\"KeyManagerPassword\">password</Set>\n" +
        "    <Set name=\"TrustStorePath\"><Property name=\"ssl.etc\"/>/keystore.jks</Set>\n" +
        "    <Set name=\"TrustStorePassword\">password</Set>\n" +
        "    <Set name=\"EndpointIdentificationAlgorithm\"></Set>\n" +
        "    <Set name=\"NeedClientAuth\"><Property name=\"jetty.ssl.needClientAuth\" default=\"false\"/></Set>\n" +
        "    <Set name=\"WantClientAuth\"><Property name=\"jetty.ssl.wantClientAuth\" default=\"false\"/></Set>\n" +
        "    <Set name=\"IncludeProtocols\">\n" +
        "      <Array type=\"java.lang.String\">\n" +
        "        <Item>TLSv1.2</Item>\n" +
        "      </Array>\n" +
        "    </Set>\n" +
        "    <Set name=\"IncludeCipherSuites\">\n" +
        "      <Array type=\"java.lang.String\">\n" +
        "        <Item>TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384</Item>\n" +
        "        <Item>TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384</Item>\n" +
        "        <Item>TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256</Item>\n" +
        "        <Item>TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256</Item>\n" +
        "      </Array>\n" +
        "    </Set>\n" +
        "    <Set name=\"ExcludeCipherSuites\">\n" +
        "      <Array type=\"java.lang.String\">\n" +
        "        <Item>SSL_RSA_WITH_RC4_128_SHA</Item>\n" +
        "        <Item>SSL_RSA_WITH_3DES_EDE_CBC_SHA</Item>\n" +
        "        <Item>SSL_RSA_WITH_DES_CBC_SHA</Item>\n" +
        "        <Item>SSL_RSA_WITH_NULL_SHA</Item>\n" +
        "        <Item>SSL_RSA_EXPORT_WITH_RC4_40_MD5</Item>\n" +
        "        <Item>SSL_RSA_EXPORT_WITH_DES40_CBC_SHA</Item>\n" +
        "      </Array>\n" +
        "    </Set>\n" +
        "  </New>\n" +
        "</Configure>"));

    SanitizedJettyFileSource source = new SanitizedJettyFileSource(CONFIG, "test/file", temp, DEFAULT);
    source.prepare();

    // Validate the content of the file
    String expectedContent = "<?xml version='1.0' encoding='UTF-8'?>\n" +
        "<!DOCTYPE Configure PUBLIC '-//Jetty//Configure//EN' 'http://www.eclipse.org/jetty/configure_9_0.dtd'>\n" +
        "<Configure id='Server' class='org.eclipse.jetty.server.Server'>\n" +
        "  <New id=\"sslContextFactory\" class=\"org.eclipse.jetty.util.ssl.SslContextFactory$Server\">\n" +
        "    <Set name=\"KeyStorePath\"><Property name=\"ssl.etc\"/>/keystore.jks</Set>\n" +
        "    <Set name=\"KeyStorePassword\"/>\n" +
        "    <Set name=\"KeyManagerPassword\"/>\n" +
        "    <Set name=\"TrustStorePath\"><Property name=\"ssl.etc\"/>/keystore.jks</Set>\n" +
        "    <Set name=\"TrustStorePassword\"/>\n" +
        "    <Set name=\"EndpointIdentificationAlgorithm\"></Set>\n" +
        "    <Set name=\"NeedClientAuth\"><Property name=\"jetty.ssl.needClientAuth\" default=\"false\"/></Set>\n" +
        "    <Set name=\"WantClientAuth\"><Property name=\"jetty.ssl.wantClientAuth\" default=\"false\"/></Set>\n" +
        "    <Set name=\"IncludeProtocols\">\n" +
        "      <Array type=\"java.lang.String\">\n" +
        "        <Item>TLSv1.2</Item>\n" +
        "      </Array>\n" +
        "    </Set>\n" +
        "    <Set name=\"IncludeCipherSuites\">\n" +
        "      <Array type=\"java.lang.String\">\n" +
        "        <Item>TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384</Item>\n" +
        "        <Item>TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384</Item>\n" +
        "        <Item>TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256</Item>\n" +
        "        <Item>TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256</Item>\n" +
        "      </Array>\n" +
        "    </Set>\n" +
        "    <Set name=\"ExcludeCipherSuites\">\n" +
        "      <Array type=\"java.lang.String\">\n" +
        "        <Item>SSL_RSA_WITH_RC4_128_SHA</Item>\n" +
        "        <Item>SSL_RSA_WITH_3DES_EDE_CBC_SHA</Item>\n" +
        "        <Item>SSL_RSA_WITH_DES_CBC_SHA</Item>\n" +
        "        <Item>SSL_RSA_WITH_NULL_SHA</Item>\n" +
        "        <Item>SSL_RSA_EXPORT_WITH_RC4_40_MD5</Item>\n" +
        "        <Item>SSL_RSA_EXPORT_WITH_DES40_CBC_SHA</Item>\n" +
        "      </Array>\n" +
        "    </Set>\n" +
        "  </New>\n" +
        "</Configure>";

    Diff diff = DiffBuilder.compare(Input.fromString(expectedContent))
        .withTest(Input.fromStream(source.getContent()))
        .ignoreWhitespace()
        .build();

    assertFalse(diff.toString(), diff.hasDifferences());
  }

  @Test
  public void testSanitizedDataStoreFileSource() throws Exception {
    File fabricDir = new File(etcDir, "fabric");
    fabricDir.mkdir();
    File dataStoreFile = new File(fabricDir, "test-store.properties");

    Files.write(dataStoreFile.toPath(), Collections.singleton("name=config\n" +
        "password=secret\n" +
        "type=jdbc\n" +
        "jdbcUrl=jdbc\\:postgresql\\://localhost\\:5432/postgres?password=secret&password=secret&pass\n" +
        "username=postgres"));

    ApplicationDirectories applicationDirectories = mock(ApplicationDirectories.class);
    when(applicationDirectories.getInstallDirectory()).thenReturn(installDir);
    when(applicationDirectories.getWorkDirectory()).thenReturn(workDir);

    InstallConfigurationCustomizer customizer = new InstallConfigurationCustomizer(applicationDirectories);

    SupportBundle supportBundle = new SupportBundle();
    customizer.customize(supportBundle);

    List<SupportBundle.ContentSource> sources = supportBundle.getSources();
    SupportBundle.ContentSource source = sources.stream()
        .filter(s -> s.getPath().equals("install/etc/fabric/test-store.properties"))
        .findFirst()
        .orElseThrow(() -> new AssertionError("SanitizedDataStoreFileSource not found"));

    final List<String> expected = Arrays.asList("password=**REDACTED**", "name=config", "type=jdbc",
        "jdbcUrl=jdbc\\:postgresql\\://localhost\\:5432/postgres?password\\=**REDACTED**&password\\=**REDACTED**",
        "username=postgres");

    // Skip the timestamp line at the top of the file
    List<String> actual;
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(source.getContent()))) {
      actual = reader.lines().skip(1).collect(Collectors.toList());
    }

    assertThat(actual, containsInAnyOrder(expected.toArray()));
  }

  @Test
  public void SanitizedNexusFileSource() throws Exception {
    File temp = new File(etcDir, "nexus.properties");

    Files.write(temp.toPath(), Collections.singleton("nexus.datastore.nexus.password=secret\n" +
        "nexus.datastore.nexus.jdbcUrl=jdbc\\:postgresql\\://localhost\\:5432/postgres?password=secret&password=secret&pass\n"
        +
        "nexus.datastore.nexus.username=postgres"));

    ApplicationDirectories applicationDirectories = mock(ApplicationDirectories.class);
    when(applicationDirectories.getInstallDirectory()).thenReturn(installDir);
    when(applicationDirectories.getWorkDirectory()).thenReturn(workDir);

    InstallConfigurationCustomizer customizer = new InstallConfigurationCustomizer(applicationDirectories);

    SupportBundle supportBundle = new SupportBundle();
    customizer.customize(supportBundle);

    List<SupportBundle.ContentSource> sources = supportBundle.getSources();
    SupportBundle.ContentSource source = sources.stream()
        .filter(s -> s.getPath().equals("install/etc/nexus.properties"))
        .findFirst()
        .orElseThrow(() -> new AssertionError("SanitizedNexusFileSource not found"));

    final List<String> expected = Arrays.asList("nexus.datastore.nexus.password=**REDACTED**",
        "nexus.datastore.nexus.jdbcUrl=jdbc\\:postgresql\\://localhost\\:5432/postgres?password\\=**REDACTED**&password\\=**REDACTED**",
        "nexus.datastore.nexus.username=postgres");

    // Skip the timestamp line at the top of the file
    List<String> actual;
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(source.getContent()))) {
      actual = reader.lines().skip(1).collect(Collectors.toList());
    }

    assertThat(actual, containsInAnyOrder(expected.toArray()));
  }

  @Test
  public void testDuplicateFilenamesInInstallAndWorkDontConflict() throws Exception {
    // Create the same filename in both install and work directories
    File installFabricDir = new File(etcDir, "fabric");
    installFabricDir.mkdir();
    File installFile = new File(installFabricDir, "nexus-store.properties");
    Files.write(installFile.toPath(), Collections.singleton("install.location=true"));

    File workEtcDir = new File(workDir, "etc");
    workEtcDir.mkdir();
    File workFabricDir = new File(workEtcDir, "fabric");
    workFabricDir.mkdir();
    File workFile = new File(workFabricDir, "nexus-store.properties");
    Files.write(workFile.toPath(), Collections.singleton("work.location=true"));

    ApplicationDirectories applicationDirectories = mock(ApplicationDirectories.class);
    when(applicationDirectories.getInstallDirectory()).thenReturn(installDir);
    when(applicationDirectories.getWorkDirectory()).thenReturn(workDir);

    InstallConfigurationCustomizer customizer = new InstallConfigurationCustomizer(applicationDirectories);

    SupportBundle supportBundle = new SupportBundle();
    customizer.customize(supportBundle);

    List<SupportBundle.ContentSource> sources = supportBundle.getSources();

    // Verify both files are included with different paths
    long installCount = sources.stream()
        .filter(s -> s.getPath().equals("install/etc/fabric/nexus-store.properties"))
        .count();

    long workCount = sources.stream()
        .filter(s -> s.getPath().equals("work/etc/fabric/nexus-store.properties"))
        .count();

    assertEquals("Install file should be included once", 1, installCount);
    assertEquals("Work file should be included once", 1, workCount);

    // Verify no duplicate paths exist
    List<String> paths = sources.stream()
        .map(SupportBundle.ContentSource::getPath)
        .collect(Collectors.toList());

    long uniquePaths = paths.stream().distinct().count();
    assertEquals("All paths should be unique (no duplicates)", paths.size(), uniquePaths);
  }
}
