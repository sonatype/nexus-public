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

import com.google.common.base.Objects;

/**
 * A named variable that matches a regular expression.
 *
 * @since 3.0
 */
public class VariableToken
    extends Token
{
  private final String name;

  public VariableToken(final String name, final String regexp) {
    super(regexp);
    this.name = name;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toRegexp() {
    return "(" + value + ")";
  }


  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof VariableToken)) {
      return false;
    }
    final VariableToken other = (VariableToken) obj;
    return Objects.equal(getName(), other.getName())
        && super.equals(obj);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name, value);
  }

  public String toString() {
    return String.format("var(%s,%s)", name, value);
  }
}
