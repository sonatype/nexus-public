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
package org.sonatype.nexus.capability;

import java.net.URI;
import java.net.URISyntaxException;

import javax.annotation.Nullable;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.text.Strings2;

import com.google.common.base.Throwables;

/**
 * Support for {@link Capability} configuration implementations.
 *
 * @since 2.7
 */
public abstract class CapabilityConfigurationSupport
    extends ComponentSupport
{
  protected boolean isEmpty(final String value) {
    return Strings2.isEmpty(value);
  }

  /**
   * Re-throws {@link URISyntaxException} as runtime exception.
   */
  protected URI parseUri(final String value) {
    try {
      return new URI(value);
    }
    catch (URISyntaxException e) {
      throw Throwables.propagate(e);
    }
  }

  /**
   * If given value is null or empty, returns default.
   *
   * @see #parseUri(String)
   */
  protected URI parseUri(final String value, @Nullable final URI defaultValue) {
    if (isEmpty(value)) {
      return defaultValue;
    }
    return parseUri(value);
  }

  protected Boolean parseBoolean(@Nullable final String value, @Nullable final Boolean defaultValue) {
    if (!isEmpty(value)) {
      return Boolean.parseBoolean(value);
    }
    return defaultValue;
  }

  /**
   * Parses the given string value as an integer or returns the specified default value if the string is {@code null}
   * or empty.
   * 
   * @since 3.1
   */
  protected Integer parseInteger(@Nullable final String value, @Nullable final Integer defaultValue) {
    if (isEmpty(value)) {
      return defaultValue;
    }
    return Integer.parseInt(value);
  }
}
