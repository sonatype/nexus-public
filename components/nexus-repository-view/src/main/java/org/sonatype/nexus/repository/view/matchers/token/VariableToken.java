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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Objects;

/**
 * A named variable that matches a regular expression.
 *
 * @since 3.0
 */
public class VariableToken
    extends Token
{
  private static final Pattern NAME_PATTERN = Pattern.compile("[a-zA-Z0-9]");

  private final String name;

  private final String regexpGroupName;

  public VariableToken(final String name, final String regexp) {
    super(regexp);
    this.name = name;
    this.regexpGroupName = toRegexpGroupName(name);
  }

  public String getName() {
    return name;
  }

  @Override
  public String toRegexp() {
    return "(?<" + regexpGroupName + ">" + value + ")";
  }

  public String getRegexpGroupName() {
    return regexpGroupName;
  }

  @Override
  public boolean equals(final Object obj) {
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

  @Override
  public String toString() {
    return String.format("var(%s,%s)", name, value);
  }

  // Group names must start with a character and consist only of letters & digits
  private static String toRegexpGroupName(final String name) {
    StringBuilder sb = new StringBuilder();
    Matcher m = NAME_PATTERN.matcher(name);

    while (m.find()) {
      sb.append(m.group());
    }

    if (sb.length() == 0) {
      throw new IllegalStateException("Token name '" + name + "' contains no valid characters.");
    }

    if (Character.isDigit(sb.charAt(0))) {
      sb.insert(0, 'g');
    }

    return sb.toString();
  }
}
