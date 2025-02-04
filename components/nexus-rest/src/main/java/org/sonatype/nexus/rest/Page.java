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
package org.sonatype.nexus.rest;

import java.util.Objects;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An object to hold pages of XOs for use in the REST API.
 *
 * @since 3.3
 */
public class Page<T>
{

  private List<T> items;

  private String continuationToken;

  public Page(
      @JsonProperty("items") List<T> items,
      @JsonProperty("continuationToken") String continuationToken)
  {
    this.items = items;
    this.continuationToken = continuationToken;
  }

  public List<T> getItems() {
    return items;
  }

  public void setItems(List<T> items) {
    this.items = items;
  }

  public String getContinuationToken() {
    return continuationToken;
  }

  public void setContinuationToken(String continuationToken) {
    this.continuationToken = continuationToken;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Page<?> page = (Page<?>) o;
    return Objects.equals(items, page.items) &&
        Objects.equals(continuationToken, page.continuationToken);
  }

  @Override
  public int hashCode() {
    return Objects.hash(items, continuationToken);
  }

  @Override
  public String toString() {
    return "Page{" +
        "items=" + items +
        ", continuationToken='" + continuationToken + '\'' +
        '}';
  }
}
