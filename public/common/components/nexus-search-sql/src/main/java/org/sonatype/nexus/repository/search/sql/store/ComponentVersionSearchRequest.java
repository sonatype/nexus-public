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
package org.sonatype.nexus.repository.search.sql.store;

import java.util.Map;

import javax.annotation.Nullable;

/**
 * MyBatis parameter object for the distinct-version queries.
 */
public class ComponentVersionSearchRequest
{
  @Nullable
  public final String filter;

  @Nullable
  public final Map<String, Object> filterParams;

  /**
   * Ready-to-use {@code LIKE} pattern for a case-insensitive substring match on the version,
   * already lowercased with any {@code % _ \} in the user's input escaped (escape char
   * {@code \}). Null when no version filter was supplied.
   *
   * <p>
   * This deliberately bypasses {@link #filter}: that expression comes from the shared search
   * {@code ExpressionBuilder}, which prohibits leading wildcards and requires three characters
   * before a trailing one — rules that make incremental substring filtering impossible.
   */
  @Nullable
  public final String versionLikePattern;

  /**
   * SQL ORDER BY expression. Resolved from a closed enum, never caller-supplied.
   */
  public final String sortExpression;

  public final String sortDirection;

  public final int limit;

  public final int offset;

  private ComponentVersionSearchRequest(final Builder builder) {
    this.filter = builder.filter;
    this.filterParams = builder.filterParams;
    this.versionLikePattern = builder.versionLikePattern;
    this.sortExpression = builder.sortExpression;
    this.sortDirection = builder.sortDirection;
    this.limit = builder.limit;
    this.offset = builder.offset;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder
  {
    private String filter;

    private Map<String, Object> filterParams;

    private String versionLikePattern;

    private String sortExpression = "cs.normalised_version";

    private String sortDirection = "DESC";

    private int limit = 20;

    private int offset = 0;

    public Builder filter(final String filter) {
      this.filter = filter;
      return this;
    }

    public Builder filterParams(final Map<String, Object> filterParams) {
      this.filterParams = filterParams;
      return this;
    }

    public Builder versionLikePattern(final String versionLikePattern) {
      this.versionLikePattern = versionLikePattern;
      return this;
    }

    public Builder sortExpression(final String sortExpression) {
      this.sortExpression = sortExpression;
      return this;
    }

    public Builder sortDirection(final String sortDirection) {
      this.sortDirection = sortDirection;
      return this;
    }

    public Builder limit(final int limit) {
      this.limit = limit;
      return this;
    }

    public Builder offset(final int offset) {
      this.offset = offset;
      return this;
    }

    public ComponentVersionSearchRequest build() {
      return new ComponentVersionSearchRequest(this);
    }
  }
}
