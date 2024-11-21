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
package org.sonatype.nexus.test.util;

import java.lang.reflect.Field;
import java.util.Optional;

public final class Whitebox
{
  private Whitebox() {
    // private
  }

  /**
   * Uses reflection to set an intenal value.
   */
  public static void setInternalState(final Object object, final String fieldName, final Object value) {
    try {
      Class<?> clazz = object.getClass();
      Field field = getDeclaredField(clazz, fieldName)
          .orElseGet(() -> getField(clazz, fieldName));
      field.setAccessible(true);
      field.set(object, value);
    }
    catch (IllegalArgumentException | IllegalAccessException | SecurityException e) {
      throw new RuntimeException(e);
    }
  }

  private static Optional<Field> getDeclaredField(final Class<?> clazz, final String fieldName) {
    try {
      return Optional.of(clazz.getDeclaredField(fieldName));
    }
    catch (NoSuchFieldException | SecurityException e) {
      return Optional.empty();
    }
  }

  private static Field getField(final Class<?> clazz, final String fieldName) {
    try {
      return clazz.getField(fieldName);
    }
    catch (NoSuchFieldException | SecurityException e) {
      throw new RuntimeException(e);
    }
  }
}
