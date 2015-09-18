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
package org.sonatype.nexus.common.collect;

import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Nested {@link AttributesMap} supporting parent/child relationship.
 *
 * @since 3.0
 */
public class NestedAttributesMap
  extends AttributesMap
{
  @VisibleForTesting
  static final String SEPARATOR = "::";

  @Nullable
  private final NestedAttributesMap parent;

  private final String key;

  public NestedAttributesMap(final String key, final Map<String, Object> backing) {
    this(null, key, backing);
  }

  @VisibleForTesting
  NestedAttributesMap(final @Nullable NestedAttributesMap parent,
                      final String key,
                      final Map<String, Object> backing)
  {
    super(backing);
    this.parent = parent;
    this.key = checkNotNull(key);
  }

  /**
   * Returns the parent of this attributes or null if there is none.
   */
  @Nullable
  public NestedAttributesMap getParent() {
    return parent;
  }

  @VisibleForTesting
  String getParentKey() {
    if (parent != null) {
      // fully-qualify parent key if it has a grandparent
      if (parent.parent != null) {
        return parent.getParentKey() + SEPARATOR + parent.getKey();
      }
      return parent.getKey();
    }
    return null;
  }

  /**
   * Return the nested attributes key.
   */
  public String getKey() {
    return key;
  }

  /**
   * Returns the key of this nested container qualified with parent if there is one.
   */
  @VisibleForTesting
  String getQualifiedKey() {
    if (parent != null) {
      return getParentKey() + SEPARATOR + key;
    }
    return key;
  }

  /**
   * Include qualified key in missing key message.
   */
  @Override
  protected String missingKeyMessage(final String key) {
    return "Missing: {" + getQualifiedKey() + "} " + key;
  }

  /**
   * Create new backing for new children attributes backing.
   */
  protected Map<String,Object> newChildBacking() {
    return Maps.newHashMap();
  }

  /**
   * Returns nested children attributes for given name.
   */
  public NestedAttributesMap child(final String name) {
    checkNotNull(name);

    Object child = backing.get(name);
    if (child == null) {
      child = newChildBacking();
      backing.put(name, child);
    }
    else {
      checkState(child instanceof Map, "child '%s' not a Map", name);
    }
    //noinspection unchecked,ConstantConditions
    return new NestedAttributesMap(this, name, (Map<String, Object>) child);
  }

  /**
   * Prevents setting {@link Map} values and require use of {@link #child}.
   */
  @Nullable
  @Override
  public Object set(final String key, final @Nullable Object value) {
    checkState(!(value instanceof Map), "Use child() to set a map value");
    return super.set(key, value);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "parent=" + getParentKey() +
        ", key='" + key + '\'' +
        ", backing=" + backing +
        '}';
  }

}
