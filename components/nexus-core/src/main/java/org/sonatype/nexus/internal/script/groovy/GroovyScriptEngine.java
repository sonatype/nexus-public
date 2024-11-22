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

import javax.script.ScriptEngine;

import groovy.lang.GroovyClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Groovy {@link ScriptEngine}.
 *
 * @since 3.0
 */
public class GroovyScriptEngine
    extends org.codehaus.groovy.jsr223.GroovyScriptEngineImpl
{
  private static final Logger log = LoggerFactory.getLogger(GroovyScriptEngine.class);

  // FIXME: Sort out how we can better avoid leaking generated classes
  // FIXME: It appears the default impl will retain generated classes, even for evaluation calls (not just for compiled)
  // FIXME: Probably needs to use GroovyClassLoader.parseClass(GroovyCodeSource codeSource, boolean shouldCacheSource)

  public GroovyScriptEngine(final GroovyClassLoader classLoader) {
    super(classLoader);
  }

  // TODO: Sub-class here to add customized support to handle class cache
}
