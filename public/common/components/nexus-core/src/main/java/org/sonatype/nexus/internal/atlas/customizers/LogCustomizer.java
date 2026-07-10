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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.ws.rs.NotFoundException;

import org.sonatype.nexus.bootstrap.entrypoint.configuration.ApplicationDirectories;
import org.sonatype.nexus.common.log.LogManager;
import org.sonatype.nexus.supportzip.FileContentSourceSupport;
import org.sonatype.nexus.supportzip.GeneratedContentSourceSupport;
import org.sonatype.nexus.supportzip.SupportBundle;
import org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Priority;
import org.sonatype.nexus.supportzip.SupportBundleCustomizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.log.LogManager.DEFAULT_LOGGER;
import static org.sonatype.nexus.common.text.Strings2.MASK;
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Priority.LOW;
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type.LOG;
import org.springframework.stereotype.Component;

/**
 * Adds log files to support bundle.
 * Redacts sensitive authentication header values while preserving authentication types.
 *
 * @since 2.7
 */
@Component
public class LogCustomizer
    implements SupportBundleCustomizer
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final LogManager logManager;

  private final ApplicationDirectories applicationDirectories;

  // Pattern to match sensitive authentication headers
  // Captures: header name, optional scheme (Bearer, Basic, Token, etc.), and the credential value
  // Matches headers like: Authorization: Bearer abc123, Authorization: Basic dXNlcjpwYXNz
  // For headers without scheme: X-Auth-Token: abc123
  // Uses .+ to match to end of line, covering multi-token credentials like Digest with multiple parameters
  private static final Pattern AUTH_HEADER_PATTERN = Pattern.compile(
      "(Authorization|WWW-Authenticate|Proxy-Authorization|Proxy-Authenticate|X-Auth-Token|X-API-Key)\\s*:\\s*(Bearer|Basic|Token|Digest|NTLM|Negotiate|AWS4-HMAC-SHA256)?\\s*.+",
      Pattern.CASE_INSENSITIVE);

  @Autowired
  public LogCustomizer(final LogManager logManager, final ApplicationDirectories applicationDirectories) {
    this.logManager = checkNotNull(logManager);
    this.applicationDirectories = checkNotNull(applicationDirectories);
  }

  @Override
  public void customize(final SupportBundle supportBundle) {
    // add source for default log
    String logName = logManager.getLogFor(DEFAULT_LOGGER)
        .orElseThrow(() -> new NotFoundException("Failed to determine log file name for " + DEFAULT_LOGGER));

    supportBundle.add(new GeneratedContentSourceSupport(LOG, "log/" + logName, LOW)
    {
      @Override
      protected void generate(final File file) {
        try (InputStream is = logManager.getLogFileStream(logName, 0, Long.MAX_VALUE)) {
          if (is != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
              String line;
              while ((line = reader.readLine()) != null) {
                String redactedLine = redactAuthenticationHeaders(line);
                writer.write(redactedLine);
                writer.newLine();
              }
            }
          }
        }
        catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }
    });

    includeFileIfExists(supportBundle, new File(applicationDirectories.getWorkDirectory(), "log/karaf.log"), "log",
        LOW);
    includeFileIfExists(supportBundle, new File(applicationDirectories.getWorkDirectory(), "log/request.log"), "log",
        LOW);
    includeFileIfExists(supportBundle, new File(applicationDirectories.getWorkDirectory(), "log/outbound-request.log"),
        "log", LOW);
  }

  private void includeFileIfExists(
      final SupportBundle supportBundle,
      final File file,
      final String prefix,
      final Priority priority)
  {
    if (file != null && file.exists()) {
      log.debug("Including file: {}", file);
      supportBundle.add(
          new FileContentSourceSupport(LOG, String.format("%s/%s", prefix, file.getName()), file, priority));
    }
    else {
      log.debug("Skipping non-existent file: {}", file);
    }
  }

  /**
   * Redacts sensitive authentication header values while preserving the authentication type.
   *
   * Examples:
   * - "Authorization: Bearer abc123..." → "Authorization: Bearer ****"
   * - "Authorization: Basic dXNlcjpwYXNz" → "Authorization: Basic ****"
   * - "Authorization: Token token=\"abc\"" → "Authorization: Token ****"
   * - "X-Auth-Token: abc123" → "X-Auth-Token: ****"
   */
  private String redactAuthenticationHeaders(final String line) {
    // Replace with captured header name, optional scheme, and MASK
    // Group 1 = header name, Group 2 = optional scheme (may be null)
    return AUTH_HEADER_PATTERN.matcher(line).replaceAll(matchResult -> {
      String header = matchResult.group(1);
      String scheme = matchResult.group(2);
      if (scheme != null) {
        return header + ": " + scheme + " " + MASK;
      }
      else {
        return header + ": " + MASK;
      }
    });
  }
}
