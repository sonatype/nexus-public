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
package org.sonatype.nexus.internal.script;

import java.util.Map;

import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditRecorder;
import org.sonatype.nexus.script.Script;
import org.sonatype.nexus.script.ScriptCreatedEvent;
import org.sonatype.nexus.script.ScriptDeletedEvent;
import org.sonatype.nexus.script.ScriptEvent;
import org.sonatype.nexus.script.ScriptRunEvent;
import org.sonatype.nexus.script.ScriptUpdatedEvent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ScriptAuditorTest
{
  private static final String SCRIPT_NAME = "my-script";

  private static final String SCRIPT_TYPE = "groovy";

  @Mock
  private AuditRecorder auditRecorder;

  @Mock
  private Script script;

  private ScriptAuditor underTest;

  @Before
  public void setup() {
    underTest = new ScriptAuditor();
    underTest.setAuditRecorder(auditRecorder);
  }

  @Test
  public void domainConstantIsScript() {
    assertEquals("script", ScriptAuditor.DOMAIN);
  }

  @Test
  public void onDoesNotRecordWhenRecordingIsDisabled() {
    when(auditRecorder.isEnabled()).thenReturn(false);

    underTest.on(new ScriptCreatedEvent(script));

    verify(auditRecorder, never()).record(any(AuditData.class));
  }

  @Test
  public void onRecordsCreatedEvent() {
    AuditData data = recordAndCapture(new ScriptCreatedEvent(script));

    assertEquals(ScriptAuditor.DOMAIN, data.getDomain());
    assertEquals("created", data.getType());
    assertEquals(SCRIPT_NAME, data.getContext());

    Map<String, Object> attributes = data.getAttributes();
    assertEquals(2, attributes.size());
    assertEquals(SCRIPT_NAME, attributes.get("name"));
    assertEquals(SCRIPT_TYPE, attributes.get("type"));
  }

  @Test
  public void onRecordsUpdatedEvent() {
    AuditData data = recordAndCapture(new ScriptUpdatedEvent(script));

    assertEquals(ScriptAuditor.DOMAIN, data.getDomain());
    assertEquals("updated", data.getType());
    assertEquals(SCRIPT_NAME, data.getContext());

    Map<String, Object> attributes = data.getAttributes();
    assertEquals(2, attributes.size());
    assertEquals(SCRIPT_NAME, attributes.get("name"));
    assertEquals(SCRIPT_TYPE, attributes.get("type"));
  }

  @Test
  public void onRecordsDeletedEvent() {
    AuditData data = recordAndCapture(new ScriptDeletedEvent(script));

    assertEquals(ScriptAuditor.DOMAIN, data.getDomain());
    assertEquals("deleted", data.getType());
    assertEquals(SCRIPT_NAME, data.getContext());

    Map<String, Object> attributes = data.getAttributes();
    assertEquals(2, attributes.size());
    assertEquals(SCRIPT_NAME, attributes.get("name"));
    assertEquals(SCRIPT_TYPE, attributes.get("type"));
  }

  @Test
  public void onRecordsRunEventWithDerivedType() {
    AuditData data = recordAndCapture(new ScriptRunEvent(script));

    assertEquals(ScriptAuditor.DOMAIN, data.getDomain());
    // ScriptRunEvent is intentionally not registered via registerType(), so
    // AuditorSupport.type() falls back to the lower-cased simple class name.
    assertEquals("scriptrunevent", data.getType());
    assertEquals(SCRIPT_NAME, data.getContext());

    Map<String, Object> attributes = data.getAttributes();
    assertEquals(2, attributes.size());
    assertEquals(SCRIPT_NAME, attributes.get("name"));
    assertEquals(SCRIPT_TYPE, attributes.get("type"));
  }

  private AuditData recordAndCapture(final ScriptEvent event) {
    when(auditRecorder.isEnabled()).thenReturn(true);
    when(script.getName()).thenReturn(SCRIPT_NAME);
    when(script.getType()).thenReturn(SCRIPT_TYPE);

    underTest.on(event);

    ArgumentCaptor<AuditData> captor = ArgumentCaptor.forClass(AuditData.class);
    verify(auditRecorder).record(captor.capture());
    return captor.getValue();
  }
}
