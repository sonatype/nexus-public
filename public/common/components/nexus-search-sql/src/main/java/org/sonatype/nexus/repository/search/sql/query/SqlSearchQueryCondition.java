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

import java.util.Map;

import static java.util.Collections.unmodifiableMap;

/**
 * Encapsulates a sql query condition's format string e.g. <code>field IN (#{field0}, #{field1}, #{field2})</code> and
 * its associated values (which in the example given will be a <code>Map&lt;String, String&gt;</code> of named
 * parameters and their associated values).
 *
 * @param Sql condition format string e.g. <code>field IN (#{field0}, #{field1}, #{field2})</code>
 * @param Mapped of named values to apply to the Sql condition format string.
 *
 * @since 3.38
 */
public record SqlSearchQueryCondition(String sqlFilter, Map<String, Object> parameters)
{
  public String getSqlFilter() {
    return sqlFilter;
  }

  public Map<String, Object> getParameters() {
    return unmodifiableMap(parameters);
  }
}
