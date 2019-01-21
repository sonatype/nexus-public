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
package org.sonatype.nexus.selector.internal;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.internal.introspection.Uberspect;
import org.apache.commons.jexl3.introspection.JexlMethod;
import org.apache.commons.jexl3.introspection.JexlPropertySet;
import org.apache.commons.jexl3.introspection.JexlUberspect;
import org.apache.commons.logging.LogFactory;

/**
 * Uberspect for Jexl which blocks writes and allows very limited methods
 *
 * @since 3.15
 */
public class SandboxJexlUberspect extends Uberspect
{
  private static final Set<String> COLLECTION_METHODS = ImmutableSet.of("contains");

  private static final Set<String> MAP_METHODS = ImmutableSet.of("get", "getOrDefault", "containsKey", "containsValue");

  private static final Set<String> STRING_METHODS = ImmutableSet.of("toUpperCase", "toLowerCase", "endsWith",
      "startsWith");

  public SandboxJexlUberspect() {
    super(LogFactory.getLog(JexlEngine.class), JexlUberspect.JEXL_STRATEGY);
  }

  @Override
  public JexlMethod getConstructor(final Object ctorHandle, final Object... args) {
    return null;
  }

  @Override
  public JexlMethod getMethod(final Object obj, final String method, final Object... args) {
    if (obj instanceof String && STRING_METHODS.contains(method)) {
      return super.getMethod(obj, method, args);
    }
    else if (obj instanceof Map && MAP_METHODS.contains(method)) {
      return super.getMethod(obj, method, args);
    }
    else if (obj instanceof Collection && COLLECTION_METHODS.contains(method)) {
      return super.getMethod(obj, method, args);
    }
    return null;
  }

  @Override
  public JexlPropertySet getPropertySet(final Object obj, final Object identifier, final Object arg) {
    return null;
  }

  @Override
  public JexlPropertySet getPropertySet(final List<PropertyResolver> resolvers, final Object obj, final Object identifier, final Object arg) {
    return null;
  }
}
