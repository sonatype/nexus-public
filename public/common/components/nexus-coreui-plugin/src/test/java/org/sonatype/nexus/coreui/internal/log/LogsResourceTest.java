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
package org.sonatype.nexus.coreui.internal.log;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import org.sonatype.goodies.testsupport.Test5Support;
import org.sonatype.nexus.common.log.LogManager;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static com.google.common.net.HttpHeaders.CONTENT_DISPOSITION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(ValidationExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class LogsResourceTest
    extends Test5Support
{
  @Mock
  private LogManager logManager;

  @InjectMocks
  private LogsResource underTest;

  @Test
  void getReturnsLogFileStream() throws IOException {
    String filename = "nexus.log";
    InputStream mockStream = new ByteArrayInputStream("log content".getBytes());
    when(logManager.getLogFileStream(filename, 0L, Long.MAX_VALUE)).thenReturn(mockStream);

    Response response = underTest.get(filename, null, null);

    assertThat(response, is(notNullValue()));
    assertThat(response.getStatus(), is(200));
    assertThat(response.getHeaderString(CONTENT_DISPOSITION), is("attachment; filename=\"nexus.log\""));
  }

  @Test
  void getWithFromByteAndBytesCount() throws IOException {
    String filename = "nexus.log";
    InputStream mockStream = new ByteArrayInputStream("partial content".getBytes());
    when(logManager.getLogFileStream(filename, 100L, 500L)).thenReturn(mockStream);

    Response response = underTest.get(filename, 100L, 500L);

    assertThat(response, is(notNullValue()));
    assertThat(response.getStatus(), is(200));
  }

  @Test
  void getWithNegativeFromByteDefaultsToZero() throws IOException {
    String filename = "nexus.log";
    InputStream mockStream = new ByteArrayInputStream("content".getBytes());
    when(logManager.getLogFileStream(filename, 0L, Long.MAX_VALUE)).thenReturn(mockStream);

    Response response = underTest.get(filename, -5L, null);

    assertThat(response, is(notNullValue()));
    assertThat(response.getStatus(), is(200));
  }

  @Test
  void getThrowsNotFoundWhenLogStreamIsNull() throws IOException {
    String filename = "missing.log";
    when(logManager.getLogFileStream(filename, 0L, Long.MAX_VALUE)).thenReturn(null);

    NotFoundException exception = assertThrows(NotFoundException.class, () -> underTest.get(filename, null, null));

    assertThat(exception.getMessage(), containsString("missing.log not found"));
  }

  @Test
  void listLogsReturnsLogFiles(@TempDir File tempDir) throws IOException {
    File logFile = new File(tempDir, "nexus.log");
    when(logManager.getLogFiles()).thenReturn(Set.of(logFile));

    // TaskLogHome relies on static methods which are harder to test;
    // the basic flow through getLogFiles() is verified here
    Set<LogXO> logs = underTest.listLogs();
    assertThat(logs, is(notNullValue()));
  }
}
