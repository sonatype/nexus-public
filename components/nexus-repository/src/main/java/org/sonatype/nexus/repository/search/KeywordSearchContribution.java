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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Named;
import javax.inject.Singleton;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;

/**
 * "Keyword" {@link SearchContribution} (adds filter as an ES query string).
 *
 * @since 3.15
 */
@Named(KeywordSearchContribution.NAME)
@Singleton
public class KeywordSearchContribution
    extends SearchContributionSupport
{
  public static final String NAME = "keyword";

  // Allow dependency searches of the form "group:name[:version][:extension][:classifier]"
  private Pattern dependencyPattern = Pattern.compile(
      "^(?<group>[^\\s:]+):(?<name>[^\\s:]+)(:(?<version>[^\\s:]+))?(:(?<extension>[^\\s:]+))?(:(?<classifier>[^\\s:]+))?$");

  @Override
  public void contribute(final BoolQueryBuilder query, final String type, final String value) {
    if (value == null) {
      return;
    }

    Matcher gavSearchMatcher = dependencyPattern.matcher(value.trim());

    if (gavSearchMatcher.matches()) {
      String group = gavSearchMatcher.group("group");
      String name = gavSearchMatcher.group("name");
      String version = gavSearchMatcher.group("version");
      String extension = gavSearchMatcher.group("extension");
      String classifier = gavSearchMatcher.group("classifier");
      final BoolQueryBuilder gavQuery = QueryBuilders.boolQuery();

      buildGavQuery(gavQuery, group, name, version, extension, classifier);

      query.must(gavQuery);
    }
    else {
      String escaped = escape(value);
      QueryStringQueryBuilder keywordQuery = QueryBuilders.queryStringQuery(escaped)
          .field("name.case_insensitive")
          .field("group.case_insensitive")
          .field("_all");
      query.must(keywordQuery);
    }
  }

  private void buildGavQuery(BoolQueryBuilder gavQuery,
                             String group,
                             String name,
                             String version,
                             String extension,
                             String classifier)
  {
    if (group != null) {
      gavQuery.must(QueryBuilders.termQuery("group.raw", group));
    }

    if (name != null) {
      gavQuery.must(QueryBuilders.termQuery("name.raw", name));
    }

    if (version != null) {
      gavQuery.must(QueryBuilders.termQuery("version", version));
    }

    if (extension != null) {
      gavQuery.must(QueryBuilders.termQuery("assets.attributes.maven2.extension", extension));
    }

    if (classifier != null) {
      gavQuery.must(QueryBuilders.termQuery("assets.attributes.maven2.classifier", classifier));
    }
  }
}
