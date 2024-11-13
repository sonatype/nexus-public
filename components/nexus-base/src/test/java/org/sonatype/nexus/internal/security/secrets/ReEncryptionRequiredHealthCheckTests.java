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
package org.sonatype.nexus.internal.security.secrets;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.crypto.secrets.SecretsService;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskScheduler;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class ReEncryptionRequiredHealthCheckTests
    extends TestSupport
{
  @Mock
  private SecretsService secretsService;

  @Mock
  private TaskScheduler taskScheduler;

  @Mock
  private TaskInfo taskInfo;

  @InjectMocks
  private ReEncryptionRequiredHealthCheck underTest;

  @Test
  public void testHealthy() throws Exception {
    when(taskScheduler.getTaskByTypeId(anyString())).thenReturn(null);
    when(secretsService.isReEncryptRequired()).thenReturn(false);
    assertThat(underTest.check().isHealthy()).isTrue();

    // when task is running should return healthy
    when(taskScheduler.getTaskByTypeId(anyString())).thenReturn(taskInfo);
    when(secretsService.isReEncryptRequired()).thenReturn(true);
    assertThat(underTest.check().isHealthy()).isTrue();
  }

  @Test
  public void testUnhealthy() throws Exception {
    when(taskScheduler.getTaskByTypeId(anyString())).thenReturn(null);
    when(secretsService.isReEncryptRequired()).thenReturn(true);
    assertThat(underTest.check().isHealthy()).isFalse();
  }
}
