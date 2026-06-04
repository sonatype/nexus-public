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
package org.sonatype.nexus.common.i18n;

import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Message source backed up by {@link ResourceBundle} instances.
 *
 */
public class ResourceBundleMessageSource
    implements MessageSource
{
  private static final Logger log = LoggerFactory.getLogger(ResourceBundleMessageSource.class);

  private final List<ResourceBundle> bundles = Lists.newArrayList();

  private final Locale locale;

  public ResourceBundleMessageSource(final Locale locale) {
    this.locale = checkNotNull(locale);
  }

  public ResourceBundleMessageSource(final Class<?>... types) {
    this(Locale.getDefault());
    add(types);
  }

  public Locale getLocale() {
    return locale;
  }

  public ResourceBundleMessageSource add(final boolean required, final Class<?>... types) {
    checkNotNull(types);

    for (Class<?> type : types) {
      try {
        ResourceBundle bundle = ResourceBundle.getBundle(type.getName(), locale, type.getClassLoader());
        bundles.add(bundle);
      }
      catch (MissingResourceException e) {
        if (required) {
          throw e;
        }
      }
    }

    return this;
  }

  public ResourceBundleMessageSource add(final Class<?>... types) {
    return add(true, types);
  }

  /**
   * Get a raw message from the resource bundles using the given code.
   */
  @Override
  public String getMessage(final String code) {
    checkNotNull(code);

    for (ResourceBundle bundle : bundles) {
      try {
        return bundle.getString(code);
      }
      catch (MissingResourceException e) {
        log.trace(e.toString(), e);
      }
    }

    throw new ResourceNotFoundException(code);
  }

  @Override
  public String getMessage(final String code, final String defaultValue) {
    try {
      return getMessage(code);
    }
    catch (ResourceNotFoundException e) {
      return defaultValue;
    }
  }

  /**
   * Format a message (based on {@link String#format} using the message
   * from the resource bundles using the given code as a pattern and the
   * given objects as arguments.
   */
  @Override
  public String format(final String code, final @Nullable Object... args) {
    String pattern = getMessage(code);
    if (args != null) {
      return String.format(pattern, args);
    }
    else {
      return pattern;
    }
  }
}
