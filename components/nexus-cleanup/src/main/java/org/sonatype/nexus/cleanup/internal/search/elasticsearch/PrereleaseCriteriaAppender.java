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

import javax.inject.Named;

import org.elasticsearch.index.query.BoolQueryBuilder;

import static java.lang.Boolean.parseBoolean;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.IS_PRERELEASE_KEY;

/**
 * Adds criteria for querying on isPrerelease
 * 
 * @since 3.14
 */
@Named(IS_PRERELEASE_KEY)
public class PrereleaseCriteriaAppender
  implements CriteriaAppender
{
  @Override
  public void append(final BoolQueryBuilder query, final String value) {
    query.must(matchQuery(IS_PRERELEASE_KEY, parseBoolean(value)));
  }
}
