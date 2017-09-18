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

import java.util.HashMap;
import java.util.Map;

/**
 * @since 3.6
 */
public class CselAssetSql
{
  private final StringBuilder sqlBuilder = new StringBuilder();

  private final Map<String, Object> sqlParameters = new HashMap<>();

  private final String parameterPrefix;

  private final String fieldPrefix;

  public CselAssetSql(final String parameterPrefix, final String fieldPrefix) {
    this.parameterPrefix = parameterPrefix;
    this.fieldPrefix = fieldPrefix;
  }

  public StringBuilder getSqlBuilder() {
    return sqlBuilder;
  }

  public Map<String, Object> getSqlParameters() {
    return sqlParameters;
  }

  public String getNextParameterName() {
    return parameterPrefix + sqlParameters.size();
  }

  public String getSql() {
    return sqlBuilder.toString();
  }

  public String getFieldPrefix() {
    return fieldPrefix;
  }

  @Override
  public String toString() {
    return "CselAssetSql{" +
        "sqlBuilder=" + sqlBuilder +
        ", sqlParameters=" + sqlParameters +
        '}';
  }
}
