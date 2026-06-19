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
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Optional;

import org.sonatype.nexus.bootstrap.entrypoint.configuration.ApplicationDirectories;
import org.sonatype.nexus.common.log.LogManager;
import org.sonatype.nexus.supportzip.SupportBundle;
import org.sonatype.nexus.supportzip.SupportBundle.ContentSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogCustomizerTest
{
  @TempDir
  File temporaryWorkDirectory;

  @Mock
  private LogManager mockLogManager;

  @Mock
  private ApplicationDirectories mockApplicationDirectories;

  private LogCustomizer underTest;

  @BeforeEach
  void setup() {
    when(mockApplicationDirectories.getWorkDirectory()).thenReturn(temporaryWorkDirectory);
    underTest = new LogCustomizer(mockLogManager, mockApplicationDirectories);
  }

  @Test
  void customizeNexusLogRedactsAuthorizationBearer() throws Exception {
    String logContent =
        "2024-01-15 10:30:45,123 INFO  [executor-thread-1] auth request - Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9\n";
    testLogRedaction(logContent,
        "2024-01-15 10:30:45,123 INFO  [executor-thread-1] auth request - Authorization: Bearer ****");
  }

  @Test
  void customizeNexusLogRedactsAuthorizationBasic() throws Exception {
    String logContent =
        "2024-01-15 10:30:45,123 INFO  [executor-thread-1] auth - Authorization: Basic dXNlcjpwYXNzd29yZA==\n";
    testLogRedaction(logContent, "2024-01-15 10:30:45,123 INFO  [executor-thread-1] auth - Authorization: Basic ****");
  }

  @Test
  void customizeNexusLogRedactsAuthorizationToken() throws Exception {
    String logContent =
        "2024-01-15 10:30:45,123 INFO  [executor-thread-1] auth - Authorization: Token token=\"abc123\"\n";
    testLogRedaction(logContent, "2024-01-15 10:30:45,123 INFO  [executor-thread-1] auth - Authorization: Token ****");
  }

  @Test
  void customizeNexusLogRedactsWWWAuthenticate() throws Exception {
    String logContent =
        "2024-01-15 10:30:45,123 INFO  [executor-thread-1] response - WWW-Authenticate: Bearer realm=\"example\"\n";
    testLogRedaction(logContent,
        "2024-01-15 10:30:45,123 INFO  [executor-thread-1] response - WWW-Authenticate: Bearer ****");
  }

  @Test
  void customizeNexusLogRedactsProxyAuthorization() throws Exception {
    String logContent =
        "2024-01-15 10:30:45,123 INFO  [executor-thread-1] proxy - Proxy-Authorization: Basic dXNlcjpwYXNz\n";
    testLogRedaction(logContent,
        "2024-01-15 10:30:45,123 INFO  [executor-thread-1] proxy - Proxy-Authorization: Basic ****");
  }

  @Test
  void customizeNexusLogRedactsXAuthToken() throws Exception {
    String logContent = "2024-01-15 10:30:45,123 INFO  [executor-thread-1] api - X-Auth-Token: abc123def456\n";
    testLogRedaction(logContent, "2024-01-15 10:30:45,123 INFO  [executor-thread-1] api - X-Auth-Token: ****");
  }

  @Test
  void customizeNexusLogRedactsXApiKey() throws Exception {
    String logContent = "2024-01-15 10:30:45,123 INFO  [executor-thread-1] api - X-API-Key: secret-key-123\n";
    testLogRedaction(logContent, "2024-01-15 10:30:45,123 INFO  [executor-thread-1] api - X-API-Key: ****");
  }

  @Test
  void customizeNexusLogRedactsAuthorizationWithoutScheme() throws Exception {
    String logContent = "2024-01-15 10:30:45,123 INFO  [executor-thread-1] auth - Authorization: rawtoken123\n";
    testLogRedaction(logContent, "2024-01-15 10:30:45,123 INFO  [executor-thread-1] auth - Authorization: ****");
  }

  @Test
  void customizeNexusLogRedactsCaseInsensitive() throws Exception {
    String logContent = "2024-01-15 10:30:45,123 INFO  [executor-thread-1] auth - authorization: bearer token123\n";
    testLogRedaction(logContent, "2024-01-15 10:30:45,123 INFO  [executor-thread-1] auth - authorization: bearer ****");
  }

  @Test
  void customizeNexusLogPreservesNonSensitiveContent() throws Exception {
    String logContent = "2024-01-15 10:30:45,123 INFO  [executor-thread-1] request - GET /repository/maven-central/\n";
    testLogRedaction(logContent, logContent.trim());
  }

  @Test
  void customizeNexusLogRedactsMultipleHeadersOnSameLine() throws Exception {
    String logContent = "Authorization: Bearer token123 and X-API-Key: secret456\n";
    // With .+, the first match consumes to end of line, so "and X-API-Key: secret456" is also redacted
    testLogRedaction(logContent, "Authorization: Bearer ****");
  }

  private void testLogRedaction(final String inputContent, final String expectedOutput) throws Exception {
    File logFile = new File(temporaryWorkDirectory, "log/nexus.log");
    logFile.getParentFile().mkdirs();
    logFile.createNewFile();

    try (FileWriter writer = new FileWriter(logFile)) {
      writer.write(inputContent);
    }

    when(mockLogManager.getLogFor(anyString())).thenReturn(Optional.of("nexus.log"));
    when(mockLogManager.getLogFileStream(anyString(), anyLong(), anyLong()))
        .thenReturn(new java.io.FileInputStream(logFile));

    SupportBundle supportBundle = new SupportBundle();
    underTest.customize(supportBundle);

    List<ContentSource> list = supportBundle.getSources();
    assertThat(list.size(), equalTo(1));

    ContentSource contentSource = list.get(0);
    contentSource.prepare();

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(contentSource.getContent()))) {
      assertThat(reader.readLine(), equalTo(expectedOutput));
    }
  }
}
