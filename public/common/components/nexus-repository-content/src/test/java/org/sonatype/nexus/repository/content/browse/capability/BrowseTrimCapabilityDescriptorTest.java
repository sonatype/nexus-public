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
package org.sonatype.nexus.repository.content.browse.capability;

import java.util.Map;
import java.util.Set;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.capability.Tag;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertTrue;

public class BrowseTrimCapabilityDescriptorTest
    extends TestSupport
{
  private BrowseTrimCapabilityDescriptor underTest;

  @Before
  public void setup() {
    underTest = new BrowseTrimCapabilityDescriptor();
  }

  @Test
  public void testType() {
    assertThat(underTest.type(), is(BrowseTrimCapabilityDescriptor.TYPE));
  }

  @Test
  public void testName() {
    assertThat(underTest.name(), is(notNullValue()));
  }

  @Test
  public void testAbout() {
    String about = underTest.about();
    assertThat(about, is(notNullValue()));
    assertThat(about, containsString("trim"));
  }

  @Test
  public void testFormFields() {
    assertThat(underTest.formFields(), hasSize(2));
  }

  @Test
  public void testCreateConfig() {
    Map<String, String> properties = Map.of(
        BrowseTrimCapabilityConfiguration.POSTGRESQL_TRIM_ENABLED, "true",
        BrowseTrimCapabilityConfiguration.BATCH_TRIM_ENABLED, "false");

    BrowseTrimCapabilityConfiguration config = underTest.createConfig(properties);

    assertThat(config, is(notNullValue()));
    assertThat(config, instanceOf(BrowseTrimCapabilityConfiguration.class));
    assertThat(config.isPostgresqlTrimEnabled(), is(true));
    assertThat(config.isBatchTrimEnabled(), is(false));
  }

  @Test
  public void testGetTags() {
    Set<Tag> tags = underTest.getTags();

    assertThat(tags, is(notNullValue()));
    assertThat(tags, hasSize(1));
    assertTrue(tags.contains(Tag.categoryTag("Repository")));
  }

  @Test
  public void testIsExposed() {
    assertTrue(underTest.isExposed());
  }

  @Test
  public void testTypeIdConstant() {
    assertThat(BrowseTrimCapabilityDescriptor.TYPE_ID, is("browse.trim"));
    assertThat(BrowseTrimCapabilityDescriptor.TYPE, is(CapabilityType.capabilityType("browse.trim")));
  }
}
