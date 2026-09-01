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

import org.sonatype.nexus.cleanup.service.CleanupService;
import org.sonatype.nexus.scheduling.RecoveryModeService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@RunWith(MockitoJUnitRunner.Silent.class)
public class CleanupTaskTest
{
  @Mock
  private CleanupService cleanupService;

  @Mock
  private RecoveryModeService recoveryModeService;

  private CleanupTask underTest;

  @Before
  public void setup() throws Exception {
    underTest = new CleanupTask(cleanupService, recoveryModeService);
  }

  @Test(expected = NullPointerException.class)
  public void constructorRejectsNullCleanupService() {
    new CleanupTask(null, recoveryModeService);
  }

  @Test
  public void runCleanup() throws Exception {
    Object result = underTest.execute();

    assertThat(result, is(nullValue()));

    ArgumentCaptor<BooleanSupplier> cancelCheckCaptor = ArgumentCaptor.forClass(BooleanSupplier.class);
    verify(cleanupService).cleanup(cancelCheckCaptor.capture());

    // the supplier passed to cleanup is wired to this::isCanceled, so it reflects the live cancellation state
    BooleanSupplier cancelCheck = cancelCheckCaptor.getValue();
    assertThat(cancelCheck.getAsBoolean(), is(false));
    underTest.cancel();
    assertThat(cancelCheck.getAsBoolean(), is(true));
  }

  @Test
  public void runCleanupLogsCancellationWhenCanceled() throws Exception {
    CleanupTask spy = spy(underTest);
    doReturn(true).when(spy).isCanceled();

    Object result = spy.execute();

    assertThat(result, is(nullValue()));

    ArgumentCaptor<BooleanSupplier> cancelCheckCaptor = ArgumentCaptor.forClass(BooleanSupplier.class);
    verify(cleanupService).cleanup(cancelCheckCaptor.capture());

    // the supplier delegates to isCanceled(), which is stubbed to true for the canceled branch
    assertThat(cancelCheckCaptor.getValue().getAsBoolean(), is(true));
  }

  @Test
  public void getMessageReturnsExpectedDescription() {
    assertThat(underTest.getMessage(), is("Run repository cleanup"));
  }

  @Test
  public void executeThrowsWhenRecoveryModeActive() {
    IllegalStateException expected = new IllegalStateException("recovery mode is enabled");
    org.mockito.Mockito.doThrow(expected)
        .when(recoveryModeService)
        .ensureNotInRecoveryMode(org.mockito.ArgumentMatchers.any());

    IllegalStateException actual = assertThrows(IllegalStateException.class, () -> underTest.execute());
    assertThat(actual, is(expected));
    verifyNoInteractions(cleanupService);
  }

  @Test
  public void executeToleratesNullRecoveryModeService() throws Exception {
    CleanupTask task = new CleanupTask(cleanupService, null);
    Object result = task.execute();
    assertThat(result, is(nullValue()));
    verify(cleanupService).cleanup(any(BooleanSupplier.class));
  }
}
