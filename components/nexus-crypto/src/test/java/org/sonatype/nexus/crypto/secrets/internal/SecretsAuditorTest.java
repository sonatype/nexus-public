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
package org.sonatype.nexus.crypto.secrets.internal;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditRecorder;
import org.sonatype.nexus.common.event.EventHelper;
import org.sonatype.nexus.crypto.secrets.ActiveKeyChangeEvent;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SecretsAuditorTest
    extends TestSupport
{
  @Mock
  private AuditRecorder auditRecorder;

  @Spy
  private SecretsAuditor underTest = new SecretsAuditor();

  @Captor
  private ArgumentCaptor<AuditData> captor;

  @Before
  public void setup() {
    underTest.setAuditRecorder(() -> auditRecorder);
  }

  @Test
  public void audit() {
    when(auditRecorder.isEnabled()).thenReturn(true);

    underTest.on(new ActiveKeyChangeEvent("foo", "bar", "baz"));

    verify(auditRecorder, times(1)).record(captor.capture());

    AuditData data = captor.getValue();

    assertThat(data, notNullValue());
    assertThat(data.getContext(), is("system"));
    assertThat(data.getDomain(), is(SecretsAuditor.DOMAIN));
    assertThat(data.getType(), is("changed"));
    assertThat(data.getAttributes(), is(ImmutableMap.of("newKeyId", "foo", "previousKeyId", "bar", "userId", "baz")));
  }

  /*
   * Verifies that if the event originated on another node then the event is not recorded
   */
  @Test
  public void audit_replicatingEventIgnored() {
    when(auditRecorder.isEnabled()).thenReturn(true);

    EventHelper.asReplicating(() -> underTest.on(new ActiveKeyChangeEvent("foo", "bar", "baz")));

    verify(auditRecorder, never()).record(any());
  }

  /*
   * Verifies that when auditing is disabled then the event is not recorded
   */
  @Test
  public void audit_disabled() {
    when(auditRecorder.isEnabled()).thenReturn(false);

    underTest.on(new ActiveKeyChangeEvent("foo", "bar", "baz"));

    verify(auditRecorder, never()).record(any());
  }
}
