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

import org.sonatype.nexus.repository.search.sql.query.syntax.Expression;

/**
 * It is a container class for the three types of expression filters that can be applied to a query;
 * component filters, asset filters, and content selector filters.
 */
public class ExpressionGroup
{
  private final Expression componentFilters;

  private final Expression assetFilters;

  public ExpressionGroup(
      final Expression componentFilters,
      final Expression assetFilters) {
    this.componentFilters = componentFilters;
    this.assetFilters = assetFilters;
  }

  public Expression getComponentFilters() {
    return componentFilters;
  }

  public Expression getAssetFilters() {
    return assetFilters;
  }

  @Override
  public String toString() {
    return String.format(
        "ExpressionGroup{componentFilters=%s, assetFilters=%s}",
        componentFilters, assetFilters
    );
  }
}
