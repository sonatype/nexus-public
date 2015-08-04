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
package org.sonatype.nexus.bootstrap;

import java.util.Map;

import org.sonatype.nexus.bootstrap.ConfigurationBuilder.Customizer;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

/**
 * Tests for {@link ConfigurationBuilder}.
 */
public class ConfigurationBuilderTest
    extends TestSupport
{
  private ConfigurationBuilder underTest;

  @Before
  public void setUp() throws Exception {
    this.underTest = new ConfigurationBuilder();
  }

  @Test(expected = IllegalStateException.class)
  public void build_notConfigured() throws Exception {
    // at least 1 property must be configured before calling build()
    underTest.build();
  }

  @Test
  public void set_withNulls() throws Exception {
    try {
      underTest.set("foo", null);
      fail();
    }
    catch (NullPointerException expected) {
      // ignore
    }

    try {
      underTest.set(null, "bar");
      fail();
    }
    catch (NullPointerException expected) {
      // ignore
    }
  }

  @Test
  public void set_overridesPrevious() throws Exception {
    underTest.set("foo", "bar");
    underTest.set("foo", "baz");

    Map<String, String> config = underTest.build();

    assertThat(config.entrySet(), hasSize(1));
    assertThat(config.get("foo"), is("baz"));
  }

  @Test
  public void properties_withMap() throws Exception {
    Map<String,String> properties = ImmutableMap.of(
        "foo", "bar",
        "a", "b"
    );
    underTest.properties(properties);

    Map<String, String> config = underTest.build();

    assertThat(config.entrySet(), hasSize(2));
    assertThat(config.get("foo"), is("bar"));
    assertThat(config.get("a"), is("b"));
  }

  @Test
  public void override() throws Exception {
    underTest.set("foo", "bar");

    // override "foo" value, but provide other values which will not be set
    Map<String, String> overrides = Maps.newHashMap();
    overrides.put("a", "b"); // this is not in original set and will not apply to configuration
    overrides.put("foo", "baz");
    underTest.override(overrides);

    Map<String, String> config = underTest.build();

    assertThat(config.entrySet(), hasSize(1));
    assertThat(config.get("foo"), is("baz"));
    // ^^^ implies config.containsKey("a") is false
  }

  @Test
  public void interpolation() throws Exception {
    underTest.set("foo", "bar");
    underTest.set("baz", "${foo}");

    Map<String, String> config = underTest.build();

    assertThat(config.entrySet(), hasSize(2));
    assertThat(config.get("foo"), is("bar"));
    assertThat(config.get("baz"), is("bar"));
  }

  @Test
  public void customizer() throws Exception {
    underTest.custom(new Customizer() {
      @Override
      public void apply(final ConfigurationBuilder builder) throws Exception {
        builder.set("foo", "bar");
      }
    });

    Map<String, String> config = underTest.build();

    assertThat(config.entrySet(), hasSize(1));
    assertThat(config.get("foo"), is("bar"));
  }
}
