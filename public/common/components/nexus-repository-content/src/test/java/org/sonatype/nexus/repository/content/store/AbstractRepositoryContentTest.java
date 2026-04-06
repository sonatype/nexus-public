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
package org.sonatype.nexus.repository.content.store;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class AbstractRepositoryContentTest
    extends TestSupport
{
  // Concrete subclass for testing
  private static class TestRepositoryContent
      extends AbstractRepositoryContent
  {
  }

  @Test
  public void testDefaultAttributes() {
    TestRepositoryContent underTest = new TestRepositoryContent();
    assertThat(underTest.attributes(), is(notNullValue()));
    assertThat(underTest.attributes().isEmpty(), is(true));
  }

  @Test
  public void testDefaultTimestampsAreNull() {
    TestRepositoryContent underTest = new TestRepositoryContent();
    assertThat(underTest.created(), is(nullValue()));
    assertThat(underTest.lastUpdated(), is(nullValue()));
  }

  @Test
  public void testSetAndGetRepositoryId() {
    TestRepositoryContent underTest = new TestRepositoryContent();
    underTest.setRepositoryId(42);
    assertThat(underTest.repositoryId, is(42));
  }

  @Test
  public void testSetAndGetAttributes() {
    TestRepositoryContent underTest = new TestRepositoryContent();
    NestedAttributesMap attrs = new NestedAttributesMap("attributes", new HashMap<>());
    attrs.set("key", "value");
    underTest.setAttributes(attrs);

    assertThat(underTest.attributes().get("key"), is("value"));
  }

  @Test
  public void testSetAndGetCreated() {
    TestRepositoryContent underTest = new TestRepositoryContent();
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    underTest.setCreated(now);

    assertThat(underTest.created(), is(now));
  }

  @Test
  public void testSetAndGetLastUpdated() {
    TestRepositoryContent underTest = new TestRepositoryContent();
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    underTest.setLastUpdated(now);

    assertThat(underTest.lastUpdated(), is(now));
  }

  @Test
  public void testToString() {
    TestRepositoryContent underTest = new TestRepositoryContent();
    underTest.setRepositoryId(7);
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    underTest.setCreated(now);
    underTest.setLastUpdated(now);

    String result = underTest.toString();
    assertThat(result, containsString("repositoryId=7"));
    assertThat(result, containsString("created="));
    assertThat(result, containsString("lastUpdated="));
  }

  @Test(expected = NullPointerException.class)
  public void testSetNullAttributesThrows() {
    TestRepositoryContent underTest = new TestRepositoryContent();
    underTest.setAttributes(null);
  }

  @Test(expected = NullPointerException.class)
  public void testSetNullCreatedThrows() {
    TestRepositoryContent underTest = new TestRepositoryContent();
    underTest.setCreated(null);
  }

  @Test(expected = NullPointerException.class)
  public void testSetNullLastUpdatedThrows() {
    TestRepositoryContent underTest = new TestRepositoryContent();
    underTest.setLastUpdated(null);
  }
}
