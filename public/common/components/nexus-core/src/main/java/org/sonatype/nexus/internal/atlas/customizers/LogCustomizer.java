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
import org.springframework.stereotype.Component;
import jakarta.ws.rs.NotFoundException;

import org.sonatype.nexus.bootstrap.entrypoint.configuration.ApplicationDirectories;
import org.sonatype.nexus.bootstrap.entrypoint.edition.NexusEditionSelector;
import org.sonatype.nexus.common.log.LogManager;
import org.sonatype.nexus.supportzip.FileContentSourceSupport;
import org.sonatype.nexus.supportzip.GeneratedContentSourceSupport;
import org.sonatype.nexus.supportzip.SupportBundle;
import org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Priority;
import org.sonatype.nexus.supportzip.SupportBundleCustomizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;
import static org.sonatype.nexus.common.log.LogManager.DEFAULT_LOGGER;
import static org.sonatype.nexus.common.text.Strings2.MASK;
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Priority.LOW;
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type.LOG;

/**
 * Adds log files to support bundle.
 * Redacts sensitive authentication header values while preserving authentication types,
 * and masks sensitive Set-Cookie session token values (e.g. Keycloak) at support-ZIP generation time.
 *
 * Skipped entirely on the cloud edition, where logs stream to stdout and no file appender is
 * configured; on every other edition a missing default appender is treated as a real error and
 * aborts the customizer.
 */
@Component
public class LogCustomizer
    implements SupportBundleCustomizer
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private static final String CLOUD_EDITION_ID = "nexus-cloud-edition";

  private final LogManager logManager;

  private final ApplicationDirectories applicationDirectories;

  private final NexusEditionSelector nexusEditionSelector;

  // Pattern to match sensitive authentication headers
  // Captures: header name, optional scheme (Bearer, Basic, Token, etc.), and the credential value
  // Matches headers like: Authorization: Bearer abc123, Authorization: Basic dXNlcjpwYXNz
  // For headers without scheme: X-Auth-Token: abc123
  // Uses .+ to match to end of line, covering multi-token credentials like Digest with multiple parameters
  private static final Pattern AUTH_HEADER_PATTERN = Pattern.compile(
      "(Authorization|WWW-Authenticate|Proxy-Authorization|Proxy-Authenticate|X-Auth-Token|X-API-Key)\\s*:\\s*(Bearer|Basic|Token|Digest|NTLM|Negotiate|AWS4-HMAC-SHA256)?\\s*.+",
      Pattern.CASE_INSENSITIVE);

  // NEXUS-54602: mask sensitive session cookies (e.g. Keycloak) in Set-Cookie headers.
  // Group 1 = prefix to preserve (optional "Set-Cookie: " + cookie name + "="); the value is masked.
  // Note: the write-time logback converter layer also honors the nexus.log.mask.cookies system property
  // for additional cookie names; this backstop covers the default Keycloak set only.
  private static final Pattern SET_COOKIE_PATTERN = Pattern.compile(
      "((?:Set-Cookie:\\s*)?(?:KEYCLOAK_IDENTITY|KEYCLOAK_IDENTITY_LEGACY|KEYCLOAK_SESSION|KEYCLOAK_SESSION_LEGACY|AUTH_SESSION_ID|AUTH_SESSION_ID_LEGACY)\\s*=)[^;,\\s\"]+",
      Pattern.CASE_INSENSITIVE);

  @Autowired
  public LogCustomizer(
      final LogManager logManager,
      final ApplicationDirectories applicationDirectories,
      final NexusEditionSelector nexusEditionSelector)
  {
    this.logManager = requireNonNull(logManager);
    this.applicationDirectories = requireNonNull(applicationDirectories);
    this.nexusEditionSelector = requireNonNull(nexusEditionSelector);
  }

  @Override
  public void customize(final SupportBundle supportBundle) {
    if (CLOUD_EDITION_ID.equals(nexusEditionSelector.getCurrent().getId())) {
      return;
    }

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
                redactedLine = redactSensitiveCookies(redactedLine);
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

  /**
   * Redacts sensitive session cookie values in Set-Cookie headers while preserving the cookie name prefix
   * and any trailing cookie attributes (Path, HttpOnly, etc.).
   *
   * Examples:
   * - "Set-Cookie: KEYCLOAK_IDENTITY=eyJhbGc...; Path=/auth" → "Set-Cookie: KEYCLOAK_IDENTITY=****; Path=/auth"
   * - "Set-Cookie: AUTH_SESSION_ID=abc123" → "Set-Cookie: AUTH_SESSION_ID=****"
   */
  private String redactSensitiveCookies(final String line) {
    return SET_COOKIE_PATTERN.matcher(line).replaceAll(mr -> mr.group(1) + MASK);
  }
}
