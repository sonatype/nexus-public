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
package org.sonatype.nexus.repository.search.sql;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.rest.SearchMappings;
import org.sonatype.nexus.repository.rest.sql.SearchField;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provides a mapping from search attribute or alias to {@link SearchField}
 */
@Named
@Singleton
public class SearchMappingService
{
  private final Map<String, SearchField> attributeToField = new HashMap<>();

  private final Map<String, Boolean> attributeToExactMatch = new HashMap<>();

  @Inject
  public SearchMappingService(final List<SearchMappings> searchMappings) {
    checkNotNull(searchMappings).stream()
        .map(SearchMappings::get)
        .flatMap(iterable -> StreamSupport.stream(iterable.spliterator(), false))
        .forEach(mapping -> {
            attributeToField.put(mapping.getAttribute(), mapping.getField());
            attributeToExactMatch.put(mapping.getAttribute(), mapping.isExactMatch());
            attributeToField.putIfAbsent(mapping.getAlias(), mapping.getField());
            attributeToExactMatch.put(mapping.getAlias(), mapping.isExactMatch());
        });
  }

  /**
   * Provides the mapping from search attribute or alias to {@link SearchField}
   */
  public Optional<SearchField> getSearchField(final String identifier) {
    return Optional.ofNullable(attributeToField.get(identifier));
  }

  /**
   * Returns a boolean from search mappings indicating whether the field should be treated as an exact match. False is
   * returned if the mapping is unknown.
   */
  public boolean isExactMatch(final String identifier) {
    return Optional.ofNullable(attributeToExactMatch.get(identifier))
        .orElse(Boolean.FALSE);
  }
}
