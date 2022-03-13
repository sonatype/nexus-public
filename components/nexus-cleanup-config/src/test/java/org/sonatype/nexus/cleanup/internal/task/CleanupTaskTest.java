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
package org.sonatype.nexus.cleanup.internal.task;

import java.util.function.BooleanSupplier;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.cleanup.service.CleanupService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

public class CleanupTaskTest
    extends TestSupport
{
  @Mock
  private CleanupService cleanupService;

  private CleanupTask underTest;

  @Before
  public void setup() throws Exception {
    underTest = new CleanupTask(cleanupService);
  }

  @Test
  public void runCleanup() throws Exception {
    underTest.execute();

    verify(cleanupService).cleanup(any(BooleanSupplier.class));
  }
}
