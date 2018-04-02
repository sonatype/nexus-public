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
package org.sonatype.nexus.repository.npm.internal.search.v1;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data carrier for a single search response object for npm V1 search.
 *
 * @since 3.7
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NpmSearchResponseObject
{
  @Nullable
  private NpmSearchResponsePackage packageEntry;

  @Nullable
  private NpmSearchResponseScore score;

  @Nullable
  private Double searchScore;

  @Nullable
  @JsonProperty("package")
  public NpmSearchResponsePackage getPackageEntry() {
    return packageEntry;
  }

  public void setPackageEntry(@Nullable final NpmSearchResponsePackage packageEntry) {
    this.packageEntry = packageEntry;
  }

  @Nullable
  public NpmSearchResponseScore getScore() {
    return score;
  }

  public void setScore(@Nullable final NpmSearchResponseScore score) {
    this.score = score;
  }

  @Nullable
  public Double getSearchScore() {
    return searchScore;
  }

  public void setSearchScore(@Nullable final Double searchScore) {
    this.searchScore = searchScore;
  }
}
