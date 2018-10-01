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
package org.sonatype.nexus.cleanup.internal.search.elasticsearch;

import org.elasticsearch.index.query.BoolQueryBuilder;

import static java.lang.String.format;
import static org.elasticsearch.index.query.QueryBuilders.rangeQuery;

/**
 * Appends criteria for less than a given time.
 * 
 * @since 3.14
 */
public abstract class LessThanTimeCriteriaAppender
  implements CriteriaAppender
{
  private static final String NOW_MINUS_SECONDS = "now-%ss";
  
  private final String field;

  public LessThanTimeCriteriaAppender(final String field) {
    this.field = field;
  }
  
  public void append(final BoolQueryBuilder query, final String value) {
    query.filter(
        rangeQuery(field)
            .lte(format(NOW_MINUS_SECONDS, value))
    );
  }
}
