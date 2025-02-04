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
package org.sonatype.nexus.common.guice;

import java.lang.reflect.Method;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.matcher.Matcher;
import com.google.inject.name.Names;
import org.aopalliance.intercept.MethodInterceptor;

/**
 * Workaround to automatically share method interceptors until
 * <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=403108">proper Sisu feature</a> is implemented.
 * 
 * <p>
 * This module is only bound once in its originating realm, when the bindInterceptor method is first called. The Nexus
 * Plugin Manager can then see this module via the injected dynamic list of AbstractInterceptorModules and will install
 * it in any plugins registered after this point.
 * 
 * <p>
 * Note: you can't contribute interceptors to earlier plugins or from a plugin to core, but the other direction works
 * fine.
 * 
 * @since 2.4
 */
public abstract class AbstractInterceptorModule
    extends AbstractModule
{
  private boolean bound;

  @Override
  protected void bindInterceptor(
      final Matcher<? super Class<?>> classMatcher,
      final Matcher<? super Method> methodMatcher,
      final MethodInterceptor... interceptors)
  {
    if (!bound) {
      // Explicitly bind module instance under a specific sub-type (not Module as Guice forbids that)
      bind(Key.get(AbstractInterceptorModule.class, Names.named(getClass().getName()))).toInstance(this);
      bound = true;
    }
    super.bindInterceptor(classMatcher, methodMatcher, interceptors);
  }
}
