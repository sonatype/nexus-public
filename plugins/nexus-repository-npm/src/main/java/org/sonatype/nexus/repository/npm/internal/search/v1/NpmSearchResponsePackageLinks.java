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

/**
 * Data carrier (mapping to JSON) for the links portion of a package entry in an npm V1 search response.
 *
 * @since 3.7
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NpmSearchResponsePackageLinks
{
  @Nullable
  private String npm;

  @Nullable
  private String homepage;

  @Nullable
  private String repository;

  @Nullable
  private String bugs;

  @Nullable
  public String getNpm() {
    return npm;
  }

  public void setNpm(@Nullable final String npm) {
    this.npm = npm;
  }

  @Nullable
  public String getHomepage() {
    return homepage;
  }

  public void setHomepage(@Nullable final String homepage) {
    this.homepage = homepage;
  }

  @Nullable
  public String getRepository() {
    return repository;
  }

  public void setRepository(@Nullable final String repository) {
    this.repository = repository;
  }

  @Nullable
  public String getBugs() {
    return bugs;
  }

  public void setBugs(@Nullable final String bugs) {
    this.bugs = bugs;
  }
}
