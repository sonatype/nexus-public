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
package org.sonatype.nexus.repository.npm.internal.search.v1;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.search.query.SearchQueryService;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.ContentTypes;
import org.sonatype.nexus.repository.view.Parameters;
import org.sonatype.nexus.repository.view.payloads.StringPayload;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singletonList;
import static org.sonatype.nexus.repository.search.query.RepositoryQueryBuilder.repositoryQuery;

/**
 * Implementation of {@code NpmSearchFacet} for proxy repositories.
 *
 * @since 3.7
 */
@Named
public class NpmSearchFacetHosted
    extends FacetSupport
    implements NpmSearchFacet
{
  private final SearchQueryService searchQueryService;

  private final NpmSearchParameterExtractor npmSearchParameterExtractor;

  private final NpmSearchResponseFactory npmSearchResponseFactory;

  private final NpmSearchResponseMapper npmSearchResponseMapper;

  private final int v1SearchMaxResults;

  @Inject
  public NpmSearchFacetHosted(final SearchQueryService searchQueryService,
                              final NpmSearchParameterExtractor npmSearchParameterExtractor,
                              final NpmSearchResponseFactory npmSearchResponseFactory,
                              final NpmSearchResponseMapper npmSearchResponseMapper,
                              @Named("${nexus.npm.v1SearchMaxResults:-250}") final int v1SearchMaxResults)
  {
    this.searchQueryService = checkNotNull(searchQueryService);
    this.npmSearchParameterExtractor = checkNotNull(npmSearchParameterExtractor);
    this.npmSearchResponseFactory = checkNotNull(npmSearchResponseFactory);
    this.npmSearchResponseMapper = checkNotNull(npmSearchResponseMapper);
    this.v1SearchMaxResults = v1SearchMaxResults;
  }

  public Content searchV1(final Parameters parameters) throws IOException {
    String text = npmSearchParameterExtractor.extractText(parameters);
    int size = npmSearchParameterExtractor.extractSize(parameters);
    int from = npmSearchParameterExtractor.extractFrom(parameters);

    // npm search V1 endpoint currently returns an empty result set if no text is provided in the request
    NpmSearchResponse response;
    if (text.isEmpty()) {
      response = npmSearchResponseFactory.buildEmptyResponse();
    }
    else {
      QueryStringQueryBuilder query = QueryBuilders.queryStringQuery(text)
          .allowLeadingWildcard(true)
          .analyzeWildcard(true);
      TermsBuilder terms = AggregationBuilders.terms("name")
          .field("assets.attributes.npm.name")
          .size(v1SearchMaxResults)
          .subAggregation(AggregationBuilders.topHits("versions")
              .addSort(SortBuilders.fieldSort("assets.attributes.npm.search_normalized_version")
                  .order(SortOrder.DESC))
              .setTrackScores(true)
              .setSize(1));

      SearchResponse searchResponse = searchQueryService.search(
          repositoryQuery(query).inRepositories(getRepository()), singletonList(terms));
      Aggregations aggregations = searchResponse.getAggregations();
      Terms nameTerms = aggregations.get("name");
      response = npmSearchResponseFactory.buildResponseForResults(nameTerms.getBuckets(), size, from);
    }

    String content = npmSearchResponseMapper.writeString(response);
    return new Content(new StringPayload(content, ContentTypes.APPLICATION_JSON));
  }
}
