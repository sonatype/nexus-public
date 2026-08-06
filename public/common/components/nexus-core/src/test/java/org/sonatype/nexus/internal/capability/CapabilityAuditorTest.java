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
package org.sonatype.nexus.internal.capability;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditRecorder;
import org.sonatype.nexus.capability.CapabilityContext;
import org.sonatype.nexus.capability.CapabilityDescriptor;
import org.sonatype.nexus.capability.CapabilityEvent;
import org.sonatype.nexus.capability.CapabilityIdentity;
import org.sonatype.nexus.capability.CapabilityReference;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link CapabilityAuditor}.
 */
@ExtendWith({MockitoExtension.class, AuthenticationExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class CapabilityAuditorTest
{
  private CapabilityAuditor underTest;

  @Mock
  private AuditRecorder auditRecorder;

  @Mock
  private CapabilityReference reference;

  @Mock
  private CapabilityContext context;

  @Mock
  private CapabilityDescriptor descriptor;

  @Mock
  private CapabilityIdentity identity;

  @BeforeEach
  void setUp() {
    underTest = new CapabilityAuditor();
    underTest.setAuditRecorder(auditRecorder);

    when(reference.context()).thenReturn(context);
    when(context.descriptor()).thenReturn(descriptor);
    when(context.id()).thenReturn(identity);
    when(context.type()).thenReturn(CapabilityType.capabilityType("test"));
    when(context.isEnabled()).thenReturn(true);
    when(context.isActive()).thenReturn(true);
    when(context.hasFailure()).thenReturn(false);
    when(context.properties()).thenReturn(Collections.emptyMap());
    when(descriptor.formFields()).thenReturn(Collections.emptyList());
    when(identity.toString()).thenReturn("test-id");
    when(auditRecorder.isEnabled()).thenReturn(true);
  }

  @Test
  void testOnEvent_WhenRecordingDisabled_SkipsAuditing() {
    // Given: Recording is disabled
    when(auditRecorder.isEnabled()).thenReturn(false);

    // When: An event is fired
    CapabilityEvent event = new CapabilityEvent.Created(reference);
    underTest.on(event);

    // Then: No audit record should be created
    verify(auditRecorder, never()).record(any(AuditData.class));
  }

  @Test
  void testOnEvent_WhenInitiatorUnknown_SkipsAuditing() {
    // Given: No @WithUser — no subject is bound (simulates startup/shutdown, no user context).
    // AuthenticationExtension still binds a SecurityManager so UserIdHelper.isUnknown() works.

    // When: An event is fired
    CapabilityEvent event = new CapabilityEvent.Created(reference);
    underTest.on(event);

    // Then: No audit record should be created (system-initiated events are not audited)
    verify(auditRecorder, never()).record(any(AuditData.class));
  }

  @Test
  @WithUser("test-user")
  void testOnEvent_WhenInitiatorKnown_RecordsAudit() {
    // Given: @WithUser binds a Shiro subject with principal "test-user"

    // When: An event is fired
    CapabilityEvent event = new CapabilityEvent.Created(reference);
    underTest.on(event);

    // Then: An audit record should be created
    verify(auditRecorder).record(any(AuditData.class));
  }

  @Test
  @WithUser("*SYSTEM")
  void testOnEvent_WhenInitiatorSystem_SkipsAuditing() {
    // Given: Subject principal is "*SYSTEM" (privileged system execution — e.g. background
    // maintenance, internal reconciliation). These are not user-initiated and should not
    // pollute the audit log.

    // When: An event is fired
    CapabilityEvent event = new CapabilityEvent.Created(reference);
    underTest.on(event);

    // Then: No audit record should be created
    verify(auditRecorder, never()).record(any(AuditData.class));
  }
}
