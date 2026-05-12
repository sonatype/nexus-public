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
package org.sonatype.nexus.audit.protect;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditRecorder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ProtectConfigAuditorTest
{
  @Mock
  private AuditRecorder auditRecorder;

  @Captor
  private ArgumentCaptor<AuditData> auditDataCaptor;

  private ProtectConfigAuditor auditor;

  @Before
  public void setUp() {
    auditor = new ProtectConfigAuditor();
    auditor.setAuditRecorder(auditRecorder);
    when(auditRecorder.isEnabled()).thenReturn(true);
  }

  @Test
  public void testFirewallProtectionChange() {
    ProtectConfigChangedEvent event = new ProtectConfigChangedEvent(
        "protect.firewall", "protection-level-changed",
        "maven-proxy", "none", "quarantine");

    auditor.on(event);

    verify(auditRecorder).record(auditDataCaptor.capture());
    AuditData data = auditDataCaptor.getValue();
    assertThat(data.getDomain(), is("protect.firewall"));
    assertThat(data.getType(), is("protection-level-changed"));
    assertThat(data.getContext(), is("maven-proxy"));
    assertThat(data.getAttributes().get("from"), is("none"));
    assertThat(data.getAttributes().get("to"), is("quarantine"));
  }

  @Test
  public void testHealthCheckToggle() {
    ProtectConfigChangedEvent event = new ProtectConfigChangedEvent(
        "protect.healthcheck", "healthcheck-toggled",
        "npm-proxy", "disabled", "enabled");

    auditor.on(event);

    verify(auditRecorder).record(auditDataCaptor.capture());
    AuditData data = auditDataCaptor.getValue();
    assertThat(data.getDomain(), is("protect.healthcheck"));
    assertThat(data.getAttributes().get("from"), is("disabled"));
    assertThat(data.getAttributes().get("to"), is("enabled"));
  }
}
