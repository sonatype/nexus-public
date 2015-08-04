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
package org.sonatype.nexus.plugins.capabilities.support.validator;

import org.sonatype.nexus.plugins.capabilities.ValidationResult;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link ValidationResult.Violation} implementation.
 *
 * @since capabilities 2.0
 */
public class DefaultViolation
    implements ValidationResult.Violation
{

  private final String key;

  private final String message;

  public DefaultViolation(final String message) {
    this.key = "*";
    this.message = checkNotNull(message);
  }

  public DefaultViolation(final String key, final String message) {
    this.key = checkNotNull(key);
    this.message = checkNotNull(message);
  }

  @Override
  public String key() {
    return key;
  }

  @Override
  public String message() {
    return message;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DefaultViolation)) {
      return false;
    }

    final DefaultViolation that = (DefaultViolation) o;

    if (!key.equals(that.key)) {
      return false;
    }
    if (!message.equals(that.message)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = key.hashCode();
    result = 31 * result + message.hashCode();
    return result;
  }
}
