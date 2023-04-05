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

import java.util.Set;

import org.sonatype.nexus.repository.search.query.SearchFilter;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryBuilder;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryCondition;

import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.stripStart;

/**
 * Allows creation of sql search query condition(s) based on a given SearchFilter.
 *
 * @see SearchFilter
 * @see SqlSearchQueryCondition
 * @since 3.38
 */
public interface SqlSearchQueryContribution
{
  /**
   * Creates {@link SqlSearchQueryCondition}(s) for the specified SearchFilter and stores it in the
   * {@link SqlSearchQueryBuilder}
   */
  void contribute(SqlSearchQueryBuilder queryBuilder, final SearchFilter searchFilter);

  /**
   * Only used for maven G.A.BV.E.C and repository names.
   *
   * Tokenization is the essence of Postgres's Full Text Search and it generally results in more search results (not
   * less) unless you perform a more restrictive search. We should accept the tokenization behaviour of
   * Full text search in the majority of cases and should only prevent the tokenization if there's
   * no other way of performing a more restrictive search for that particular use case.
   *
   * Note: nexus doesn't allow G.A.BV.E.C or repository name to begin with a '/'.
   * Thus, it's ok to use '/' as a marker to prevent Full Text search tokenization.
   */
  static String preventTokenization(final String searchTerm) {
    return isBlank(searchTerm) ? "" : "/" + searchTerm;
  }

  static Set<String> preventTokenization(final Set<String> searchTerms) {
    return searchTerms.stream().map(SqlSearchQueryContribution::preventTokenization).collect(toSet());
  }

  static String matchUntokenizedValue(final String searchTerm) {
    if (startsWith(searchTerm, "/")) {
      return stripStart(searchTerm, "/");
    }
    return preventTokenization(searchTerm);
  }
}
