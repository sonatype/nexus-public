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
package org.sonatype.nexus.security.internal;

import java.util.Set;

import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditRecorder;
import org.sonatype.nexus.security.role.Role;
import org.sonatype.nexus.security.role.RoleCreatedEvent;
import org.sonatype.nexus.security.role.RoleDeletedEvent;
import org.sonatype.nexus.security.role.RoleUpdatedEvent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class RoleAuditorTest
{
  @Mock
  private AuditRecorder auditRecorder;

  @Captor
  private ArgumentCaptor<AuditData> auditDataCaptor;

  private RoleAuditor auditor;

  private Role testRole;

  @Before
  public void setup() {
    auditor = new RoleAuditor();
    auditor.setAuditRecorder(auditRecorder);

    testRole = new Role();
    testRole.setRoleId("test-role");
    testRole.setName("Test Role");
    testRole.setDescription("A test role");
    testRole.setSource("default");
    testRole.setPrivileges(Set.of("nx-all"));
    testRole.setRoles(Set.of("nx-admin"));
  }

  @Test
  public void testRoleCreatedEvent_whenRecording() {
    when(auditRecorder.isEnabled()).thenReturn(true);

    auditor.on(new RoleCreatedEvent(testRole));

    verify(auditRecorder).record(auditDataCaptor.capture());
    AuditData data = auditDataCaptor.getValue();

    assertThat(data.getDomain(), is("security.role"));
    assertThat(data.getType(), is("created"));
    assertThat(data.getContext(), is("test-role"));
    assertThat(data.getAttributes().get("id"), is("test-role"));
    assertThat(data.getAttributes().get("name"), is("Test Role"));
    assertThat(data.getAttributes().get("source"), is("default"));
    assertThat(data.getAttributes().get("privileges"), is("nx-all"));
    assertThat(data.getAttributes().get("roles"), is("nx-admin"));
  }

  @Test
  public void testRoleUpdatedEvent_whenRecording() {
    when(auditRecorder.isEnabled()).thenReturn(true);

    auditor.on(new RoleUpdatedEvent(testRole));

    verify(auditRecorder).record(auditDataCaptor.capture());
    AuditData data = auditDataCaptor.getValue();

    assertThat(data.getDomain(), is("security.role"));
    assertThat(data.getType(), is("updated"));
    assertThat(data.getContext(), is("test-role"));
  }

  @Test
  public void testRoleDeletedEvent_whenRecording() {
    when(auditRecorder.isEnabled()).thenReturn(true);

    auditor.on(new RoleDeletedEvent(testRole));

    verify(auditRecorder).record(auditDataCaptor.capture());
    AuditData data = auditDataCaptor.getValue();

    assertThat(data.getDomain(), is("security.role"));
    assertThat(data.getType(), is("deleted"));
    assertThat(data.getContext(), is("test-role"));
  }

  @Test
  public void testNoAudit_whenNotRecording() {
    when(auditRecorder.isEnabled()).thenReturn(false);

    auditor.on(new RoleCreatedEvent(testRole));

    verify(auditRecorder, never()).record(any());
  }
}
