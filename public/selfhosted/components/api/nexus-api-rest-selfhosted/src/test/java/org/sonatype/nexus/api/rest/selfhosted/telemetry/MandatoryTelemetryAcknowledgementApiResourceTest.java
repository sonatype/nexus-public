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
package org.sonatype.nexus.api.rest.selfhosted.telemetry;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.sonatype.nexus.api.rest.selfhosted.telemetry.model.MandatoryTelemetryAcknowledgementStatus;
import org.sonatype.nexus.kv.GlobalKeyValueStore;
import org.sonatype.nexus.kv.NexusKeyValue;
import org.sonatype.nexus.kv.ValueType;
import org.sonatype.nexus.rest.ValidationErrorsException;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class MandatoryTelemetryAcknowledgementApiResourceTest
{
  private static final String KEY = "nexus.telemetry.mandatory.acknowledgement";

  @Mock
  private GlobalKeyValueStore mockGlobalKeyValueStore;

  @InjectMocks
  private MandatoryTelemetryAcknowledgementApiResource underTest;

  @Test
  void testGetReportsNotAcknowledgedWhenKeyAbsent() {
    when(mockGlobalKeyValueStore.getKey(anyString())).thenReturn(Optional.empty());

    MandatoryTelemetryAcknowledgementStatus status = underTest.getMandatoryTelemetryAcknowledgement();

    assertFalse(status.isAcknowledgement());
  }

  @Test
  void testGetReportsTheStoredRecord() {
    NexusKeyValue kv = new NexusKeyValue();
    kv.setValue(Map.of("acknowledgement", true));
    when(mockGlobalKeyValueStore.getKey(anyString())).thenReturn(Optional.of(kv));

    MandatoryTelemetryAcknowledgementStatus status = underTest.getMandatoryTelemetryAcknowledgement();

    assertTrue(status.isAcknowledgement());
  }

  @Test
  void testGetToleratesAMalformedRow() {
    Map<String, Object> value = new HashMap<>();
    value.put("acknowledgement", null);
    NexusKeyValue kv = new NexusKeyValue();
    kv.setValue(value);
    when(mockGlobalKeyValueStore.getKey(anyString())).thenReturn(Optional.of(kv));

    MandatoryTelemetryAcknowledgementStatus status = underTest.getMandatoryTelemetryAcknowledgement();

    assertFalse(status.isAcknowledgement());
  }

  @Test
  void testAcknowledgeWritesOnlyTheFlagFlatAndUnderTheRightKey() {
    when(mockGlobalKeyValueStore.getKey(anyString())).thenReturn(Optional.empty());

    underTest.acknowledgeMandatoryTelemetry(acknowledgement(true));

    ArgumentCaptor<NexusKeyValue> captor = ArgumentCaptor.forClass(NexusKeyValue.class);
    verify(mockGlobalKeyValueStore, times(1)).setKey(captor.capture());
    NexusKeyValue written = captor.getValue();

    assertEquals(KEY, written.key());
    assertEquals(ValueType.OBJECT, written.type());

    // Layout regression guard: NexusKeyValue.setValue(Map) writes FLAT, so the flag must be a direct
    // entry. Had the implementation used setValue(Object) this read would return null and the
    // onboarding item's guarded read would silently show the card forever.
    Map<String, Object> value = written.value();
    assertEquals(Boolean.TRUE, value.get("acknowledgement"));
    assertNull(value.get("value"), "value must be flat, not nested under a \"value\" sub-key");

    // The stored record is exactly {"acknowledgement": true} — no user, timestamp, or version.
    assertEquals(1, value.size());
  }

  @Test
  void testAcknowledgeIsIdempotent() {
    NexusKeyValue existing = new NexusKeyValue();
    existing.setValue(Map.of("acknowledgement", true));
    when(mockGlobalKeyValueStore.getKey(anyString())).thenReturn(Optional.of(existing));

    underTest.acknowledgeMandatoryTelemetry(acknowledgement(true));

    verify(mockGlobalKeyValueStore, never()).setKey(any(NexusKeyValue.class));
  }

  @Test
  void testAcknowledgeWritesWhenTheExistingRowIsMalformed() {
    // Not genuinely acknowledged, so a real acknowledgement must overwrite it.
    NexusKeyValue partial = new NexusKeyValue();
    partial.setValue(Map.of("acknowledgement", "true"));
    when(mockGlobalKeyValueStore.getKey(anyString())).thenReturn(Optional.of(partial));

    underTest.acknowledgeMandatoryTelemetry(acknowledgement(true));

    verify(mockGlobalKeyValueStore, times(1)).setKey(any(NexusKeyValue.class));
  }

  @Test
  void testAcknowledgeRejectsFalse() {
    // The record only moves one way, so {"acknowledgement": false} is a client mistake, not a
    // request to revoke. Rejecting before the store is touched keeps the record intact.
    assertThrows(ValidationErrorsException.class,
        () -> underTest.acknowledgeMandatoryTelemetry(acknowledgement(false)));

    verify(mockGlobalKeyValueStore, never()).setKey(any(NexusKeyValue.class));
  }

  @Test
  void testAcknowledgeRejectsAMissingBody() {
    assertThrows(ValidationErrorsException.class, () -> underTest.acknowledgeMandatoryTelemetry(null));

    verify(mockGlobalKeyValueStore, never()).setKey(any(NexusKeyValue.class));
  }

  private static MandatoryTelemetryAcknowledgementStatus acknowledgement(final boolean value) {
    MandatoryTelemetryAcknowledgementStatus status = new MandatoryTelemetryAcknowledgementStatus();
    status.setAcknowledgement(value);
    return status;
  }
}
