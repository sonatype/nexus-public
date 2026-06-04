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
package org.sonatype.nexus.repository.search.sql.query;

import org.sonatype.nexus.repository.search.sql.SqlSearchQueryContribution;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryContributionSupport;
import org.sonatype.nexus.repository.search.sql.query.syntax.StringTerm;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Base implementation for {@link SqlSearchQueryContribution}
 *
 * @since 3.38
 */
@Component
@Qualifier("default")
public class DefaultSqlSearchQueryContribution
    extends SqlSearchQueryContributionSupport
{
  @Override
  protected StringTerm createMatchTerm(final boolean exact, final String value) {
    // Use parent implementation which respects the exact parameter
    // When exact=true, creates ExactTerm for exact matching using the non-tsvector column
    // When exact=false, creates LenientTerm for case-insensitive full-text search
    return super.createMatchTerm(exact, value);
  }
}
