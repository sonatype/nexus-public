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

import java.util.regex.Pattern

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.upgrade.plan.DependencyResolver.CyclicDependencyException
import org.sonatype.nexus.upgrade.plan.DependencyResolver.UnresolvedDependencyException

import org.junit.Before
import org.junit.Test

/**
 * Tests for {@link DependencyResolver}.
 */
class DependencyResolverTest
    extends TestSupport
{
  static class Thing
      implements DependencySource<Thing>, DependencySource.DependsOnAware<Thing>
  {
    String id

    List<Dependency<Thing>> dependencies = []

    Collection<Thing> dependsOn

    /**
     * Creates a dependency which requires a thing with the given identifier.
     */
    static Dependency<Thing> dependency(final String id) {
      return new Dependency<Thing>() {
        @Override
        boolean satisfiedBy(final Thing other) {
          return other.id == id
        }

        @Override
        String toString() {
          return "EXISTS($id)"
        }
      }
    }

    /**
     * Creates a dependency which requires a thing which has and identifier matching the given pattern.
     */
    static Dependency<Thing> dependency(final Pattern pattern) {
      return new Dependency<Thing>() {
        @Override
        boolean satisfiedBy(final Thing other) {
          return pattern.matcher(other.id).matches()
        }

        @Override
        String toString() {
          return "MATCHES($pattern)"
        }
      }
    }

    @Override
    public String toString() {
      return getClass().getSimpleName() + "{" +
          "id='" + id + '\'' +
          '}';
    }
  }

  private DependencyResolver<Thing> resolver

  @Before
  void setUp() {
    resolver = new DependencyResolver<Thing>()
  }

  @Test(expected = IllegalStateException.class)
  void 'can not add duplicate sources'() {
    Thing thing = new Thing(id: 'foo')
    resolver.add(thing, thing)
  }

  @Test(expected = IllegalStateException.class)
  void 'at least one source required to resolve'() {
    resolver.resolve()
  }

  @Test
  void 'complex usage'() {
    resolver.add(
        new Thing(id: 'structure.house', dependencies: [
            // a bunch of rooms
            Thing.dependency(~/room\..*/),
            // and a garage
            Thing.dependency('structure.garage')
        ]),
        new Thing(id: 'structure.garage'),
        new Thing(id: 'room.kitchen'),
        new Thing(id: 'room.bathroom'),
        new Thing(id: 'room.bedroom', dependencies: [
            Thing.dependency('room.bathroom')
        ]),
        new Thing(id: 'room.living', dependencies: [
            // everything connects to living room
            Thing.dependency(~/room\..*/)
        ]),
        new Thing(id: 'car.toyota', dependencies: [
            // car needs a garage
            Thing.dependency('structure.garage')
        ]),
        new Thing(id: 'structure.gazebo')
    )

    def resolved = resolver.resolve().ordered
    assert resolved.size() == 8

    resolved.each {log it}

    def beforeOther = {thing, other ->
      assert resolved.findIndexOf {it.id == thing} < resolved.findIndexOf {it.id == other}
    }

    // all rooms before house
    resolved.findAll {it.id =~ /room\..*/}.each {
      beforeOther it.id, 'structure.house'
    }

    // garage is before house
    beforeOther 'structure.garage', 'structure.house'

    // bathroom is before bedroom
    beforeOther 'room.bathroom', 'room.bedroom'
  }

  @Test
  void 'resolve orders based on dependencies'() {
    resolver.add(
        new Thing(id: 'a', dependencies: [
            Thing.dependency('b')
        ]),
        new Thing(id: 'b', dependencies: [
            Thing.dependency('c')
        ]),
        new Thing(id: 'c')
    )
    def ordered = resolver.resolve().ordered

    assert ordered.size() == 3
    assert ordered[0].id == 'c'
    assert ordered[1].id == 'b'
    assert ordered[2].id == 'a'
  }

  @Test
  void 'resolve collects depends on'() {
    def a = new Thing(id: 'a', dependencies: [
        Thing.dependency('b')
    ])
    def b = new Thing(id: 'b', dependencies: [
        Thing.dependency('c')
    ])
    def c = new Thing(id: 'c')

    resolver.add(a, b, c)
    def dependsOn = resolver.resolve().dependsOn

    assert dependsOn.containsEntry(a, b)
    assert dependsOn.containsEntry(b, c)
    assert !dependsOn.containsKey(c)

    // aware
    assert a.dependsOn != null
    assert a.dependsOn.contains(b)
    assert b.dependsOn != null
    assert b.dependsOn.contains(c)
    assert c.dependsOn != null
    assert c.dependsOn.empty
  }

  @Test(expected = CyclicDependencyException.class)
  void 'cyclic dependency throws exception'() {
    resolver.add(
        new Thing(id: 'a', dependencies: [
            Thing.dependency('b')
        ]),
        new Thing(id: 'b', dependencies: [
            Thing.dependency('a')
        ])
    )
    resolver.resolve()
  }

  @Test(expected = UnresolvedDependencyException.class)
  void 'unresolved dependency throws exception'() {
    resolver.add(
        new Thing(id: 'a', dependencies: [
            Thing.dependency('b')
        ])
    )
    resolver.resolve()
  }
}
