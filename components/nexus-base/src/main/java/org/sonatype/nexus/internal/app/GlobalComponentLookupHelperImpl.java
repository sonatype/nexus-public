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
package org.sonatype.nexus.internal.app;

import java.util.Iterator;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.GlobalComponentLookupHelper;

import com.google.inject.Key;
import org.eclipse.sisu.BeanEntry;
import org.eclipse.sisu.inject.BeanLocator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link GlobalComponentLookupHelper}.
 *
 * @since 3.0
 */
@Named
@Singleton
public class GlobalComponentLookupHelperImpl
    extends ComponentSupport
    implements GlobalComponentLookupHelper
{
  private final ClassLoader classLoader;

  private final BeanLocator beanLocator;

  @Inject
  public GlobalComponentLookupHelperImpl(@Named("nexus-uber") final ClassLoader classLoader,
                                         final BeanLocator beanLocator)
  {
    this.classLoader = checkNotNull(classLoader);
    this.beanLocator = checkNotNull(beanLocator);
  }

  @Override
  @Nullable
  public Object lookup(final String className) {
    checkNotNull(className);
    try {
      log.trace("Looking up component: {}", className);
      Class type = classLoader.loadClass(className);

      @SuppressWarnings("unchecked")
      Iterator<BeanEntry> iter = beanLocator.locate(Key.get(type)).iterator();
      if (iter.hasNext()) {
        return iter.next().getValue();
      }
      else {
        log.trace("Component not found: {}", className);
      }
    }
    catch (Exception e) {
      log.trace("Unable to lookup component: {}; ignoring", className, e);
    }
    return null;
  }

  @Override
  @Nullable
  public Class<?> type(final String className) {
    checkNotNull(className);
    try {
      log.trace("Looking up type: {}", className);
      return classLoader.loadClass(className);
    }
    catch (Exception e) {
      log.trace("Unable to lookup type: {}; ignoring", className, e);
    }
    return null;
  }
}
