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
package org.sonatype.nexus.coreui.search;

import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;

/**
 * Contributor to search query/filter.
 *
 * @since 3.0
 */
public interface SearchContribution
{

  /**
   * Contribute to search query.
   *
   * @param query to contribute to
   * @param type   type of filter
   * @param value  value of filter
   */
  void contribute(BoolQueryBuilder query, String type, String value);

  /**
   * Contribute to search filter.
   *
   * @param filter to contribute to
   * @param type   type of filter
   * @param value  value of filter
   */
  void contribute(BoolFilterBuilder filter, String type, String value);

}
