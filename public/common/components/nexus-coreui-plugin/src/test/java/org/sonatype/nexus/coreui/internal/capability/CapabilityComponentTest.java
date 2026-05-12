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
package org.sonatype.nexus.coreui.internal.capability;

import java.util.List;
import java.util.Map;

import org.sonatype.nexus.capability.Capability;
import org.sonatype.nexus.capability.CapabilityContext;
import org.sonatype.nexus.capability.CapabilityDescriptor;
import org.sonatype.nexus.capability.CapabilityDescriptorRegistry;
import org.sonatype.nexus.capability.CapabilityIdentity;
import org.sonatype.nexus.capability.CapabilityReference;
import org.sonatype.nexus.capability.CapabilityRegistry;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.rapture.PasswordPlaceholder;
import org.sonatype.nexus.internal.scheduling.SchedulerCapabilityDescriptor;
import org.sonatype.nexus.repository.capability.StorageSettingsCapabilityDescriptor;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;

import com.google.common.base.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link CapabilityComponent}.
 */
@ExtendWith(MockitoExtension.class)
@ExtendWith(ValidationExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class CapabilityComponentTest
{
  @Mock
  private CapabilityDescriptorRegistry capabilityDescriptorRegistry;

  @Mock
  private CapabilityRegistry capabilityRegistry;

  @Mock
  private Capability capability;

  @Mock
  private CapabilityContext capabilityContext;

  @Mock
  private CapabilityReference capabilityReference;

  @Mock
  private CapabilityIdentity capabilityId;

  @Mock
  private CapabilityDescriptor capabilityDescriptor;

  @Captor
  private ArgumentCaptor<String> stringArgumentCaptor;

  @Captor
  private ArgumentCaptor<Map<String, String>> mapArgumentCaptor;

  private CapabilityComponent underTest;

  @BeforeEach
  void setup() {
    underTest = new CapabilityComponent(capabilityDescriptorRegistry, capabilityRegistry);
    lenient().when(capabilityContext.properties())
        .thenReturn(
            Map.of("username", "username", "password", "its a secret to everybody"));
    lenient().when(capabilityContext.descriptor()).thenReturn(capabilityDescriptor);
    lenient().when(capabilityContext.id()).thenReturn(capabilityId);
    lenient().when(capabilityReference.capability()).thenReturn(capability);
    lenient().when(capabilityReference.context()).thenReturn(capabilityContext);
    lenient().when(capabilityRegistry.get(any(CapabilityIdentity.class))).thenReturn(capabilityReference);
    lenient().when(capabilityRegistry.get(any(Predicate.class))).thenReturn(singletonList(capabilityReference));
    lenient().when(capabilityRegistry.update(any(), anyBoolean(), any(), any())).thenReturn(capabilityReference);
    lenient().when(capabilityDescriptor.type()).thenReturn(mock(CapabilityType.class));
    lenient().when(capability.isPasswordProperty(any())).thenAnswer(i -> "password".equals(i.getArgument(0)));
  }

  @Test
  void testReadPasswordNotCleartext() {
    List<CapabilityXO> capabilities = underTest.read();
    assertThat(capabilities, hasSize(1));
    assertThat(capabilities.get(0).getProperties().get("username"), is("username"));
    assertThat(capabilities.get(0).getProperties().get("password"), is(PasswordPlaceholder.get()));
    verify(capability, times(2)).isPasswordProperty(any());
  }

  @Test
  void testReadPasswordEmptyForPKI() {
    when(capabilityContext.properties()).thenReturn(
        Map.of("username", "username", "password", "its a secret to everybody", "authenticationType", "PKI"));
    List<CapabilityXO> capabilities = underTest.read();

    assertThat(capabilities, hasSize(1));
    assertThat(capabilities.get(0).getProperties().get("password"), is(""));
  }

  @Test
  void testUpdatePlacheholderRetainsCurrentPassword() {
    CapabilityXO capabilityXO = new CapabilityXO();
    capabilityXO.setId("mycap");
    capabilityXO.setProperties(Map.of("username", "username", "password", PasswordPlaceholder.get()));
    capabilityXO.setEnabled(true);

    underTest.update(capabilityXO);

    verify(capabilityRegistry).update(any(), anyBoolean(), any(), mapArgumentCaptor.capture());
    assertThat(mapArgumentCaptor.getValue().get("username"), is("username"));
    assertThat(mapArgumentCaptor.getValue().get("password"), is("its a secret to everybody"));
  }

  @Test
  void shouldSetSystemTrueForSystemCapability() {
    // Given: A scheduler capability (system capability)
    CapabilityType schedulerType = CapabilityType.capabilityType(SchedulerCapabilityDescriptor.TYPE_ID);
    when(capabilityDescriptor.type()).thenReturn(schedulerType);
    when(capabilityDescriptor.name()).thenReturn("Scheduler");
    when(capabilityContext.isEnabled()).thenReturn(true);
    when(capabilityContext.isActive()).thenReturn(true);
    when(capabilityContext.hasFailure()).thenReturn(false);
    when(capabilityContext.stateDescription()).thenReturn("Active");
    when(capabilityContext.notes()).thenReturn("System capability");
    when(capabilityId.toString()).thenReturn("scheduler-id");
    when(capability.isPasswordProperty(any())).thenReturn(false);
    when(capabilityRegistry.get(any(Predicate.class))).thenReturn(singletonList(capabilityReference));

    // When: Reading capabilities
    List<CapabilityXO> results = underTest.read();

    // Then: Should have isSystem = true
    assertThat(results, hasSize(1));
    CapabilityXO result = results.get(0);
    assertThat(result.isSystem(), is(true));
    assertThat(result.getTypeName(), is("Scheduler"));
  }

  @Test
  void shouldSetSystemFalseForRegularCapability() {
    // Given: A regular capability (not system capability)
    CapabilityType regularType = CapabilityType.capabilityType("webhook.repository");
    when(capabilityDescriptor.type()).thenReturn(regularType);
    when(capabilityDescriptor.name()).thenReturn("Repository Webhook");
    when(capabilityContext.isEnabled()).thenReturn(true);
    when(capabilityContext.isActive()).thenReturn(true);
    when(capabilityContext.hasFailure()).thenReturn(false);
    when(capabilityContext.stateDescription()).thenReturn("Active");
    when(capabilityContext.notes()).thenReturn("Regular capability");
    when(capabilityId.toString()).thenReturn("webhook-id");
    when(capability.isPasswordProperty(any())).thenReturn(false);
    when(capabilityRegistry.get(any(Predicate.class))).thenReturn(singletonList(capabilityReference));

    // When: Reading capabilities
    List<CapabilityXO> results = underTest.read();

    // Then: Should have isSystem = false (default)
    assertThat(results, hasSize(1));
    CapabilityXO result = results.get(0);
    assertThat(result.isSystem(), is(false));
    assertThat(result.getTypeName(), is("Repository Webhook"));
  }

  @Test
  void shouldThrowExceptionWhenRemovingSystemCapability() {
    // Given: A scheduler capability (system capability)
    CapabilityType schedulerType = CapabilityType.capabilityType(SchedulerCapabilityDescriptor.TYPE_ID);
    lenient().when(capabilityDescriptor.type()).thenReturn(schedulerType);
    lenient().when(capabilityDescriptor.name()).thenReturn("Scheduler");
    lenient().when(capabilityId.toString()).thenReturn("scheduler-id");

    // When/Then: Attempting to remove should throw exception
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> underTest.remove("scheduler-id"));

    assertThat(exception.getMessage(), is("Cannot delete system capability: Scheduler"));

    // Verify registry.remove was never called
    verify(capabilityRegistry, times(0)).remove(any());
  }

  @Test
  void shouldSucceedWhenRemovingRegularCapability() {
    // Given: A regular capability (not system capability)
    CapabilityType regularType = CapabilityType.capabilityType("webhook.repository");
    lenient().when(capabilityDescriptor.type()).thenReturn(regularType);
    lenient().when(capabilityId.toString()).thenReturn("webhook-id");

    // When: Removing regular capability
    underTest.remove("webhook-id");

    // Then: Should call registry remove
    verify(capabilityRegistry, times(1)).remove(any(CapabilityIdentity.class));
  }

  @Test
  void shouldCallRemoveForNullReference() {
    // Given: No capability reference found
    when(capabilityRegistry.get(any(CapabilityIdentity.class))).thenReturn(null);

    // When: Attempting to remove non-existent capability
    underTest.remove("non-existent-id");

    // Then: Should still call registry remove (let registry handle the error)
    verify(capabilityRegistry, times(1)).remove(any(CapabilityIdentity.class));
  }

  @Test
  void shouldSetSystemTrueForStorageSettingsCapability() {
    // Given: A storage settings capability (system capability)
    CapabilityType storageType = CapabilityType.capabilityType(StorageSettingsCapabilityDescriptor.TYPE_ID);
    when(capabilityDescriptor.type()).thenReturn(storageType);
    when(capabilityDescriptor.name()).thenReturn("Storage Settings");
    when(capabilityContext.isEnabled()).thenReturn(true);
    when(capabilityContext.isActive()).thenReturn(true);
    when(capabilityContext.hasFailure()).thenReturn(false);
    when(capabilityContext.stateDescription()).thenReturn("Active");
    when(capabilityContext.notes()).thenReturn("System capability");
    when(capabilityId.toString()).thenReturn("storage-id");
    when(capability.isPasswordProperty(any())).thenReturn(false);
    when(capabilityRegistry.get(any(Predicate.class))).thenReturn(singletonList(capabilityReference));

    // When: Reading capabilities
    List<CapabilityXO> results = underTest.read();

    // Then: Should have isSystem = true
    assertThat(results, hasSize(1));
    CapabilityXO result = results.get(0);
    assertThat(result.isSystem(), is(true));
    assertThat(result.getTypeName(), is("Storage Settings"));
  }
}
