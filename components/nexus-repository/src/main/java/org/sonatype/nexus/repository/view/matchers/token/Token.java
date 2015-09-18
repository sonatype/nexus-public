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
package org.sonatype.nexus.repository.view.matchers.token;

import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A portion of a parsed pattern template.
 *
 * @since 3.0
 */
public abstract class Token
{
  protected final String value;

  protected Token(final String value) {
    this.value = checkNotNull(value);
  }

  public abstract String toRegexp();

  private static final List<Character> REGEXP_CHARS = Arrays.asList('[', ']', '{', '}', '(', ')', '\\', '.');

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Token)) {
      return false;
    }

    Token token = (Token) o;
    return value.equals(token.value);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }
}
