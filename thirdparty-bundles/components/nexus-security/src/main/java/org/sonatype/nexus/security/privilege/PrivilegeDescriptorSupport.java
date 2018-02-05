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
package org.sonatype.nexus.security.privilege;

import java.util.Arrays;
import java.util.List;

import org.sonatype.nexus.security.config.CPrivilege;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_UNDERSCORE;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Support for {@link PrivilegeDescriptor} implementations.
 *
 * @since 3.0
 */
public abstract class PrivilegeDescriptorSupport
    implements PrivilegeDescriptor
{
  public static final String ALL = "*";

  private final String type;

  public PrivilegeDescriptorSupport(final String type) {
    this.type = checkNotNull(type);
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "type='" + type + '\'' +
        '}';
  }

  /**
   * Helper to read a privilege property and return default-value if unset or empty.
   */
  protected String readProperty(final CPrivilege privilege, final String name, final String defaultValue) {
    String value = privilege.getProperty(name);
    if (Strings.nullToEmpty(value).isEmpty()) {
      value = defaultValue;
    }
    return value;
  }

  /**
   * Helper to read a required privilege property.
   *
   * @throws IllegalStateException Missing required property.
   */
  protected String readProperty(final CPrivilege privilege, final String name) {
    String value = privilege.getProperty(name);
    checkState(!Strings.nullToEmpty(value).isEmpty(), "Missing required property: %s", name);
    return value;
  }

  /**
   * Helper to read a privilege property and parse out list.
   */
  protected List<String> readListProperty(final CPrivilege privilege, final String name, final String defaultValue) {
    String value = readProperty(privilege, name, defaultValue);
    return Lists.newArrayList(Splitter.on(',').omitEmptyStrings().trimResults().split(value));
  }

  /**
   * Returns {@code "all"} if passed in {@link #ALL} constant to denote all repositories, or the {@code name} parameter
   * as is.
   */
  protected static String humanizeName(final String name, final String format) {
    if (ALL.equals(name)) {
      if (ALL.equals(format)) {
        return "all";
      }
      else {
        return "all '" + format + "'-format";
      }
    }
    else {
      return name;
    }
  }

  /**
   * Returns {@code "X privilege"} or {@code "X privileges"}, where {@code X} part is comma separated list of actions
   * passed in as {@code actions} parameter that are Capitalized. If {@link #ALL} is passed in, returns string {@code
   * "All privileges"}. There must be at least one input argument to the {@code actions} vararg input parameter.
   */
  protected static String humanizeActions(final String... actions) {
    checkArgument(actions.length > 0);
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(Joiner.on(", ").join(Iterables.transform(Arrays.asList(actions), new Function<String, String>()
    {
      @Override
      public String apply(final String action) {
        if (ALL.equals(action)) {
          return "All";
        }
        else {
          return UPPER_UNDERSCORE.to(UPPER_CAMEL, action);
        }
      }
    })));
    if (actions.length > 1 || ALL.equals(actions[0])) {
      stringBuilder.append(" privileges");
    }
    else {
      stringBuilder.append(" privilege");
    }
    return stringBuilder.toString();
  }
}
