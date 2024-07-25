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

import org.sonatype.goodies.common.ComponentSupport;

import com.google.common.annotations.VisibleForTesting;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.RegexpQueryBuilder;

import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.REGEX_KEY;
import static org.sonatype.nexus.cleanup.storage.config.RegexCriteriaValidator.validate;

/**
 * Appends criteria for matches based on regular expression
 *
 * @since 3.19
 */
@Named(REGEX_KEY)
public class RegexCriteriaAppender
    extends ComponentSupport
    implements CriteriaAppender
{
  @VisibleForTesting
  public static final String DEFAULT_REGEX_MATCH_ON = "assets.name";

  @Override
  public void append(final BoolQueryBuilder query, final String expression) {
    query.must(new RegexpQueryBuilder(DEFAULT_REGEX_MATCH_ON, validate(expression)));
  }
}
