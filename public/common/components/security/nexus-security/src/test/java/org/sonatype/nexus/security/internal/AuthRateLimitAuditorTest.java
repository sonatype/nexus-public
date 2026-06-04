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

import java.util.Arrays;

import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditRecorder;
import org.sonatype.nexus.security.authc.AuthRateLimitedEvent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AuthRateLimitAuditorTest
{
  @Mock
  private AuditRecorder auditRecorder;

  @Captor
  private ArgumentCaptor<AuditData> auditDataCaptor;

  private AuthRateLimitAuditor auditor;

  @Before
  public void setUp() {
    auditor = new AuthRateLimitAuditor();
    auditor.setAuditRecorder(auditRecorder);
  }

  @Test
  public void testAuditEvent_whenRecording() {
    when(auditRecorder.isEnabled()).thenReturn(true);

    auditor.on(new AuthRateLimitedEvent("jsmith", 6, 30L, "1.2.3.4", "BASIC"));

    verify(auditRecorder).record(auditDataCaptor.capture());
    AuditData data = auditDataCaptor.getValue();

    assertThat(data.getDomain(), is("security.auth.ratelimit"));
    assertThat(data.getType(), is("blocked"));
    assertThat(data.getContext(), is("jsmith"));
    assertThat(data.getAttributes().get("username"), is("jsmith"));
    assertThat(data.getAttributes().get("attemptCount"), is(6));
    assertThat(data.getAttributes().get("retryAfterSeconds"), is(30L));
    assertThat(data.getAttributes().get("sourceIp"), is("1.2.3.4"));
    assertThat(data.getAttributes().get("authMethod"), is("BASIC"));
  }

  @Test
  public void testAuditEvent_nullIpOmitted() {
    when(auditRecorder.isEnabled()).thenReturn(true);

    auditor.on(new AuthRateLimitedEvent("jsmith", 6, 30L, null, "UI"));

    verify(auditRecorder).record(auditDataCaptor.capture());
    AuditData data = auditDataCaptor.getValue();

    assertThat(data.getAttributes().containsKey("sourceIp"), is(false));
    assertThat(data.getAttributes().get("authMethod"), is("UI"));
  }

  @Test
  public void testNoAudit_whenNotRecording() {
    when(auditRecorder.isEnabled()).thenReturn(false);

    auditor.on(new AuthRateLimitedEvent("jsmith", 6, 30L, "1.2.3.4", "BASIC"));

    verify(auditRecorder, never()).record(any());
  }

  @Test
  public void testClass_hasConditionalOnPropertyForEnabledFlag() {
    ConditionalOnProperty annotation = AuthRateLimitAuditor.class.getAnnotation(ConditionalOnProperty.class);

    assertThat("@ConditionalOnProperty must be present", annotation, is(notNullValue()));
    assertThat(Arrays.asList(annotation.name()), hasItem("nexus.auth.ratelimit.enabled"));
    assertThat(annotation.havingValue(), is("true"));
    assertThat(annotation.matchIfMissing(), is(true));
  }
}
