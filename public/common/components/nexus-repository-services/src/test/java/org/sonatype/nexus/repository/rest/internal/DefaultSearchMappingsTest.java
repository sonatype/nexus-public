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
package org.sonatype.nexus.repository.rest.internal;

import java.util.stream.StreamSupport;

import org.sonatype.nexus.repository.rest.SearchMapping;
import org.sonatype.nexus.repository.rest.sql.SearchField;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link DefaultSearchMappings}.
 */
public class DefaultSearchMappingsTest
{
  private DefaultSearchMappings underTest;

  @Before
  public void setUp() {
    underTest = new DefaultSearchMappings();
  }

  @Test
  public void testGet_ReturnsSearchMappings() {
    Iterable<SearchMapping> mappings = underTest.get();

    assertThat(mappings, is(notNullValue()));
    assertTrue("Should contain at least one mapping",
        StreamSupport.stream(mappings.spliterator(), false).findAny().isPresent());
  }

  @Test
  public void testGet_ContainsLastUpdatedMapping() {
    Iterable<SearchMapping> mappings = underTest.get();

    boolean hasLastUpdatedMapping = StreamSupport.stream(mappings.spliterator(), false)
        .anyMatch(mapping -> "last_updated".equals(mapping.getAlias())
            && "last_modified".equals(mapping.getAttribute())
            && SearchField.LAST_MODIFIED.equals(mapping.getField()));

    assertTrue("Should contain last_updated mapping to LAST_MODIFIED field", hasLastUpdatedMapping);
  }

  @Test
  public void testGet_ContainsLastBlobUpdatedMapping() {
    Iterable<SearchMapping> mappings = underTest.get();

    boolean hasLastBlobUpdatedMapping = StreamSupport.stream(mappings.spliterator(), false)
        .anyMatch(mapping -> "lastBlobUpdated".equals(mapping.getAlias())
            && "last_modified".equals(mapping.getAttribute())
            && SearchField.LAST_MODIFIED.equals(mapping.getField()));

    assertTrue("Should contain lastBlobUpdated mapping to LAST_MODIFIED field", hasLastBlobUpdatedMapping);
  }

  @Test
  public void testGet_ContainsExpectedCoreMappings() {
    Iterable<SearchMapping> mappings = underTest.get();

    // Verify some core mappings exist
    boolean hasRepositoryMapping = StreamSupport.stream(mappings.spliterator(), false)
        .anyMatch(mapping -> "repository".equals(mapping.getAlias()));
    boolean hasFormatMapping = StreamSupport.stream(mappings.spliterator(), false)
        .anyMatch(mapping -> "format".equals(mapping.getAlias()));
    boolean hasGroupMapping = StreamSupport.stream(mappings.spliterator(), false)
        .anyMatch(mapping -> "group".equals(mapping.getAlias()));

    assertTrue("Should contain repository mapping", hasRepositoryMapping);
    assertTrue("Should contain format mapping", hasFormatMapping);
    assertTrue("Should contain group mapping", hasGroupMapping);
  }
}
