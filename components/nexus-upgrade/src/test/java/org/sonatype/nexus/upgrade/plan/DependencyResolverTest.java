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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.upgrade.plan.DependencyResolver.CyclicDependencyException;
import org.sonatype.nexus.upgrade.plan.DependencyResolver.UnresolvedDependencyException;
import org.sonatype.nexus.upgrade.plan.DependencySource.DependsOnAware;

import com.google.common.collect.Multimap;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link DependencyResolver}.
 */
public class DependencyResolverTest
    extends TestSupport
{

  static class Thing
      implements DependencySource<Thing>, DependsOnAware<Thing>
  {
    String id;

    List<Dependency<Thing>> dependencies = new ArrayList<>();

    Collection<Thing> dependsOn;

    /**
     * Creates a dependency which requires a thing with the given identifier.
     */
    static Dependency<Thing> dependency(final String id) {
      return new Dependency<Thing>()
      {
        @Override
        public boolean satisfiedBy(final Thing other) {
          return other.id.equals(id);
        }

        @Override
        public String toString() {
          return "EXISTS(" + id + ")";
        }
      };
    }

    /**
     * Creates a dependency which requires a thing which has an identifier matching the given pattern.
     */
    static Dependency<Thing> dependency(final Pattern pattern) {
      return new Dependency<Thing>()
      {
        @Override
        public boolean satisfiedBy(final Thing other) {
          return pattern.matcher(other.id).matches();
        }

        @Override
        public String toString() {
          return "MATCHES(" + pattern + ")";
        }
      };
    }

    @Override
    public List<Dependency<Thing>> getDependencies() {
      return dependencies;
    }

    @Override
    public String toString() {
      return getClass().getSimpleName() + "{" +
          "id='" + id + '\'' +
          '}';
    }

    @Override
    public void setDependsOn(final Collection<Thing> dependsOn) {
      this.dependsOn = dependsOn;
    }

    @Override
    public boolean equals(final Object o) {
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Thing thing = (Thing) o;
      return Objects.equals(id, thing.id);
    }

    @Override
    public int hashCode() {
      return Objects.hash(id);
    }
  }

  private DependencyResolver<Thing> resolver;

  @Before
  public void setUp() {
    resolver = new DependencyResolver<>();
  }

  @Test(expected = IllegalStateException.class)
  public void cannotAddDuplicateSources() {
    Thing thing = new Thing();
    thing.id = "foo";
    resolver.add(thing, thing);
  }

  @Test(expected = IllegalStateException.class)
  public void atLeastOneSourceRequiredToResolve() {
    resolver.resolve();
  }

  @Test
  public void complexUsage() {
    resolver.add(
        new Thing()
        {
          {
            id = "structure.house";
            dependencies = List.of(
                // a bunch of rooms
                Thing.dependency(Pattern.compile("room\\..*")),
                // and a garage
                Thing.dependency("structure.garage"));
          }
        },
        new Thing()
        {
          {
            id = "structure.garage";
          }
        },
        new Thing()
        {
          {
            id = "room.kitchen";
          }
        },
        new Thing()
        {
          {
            id = "room.bathroom";
          }
        },
        new Thing()
        {
          {
            id = "room.bedroom";
            dependencies = List.of(
                Thing.dependency("room.bathroom"));
          }
        },
        new Thing()
        {
          {
            id = "room.living";
            dependencies = List.of(
                // everything connects to living room
                Thing.dependency(Pattern.compile("room\\..*")));
          }
        },
        new Thing()
        {
          {
            id = "car.toyota";
            dependencies = List.of(
                // car needs a garage
                Thing.dependency("structure.garage"));
          }
        },
        new Thing()
        {
          {
            id = "structure.gazebo";
          }
        });

    List<Thing> resolved = resolver.resolve().ordered;
    assertEquals(8, resolved.size());

    resolved.forEach(thing -> logger.info(thing.toString()));

    // all rooms before house
    Thing house = resolved.stream().filter(t -> t.id.equals("structure.house")).findFirst().orElseThrow();
    for (Thing room : resolved.stream().filter(t -> t.id.contains("room")).toList()) {
      assertTrue(resolved.indexOf(room) < resolved.indexOf(house));
    }

    // garage is before house
    Thing garage = resolved.stream().filter(t -> t.id.equals("structure.garage")).findFirst().orElseThrow();
    assertTrue(resolved.indexOf(garage) < resolved.indexOf(house));

    // bathroom is before bedroom
    Thing bathroom = resolved.stream().filter(t -> t.id.equals("room.bathroom")).findFirst().orElseThrow();
    Thing bedroom = resolved.stream().filter(t -> t.id.equals("room.bedroom")).findFirst().orElseThrow();
    assertTrue(resolved.indexOf(bathroom) < resolved.indexOf(bedroom));
  }

  @Test
  public void resolveOrdersBasedOnDependencies() {
    resolver.add(
        new Thing()
        {
          {
            id = "a";
            dependencies = List.of(
                Thing.dependency("b"));
          }
        },
        new Thing()
        {
          {
            id = "b";
            dependencies = List.of(
                Thing.dependency("c"));
          }
        },
        new Thing()
        {
          {
            id = "c";
          }
        });
    List<Thing> ordered = resolver.resolve().ordered;

    assertEquals(3, ordered.size());
    assertEquals("c", ordered.get(0).id);
    assertEquals("b", ordered.get(1).id);
    assertEquals("a", ordered.get(2).id);
  }

  @Test
  public void resolveCollectsDependsOn() {
    Thing a = new Thing()
    {
      {
        id = "a";
        dependencies = List.of(
            Thing.dependency("b"));
      }
    };
    Thing b = new Thing()
    {
      {
        id = "b";
        dependencies = List.of(
            Thing.dependency("c"));
      }
    };
    Thing c = new Thing()
    {
      {
        id = "c";
      }
    };

    resolver.add(a, b, c);
    Multimap<Thing, Thing> dependsOn = resolver.resolve().dependsOn;

    assertTrue(dependsOn.containsEntry(a, b));
    assertTrue(dependsOn.containsEntry(b, c));
    assertTrue(!dependsOn.containsKey(c));

    assertTrue(a.dependsOn.contains(b));
    assertTrue(b.dependsOn.contains(c));
    assertTrue(c.dependsOn.isEmpty());
  }

  @Test(expected = CyclicDependencyException.class)
  public void cyclicDependencyThrowsException() {
    resolver.add(
        new Thing()
        {
          {
            id = "a";
            dependencies = List.of(
                Thing.dependency("b"));
          }
        },
        new Thing()
        {
          {
            id = "b";
            dependencies = List.of(
                Thing.dependency("a"));
          }
        });
    resolver.resolve();
  }

  @Test(expected = UnresolvedDependencyException.class)
  public void unresolvedDependencyThrowsException() {
    resolver.add(
        new Thing()
        {
          {
            id = "a";
            dependencies = List.of(
                Thing.dependency("b"));
          }
        });
    resolver.resolve();
  }
}
