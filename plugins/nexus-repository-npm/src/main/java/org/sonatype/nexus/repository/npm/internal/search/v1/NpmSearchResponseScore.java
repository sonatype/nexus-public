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
 * Data carrier (mapping to JSON) that contains the search result score information for a particular package for npm
 * search V1.
 *
 * @since 3.7
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NpmSearchResponseScore
{
  @Nullable
  private Double finalScore;

  @Nullable
  private NpmSearchResponseScoreDetail detail;

  @Nullable
  @JsonProperty("final")
  public Double getFinalScore() {
    return finalScore;
  }

  public void setFinalScore(@Nullable final Double finalScore) {
    this.finalScore = finalScore;
  }

  @Nullable
  public NpmSearchResponseScoreDetail getDetail() {
    return detail;
  }

  public void setDetail(@Nullable final NpmSearchResponseScoreDetail detail) {
    this.detail = detail;
  }
}
