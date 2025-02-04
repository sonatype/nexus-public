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
package org.sonatype.nexus.internal.script.groovy;

import groovy.lang.Binding;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.script.ScriptCleanupHandler;
import org.sonatype.nexus.internal.script.ScriptServiceImpl;
import org.sonatype.nexus.internal.script.ScriptTask;
import org.sonatype.nexus.scheduling.TaskSupport;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GroovyScriptEngineFactoryTest
{

  @Mock
  private ApplicationDirectories applicationDirectories;

  @Mock
  private ScriptCleanupHandler scriptCleanupHandler;

  @Test
  public void itWillCallTheCleanupHelperAfterExecutingTheScript() throws ScriptException {
    GroovyScriptEngineFactory factory =
        new GroovyScriptEngineFactory(getClass().getClassLoader(), applicationDirectories);
    ScriptEngine engine = factory.getScriptEngine();
    Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
    bindings.put(ScriptServiceImpl.SCRIPT_CLEANUP_HANDLER, scriptCleanupHandler);

    Object result = engine.eval("return 1", bindings);
    assertEquals(1, result);
    verify(scriptCleanupHandler, times(1)).cleanup(any());
  }

  @Test
  public void itWillCallTheCleanupHelperAfterExecutingTheScriptEvenWhenAnExceptionIsThrown() {
    GroovyScriptEngineFactory factory =
        new GroovyScriptEngineFactory(getClass().getClassLoader(), applicationDirectories);
    ScriptEngine engine = factory.getScriptEngine();
    Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
    bindings.put(ScriptServiceImpl.SCRIPT_CLEANUP_HANDLER, scriptCleanupHandler);

    assertThrows(ScriptException.class, () -> {
      engine.eval("throw new RuntimeException(\"bad\")", bindings);
    });

    verify(scriptCleanupHandler, times(1)).cleanup(any());
  }

  @Test
  public void itWillCreateANamedContextForTheExecutorOfTheGroovyScriptEngine() {
    GroovyScriptEngineFactory factory =
        new GroovyScriptEngineFactory(getClass().getClassLoader(), applicationDirectories);
    ScriptEngine engine = factory.getScriptEngine();
    Binding binding = new Binding();
    binding.setVariable("task", null);
    binding.setVariable("scriptName", "testScript");

    String expectedContext = "Script 'testScript'";
    String actualContext = GroovyScriptEngineFactory.getContext(binding);

    assertEquals(expectedContext, actualContext);

    ScriptTask task = mock(ScriptTask.class);
    when(task.getName()).thenReturn("myTask");
    binding.setVariable("task", task);
    binding.setVariable("scriptName", null);

    expectedContext = "Task 'myTask'";
    actualContext = GroovyScriptEngineFactory.getContext(binding);

    assertEquals(expectedContext, actualContext);

    binding.setVariable("task", null);
    binding.setVariable("scriptName", null);

    expectedContext = "An unknown script";
    actualContext = GroovyScriptEngineFactory.getContext(binding);

    assertEquals(expectedContext, actualContext);

    TaskSupport taskSupport = mock(TaskSupport.class);
    binding.setVariable("task", taskSupport);
    binding.setVariable("scriptName", null);

    expectedContext = "An unknown script";
    actualContext = GroovyScriptEngineFactory.getContext(binding);

    assertEquals(expectedContext, actualContext);
  }
}
