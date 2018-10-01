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
package org.sonatype.nexus.repository.search;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;

import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.FORMAT;

/**
 * Generates search results consumable by the UI
 *
 * @since 3.14
 */
@Named
@Singleton
public class SearchResultsGeneratorImpl
    implements SearchResultsGenerator
{

  private final Map<String, SearchResultComponentGenerator> searchResultComponentGeneratorMap;

  private final SearchResultComponentGenerator defaultSearchResultComponentGenerator;

  @Inject
  SearchResultsGeneratorImpl(
      final Map<String, SearchResultComponentGenerator> searchResultComponentGeneratorMap)
  {
    this.searchResultComponentGeneratorMap = searchResultComponentGeneratorMap;
    this.defaultSearchResultComponentGenerator = searchResultComponentGeneratorMap
        .get(DefaultSearchResultComponentGenerator.DEFAULT_SEARCH_RESULT_COMPONENT_GENERATOR_KEY);
  }

  @Override
  public List<SearchResultComponent> getSearchResultList(final SearchResponse response) {
    List<SearchResultComponent> searchResultComponents = new ArrayList<>((int) response.getHits().getTotalHits());
    Set<String> componentIdSet = new HashSet<>();

    for (SearchHit hit : response.getHits().getHits()) {
      Map<String, Object> source = hit.getSource();
      String format = source.get(FORMAT).toString();
      SearchResultComponentGenerator generator = searchResultComponentGeneratorMap
          .getOrDefault(format, defaultSearchResultComponentGenerator);
      SearchResultComponent component = generator.from(hit, componentIdSet);
      if (null != component) {
        componentIdSet.add(component.getId());
        searchResultComponents.add(component);
      }
    }

    return searchResultComponents;
  }
}
