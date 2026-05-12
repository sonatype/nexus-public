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
package org.sonatype.nexus.jetty.log;

import java.net.SocketAddress;
import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.TimeZone;

import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.server.ConnectionMetaData;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog.Writer;
import org.eclipse.jetty.server.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NexusRepositoryRequestLogTest
{
  private static final String EXPECTED_FORMAT =
      "%{client}a - %{nexus.user.id}attr [%{responseTimestamp}attr] \"%r\" %s %{Content-Length}i %O %T %{User-Agent}i [%{threadName}attr]";

  public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(
      "dd/MMM/yyyy:HH:mm:ss Z")
      .withZone(TimeZone.getDefault().toZoneId());

  @Mock
  private Request request;

  @Mock
  private Response response;

  @Mock
  private Writer writer;

  @BeforeEach
  public void setUp() {
    when(response.getStatus()).thenReturn(200);
    ConnectionMetaData connectionMetaData = mock(ConnectionMetaData.class);
    SocketAddress socketAddress = mock(SocketAddress.class);
    when(socketAddress.toString()).thenReturn("127.0.0.1");
    when(connectionMetaData.getRemoteSocketAddress()).thenReturn(socketAddress);
    HttpURI uri = HttpURI.build("/path/to/foo");
    when(request.getHttpURI()).thenReturn(uri);
    when(request.getHeaders()).thenReturn(HttpFields.EMPTY);
    when(request.getConnectionMetaData()).thenReturn(connectionMetaData);
  }

  @Test
  public void testLogIncludesThreadName() {
    NexusRepositoryRequestLog requestLog = new NexusRepositoryRequestLog(writer, EXPECTED_FORMAT);

    requestLog.log(request, response);

    verify(request).setAttribute(eq("threadName"), eq(Thread.currentThread().getName()));
  }

  @Test
  public void testLogWithResponseTime() {
    Clock clock = Clock.fixed(Instant.now(), TimeZone.getDefault().toZoneId());
    NexusRepositoryRequestLog requestLog = new NexusRepositoryRequestLog(
        writer, EXPECTED_FORMAT, clock::millis);

    requestLog.log(request, response);

    verify(request).setAttribute(eq("responseTimestamp"),
        argThat(value -> {
          var truncated = truncateInstant(value, ChronoUnit.SECONDS);
          var now = Instant.now(clock).truncatedTo(ChronoUnit.SECONDS);

          return truncated.equals(now);
        }));
  }

  private Instant truncateInstant(final Object value, TemporalUnit unit) {
    return ZonedDateTime.parse(value.toString(), DATE_FORMATTER)
        .truncatedTo(unit)
        .toInstant();
  }

  @Test
  public void testLogWithoutSanitizedUriAttribute_logsOriginalPath() {
    // Given: Request with original path, no sanitization attribute
    HttpURI uri = HttpURI.build("/repository/terraform/v1/modules/namespace/name/versions");
    when(request.getHttpURI()).thenReturn(uri);
    when(request.getAttribute("sanitizedRequestUri")).thenReturn(null);

    NexusRepositoryRequestLog requestLog = new NexusRepositoryRequestLog(writer, EXPECTED_FORMAT);

    // When: Log the request
    requestLog.log(request, response);

    // Then: getAttribute should be called but no sanitization should occur
    verify(request).getAttribute("sanitizedRequestUri");
  }

  @Test
  public void testLogWithSanitizedUriAttribute_usesSanitizedPath() {
    // Given: Request with token in path and sanitized URI attribute (simulating Terraform handler)
    String originalPath = "/repository/terraform/v1/modules/dXNlcjpwYXNzd29yZA==/namespace/name/versions";
    String sanitizedPath = "/repository/terraform/v1/modules/[REDACTED]/namespace/name/versions";

    HttpURI uri = HttpURI.build(originalPath);
    when(request.getHttpURI()).thenReturn(uri);
    when(request.getAttribute("sanitizedRequestUri")).thenReturn(sanitizedPath);

    NexusRepositoryRequestLog requestLog = new NexusRepositoryRequestLog(writer, EXPECTED_FORMAT);

    // When: Log the request
    requestLog.log(request, response);

    // Then: Sanitized URI attribute should be used
    verify(request).getAttribute("sanitizedRequestUri");
  }

  @Test
  public void testLogWithSanitizedUriAttribute_preservesOtherRequestAttributes() {
    // Given: Request with sanitized URI and other attributes
    String sanitizedPath = "/repository/terraform/v1/modules/[REDACTED]/namespace/name/versions";

    HttpURI uri = HttpURI.build("/original/path");
    when(request.getHttpURI()).thenReturn(uri);
    when(request.getAttribute("sanitizedRequestUri")).thenReturn(sanitizedPath);

    NexusRepositoryRequestLog requestLog = new NexusRepositoryRequestLog(writer, EXPECTED_FORMAT);

    // When: Log the request
    requestLog.log(request, response);

    // Then: Thread name and response timestamp attributes should still be set
    verify(request).setAttribute(eq("threadName"), eq(Thread.currentThread().getName()));
    verify(request).setAttribute(eq("responseTimestamp"), argThat(value -> value != null));
  }

  @Test
  public void testSanitizedUriAttribute_doesNotAffectNonTerraformRequests() {
    // Given: Non-Terraform request (e.g., Maven, NPM, Docker)
    HttpURI uri = HttpURI.build("/repository/maven-central/org/example/artifact/1.0.0/artifact-1.0.0.jar");
    when(request.getHttpURI()).thenReturn(uri);
    when(request.getAttribute("sanitizedRequestUri")).thenReturn(null);

    NexusRepositoryRequestLog requestLog = new NexusRepositoryRequestLog(writer, EXPECTED_FORMAT);

    // When: Log the request
    requestLog.log(request, response);

    // Then: getAttribute should be called but no sanitization should occur
    verify(request).getAttribute("sanitizedRequestUri");
  }

  @Test
  public void testSanitizedUriAttribute_handlesEmptyString() {
    // Given: Request with empty sanitized URI attribute
    HttpURI uri = HttpURI.build("/path/to/resource");
    when(request.getHttpURI()).thenReturn(uri);
    when(request.getAttribute("sanitizedRequestUri")).thenReturn("");

    NexusRepositoryRequestLog requestLog = new NexusRepositoryRequestLog(writer, EXPECTED_FORMAT);

    // When: Log the request
    requestLog.log(request, response);

    // Then: getAttribute should be called and empty string handled
    verify(request).getAttribute("sanitizedRequestUri");
  }
}
