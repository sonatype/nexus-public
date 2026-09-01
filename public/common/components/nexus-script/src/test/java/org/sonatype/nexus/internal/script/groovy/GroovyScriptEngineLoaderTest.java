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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.sonatype.nexus.bootstrap.entrypoint.configuration.ApplicationDirectories;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class GroovyScriptEngineLoaderTest
{
  /**
   * The names reported by the real {@link org.codehaus.groovy.jsr223.GroovyScriptEngineFactory} that
   * {@link GroovyScriptEngineFactory} extends. {@code doStart()} must register exactly these.
   */
  private static final Set<String> EXPECTED_ENGINE_NAMES = new HashSet<>(Arrays.asList("groovy", "Groovy"));

  @Mock
  private ApplicationDirectories applicationDirectories;

  @Mock
  private ScriptEngineManager scriptEngineManager;

  @Test
  public void itWillThrowWhenApplicationDirectoriesIsNull() {
    assertThrows(NullPointerException.class,
        () -> new GroovyScriptEngineLoader(null, scriptEngineManager));
    verifyNoInteractions(scriptEngineManager);
  }

  @Test
  public void itWillThrowWhenScriptEngineManagerIsNull() {
    assertThrows(NullPointerException.class,
        () -> new GroovyScriptEngineLoader(applicationDirectories, null));
    // the constructor only stores applicationDirectories via checkNotNull; it must not invoke it
    verifyNoInteractions(applicationDirectories);
  }

  @Test
  public void itWillNotRegisterAnyEngineBeforeStart() {
    new GroovyScriptEngineLoader(applicationDirectories, scriptEngineManager);

    // registration is the responsibility of doStart(), never the constructor
    verifyNoInteractions(scriptEngineManager);
  }

  @Test
  public void itWillRegisterEveryGroovyEngineNameOnStart() throws Exception {
    GroovyScriptEngineLoader loader = new GroovyScriptEngineLoader(applicationDirectories, scriptEngineManager);

    loader.start();

    ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<ScriptEngineFactory> factoryCaptor = ArgumentCaptor.forClass(ScriptEngineFactory.class);
    // the real Groovy JSR-223 factory reports exactly two names: "groovy" and "Groovy"
    verify(scriptEngineManager, times(2)).registerEngineName(nameCaptor.capture(), factoryCaptor.capture());

    // assert the exact engine-name constants are registered, not merely "some non-empty set of names"
    assertEquals(EXPECTED_ENGINE_NAMES, new HashSet<>(nameCaptor.getAllValues()));

    // every name must be registered against the SAME GroovyScriptEngineFactory instance
    List<ScriptEngineFactory> registeredFactories = factoryCaptor.getAllValues();
    ScriptEngineFactory factory = registeredFactories.get(0);
    assertTrue(factory instanceof GroovyScriptEngineFactory);
    for (ScriptEngineFactory registered : registeredFactories) {
      assertSame(factory, registered);
    }

    // no spurious registrations or other interactions beyond the two expected ones
    verifyNoMoreInteractions(scriptEngineManager);
  }

  @Test
  public void groovyEngineNamesMatchTheRealFactoryContract() {
    // anchor the hard-coded expectation to the production-adjacent factory so a Groovy upgrade that
    // changes the reported names fails loudly here rather than silently weakening the loader test
    List<String> names = new GroovyScriptEngineFactory(applicationDirectories).getNames();

    assertEquals(EXPECTED_ENGINE_NAMES, new HashSet<>(names));
  }
}
