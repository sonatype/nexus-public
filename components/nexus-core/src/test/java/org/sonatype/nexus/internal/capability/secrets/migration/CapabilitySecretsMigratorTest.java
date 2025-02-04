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
package org.sonatype.nexus.internal.capability.secrets.migration;

import java.util.ArrayList;
import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.capability.CapabilityContext;
import org.sonatype.nexus.capability.CapabilityDescriptor;
import org.sonatype.nexus.capability.CapabilityReference;
import org.sonatype.nexus.capability.CapabilityRegistry;
import org.sonatype.nexus.formfields.PasswordFormField;
import org.sonatype.nexus.scheduling.CancelableHelper;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CapabilitySecretsMigratorTest
    extends TestSupport
{
  @Mock
  private CapabilityRegistry capabilityRegistry;

  private MockedStatic<CancelableHelper> cancelableHelperMock;

  private CapabilitySecretsMigrator underTest;

  private int canceledStateCheckCount = 0;

  @Before
  public void setUp() {
    underTest = new CapabilitySecretsMigrator(capabilityRegistry);
    cancelableHelperMock = mockStatic(CancelableHelper.class);
  }

  @Test
  public void testMigrationWorksAsExpected() {
    List<CapabilityReference> capabilityReferences = mockCapabilities(10, 5);
    doReturn(capabilityReferences).when(capabilityRegistry).getAll();

    underTest.migrate();

    verify(capabilityRegistry, times(5)).migrateSecrets(any(CapabilityReference.class), any());
  }

  @Test
  public void testMigrationStopsIfCancelled() {
    simulateTaskInterrupted(6);
    List<CapabilityReference> capabilityReferences = mockCapabilities(20, 10);
    doReturn(capabilityReferences).when(capabilityRegistry).getAll();

    TaskInterruptedException expected = assertThrows(TaskInterruptedException.class, underTest::migrate);

    assertNotNull(expected);
    verify(capabilityRegistry, times(6)).migrateSecrets(any(CapabilityReference.class), any());
  }

  private void simulateTaskInterrupted(final int targetCheck) {
    cancelableHelperMock.when(CancelableHelper::checkCancellation)
        .thenAnswer(i -> {
          canceledStateCheckCount++;
          // force failure after we checked targetCheck times
          if (canceledStateCheckCount > targetCheck) {
            throw new TaskInterruptedException("expected", true);
          }

          return false;
        });
  }

  @After
  public void tearDown() {
    cancelableHelperMock.close();
    canceledStateCheckCount = 0;
  }

  private List<CapabilityReference> mockCapabilities(final int size, final int encryptedFieldsCapabilities) {
    List<CapabilityReference> references = new ArrayList<>();

    for (int i = 0; i < size; i++) {
      CapabilityReference reference;
      if (i < encryptedFieldsCapabilities) {
        reference = mockCapabilityReference(true);
      }
      else {
        reference = mockCapabilityReference(false);
      }
      references.add(reference);
    }

    return references;
  }

  private CapabilityReference mockCapabilityReference(boolean hasEncryptedField) {
    CapabilityReference reference = mock(CapabilityReference.class);
    CapabilityContext context = mock(CapabilityContext.class);
    CapabilityDescriptor descriptor = mock(CapabilityDescriptor.class);

    when(reference.context()).thenReturn(context);
    when(context.descriptor()).thenReturn(descriptor);

    if (hasEncryptedField) {
      when(descriptor.formFields()).thenReturn(
          ImmutableList.of(new PasswordFormField("test", "test", "?", true)));
    }
    else {
      when(descriptor.formFields()).thenReturn(ImmutableList.of());
    }

    return reference;
  }
}
