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
package org.sonatype.nexus.internal.jwt;

import java.time.OffsetDateTime;

import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditRecorder;
import org.sonatype.nexus.security.JwtHelper;
import org.sonatype.nexus.security.jwt.JwtSessionRevocationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtSessionInvalidatorImplTest
{
  @Mock
  private JwtSessionRevocationService revocationService;

  @Mock
  private JwtHelper jwtHelper;

  @Mock
  private AuditRecorder auditRecorder;

  private JwtSessionInvalidatorImpl underTest;

  @BeforeEach
  void setup() {
    when(jwtHelper.getExpirySeconds()).thenReturn(1800);
    underTest = new JwtSessionInvalidatorImpl(revocationService, jwtHelper, auditRecorder);
  }

  @Test
  void invalidate_callsInvalidateUserOnRevocationService_withNowAndNowPlusJwtLifetime() {
    OffsetDateTime before = OffsetDateTime.now();

    int count = underTest.invalidateSessionsForUser("alice", "default");

    assertThat(count, is(1));

    ArgumentCaptor<OffsetDateTime> cutoffCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
    ArgumentCaptor<OffsetDateTime> validUntilCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
    verify(revocationService).invalidateUser(
        eq("alice"), eq("default"), cutoffCaptor.capture(), validUntilCaptor.capture());

    OffsetDateTime cutoff = cutoffCaptor.getValue();
    OffsetDateTime validUntil = validUntilCaptor.getValue();
    OffsetDateTime after = OffsetDateTime.now();

    assertThat(!cutoff.isBefore(before), is(true));
    assertThat(!cutoff.isAfter(after), is(true));
    assertThat(validUntil.isAfter(cutoff), is(true));
    // validUntil ≈ cutoff + 1800s (within a small skew)
    long gapSeconds = java.time.Duration.between(cutoff, validUntil).getSeconds();
    assertThat(gapSeconds, is(1800L));
  }

  @Test
  void invalidate_auditRecorded_whenSuccess() {
    when(auditRecorder.isEnabled()).thenReturn(true);

    underTest.invalidateSessionsForUser("alice", "default");

    ArgumentCaptor<AuditData> captor = ArgumentCaptor.forClass(AuditData.class);
    verify(auditRecorder).record(captor.capture());

    AuditData data = captor.getValue();
    assertThat(data.getDomain(), is("security.session"));
    assertThat(data.getType(), is("password-change-invalidation"));
    assertThat(data.getAttributes().get("username"), is("alice"));
    assertThat(data.getAttributes().get("sessionType"), is("jwt"));
    assertThat(data.getAttributes().get("sessionCount"), is("1"));
  }

  @Test
  void invalidate_auditNotRecorded_whenAuditDisabled() {
    when(auditRecorder.isEnabled()).thenReturn(false);

    underTest.invalidateSessionsForUser("alice", "default");

    verify(auditRecorder, never()).record(any(AuditData.class));
  }

  @Test
  void invalidate_serviceThrows_returnsZero_andDoesNotAudit() {
    doThrow(new RuntimeException("boom")).when(revocationService)
        .invalidateUser(anyString(), anyString(), any(), any());
    when(auditRecorder.isEnabled()).thenReturn(true);

    int count = underTest.invalidateSessionsForUser("alice", "default");

    assertThat(count, is(0));
    verify(auditRecorder, never()).record(any(AuditData.class));
  }
}
