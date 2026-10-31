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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

import org.sonatype.nexus.common.app.GlobalComponentLookupHelper;
import org.sonatype.nexus.common.script.ScriptApi;
import org.sonatype.nexus.common.script.ScriptCleanupHandler;
import org.sonatype.nexus.common.script.ScriptService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ScriptServiceImplTest
{
  @Mock
  private ScriptEngineManager engineManager;

  @Test
  public void groovyScriptEngineIsReturnedCorrectly() {
    ScriptService scriptService = createScriptService(true);
    ScriptEngine groovyEngine = mock(ScriptEngine.class);
    when(engineManager.getEngineByName("groovy")).thenReturn(groovyEngine);

    ScriptEngine engine = scriptService.engineForLanguage("groovy");

    assertNotNull(engine);
    assertSame(groovyEngine, engine);
  }

  @Test
  public void exceptionIsThrownWhenJavascriptEngineIsRequested() {
    ScriptService scriptService = createScriptService(true);

    IllegalScriptLanguageException exception = assertThrows(IllegalScriptLanguageException.class,
        () -> scriptService.engineForLanguage("javascript"));
    assertEquals("Language: javascript is not allowed", exception.getMessage());
  }

  @Test
  public void javascriptEngineIsReturnedWhenGroovyOnlyIsSetToFalse() {
    ScriptService scriptService = createScriptService(false);
    ScriptEngine javascriptEngine = mock(ScriptEngine.class);
    when(engineManager.getEngineByName("javascript")).thenReturn(javascriptEngine);

    ScriptEngine engine = scriptService.engineForLanguage("javascript");

    assertNotNull(engine);
    assertSame(javascriptEngine, engine);
  }

  @Test
  public void constructorRejectsNullEngineManager() {
    assertThrows(NullPointerException.class, () -> new ScriptServiceImpl(
        null, mock(GlobalComponentLookupHelper.class), new ArrayList<>(), mock(ScriptCleanupHandler.class), true));
  }

  @Test
  public void constructorRejectsNullLookupHelper() {
    assertThrows(NullPointerException.class, () -> new ScriptServiceImpl(
        engineManager, null, new ArrayList<>(), mock(ScriptCleanupHandler.class), true));
  }

  @Test
  public void constructorRejectsNullScriptApis() {
    assertThrows(NullPointerException.class, () -> new ScriptServiceImpl(
        engineManager, mock(GlobalComponentLookupHelper.class), null, mock(ScriptCleanupHandler.class), true));
  }

  @Test
  public void constructorRejectsNullScriptCleanupHandler() {
    assertThrows(NullPointerException.class, () -> new ScriptServiceImpl(
        engineManager, mock(GlobalComponentLookupHelper.class), new ArrayList<>(), null, true));
  }

  @Test
  public void exceptionIsThrownWhenEngineIsMissing() {
    ScriptService scriptService = createScriptService(true);

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> scriptService.engineForLanguage("groovy"));
    assertEquals("Missing engine for language: groovy", exception.getMessage());
  }

  @Test
  public void exceptionIsThrownWhenLanguageIsNull() {
    ScriptService scriptService = createScriptService(true);

    assertThrows(NullPointerException.class, () -> scriptService.engineForLanguage(null));
  }

  @Test
  public void applyDefaultBindingsPopulatesContainerCleanupHandlerAndScriptApis() {
    GlobalComponentLookupHelper lookupHelper = mock(GlobalComponentLookupHelper.class);
    ScriptCleanupHandler scriptCleanupHandler = mock(ScriptCleanupHandler.class);
    ScriptApi scriptApi = mock(ScriptApi.class);
    when(scriptApi.getName()).thenReturn("myApi");

    List<ScriptApi> scriptApis = new ArrayList<>();
    scriptApis.add(scriptApi);

    ScriptService scriptService =
        new ScriptServiceImpl(engineManager, lookupHelper, scriptApis, scriptCleanupHandler, true);

    Bindings bindings = new SimpleBindings();
    scriptService.applyDefaultBindings(bindings);

    assertEquals(lookupHelper, bindings.get("container"));
    assertEquals(scriptCleanupHandler, bindings.get(ScriptServiceImpl.SCRIPT_CLEANUP_HANDLER));
    assertEquals(scriptApi, bindings.get("myApi"));
  }

  @Test
  public void applyDefaultBindingsThrowsForNullBindings() {
    ScriptService scriptService = createScriptService(true);

    assertThrows(NullPointerException.class, () -> scriptService.applyDefaultBindings(null));
  }

  @Test
  public void createContextReturnsContextWithEngineBindings() {
    ScriptService scriptService = createScriptService(true);
    ScriptEngine engine = mock(ScriptEngine.class);
    Bindings engineBindings = new SimpleBindings();
    when(engineManager.getEngineByName("groovy")).thenReturn(engine);
    when(engine.createBindings()).thenReturn(engineBindings);

    ScriptContext context = scriptService.createContext("groovy");

    assertNotNull(context);
    assertSame(engineBindings, context.getBindings(ScriptContext.ENGINE_SCOPE));
  }

  @Test
  public void customizeBindingsWithScopeAppliesDefaultsAndCustomizations() {
    GlobalComponentLookupHelper lookupHelper = mock(GlobalComponentLookupHelper.class);
    ScriptCleanupHandler scriptCleanupHandler = mock(ScriptCleanupHandler.class);
    ScriptService scriptService =
        new ScriptServiceImpl(engineManager, lookupHelper, new ArrayList<>(), scriptCleanupHandler, true);

    ScriptContext context = new SimpleScriptContext();
    context.setBindings(new SimpleBindings(), ScriptContext.ENGINE_SCOPE);

    Map<String, Object> customizations = new HashMap<>();
    customizations.put("foo", "bar");

    scriptService.customizeBindings(context, ScriptContext.ENGINE_SCOPE, customizations);

    Bindings bindings = context.getBindings(ScriptContext.ENGINE_SCOPE);
    assertEquals(lookupHelper, bindings.get("container"));
    assertEquals(scriptCleanupHandler, bindings.get(ScriptServiceImpl.SCRIPT_CLEANUP_HANDLER));
    assertEquals("bar", bindings.get("foo"));
  }

  @Test
  public void customizeBindingsDelegatesToEngineScope() {
    GlobalComponentLookupHelper lookupHelper = mock(GlobalComponentLookupHelper.class);
    ScriptCleanupHandler scriptCleanupHandler = mock(ScriptCleanupHandler.class);
    ScriptService scriptService =
        new ScriptServiceImpl(engineManager, lookupHelper, new ArrayList<>(), scriptCleanupHandler, true);

    ScriptContext context = new SimpleScriptContext();
    context.setBindings(new SimpleBindings(), ScriptContext.ENGINE_SCOPE);

    Map<String, Object> customizations = new HashMap<>();
    customizations.put("baz", "qux");

    scriptService.customizeBindings(context, customizations);

    Bindings bindings = context.getBindings(ScriptContext.ENGINE_SCOPE);
    assertEquals(lookupHelper, bindings.get("container"));
    assertEquals("qux", bindings.get("baz"));
  }

  @Test
  public void evalWithContextDelegatesToEngine() throws Exception {
    ScriptService scriptService = createScriptService(true);
    ScriptEngine engine = mock(ScriptEngine.class);
    ScriptContext context = new SimpleScriptContext();
    when(engineManager.getEngineByName("groovy")).thenReturn(engine);
    when(engine.eval("script", context)).thenReturn("result");

    Object result = scriptService.eval("groovy", "script", context);

    assertEquals("result", result);
  }

  @Test
  public void evalWithCustomBindingsCreatesContextCustomizesAndEvaluates() throws Exception {
    GlobalComponentLookupHelper lookupHelper = mock(GlobalComponentLookupHelper.class);
    ScriptCleanupHandler scriptCleanupHandler = mock(ScriptCleanupHandler.class);
    ScriptService scriptService =
        new ScriptServiceImpl(engineManager, lookupHelper, new ArrayList<>(), scriptCleanupHandler, true);

    ScriptEngine engine = mock(ScriptEngine.class);
    Bindings engineBindings = new SimpleBindings();
    when(engineManager.getEngineByName("groovy")).thenReturn(engine);
    when(engine.createBindings()).thenReturn(engineBindings);
    when(engine.eval(any(String.class), any(ScriptContext.class))).thenReturn("result");

    Map<String, Object> customBindings = new HashMap<>();
    customBindings.put("foo", "bar");

    Object result = scriptService.eval("groovy", "script", customBindings);

    assertEquals("result", result);
    assertEquals("bar", engineBindings.get("foo"));
    assertEquals(lookupHelper, engineBindings.get("container"));
  }

  @Test
  public void createContextThrowsForDisallowedLanguage() {
    ScriptService scriptService = createScriptService(true);

    assertThrows(IllegalScriptLanguageException.class, () -> scriptService.createContext("javascript"));
  }

  @Test
  public void applyDefaultBindingsAddsAllScriptApis() {
    GlobalComponentLookupHelper lookupHelper = mock(GlobalComponentLookupHelper.class);
    ScriptCleanupHandler scriptCleanupHandler = mock(ScriptCleanupHandler.class);
    ScriptApi firstApi = mock(ScriptApi.class);
    ScriptApi secondApi = mock(ScriptApi.class);
    when(firstApi.getName()).thenReturn("first");
    when(secondApi.getName()).thenReturn("second");

    List<ScriptApi> scriptApis = new ArrayList<>();
    scriptApis.add(firstApi);
    scriptApis.add(secondApi);

    ScriptService scriptService =
        new ScriptServiceImpl(engineManager, lookupHelper, scriptApis, scriptCleanupHandler, true);

    Bindings bindings = new SimpleBindings();
    scriptService.applyDefaultBindings(bindings);

    assertSame(firstApi, bindings.get("first"));
    assertSame(secondApi, bindings.get("second"));
  }

  @Test
  public void customizeBindingsHonorsTheGivenScope() {
    GlobalComponentLookupHelper lookupHelper = mock(GlobalComponentLookupHelper.class);
    ScriptCleanupHandler scriptCleanupHandler = mock(ScriptCleanupHandler.class);
    ScriptService scriptService =
        new ScriptServiceImpl(engineManager, lookupHelper, new ArrayList<>(), scriptCleanupHandler, true);

    ScriptContext context = new SimpleScriptContext();
    context.setBindings(new SimpleBindings(), ScriptContext.GLOBAL_SCOPE);

    Map<String, Object> customizations = new HashMap<>();
    customizations.put("foo", "bar");

    scriptService.customizeBindings(context, ScriptContext.GLOBAL_SCOPE, customizations);

    Bindings globalBindings = context.getBindings(ScriptContext.GLOBAL_SCOPE);
    assertEquals(lookupHelper, globalBindings.get("container"));
    assertEquals(scriptCleanupHandler, globalBindings.get(ScriptServiceImpl.SCRIPT_CLEANUP_HANDLER));
    assertEquals("bar", globalBindings.get("foo"));

    // the engine scope must not be touched when a different scope is requested
    Bindings engineBindings = context.getBindings(ScriptContext.ENGINE_SCOPE);
    assertNull(engineBindings.get("container"));
    assertNull(engineBindings.get("foo"));
  }

  @Test
  public void customizationsOverrideDefaultBindings() {
    GlobalComponentLookupHelper lookupHelper = mock(GlobalComponentLookupHelper.class);
    ScriptCleanupHandler scriptCleanupHandler = mock(ScriptCleanupHandler.class);
    ScriptService scriptService =
        new ScriptServiceImpl(engineManager, lookupHelper, new ArrayList<>(), scriptCleanupHandler, true);

    ScriptContext context = new SimpleScriptContext();
    context.setBindings(new SimpleBindings(), ScriptContext.ENGINE_SCOPE);

    Map<String, Object> customizations = new HashMap<>();
    customizations.put("container", "overridden");

    scriptService.customizeBindings(context, ScriptContext.ENGINE_SCOPE, customizations);

    // customizations are applied after the defaults, so they win on key collision
    assertEquals("overridden", context.getBindings(ScriptContext.ENGINE_SCOPE).get("container"));
  }

  @Test
  public void evalWithContextThrowsForNullLanguage() {
    ScriptService scriptService = createScriptService(true);

    assertThrows(NullPointerException.class,
        () -> scriptService.eval(null, "script", new SimpleScriptContext()));
  }

  @Test
  public void evalWithContextThrowsForNullScript() {
    ScriptService scriptService = createScriptService(true);
    when(engineManager.getEngineByName("groovy")).thenReturn(mock(ScriptEngine.class));

    assertThrows(NullPointerException.class,
        () -> scriptService.eval("groovy", null, new SimpleScriptContext()));
  }

  @Test
  public void evalWithContextThrowsForNullContext() {
    ScriptService scriptService = createScriptService(true);
    when(engineManager.getEngineByName("groovy")).thenReturn(mock(ScriptEngine.class));

    assertThrows(NullPointerException.class,
        () -> scriptService.eval("groovy", "script", (ScriptContext) null));
  }

  @Test
  public void evalWithCustomBindingsThrowsForNullCustomBindings() {
    ScriptService scriptService = createScriptService(true);
    ScriptEngine engine = mock(ScriptEngine.class);
    when(engineManager.getEngineByName("groovy")).thenReturn(engine);
    when(engine.createBindings()).thenReturn(new SimpleBindings());

    assertThrows(NullPointerException.class,
        () -> scriptService.eval("groovy", "script", (Map<String, Object>) null));
  }

  private ScriptService createScriptService(final boolean groovyOnly) {
    return new ScriptServiceImpl(
        engineManager,
        mock(GlobalComponentLookupHelper.class),
        new ArrayList<>(),
        mock(ScriptCleanupHandler.class),
        groovyOnly);
  }
}
