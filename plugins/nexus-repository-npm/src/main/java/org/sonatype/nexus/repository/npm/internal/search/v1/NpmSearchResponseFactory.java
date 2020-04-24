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

import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.npm.internal.search.v1.orient.NpmSearchHitExtractor;

import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHits;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

/**
 * Factory for creating npm V1 search responses from the appropriate source information. This is primarily intended for
 * use in marshaling search results from Elasticsearch into the JSON payloads to be returned to the client.
 *
 * @since 3.7
 */
@Named
@Singleton
public class NpmSearchResponseFactory
    extends ComponentSupport
{
  private static final String SEARCH_RESPONSE_PACKAGE_DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

  private static final String SEARCH_RESPONSE_DATE_PATTERN = "EEE MMM dd yyyy HH:mm:ss 'GMT'Z '('z')'";

  private static final DateTimeFormatter SEARCH_RESPONSE_PACKAGE_DATE_FORMAT = DateTimeFormat
      .forPattern(SEARCH_RESPONSE_PACKAGE_DATE_PATTERN);

  private static final DateTimeFormatter SEARCH_RESPONSE_DATE_FORMAT = DateTimeFormat
      .forPattern(SEARCH_RESPONSE_DATE_PATTERN);

  private static final Double DEFAULT_SCORE = 0.0;

  private final NpmSearchHitExtractor npmSearchHitExtractor;

  @Inject
  public NpmSearchResponseFactory(final NpmSearchHitExtractor npmSearchHitExtractor) {
    this.npmSearchHitExtractor = checkNotNull(npmSearchHitExtractor);
  }

  /**
   * Builds an empty search response (used in the scenario where no search text was provided, to mimic npm registry
   * behavior as of this writing.
   */
  public NpmSearchResponse buildEmptyResponse() {
    NpmSearchResponse response = new NpmSearchResponse();
    response.setObjects(emptyList());
    response.setTime(formatSearchResponseDate(DateTime.now()));
    response.setTotal(0);
    return response;
  }

  /**
   * Builds a search response containing each of the included search buckets.
   */
  public NpmSearchResponse buildResponseForResults(final List<Terms.Bucket> buckets, final int size, final int from) {
    List<NpmSearchResponseObject> objects = buckets.stream()
        .map(bucket -> (TopHits) bucket.getAggregations().get("versions"))
        .map(TopHits::getHits)
        .map(searchHits -> searchHits.getAt(0))
        .map(this::buildSearchResponseObject)
        .skip(from)
        .limit(size)
        .collect(toList());

    return buildResponseForObjects(objects);
  }

  /**
   * Builds a search response containing the specified objects.
   */
  public NpmSearchResponse buildResponseForObjects(final List<NpmSearchResponseObject> objects) {
    NpmSearchResponse response = new NpmSearchResponse();
    response.setObjects(objects);
    response.setTime(formatSearchResponseDate(DateTime.now()));
    response.setTotal(objects.size());
    return response;
  }

  /**
   * Builds a single package's search response object based on a provided search hit.
   */
  private NpmSearchResponseObject buildSearchResponseObject(final SearchHit searchHit) {
    NpmSearchResponsePerson person = buildPerson(searchHit);
    NpmSearchResponseScore score = buildPackageScore();
    NpmSearchResponsePackageLinks links = buildPackageLinks(searchHit);

    NpmSearchResponsePackage searchPackage = new NpmSearchResponsePackage();
    searchPackage.setDate(formatSearchResponsePackageDate(npmSearchHitExtractor.extractLastModified(searchHit)));
    searchPackage.setName(npmSearchHitExtractor.extractName(searchHit));
    searchPackage.setVersion(npmSearchHitExtractor.extractVersion(searchHit));
    searchPackage.setDescription(npmSearchHitExtractor.extractDescription(searchHit));
    searchPackage.setKeywords(npmSearchHitExtractor.extractKeywords(searchHit));
    searchPackage.setPublisher(person);
    searchPackage.setMaintainers(person == null ? emptyList() : singletonList(person));
    searchPackage.setLinks(links);

    NpmSearchResponseObject searchObject = new NpmSearchResponseObject();
    searchObject.setPackageEntry(searchPackage);
    searchObject.setSearchScore((double) searchHit.getScore());
    searchObject.setScore(score);
    return searchObject;
  }

  /**
   * Builds the package links where available. Since we will not have a link to the npm registry, we just have the bugs,
   * homepage, and repository links to include (if present in the original package.json for the tarball).
   */
  private NpmSearchResponsePackageLinks buildPackageLinks(final SearchHit searchHit) {
    NpmSearchResponsePackageLinks links = new NpmSearchResponsePackageLinks();
    links.setBugs(npmSearchHitExtractor.extractBugsUrl(searchHit));
    links.setHomepage(npmSearchHitExtractor.extractHomepage(searchHit));
    links.setRepository(npmSearchHitExtractor.extractRepositoryUrl(searchHit));
    return links;
  }

  /**
   * Builds the score detail portion of a package included in the response response, substituting in the default score
   * for all values. We do not know these values for hosted since they are derived from a variety of metrics that aren't
   * available to us, so we just plug in zero values to indicate we have no particular information.
   * */
  private NpmSearchResponseScore buildPackageScore() {
    // We do not support these fields for hosted as they require information we cannot generate for uploaded tarballs
    // (such as Github stars, results of running various linters/checks, comparisons to known NSP vulnerabilities).
    // For hosted we just default to plugging in zeroes since we have no meaningful scores or rankings to return.
    NpmSearchResponseScoreDetail scoreDetail = new NpmSearchResponseScoreDetail();
    scoreDetail.setMaintenance(DEFAULT_SCORE);
    scoreDetail.setPopularity(DEFAULT_SCORE);
    scoreDetail.setQuality(DEFAULT_SCORE);

    // the final score will also be zero since we don't support/weight by the maintenance/popularity/quality fields
    NpmSearchResponseScore score = new NpmSearchResponseScore();
    score.setFinalScore(DEFAULT_SCORE);
    score.setDetail(scoreDetail);
    return score;
  }

  /**
   * Builds the person information for the search response if the author name or author email fields are present.
   */
  @Nullable
  private NpmSearchResponsePerson buildPerson(final SearchHit searchHit) {
    // username is not available to us, so we substitute the actual name for these results
    String username = npmSearchHitExtractor.extractAuthorName(searchHit);
    String email = npmSearchHitExtractor.extractAuthorEmail(searchHit);

    // if we have at least one of the values, we should send something back, otherwise there's nothing meaningful here
    if (username == null && email == null) {
      return null;
    }
    else {
      NpmSearchResponsePerson person = new NpmSearchResponsePerson();
      person.setUsername(username);
      person.setEmail(email);
      return person;
    }
  }

  /**
   * Returns a formatted date string suitable for a search response package entry.
   */
  private String formatSearchResponsePackageDate(final DateTime dateTime) {
    return SEARCH_RESPONSE_PACKAGE_DATE_FORMAT.print(dateTime.toDateTime(DateTimeZone.UTC));
  }

  /**
   * Returns a formatted date string suitable for a search response entry.
   */
  private String formatSearchResponseDate(final DateTime dateTime) {
    return SEARCH_RESPONSE_DATE_FORMAT.print(dateTime.toDateTime(DateTimeZone.UTC));
  }
}
