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
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.raw.internal.RawFormat;
import org.sonatype.nexus.repository.search.SearchRequest;
import org.sonatype.nexus.repository.search.query.SearchFilter;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryContributionSupport;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static org.sonatype.nexus.repository.search.index.SearchConstants.FORMAT;

/**
 * Routes the user-facing {@value #NAME_RAW} filter to the internal lenient {@link RawSearchMappings#RAW_NAME}
 * alias whenever the search is scoped to {@code format=raw}.
 *
 * <p>
 * Raw stores the full asset path as the component name (e.g. {@code /foo/bar/report.pdf}), so the default
 * exact match against the {@code name} column never succeeds when users type just the basename
 * (e.g. {@code report.pdf}). Rewriting the filter to {@code raw.name} sends the query through the lenient
 * (tsvector) path where {@link RawSearchCustomFieldContributor} has indexed the basename, making filename
 * search work as users expect.
 *
 * <p>
 * No new UI criterion is introduced — the existing {@code name.raw} "Name" field continues to be the
 * user-facing search input across all UIs (React preview UI, Classic ExtJS UI, REST API). Non-raw formats
 * are unaffected because the rewrite is gated on the {@code format=raw} filter being present.
 */
@Component
@Qualifier(NameRawSqlSearchQueryContribution.NAME_RAW)
public class NameRawSqlSearchQueryContribution
    extends SqlSearchQueryContributionSupport
{
  /**
   * The user-facing search criterion property name. Mirrors
   * {@code DefaultSearchMappings.NAME_RAW} without depending on the {@code internal} package.
   */
  public static final String NAME_RAW = "name.raw";

  @Override
  public SearchRequest modifyRequest(final SearchRequest request) {
    if (!isRawFormat(request)) {
      return request;
    }
    List<SearchFilter> rewritten = request.getSearchFilters()
        .stream()
        .map(NameRawSqlSearchQueryContribution::rewriteIfNameRaw)
        .collect(Collectors.toList());
    return SearchRequest.builder()
        .from(request)
        .replaceSearchFilters(rewritten)
        .build();
  }

  private static boolean isRawFormat(final SearchRequest request) {
    return request.getSearchFilters()
        .stream()
        .anyMatch(filter -> filter != null
            && FORMAT.equalsIgnoreCase(filter.getProperty())
            && RawFormat.NAME.equalsIgnoreCase(filter.getValue()));
  }

  private static SearchFilter rewriteIfNameRaw(@Nullable final SearchFilter filter) {
    if (filter == null || !NAME_RAW.equals(filter.getProperty())) {
      return filter;
    }
    return new SearchFilter(RawSearchMappings.RAW_NAME, filter.getValue(), filter.operator());
  }
}
