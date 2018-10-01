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
package org.sonatype.nexus.coreui.internal.search;

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
 * @since 3.0
 */
@Named("keyword")
@Singleton
public class KeywordSearchContribution
    extends SearchContributionSupport
{
  // Allow maven/gradle dependency names of the form "group:name:version:classifier@extension"
  private Pattern dependencyPattern = Pattern.compile(
      "^(?<group>[^\\s:]+):(?<name>[^\\s:]+)(:(?<version>[^\\s:]+))?(:(?<classifier>[^\\s:]+))?(@(?<extension>[^\\s:]+))?$");

  @Override
  public void contribute(final BoolQueryBuilder query, final String type, final String value) {
    if (value == null) {
      return;
    }

    final String group;
    final String name;
    final String version;

    Matcher gavSearchMatcher = dependencyPattern.matcher(value.trim());
    if (gavSearchMatcher.matches()) {
      group = gavSearchMatcher.group("group");
      name = gavSearchMatcher.group("name");
      version = gavSearchMatcher.group("version");
    }
    else {
      group = null;
      name = null;
      version = null;
    }

    QueryStringQueryBuilder keywordQuery = QueryBuilders.queryStringQuery(value)
        .field("name.case_insensitive")
        .field("group.case_insensitive")
        .field("_all");

    BoolQueryBuilder gavQuery = QueryBuilders.boolQuery();

    if (group != null && name != null) { // the query could be a group:artifact query or a keyword query
      keywordQuery.lenient(true);
      gavQuery.must(QueryBuilders.termQuery("group", group));
      gavQuery.must(QueryBuilders.termQuery("name.raw", name));
    }
    else {
      query.must(keywordQuery);
    }

    if (version != null) { // the keyword query will be invalid - assume a group:artifact:version query
      gavQuery.must(QueryBuilders.termQuery("version", version));
      query.must(gavQuery);
    }
    else {
      query.must(QueryBuilders.boolQuery().should(keywordQuery).should(gavQuery));
    }
  }
}
