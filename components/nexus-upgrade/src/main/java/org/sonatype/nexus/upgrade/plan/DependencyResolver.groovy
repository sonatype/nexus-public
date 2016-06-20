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
package org.sonatype.nexus.upgrade.plan

import org.sonatype.goodies.common.ComponentSupport
import org.sonatype.goodies.common.OID
import org.sonatype.nexus.common.text.Plural

import com.google.common.collect.HashMultimap
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import org.codehaus.plexus.util.dag.CycleDetectedException
import org.codehaus.plexus.util.dag.DAG
import org.codehaus.plexus.util.dag.TopologicalSorter

import static com.google.common.base.Preconditions.checkNotNull
import static com.google.common.base.Preconditions.checkState

/**
 * {@link Dependency} resolver.
 *
 * @since 3.1
 */
class DependencyResolver<T extends DependencySource>
  extends ComponentSupport
{
  /**
   * Mapping of label to source.
   */
  private final Map<String,T> sources = [:]

  /**
   * Helper to generate string label for source.
   */
  private static String labelOf(final DependencySource<T> source) {
    return OID.render(source)
  }

  /**
   * Add sources.
   */
  void add(final T... sources) {
    checkNotNull sources
    for (source in sources) {
      // object equality is important for label generation ATM, so complain if there are duplicates
      checkState !this.sources.containsValue(source), 'Duplicate source: %s', source
      this.sources[labelOf(source)] = source
    }
  }

  /**
   * Add sources.
   */
  void add(final Collection<T> sources) {
    checkNotNull sources
    for (source in sources) {
      add(source)
    }
  }

  /**
   * Resolution.
   */
  public static class Resolution<T>
  {
    final List<T> ordered

    final Multimap<T,T> dependsOn

    Resolution(final List<T> ordered, final Multimap<T, T> dependsOn) {
      this.ordered = ImmutableList.copyOf(ordered)
      this.dependsOn = ImmutableMultimap.copyOf(dependsOn)
    }
  }

  /**
   * Resolve dependencies.
   *
   * @throws UnresolvedDependencyException  Thrown after resolution if any sources have unresolved dependencies.
   * @throws CyclicDependencyException      Thrown as resolving if any dependencies introduce cycles.
   */
  Resolution<T> resolve() {
    //noinspection GroovyAssignabilityCheck
    checkState !sources.isEmpty(), 'one or more sources required'

    DAG graph = new DAG()
    Multimap<T,Dependency> unresolved = HashMultimap.create()

    try {
      sources.each {label, source ->
        // register source with graph
        graph.addVertex(label)

        // scan for satisfied dependencies
        for (dependency in source.dependencies) {
          boolean satisfied = false

          sources.each {_label, _source ->
            // skip originating source from checking its own dependencies
            if (_source != source && dependency.satisfiedBy(_source)) {
              log.debug "$source depends on $_source"
              graph.addEdge(label, _label)
              satisfied = true
            }
          }

          if (!satisfied) {
            log.warn "$source requires $dependency"
            unresolved.put(source, dependency)
          }
        }
      }
    }
    catch (CycleDetectedException e) {
      // translate DAG exception
      List<T> cycle = e.cycle.collect { sources[it] }
      throw new CyclicDependencyException(cycle)
    }

    // build depends-on graph
    Multimap<T,T> dependsOn = HashMultimap.create()
    sources.each { label, source ->
      for (child in graph.getChildLabels(label)) {
        dependsOn.put(source, sources[child])
      }
      if (source instanceof DependencySource.DependsOnAware) {
        source.dependsOn = dependsOn.get(source)
      }
    }

    // handle unresolved dependencies
    if (!unresolved.empty) {
      for (source in unresolved.keySet()) {
        if (source instanceof DependencySource.UnresolvedDependencyAware) {
          source.unresolvedDependencies = unresolved.get(source)
        }
      }
      throw new UnresolvedDependencyException(unresolved)
    }

    return new Resolution<T>(TopologicalSorter.sort(graph).collect { sources[it] }, dependsOn)
  }

  /**
   * Thrown when a cycle is detected.
   */
  static class CyclicDependencyException<T>
    extends RuntimeException
  {
    final List<T> cycle

    CyclicDependencyException(final List<T> cycle) {
      this.cycle = ImmutableList.copyOf(cycle)
    }

    @Override
    String getMessage() {
      return "Cycle detected: ${cycle.join(' --> ')}"
    }
  }

  /**
   * Thrown when an unresolved dependency is detected.
   */
  static class UnresolvedDependencyException<T>
    extends RuntimeException
  {
    final Multimap<T,Dependency> unresolved

    UnresolvedDependencyException(final Multimap<T,Dependency> unresolved) {
      this.unresolved = ImmutableMultimap.copyOf(unresolved)
    }

    @Override
    String getMessage() {
      def count = unresolved.size()
      def buff = new StringBuilder()
      Plural.append(buff, count, 'unresolved dependency', 'unresolved dependencies')
      buff.append(': ')
      def iter = unresolved.keySet().iterator()
      while (iter.hasNext()) {
        def source = iter.next()
        buff.append(source).append(' requires ').append(unresolved.get(source).join(' AND '))
        if (iter.hasNext()) {
          buff.append(', ')
        }
      }
      return buff.toString()
    }
  }
}
