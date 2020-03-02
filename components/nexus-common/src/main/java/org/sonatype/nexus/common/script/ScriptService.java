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
package org.sonatype.nexus.common.script;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

/**
 * Script service.
 *
 * @since 3.0
 */
public interface ScriptService
{
  /**
   * Return engine for given language.
   *
   * @throws IllegalStateException Missing engine
   */
  @Nonnull
  ScriptEngine engineForLanguage(String language);

  /**
   * Apply default bindings.
   */
  void applyDefaultBindings(Bindings bindings);

  /**
   * Create a context for running scripts, with {@link Bindings} available as appropriate for the language.
   */
  @Nonnull
  ScriptContext createContext(String language);

  /**
   * Add customizations to the context bindings in a specific scope.
   */
  void customizeBindings(ScriptContext context, int scope, Map<String, Object> customizations);

  /**
   * Add customizations to the context bindings in the {@link ScriptContext#ENGINE_SCOPE}.
   */
  void customizeBindings(ScriptContext context, Map<String, Object> customizations);

  /**
   * Evaluate a script with the given context.
   */
  Object eval(String language, String script, ScriptContext context) throws ScriptException;

  /**
   * Evaluate a script in a context with customBindings applied.
   */
  Object eval(String language, String script, Map<String, Object> customBindings) throws ScriptException;
}
