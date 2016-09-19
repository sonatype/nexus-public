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
package org.sonatype.nexus.repository.manager.internal;

import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.Facet;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Helper to provide {@link Facet} lookup.
 *
 * @since 3.0
 */
public class FacetLookup
  extends ComponentSupport
  implements Iterable<Facet>
{
  /**
   * All facet instances.  Retain order.
   */
  private final Set<Facet> facets = Sets.newLinkedHashSet();

  /**
   * Exposed type to facet instance map.
   */
  private final Map<Class<? extends Facet>, Facet> exposed = Maps.newHashMap();

  /**
   * Add a facet and bind exposed types.
   */
  public void add(final Facet facet) {
    checkNotNull(facet);

    // discover all types to expose facet
    Class<? extends Facet> root = facet.getClass();
    List<Class<? extends Facet>> types = exposedTypes(root);
    checkState(!types.isEmpty(), "No exposed facets: %s", root);

    log.trace("Adding facet: {}, exposed as: {}", facet, types);

    // verify we are not clobbering existing facet types
    for (Class<? extends Facet> type : types) {
      checkState(!exposed.containsKey(type), "Duplicate exposed facet type: %s, root type: %s", type, root);
    }

    facets.add(facet);

    // map all exposed types to given facet
    for (Class<? extends Facet> type : types) {
      exposed.put(type, facet);
    }
  }

  /**
   * Returns list of all types of given root type which implement {@link Facet}
   * and are marked with {@link Facet.Exposed}.
   */
  @SuppressWarnings("unchecked")
  private List<Class<? extends Facet>> exposedTypes(final Class<? extends Facet> root) {
    List<Class<? extends Facet>> exposed = Lists.newArrayList();

    for (Class<?> type : TypeToken.of(root).getTypes().rawTypes()) {
      if (Facet.class.isAssignableFrom(type)) {
        for (Annotation annotation : type.getDeclaredAnnotations()) {
          if (annotation.annotationType() == Facet.Exposed.class) {
            exposed.add((Class<? extends Facet>) type);
          }
        }
      }
    }

    return exposed;
  }

  /**
   * Get a facet instance by exposed type.
   */
  @SuppressWarnings("unchecked")
  @Nullable
  public <T extends Facet> T get(final Class<T> type) {
    checkNotNull(type);
    return (T) exposed.get(type);
  }

  /**
   * Clear all facet bindings.
   */
  public void clear() {
    facets.clear();
    exposed.clear();
  }

  /**
   * List all facet instances.
   */
  @Override
  public Iterator<Facet> iterator() {
    return Iterators.unmodifiableIterator(facets.iterator());
  }

  /**
   * Returns reversed list of all facet instances.
   */
  public Iterable<Facet> reverse() {
    return ImmutableList.copyOf(facets).reverse();
  }
}
