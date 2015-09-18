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
package org.sonatype.nexus.common.app;

import javax.annotation.Nullable;

/**
 * Helper to lookup components in global context.
 *
 * In a few places, components need to be looked up by class-name and need to use the uber class-loader to resolve classes.
 * This helper contains this logic in one place for re-use.
 *
 * @since 3.0
 */
public interface GlobalComponentLookupHelper
{
  /**
   * Lookup a component by class-name.
   *
   * @return Component reference, or {@code null} if the component was not found.
   */
  @Nullable
  Object lookup(String className);

  // TODO: Consider adding lookup(Class) and lookup(Key) helpers?

 /**
   * Lookup a type by class-name.
   *
   * @return Type reference, or {@code null} if the type was not found.
   */
  @Nullable
  Class<?> type(String className);
}
