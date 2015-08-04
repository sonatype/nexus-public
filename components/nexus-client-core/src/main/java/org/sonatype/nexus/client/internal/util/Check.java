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
package org.sonatype.nexus.client.internal.util;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Simple internal utility class to perform checks of arguments. Inspired by Google's {@code Preconditions}.
 *
 * @since 2.1
 * @deprecated Use Google Guava {@link Preconditions} instead.
 */
public class Check
{

  @Deprecated
  public static boolean isBlank(final String t) {
    return t == null || t.trim().isEmpty();
  }

  @Deprecated
  public static String notBlank(final String t, final String name) {
    return argument(!isBlank(t), t, String.format("\"%s\" is blank!", name));
  }

  @Deprecated
  public static <T> T notNull(final T t, final Class<?> clazz) {
    return notNull(t, String.format("%s is null!", clazz.getSimpleName()));
  }

  @Deprecated
  public static <T> T notNull(final T t, final Object message) {
    return checkNotNull(t, message);
  }

  @Deprecated
  public static void argument(boolean condition, final Object message) {
    argument(condition, null, message);
  }

  @Deprecated
  public static <T> T argument(boolean condition, final T t, final Object message) {
    checkArgument(condition, message);
    return t;
  }
}
