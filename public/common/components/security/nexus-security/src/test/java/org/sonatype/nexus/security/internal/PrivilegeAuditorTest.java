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

import java.util.Map;

import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditRecorder;
import org.sonatype.nexus.security.privilege.Privilege;
import org.sonatype.nexus.security.privilege.PrivilegeCreatedEvent;
import org.sonatype.nexus.security.privilege.PrivilegeDeletedEvent;
import org.sonatype.nexus.security.privilege.PrivilegeUpdatedEvent;

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
public class PrivilegeAuditorTest
{
  @Mock
  private AuditRecorder auditRecorder;

  @Captor
  private ArgumentCaptor<AuditData> auditDataCaptor;

  private PrivilegeAuditor auditor;

  private Privilege testPrivilege;

  @Before
  public void setup() {
    auditor = new PrivilegeAuditor();
    auditor.setAuditRecorder(auditRecorder);

    testPrivilege = new Privilege();
    testPrivilege.setId("test-privilege");
    testPrivilege.setName("Test Privilege");
    testPrivilege.setDescription("A test privilege");
    testPrivilege.setType("application");
    testPrivilege.setProperties(Map.of("domain", "repository-admin", "actions", "read,browse"));
  }

  @Test
  public void testPrivilegeCreatedEvent_whenRecording() {
    when(auditRecorder.isEnabled()).thenReturn(true);

    auditor.on(new PrivilegeCreatedEvent(testPrivilege));

    verify(auditRecorder).record(auditDataCaptor.capture());
    AuditData data = auditDataCaptor.getValue();

    assertThat(data.getDomain(), is("security.privilege"));
    assertThat(data.getType(), is("created"));
    assertThat(data.getContext(), is("test-privilege"));
    assertThat(data.getAttributes().get("id"), is("test-privilege"));
    assertThat(data.getAttributes().get("name"), is("Test Privilege"));
    assertThat(data.getAttributes().get("type"), is("application"));
    assertThat(data.getAttributes().get("property.domain"), is("repository-admin"));
    assertThat(data.getAttributes().get("property.actions"), is("read,browse"));
  }

  @Test
  public void testPrivilegeUpdatedEvent_whenRecording() {
    when(auditRecorder.isEnabled()).thenReturn(true);

    auditor.on(new PrivilegeUpdatedEvent(testPrivilege));

    verify(auditRecorder).record(auditDataCaptor.capture());
    AuditData data = auditDataCaptor.getValue();

    assertThat(data.getDomain(), is("security.privilege"));
    assertThat(data.getType(), is("updated"));
    assertThat(data.getContext(), is("test-privilege"));
  }

  @Test
  public void testPrivilegeDeletedEvent_whenRecording() {
    when(auditRecorder.isEnabled()).thenReturn(true);

    auditor.on(new PrivilegeDeletedEvent(testPrivilege));

    verify(auditRecorder).record(auditDataCaptor.capture());
    AuditData data = auditDataCaptor.getValue();

    assertThat(data.getDomain(), is("security.privilege"));
    assertThat(data.getType(), is("deleted"));
    assertThat(data.getContext(), is("test-privilege"));
  }

  @Test
  public void testNoAudit_whenNotRecording() {
    when(auditRecorder.isEnabled()).thenReturn(false);

    auditor.on(new PrivilegeCreatedEvent(testPrivilege));

    verify(auditRecorder, never()).record(any());
  }
}
