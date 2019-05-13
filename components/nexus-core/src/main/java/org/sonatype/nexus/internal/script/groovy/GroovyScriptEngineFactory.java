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

import java.io.File;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.script.ScriptCleanupHandler;
import org.sonatype.nexus.internal.script.ScriptTask;

import com.google.common.annotations.VisibleForTesting;
import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.sonatype.nexus.internal.script.ScriptServiceImpl.SCRIPT_CLEANUP_HANDLER;

/**
 * Groovy {@link ScriptEngineFactory}.
 *
 * @since 3.0
 */
@Named("groovy")
@Singleton
public class GroovyScriptEngineFactory
    extends org.codehaus.groovy.jsr223.GroovyScriptEngineFactory
{
  private static final Logger log = LoggerFactory.getLogger(GroovyScriptEngineFactory.class);

  private final ClassLoader classLoader;

  private final ApplicationDirectories applicationDirectories;

  private GroovyScriptEngine engine;

  @Inject
  public GroovyScriptEngineFactory(@Named("nexus-uber") final ClassLoader classLoader,
                                   final ApplicationDirectories applicationDirectories)
  {
    this.classLoader = checkNotNull(classLoader);
    this.applicationDirectories = checkNotNull(applicationDirectories);
  }

  private GroovyScriptEngine create() {
    // custom the configuration of the compiler
    CompilerConfiguration cc = new CompilerConfiguration();
    cc.setTargetDirectory(new File(applicationDirectories.getTemporaryDirectory(), "groovy-classes"));
    cc.setSourceEncoding("UTF-8");
    cc.setScriptBaseClass(ScriptWithCleanup.class.getName());
    cc.addCompilationCustomizers(secureASTCustomizer());
    GroovyClassLoader gcl = new GroovyClassLoader(classLoader, cc);

    engine = new GroovyScriptEngine(gcl);

    // HACK: For testing
    log.info("Created engine: {}", engine);

    return engine;
  }

  /**
   * Secure potentially dangerous calls in scripts.
   */
  private CompilationCustomizer secureASTCustomizer() {
    SecureASTCustomizer secureASTCustomizer = new SecureASTCustomizer();
    secureASTCustomizer.setImportsBlacklist(Collections.singletonList("java.lang.System"));
    secureASTCustomizer.setReceiversBlackList(Collections.singletonList(System.class.getName()));
    secureASTCustomizer.setIndirectImportCheckEnabled(true);
    return secureASTCustomizer;
  }

  // TODO: Groovy engine is thread-safe, so re-use the engine instance instead of creating new ones each time asked
  // TODO: sort out if there are any issues with engine scope bindings, or other wrinkles involved with shared engine

  // FIXME: Cope with the script-source -> class cache that the default impl has?
  // FIXME: ... in addition to the class cache which the GCL has

  @Override
  public synchronized ScriptEngine getScriptEngine() {
    if (engine == null) {
      engine = create();
    }
    return engine;
  }

  @VisibleForTesting
  static String getContext(final Binding binding) {
    Optional<String> taskContext = getVariable(binding, "task", ScriptTask.class)
        .map(ts -> format("Task '%s'", ts.getName()));
    Optional<String> scriptContext = getVariable(binding,"scriptName", String.class)
        .map(name -> format("Script '%s'" , name));
    return Stream.of(taskContext, scriptContext)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst()
        .orElse("An unknown script");
  }

  private static <T> Optional<T> getVariable(final Binding binding, final String name, final Class<T> type) {
    if (binding.hasVariable(name)) {
      Object instance = binding.getVariable(name);
      if(type.isInstance(instance)) {
        return (Optional<T>) Optional.of(instance);
      }
    }
    return Optional.empty();
  }

  public abstract static class ScriptWithCleanup
      extends Script
  {

    @Override
    public Object run() {
      try {
        return scriptBody();
      }
      finally {
        Object scriptCleanupHelper = this.getBinding().getVariable(SCRIPT_CLEANUP_HANDLER);
        if (scriptCleanupHelper instanceof ScriptCleanupHandler) {
          ((ScriptCleanupHandler) scriptCleanupHelper).cleanup(getContext(this.getBinding()));
        }
      }
    }

    protected abstract Object scriptBody();
  }
}
