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
package org.sonatype.nexus.repository.browse.internal;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Data carrier with fields commonly used by SQL builders for the {@link BrowseServiceImpl} implementation. Also does
 * a quick check on the sortProperty and sortDirection fields as defensive programming against SQL injection.
 *
 * @since 3.1
 */
public class QueryOptions
{
  private static final List<String> SORT_PROPERTIES = Arrays.asList("name", "group", "version");

  private static final List<String> SORT_DIRECTIONS = Arrays.asList("asc", "desc");

  private final String filter;

  private final String sortProperty;

  private final String sortDirection;

  private final Integer start;

  private final Integer limit;

  public QueryOptions(@Nullable String filter,
                      @Nullable String sortProperty,
                      @Nullable String sortDirection,
                      @Nullable Integer start,
                      @Nullable Integer limit)
  {
    checkArgument(sortProperty == null || SORT_PROPERTIES.contains(sortProperty.toLowerCase(Locale.ENGLISH)));
    checkArgument(sortDirection == null || SORT_DIRECTIONS.contains(sortDirection.toLowerCase(Locale.ENGLISH)));
    this.filter = filter;
    this.sortProperty = sortProperty;
    this.sortDirection = sortDirection;
    this.start = start;
    this.limit = limit;
  }

  @Nullable
  public String getFilter() {
    return filter;
  }

  @Nullable
  public String getSortProperty() {
    return sortProperty;
  }

  @Nullable
  public String getSortDirection() {
    return sortDirection;
  }

  @Nullable
  public Integer getStart() {
    return start;
  }

  @Nullable
  public Integer getLimit() {
    return limit;
  }
}
