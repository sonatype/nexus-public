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

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.GlobalComponentLookupHelper;
import org.sonatype.nexus.common.script.ScriptService;

import org.eclipse.sisu.inject.BeanLocator;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Default {@link ScriptService} implementation.
 *
 * @since 3.0
 */
@Named
@Singleton
public class ScriptServiceImpl
    extends ComponentSupport
    implements ScriptService
{
  private final ScriptEngineManager engineManager;

  private final BeanLocator beanLocator;

  private final GlobalComponentLookupHelper lookupHelper;

  @Inject
  public ScriptServiceImpl(final ScriptEngineManager engineManager,
                           final BeanLocator beanLocator,
                           final GlobalComponentLookupHelper lookupHelper)
  {
    this.engineManager = checkNotNull(engineManager);
    this.beanLocator = checkNotNull(beanLocator);
    this.lookupHelper = checkNotNull(lookupHelper);
  }

  @Override
  public ScriptEngineManager getEngineManager() {
    return engineManager;
  }

  @Override
  @Nonnull
  public ScriptEngine engineForLanguage(final String language) {
    checkNotNull(language);

    log.trace("Resolving engine for language: {}", language);
    ScriptEngine engine = engineManager.getEngineByName(language);
    checkState(engine != null, "Missing engine for language: %s", language);

    log.trace("Engine: {}", engine);
    return engine;
  }

  @Override
  @Nonnull
  public ScriptEngine engineForExtension(final String extension) {
    checkNotNull(extension);

    log.trace("Resolving engine for extension: {}", extension);
    ScriptEngine engine = engineManager.getEngineByExtension(extension);
    checkState(engine != null, "Missing engine for extension: %s", extension);

    log.trace("Engine: {}", engine);
    return engine;
  }

  @Override
  @Nonnull
  public ScriptEngine engineForMimeType(final String mimeType) {
    checkNotNull(mimeType);

    log.trace("Resolving engine for mime-type: {}", mimeType);
    ScriptEngine engine = engineManager.getEngineByMimeType(mimeType);
    checkState(engine != null, "Missing engine for mime-type: %s", mimeType);

    log.trace("Engine: {}", engine);
    return engine;
  }

  @Override
  public void applyDefaultBindings(final Bindings bindings) {
    checkNotNull(bindings);

    // TODO: Could potentially expose these in GLOBAL_SCOPE bindings?
    // TODO: Consider options to allow scripts more easily become injected aware?

    bindings.put("beanLocator", beanLocator);
    bindings.put("container", lookupHelper);
  }
}
