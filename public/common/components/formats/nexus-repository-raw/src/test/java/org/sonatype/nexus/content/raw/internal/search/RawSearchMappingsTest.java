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
package org.sonatype.nexus.content.raw.internal.search;

import java.util.List;
import java.util.stream.StreamSupport;

import org.sonatype.nexus.repository.rest.SearchMapping;
import org.sonatype.nexus.repository.rest.sql.SearchField;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RawSearchMappingsTest
{
  private final RawSearchMappings underTest = new RawSearchMappings();

  @Test
  void rawNameMappingIsRegistered() {
    List<SearchMapping> mappings = StreamSupport.stream(underTest.get().spliterator(), false)
        .toList();

    assertThat(mappings).hasSize(1);
    SearchMapping mapping = mappings.get(0);
    assertThat(mapping.getAlias()).isEqualTo(RawSearchMappings.RAW_NAME);
    assertThat(mapping.getAttribute()).isEqualTo(RawSearchMappings.RAW_NAME_ATTRIBUTE);
    assertThat(mapping.getDescription()).isEqualTo("Raw asset filename (basename)");
    assertThat(mapping.getField()).isEqualTo(SearchField.NAME);
  }

  @Test
  void rawNameMappingIsNotExactMatch() {
    SearchMapping mapping = StreamSupport.stream(underTest.get().spliterator(), false)
        .findFirst()
        .orElseThrow();

    assertThat(mapping.isExactMatch()).isFalse();
  }
}
