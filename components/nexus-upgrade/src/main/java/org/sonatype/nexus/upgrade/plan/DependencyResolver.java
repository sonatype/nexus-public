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
package org.sonatype.nexus.upgrade.plan;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.goodies.common.OID;
import org.sonatype.nexus.common.text.Plural;
import org.sonatype.nexus.upgrade.plan.DependencySource.DependsOnAware;
import org.sonatype.nexus.upgrade.plan.DependencySource.UnresolvedDependencyAware;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.codehaus.plexus.util.dag.DAG;
import org.codehaus.plexus.util.dag.TopologicalSorter;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * {@link Dependency} resolver.
 *
 * @since 3.1
 */
public class DependencyResolver<T extends DependencySource<T>>
    extends ComponentSupport
{
  boolean warnOnMissingDependencies;

  /**
   * Mapping of label to source.
   */
  private final Map<String, T> sources = new HashMap<>();

  /**
   * Helper to generate string label for source.
   */
  private static String labelOf(final DependencySource<?> source) {
    return OID.render(source);
  }

  /**
   * Add sources.
   */
  @SafeVarargs
  public final void add(final T... sources) {
    checkNotNull(sources);
    for (T source : sources) {
      // object equality is important for label generation ATM, so complain if there are duplicates
      checkState(!this.sources.containsValue(source), "Duplicate source: %s", source);
      this.sources.put(labelOf(source), source);
    }
  }

  /**
   * Add sources.
   */
  public void add(final Collection<T> sources) {
    checkNotNull(sources);
    for (T source : sources) {
      add(source);
    }
  }

  /**
   * Resolution.
   */
  public static class Resolution<T>
  {
    final List<T> ordered;

    final Multimap<T, T> dependsOn;

    Resolution(final List<T> ordered, final Multimap<T, T> dependsOn) {
      this.ordered = ImmutableList.copyOf(ordered);
      this.dependsOn = ImmutableMultimap.copyOf(dependsOn);
    }
  }

  /**
   * Resolve dependencies.
   *
   * @throws UnresolvedDependencyException Thrown after resolution if any sources have unresolved dependencies.
   * @throws CyclicDependencyException Thrown as resolving if any dependencies introduce cycles.
   */
  public Resolution<T> resolve() throws UnresolvedDependencyException, CyclicDependencyException {
    checkState(!sources.isEmpty(), "one or more sources required");

    DAG graph = new DAG();
    Multimap<DependencySource<?>, Dependency<?>> unresolved = HashMultimap.create();

    try {
      for (Map.Entry<String, T> entry : sources.entrySet()) {
        String label = entry.getKey();
        T source = entry.getValue();
        // register source with graph
        graph.addVertex(label);

        // scan for satisfied dependencies
        for (Dependency<T> dependency : source.getDependencies()) {
          boolean satisfied = false;

          for (Map.Entry<String, T> innerEntry : sources.entrySet()) {
            T _source = innerEntry.getValue();
            // skip originating source from checking its own dependencies
            if (_source != source && dependency.satisfiedBy(_source)) {
              log.debug("{} depends on {}", source, _source);
              graph.addEdge(label, innerEntry.getKey());
              satisfied = true;
            }
          }

          if (!satisfied) {
            log.warn("{} requires {}", source, dependency);
            unresolved.put(source, dependency);
          }
        }
      }
    }
    catch (CycleDetectedException e) {
      // translate DAG exception
      List<T> cycle = e.getCycle().stream().map(sources::get).toList();
      throw new CyclicDependencyException(cycle);
    }

    // build depends-on graph
    Multimap<T, T> dependsOn = HashMultimap.create();
    for (Map.Entry<String, T> entry : sources.entrySet()) {
      String label = entry.getKey();
      T source = entry.getValue();
      for (String child : graph.getChildLabels(label)) {
        dependsOn.put(source, sources.get(child));
      }
      if (source instanceof DependsOnAware) {
        ((DependsOnAware) source).setDependsOn(dependsOn.get(source));
      }
    }

    // handle unresolved dependencies
    if (!unresolved.isEmpty()) {
      for (DependencySource<?> source : unresolved.keySet()) {
        if (source instanceof UnresolvedDependencyAware) {
          try {
            ((UnresolvedDependencyAware) source).setUnresolvedDependencies(unresolved.get(source));
          }
          catch (Exception e) {
            throw new RuntimeException(e);
          }
        }
      }
      UnresolvedDependencyException dependencyException = new UnresolvedDependencyException(unresolved);
      if (warnOnMissingDependencies) {
        log.warn(dependencyException.getMessage());
      }
      else {
        throw dependencyException;
      }
    }

    return new Resolution<>(TopologicalSorter.sort(graph).stream().map(sources::get).toList(), dependsOn);
  }

  /**
   * Thrown when a cycle is detected.
   */
  public static class CyclicDependencyException
      extends RuntimeException
  {
    private final List<?> cycle;

    CyclicDependencyException(final List<?> cycle) {
      this.cycle = ImmutableList.copyOf(cycle);
    }

    @Override
    public String getMessage() {
      return "Cycle detected: " + String.join(" --> ", cycle.stream().map(Object::toString).toList());
    }
  }

  /**
   * Thrown when an unresolved dependency is detected.
   */
  public static class UnresolvedDependencyException
      extends RuntimeException
  {
    private final Multimap<DependencySource<?>, Dependency<?>> unresolved;

    UnresolvedDependencyException(final Multimap<DependencySource<?>, Dependency<?>> unresolved) {
      this.unresolved = ImmutableMultimap.copyOf(unresolved);
    }

    @Override
    public String getMessage() {
      StringBuilder buff = new StringBuilder();
      Plural.append(buff, unresolved.size(), "unresolved dependency", "unresolved dependencies");
      buff.append(": ");
      for (DependencySource<?> source : unresolved.keySet()) {
        buff.append(source)
            .append(" requires ")
            .append(String.join(" AND ", unresolved.get(source).stream().map(Object::toString).toList()));
        buff.append(", ");
      }
      return buff.toString();
    }
  }
}
