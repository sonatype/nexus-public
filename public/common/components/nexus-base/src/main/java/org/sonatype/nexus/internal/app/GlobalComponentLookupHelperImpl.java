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

import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.app.GlobalComponentLookupHelper;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

/**
 * Default {@link GlobalComponentLookupHelper}.
 */
@Component
public class GlobalComponentLookupHelperImpl
    implements ApplicationContextAware, GlobalComponentLookupHelper
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  /**
   * Matches a syntactically valid Java binary class name (dot-separated identifiers, each starting
   * with a letter, {@code _} or {@code $}). Callers always pass fully-qualified class names, so a
   * legitimate lookup always matches; anything else (URIs such as {@code jar:file:/proc/self/fd/3},
   * paths, or other injected strings) is rejected before it can reach the classloader (NEXUS-54946).
   */
  private static final Pattern SAFE_CLASS_NAME =
      Pattern.compile("^[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*$");

  private ApplicationContext applicationContext;

  @Override
  @Nullable
  public Object lookup(final String className) {
    requireNonNull(className);
    requireNonNull(applicationContext);
    try {
      log.trace("Looking up component by class-name: {}", className);
      Class<?> type = getClass().getClassLoader().loadClass(className);
      return lookup(type);
    }
    catch (Exception e) {
      log.trace("Unable to lookup component by class-name: {}; ignoring", className, e);
    }
    return null;
  }

  @Override
  @Nullable
  public <T> T lookup(final Class<T> clazz) {
    requireNonNull(clazz);

    return applicationContext.getBean(clazz);
  }

  @Override
  @Nullable
  public <T> T lookup(final Class<T> clazz, final String name) {
    requireNonNull(clazz);
    requireNonNull(name);

    return applicationContext.getBean(name, clazz);
  }

  @Override
  @Nullable
  public Class<?> type(final String className) {
    requireNonNull(className);
    if (!SAFE_CLASS_NAME.matcher(className).matches()) {
      log.warn("Rejecting type lookup for value that is not a Java binary class name: {}", className);
      return null;
    }
    try {
      log.trace("Looking up type: {}", className);
      return getClass().getClassLoader().loadClass(className);
    }
    catch (Exception e) {
      log.trace("Unable to lookup type: {}; ignoring", className, e);
    }
    return null;
  }

  @Override
  public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }
}
