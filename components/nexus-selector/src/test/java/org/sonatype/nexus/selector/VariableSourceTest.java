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
package org.sonatype.nexus.selector;

import org.junit.Test;

import static com.google.common.collect.ImmutableMap.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VariableSourceTest
{
  static final String FOO_BAR = "foo.bar";
  static final String FOO = "foo";
  static final String MOO = "moo";
  static final String BAZ = "baz";
  static final String GOO = "goo";
  static final String BAR = "bar";

  @Test
  public void test3ResolversFirstWins() {
    {
      VariableSource source = new VariableSourceBuilder()
          .addResolver(new ConstantVariableResolver(MOO, FOO_BAR))
          .addResolver(new ConstantVariableResolver(BAZ, FOO_BAR))
          .addResolver(new PropertiesResolver<>(FOO, of(BAR, GOO)))
          .build();
      assertEquals(1, source.getVariableSet().size());
      assertTrue(source.getVariableSet().contains(FOO_BAR));
      assertEquals(MOO, source.get(FOO_BAR).get());
    }
    {
      VariableSource source = new VariableSourceBuilder()
          .addResolver(new ConstantVariableResolver(BAZ, FOO_BAR))
          .addResolver(new PropertiesResolver<>(FOO, of(BAR, GOO)))
          .addResolver(new ConstantVariableResolver(MOO, FOO_BAR))
          .build();
      assertEquals(1, source.getVariableSet().size());
      assertTrue(source.getVariableSet().contains(FOO_BAR));
      assertEquals(BAZ, source.get(FOO_BAR).get());
    }
    {
      VariableSource source = new VariableSourceBuilder()
          .addResolver(new PropertiesResolver<>(FOO, of(BAR, GOO)))
          .addResolver(new ConstantVariableResolver(MOO, FOO_BAR))
          .addResolver(new ConstantVariableResolver(BAZ, FOO_BAR))
          .build();
      assertEquals(1, source.getVariableSet().size());
      assertTrue(source.getVariableSet().contains(FOO_BAR));
      assertEquals(GOO, source.get(FOO_BAR).get());
    }
  }

  @Test
  public void testNoResolvers() {
    VariableSource source = new VariableSourceBuilder().build();
    assertEquals(0, source.getVariableSet().size());
    assertFalse(source.getVariableSet().contains(FOO_BAR));
    assertFalse(source.get(FOO_BAR).isPresent());
  }
}
