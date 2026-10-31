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
package org.sonatype.nexus.script.plugin.internal.rest;

import java.util.Map;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.script.ScriptService;
import org.sonatype.nexus.script.Script;
import org.sonatype.nexus.script.ScriptManager;
import org.sonatype.nexus.script.ScriptResultXO;
import org.sonatype.nexus.script.plugin.internal.security.ScriptPermission;
import org.sonatype.nexus.security.SecurityHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ScriptResource}.
 */
@ExtendWith(MockitoExtension.class)
class ScriptResourceTest
{
  private static final String SCRIPT_NAME = "test-script";

  private static final String SCRIPT_CONTENT = "return 'test'";

  private static final String SCRIPT_TYPE = "groovy";

  private static final String SCRIPT_ARGS = "test-args";

  @Mock
  private ScriptManager scriptManager;

  @Mock
  private SecurityHelper securityHelper;

  @Mock
  private ScriptService scriptService;

  @Mock
  private EventManager eventManager;

  @Mock
  private Script mockScript;

  private ScriptResource underTest;

  @BeforeEach
  void setup() {
    underTest = new ScriptResource(scriptManager, securityHelper, scriptService, eventManager);

    lenient().when(mockScript.getName()).thenReturn(SCRIPT_NAME);
    lenient().when(mockScript.getContent()).thenReturn(SCRIPT_CONTENT);
    lenient().when(mockScript.getType()).thenReturn(SCRIPT_TYPE);
  }

  @Test
  @SuppressWarnings("unchecked")
  void testRunScriptSucceedsWhenEnabled() throws Exception {
    when(scriptManager.isEnabled()).thenReturn(true);
    when(scriptManager.get(SCRIPT_NAME)).thenReturn(mockScript);
    when(scriptService.eval(eq(SCRIPT_TYPE), eq(SCRIPT_CONTENT), any(Map.class))).thenReturn("result");

    ScriptResultXO result = underTest.run(SCRIPT_NAME, SCRIPT_ARGS);

    assertThat(result.getName(), is(SCRIPT_NAME));
    assertThat(result.getResult(), is("result"));
    verify(scriptService).eval(eq(SCRIPT_TYPE), eq(SCRIPT_CONTENT), any(Map.class));
    verify(eventManager).post(any());
  }

  @Test
  void testRunScriptNotFound() {
    when(scriptManager.isEnabled()).thenReturn(true);
    when(scriptManager.get(SCRIPT_NAME)).thenReturn(null);

    WebApplicationException exception = assertThrows(
        WebApplicationException.class,
        () -> underTest.run(SCRIPT_NAME, SCRIPT_ARGS));

    assertThat(exception.getResponse().getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testRunScriptExecutionFailure() throws Exception {
    when(scriptManager.isEnabled()).thenReturn(true);
    when(scriptManager.get(SCRIPT_NAME)).thenReturn(mockScript);
    when(scriptService.eval(eq(SCRIPT_TYPE), eq(SCRIPT_CONTENT), any(Map.class)))
        .thenThrow(new RuntimeException("Script execution error"));

    WebApplicationException exception = assertThrows(
        WebApplicationException.class,
        () -> underTest.run(SCRIPT_NAME, SCRIPT_ARGS));

    assertThat(exception.getResponse().getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));

    ScriptResultXO result = (ScriptResultXO) exception.getResponse().getEntity();
    assertThat(result.getName(), is(SCRIPT_NAME));
    assertThat(result.getResult(), is("Script execution error"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testRunScriptExecutionFailureWithCheckedException() throws Exception {
    when(scriptManager.isEnabled()).thenReturn(true);
    when(scriptManager.get(SCRIPT_NAME)).thenReturn(mockScript);
    when(scriptService.eval(eq(SCRIPT_TYPE), eq(SCRIPT_CONTENT), any(Map.class)))
        .thenThrow(new javax.script.ScriptException("Script compilation error"));

    WebApplicationException exception = assertThrows(
        WebApplicationException.class,
        () -> underTest.run(SCRIPT_NAME, SCRIPT_ARGS));

    assertThat(exception.getResponse().getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));

    ScriptResultXO result = (ScriptResultXO) exception.getResponse().getEntity();
    assertThat(result.getName(), is(SCRIPT_NAME));
    assertThat(result.getResult(), is("Script compilation error"));
  }

  @Test
  void testRunScriptReturns410WhenDisabled() throws Exception {
    when(scriptManager.isEnabled()).thenReturn(false);
    // Explicitly stub get() to show we're testing blocking an existing script
    lenient().when(scriptManager.get(SCRIPT_NAME)).thenReturn(mockScript);

    WebApplicationException exception = assertThrows(
        WebApplicationException.class,
        () -> underTest.run(SCRIPT_NAME, SCRIPT_ARGS));

    assertThat(exception.getResponse().getStatus(), is(Response.Status.GONE.getStatusCode()));

    ScriptResultXO result = (ScriptResultXO) exception.getResponse().getEntity();
    assertThat(result.getName(), is(SCRIPT_NAME));
    assertThat(result.getResult(), is("Script execution is disabled"));

    verify(scriptService, never()).eval(any(), any(), any(Map.class));
    verify(eventManager, never()).post(any());
  }

  @Test
  void testRunScriptSecurityCheckTakesPrecedenceOverFeatureFlag() {
    org.apache.shiro.authz.AuthorizationException authException =
        new org.apache.shiro.authz.AuthorizationException("User lacks required permission");

    doThrow(authException).when(securityHelper).ensurePermitted(any(ScriptPermission.class));

    org.apache.shiro.authz.AuthorizationException exception = assertThrows(
        org.apache.shiro.authz.AuthorizationException.class,
        () -> underTest.run(SCRIPT_NAME, SCRIPT_ARGS));

    assertThat(exception.getMessage(), is("User lacks required permission"));

    verify(scriptManager, never()).isEnabled();
  }
}
