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

import org.eclipse.sisu.inject.BeanLocator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import org.sonatype.nexus.common.app.GlobalComponentLookupHelper;
import org.sonatype.nexus.common.script.ScriptCleanupHandler;
import org.sonatype.nexus.common.script.ScriptService;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ScriptServiceImplTest
{

  @Mock
  private ScriptEngineManager engineManager;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void groovyScriptEngineIsReturnedCorrectly() {
    ScriptService scriptService = createScriptService(true);
    when(engineManager.getEngineByName("groovy")).thenReturn(mock(ScriptEngine.class));

    ScriptEngine engine = scriptService.engineForLanguage("groovy");

    assertNotNull(engine);
  }

  @Test
  public void exceptionIsThrownWhenJavascriptEngineIsRequested() {
    ScriptService scriptService = createScriptService(true);

    assertThrows(IllegalScriptLanguageException.class, () -> {
      scriptService.engineForLanguage("javascript");
    });
  }

  @Test
  public void javascriptEngineIsReturnedWhenGroovyOnlyIsSetToFalse() {
    ScriptService scriptService = createScriptService(false);
    when(engineManager.getEngineByName("javascript")).thenReturn(mock(ScriptEngine.class));

    ScriptEngine engine = scriptService.engineForLanguage("javascript");

    assertNotNull(engine);
  }

  private ScriptService createScriptService(boolean groovyOnly) {
    return new ScriptServiceImpl(
        engineManager,
        mock(BeanLocator.class),
        mock(GlobalComponentLookupHelper.class),
        new ArrayList<>(),
        mock(ScriptCleanupHandler.class),
        groovyOnly);
  }
}
