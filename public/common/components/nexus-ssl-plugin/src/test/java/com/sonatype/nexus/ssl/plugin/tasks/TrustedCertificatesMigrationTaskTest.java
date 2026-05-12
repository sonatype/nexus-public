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
package com.sonatype.nexus.ssl.plugin.tasks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class TrustedCertificatesMigrationTaskTest
{
  @Mock
  private TrustedCertificateMigrationService trustedCertificateMigrationService;

  private TrustedCertificatesMigrationTask underTest;

  @Before
  public void setUp() throws Exception {
    underTest = new TrustedCertificatesMigrationTask(trustedCertificateMigrationService);
  }

  @Test
  public void execute() throws Exception {
    underTest.execute();
    verify(trustedCertificateMigrationService).migrate();
  }

  @Test
  public void executesWhenExecuteTaskFails() throws Exception {
    doThrow(new RuntimeException("someError")).when(trustedCertificateMigrationService)
        .migrate();

    assertThrows(RuntimeException.class, () -> underTest.execute());

    verify(trustedCertificateMigrationService).migrate();
  }
}
