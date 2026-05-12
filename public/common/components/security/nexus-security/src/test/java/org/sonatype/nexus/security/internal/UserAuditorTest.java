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
import org.sonatype.nexus.security.role.RoleIdentifier;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserCreatedEvent;
import org.sonatype.nexus.security.user.UserDeletedEvent;
import org.sonatype.nexus.security.user.UserStatus;
import org.sonatype.nexus.security.user.UserUpdatedEvent;

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
public class UserAuditorTest
{
  @Mock
  private AuditRecorder auditRecorder;

  @Captor
  private ArgumentCaptor<AuditData> auditDataCaptor;

  private UserAuditor auditor;

  private User testUser;

  @Before
  public void setup() {
    auditor = new UserAuditor();
    auditor.setAuditRecorder(auditRecorder);

    testUser = new User();
    testUser.setUserId("testuser");
    testUser.setFirstName("Test");
    testUser.setLastName("User");
    testUser.setEmailAddress("test@example.com");
    testUser.setSource("default");
    testUser.setStatus(UserStatus.active);
    testUser.setRoles(Set.of(new RoleIdentifier("default", "nx-admin")));
  }

  @Test
  public void testUserCreatedEvent_whenRecording() {
    when(auditRecorder.isEnabled()).thenReturn(true);

    auditor.on(new UserCreatedEvent(testUser));

    verify(auditRecorder).record(auditDataCaptor.capture());
    AuditData data = auditDataCaptor.getValue();

    assertThat(data.getDomain(), is("security.user"));
    assertThat(data.getType(), is("created"));
    assertThat(data.getContext(), is("testuser"));
    assertThat(data.getAttributes().get("id"), is("testuser"));
    assertThat(data.getAttributes().get("name"), is("Test User"));
    assertThat(data.getAttributes().get("email"), is("test@example.com"));
    assertThat(data.getAttributes().get("source"), is("default"));
    assertThat(data.getAttributes().get("status"), is("active"));
    assertThat(data.getAttributes().get("roles"), is("nx-admin"));
  }

  @Test
  public void testUserUpdatedEvent_whenRecording() {
    when(auditRecorder.isEnabled()).thenReturn(true);

    auditor.on(new UserUpdatedEvent(testUser));

    verify(auditRecorder).record(auditDataCaptor.capture());
    AuditData data = auditDataCaptor.getValue();

    assertThat(data.getDomain(), is("security.user"));
    assertThat(data.getType(), is("updated"));
    assertThat(data.getContext(), is("testuser"));
  }

  @Test
  public void testUserDeletedEvent_whenRecording() {
    when(auditRecorder.isEnabled()).thenReturn(true);

    auditor.on(new UserDeletedEvent(testUser));

    verify(auditRecorder).record(auditDataCaptor.capture());
    AuditData data = auditDataCaptor.getValue();

    assertThat(data.getDomain(), is("security.user"));
    assertThat(data.getType(), is("deleted"));
    assertThat(data.getContext(), is("testuser"));
  }

  @Test
  public void testNoAudit_whenNotRecording() {
    when(auditRecorder.isEnabled()).thenReturn(false);

    auditor.on(new UserCreatedEvent(testUser));

    verify(auditRecorder, never()).record(any());
  }
}
