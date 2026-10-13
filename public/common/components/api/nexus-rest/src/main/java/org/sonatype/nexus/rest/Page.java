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
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
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

  /**
   * True total match count for the query, when the search backend can supply it
   * cheaply. Serialized only when present so responses that do not carry a count
   * are unchanged.
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Long total;

  /**
   * Convenience constructor for the common case of a page with no total. Delegates to the
   * canonical constructor below. Deliberately carries no {@code @JsonProperty} annotations:
   * Jackson allows only one property-based creator per type, so annotating this one too would
   * make it a second creator and fail type introspection with "Conflicting property-based
   * creators" — a failure that aborts Spring context refresh at startup. The three-arg
   * constructor is the sole Jackson creator.
   */
  public Page(List<T> items, String continuationToken) {
    this(items, continuationToken, null);
  }

  @JsonCreator
  public Page(
      @JsonProperty("items") List<T> items,
      @JsonProperty("continuationToken") String continuationToken,
      @JsonProperty("total") Long total)
  {
    this.items = items;
    this.continuationToken = continuationToken;
    this.total = total;
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

  public Long getTotal() {
    return total;
  }

  public void setTotal(Long total) {
    this.total = total;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Page<?> page = (Page<?>) o;
    return Objects.equals(items, page.items) &&
        Objects.equals(continuationToken, page.continuationToken) &&
        Objects.equals(total, page.total);
  }

  @Override
  public int hashCode() {
    return Objects.hash(items, continuationToken, total);
  }

  @Override
  public String toString() {
    return "Page{" +
        "items=" + items +
        ", continuationToken='" + continuationToken + '\'' +
        ", total=" + total +
        '}';
  }
}
