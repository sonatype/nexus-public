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
package org.sonatype.nexus.selector;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Builds a SQL filter from a list of content selectors.
 */
public interface SelectorFilterBuilder
{
  /**
   * Builds a SQL filter for a list of content selectors.
   *
   * @param format           format of the repository that the filter will run on
   * @param pathAlias        alias that can be used to refer to the path inside the SQL
   * @param selectors        set of content selectors to build filter from
   * @param filterParameters "out" parameter in which filter parameter keys and values are placed
   * @return filter query ready to be appended to a SQL statement
   */
  @Nullable
  String buildFilter(
      final String format,
      final String pathAlias,
      final List<SelectorConfiguration> selectors,
      final Map<String, Object> filterParameters);
}
