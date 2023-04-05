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
package org.sonatype.nexus.repository.search.sql;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Holds content selector sql condition formats and associated values
 *
 * @see org.sonatype.nexus.repository.search.table.TableSearchContentSelectorSqlFilterGenerator
 */
public class SqlSearchContentSelectorFilter
{
  private final StringBuilder queryFormat = new StringBuilder();

  private final Map<String, String> queryParameters = new HashMap<>();

  public void appendQueryFormatPart(final String value) {
    checkArgument(isNotBlank(value), "Value cannot be blank.");
    queryFormat.append(value);
  }

  public void putQueryParameters(final Map<String, String> queryParameters) {
    checkArgument(!queryParameters.isEmpty(), "Parameters cannot be empty.");

    this.queryParameters.putAll(queryParameters);
  }

  public void insert(final int position, final String value) {
    checkArgument(position >= 0, "Position cannot be negative.");
    checkArgument(isNotBlank(value), "Value cannot be blank.");
    queryFormat.insert(position, value);
  }

  public boolean hasFilters() {
    return !queryFormat.toString().isEmpty() && !queryParameters.isEmpty();
  }

  public String queryFormat() {
    return queryFormat.toString();
  }

  public Map<String, String> queryParameters() {
    return Collections.unmodifiableMap(queryParameters);
  }
}
