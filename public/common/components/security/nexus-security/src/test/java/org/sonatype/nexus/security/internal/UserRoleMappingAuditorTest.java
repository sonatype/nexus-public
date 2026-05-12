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
import org.sonatype.nexus.security.user.UserRoleMappingCreatedEvent;
import org.sonatype.nexus.security.user.UserRoleMappingDeletedEvent;
import org.sonatype.nexus.security.user.UserRoleMappingUpdatedEvent;

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
public class UserRoleMappingAuditorTest
{
  @Mock
  private AuditRecorder auditRecorder;

  @Captor
  private ArgumentCaptor<AuditData> auditDataCaptor;

  private UserRoleMappingAuditor auditor;

  private static final String USER_ID = "testuser";

  private static final String USER_SOURCE = "default";

  private static final Set<String> ROLES = Set.of("nx-admin", "nx-developer");

  @Before
  public void setup() {
    auditor = new UserRoleMappingAuditor();
    auditor.setAuditRecorder(auditRecorder);
  }

  @Test
  public void testUserRoleMappingCreatedEvent_whenRecording() {
    when(auditRecorder.isEnabled()).thenReturn(true);

    auditor.on(new UserRoleMappingCreatedEvent(USER_ID, USER_SOURCE, ROLES));

    verify(auditRecorder).record(auditDataCaptor.capture());
    AuditData data = auditDataCaptor.getValue();

    assertThat(data.getDomain(), is("security.user-role-mapping"));
    assertThat(data.getType(), is("created"));
    assertThat(data.getContext(), is(USER_ID));
    assertThat(data.getAttributes().get("id"), is(USER_ID));
    assertThat(data.getAttributes().get("source"), is(USER_SOURCE));
  }

  @Test
  public void testUserRoleMappingUpdatedEvent_whenRecording() {
    when(auditRecorder.isEnabled()).thenReturn(true);

    auditor.on(new UserRoleMappingUpdatedEvent(USER_ID, USER_SOURCE, ROLES));

    verify(auditRecorder).record(auditDataCaptor.capture());
    AuditData data = auditDataCaptor.getValue();

    assertThat(data.getDomain(), is("security.user-role-mapping"));
    assertThat(data.getType(), is("updated"));
    assertThat(data.getContext(), is(USER_ID));
  }

  @Test
  public void testUserRoleMappingDeletedEvent_whenRecording() {
    when(auditRecorder.isEnabled()).thenReturn(true);

    auditor.on(new UserRoleMappingDeletedEvent(USER_ID, USER_SOURCE));

    verify(auditRecorder).record(auditDataCaptor.capture());
    AuditData data = auditDataCaptor.getValue();

    assertThat(data.getDomain(), is("security.user-role-mapping"));
    assertThat(data.getType(), is("deleted"));
    assertThat(data.getContext(), is(USER_ID));
  }

  @Test
  public void testNoAudit_whenNotRecording() {
    when(auditRecorder.isEnabled()).thenReturn(false);

    auditor.on(new UserRoleMappingCreatedEvent(USER_ID, USER_SOURCE, ROLES));

    verify(auditRecorder, never()).record(any());
  }
}
