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
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.sonatype.goodies.common.ComponentSupport;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provides the {@link ScriptEngineManager}.
 *
 * @since 3.0
 */
@Named
@Singleton
public class ScriptEngineManagerProvider
    extends ComponentSupport
    implements Provider<ScriptEngineManager>
{
  public static final String DEFAULT_LANGUAGE = "groovy";

  // TODO: Could consider a Mediator, except the ScriptEngineManager provides no means to "unregister"

  private final List<ScriptEngineFactory> factories;

  @Inject
  public ScriptEngineManagerProvider(final List<ScriptEngineFactory> factories) {
    this.factories = checkNotNull(factories);
  }

  @Override
  public ScriptEngineManager get() {
    // limit detection of engines to the runtime's default engines, other engines should register via guice
    ScriptEngineManager engineManager = new ScriptEngineManager(ClassLoader.getSystemClassLoader());

    List<ScriptEngineFactory> available = new ArrayList<>();
    available.addAll(engineManager.getEngineFactories()); // detected by runtime

    // Register engine-factories detected via injection
    for (ScriptEngineFactory factory : factories) {
      log.debug("Registering engine-factory: {}", factory);

      for (String name : factory.getNames()) {
        engineManager.registerEngineName(name, factory);
      }

      for (String mimeType : factory.getMimeTypes()) {
        engineManager.registerEngineMimeType(mimeType, factory);
      }

      for (String ext : factory.getExtensions()) {
        engineManager.registerEngineExtension(ext, factory);
      }

      available.add(factory);
    }

    // Dump some information about detected engine factories
    log.info("Detected {} engine-factories", available.size());

    for (ScriptEngineFactory factory : available) {
      log.info("Engine-factory: {} v{}; language={}, version={}, names={}, mime-types={}, extensions={}",
          factory.getEngineName(),
          factory.getEngineVersion(),
          factory.getLanguageName(),
          factory.getLanguageVersion(),
          factory.getNames(),
          factory.getMimeTypes(),
          factory.getExtensions()
      );
    }

    log.info("Default language: {}", DEFAULT_LANGUAGE);

    return engineManager;
  }
}
