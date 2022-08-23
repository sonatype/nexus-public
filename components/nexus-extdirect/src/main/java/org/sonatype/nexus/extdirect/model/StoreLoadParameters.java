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
package org.sonatype.nexus.extdirect.model;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Ext Store load parameters.
 *
 * @see <a href="http://docs.sencha.com/extjs/4.2.2/#!/api/Ext.toolbar.Paging">Ext.toolbar.Paging</a>
 * @since 3.0
 */
public class StoreLoadParameters
{
  private Integer page;

  private Integer start;

  private Integer limit;

  private List<Sort> sort;

  private List<Filter> filter;

  private String query;

  private boolean formatSearch;

  public Integer getPage() {
    return page;
  }

  public void setPage(final Integer page) {
    this.page = page;
  }

  public StoreLoadParameters filters(final List<Filter> filter) {
    this.filter = filter;
    return this;
  }

  public StoreLoadParameters limit(final Integer limit) {
    this.limit = limit;
    return this;
  }

  public StoreLoadParameters page(final Integer page) {
    this.page = page;
    return this;
  }

  public StoreLoadParameters sort(final List<Sort> sort) {
    this.sort = sort;
    return this;
  }

  public StoreLoadParameters start(final Integer start) {
    this.start = start;
    return this;
  }

  public Integer getStart() {
    return start;
  }

  public void setStart(final Integer start) {
    this.start = start;
  }

  public Integer getLimit() {
    return limit;
  }

  public void setLimit(final Integer limit) {
    this.limit = limit;
  }

  public List<Filter> getFilters() {
    return filter;
  }

  public void setFilter(final List<Filter> filter) {
    this.filter = filter;
  }

  public String getFilter(String property) {
    checkNotNull(property, "property");
    if (filter != null) {
      for (Filter item : filter) {
        if (property.equals(item.getProperty())) {
          return item.getValue();
        }
      }
    }
    return null;
  }

  public List<Sort> getSort() {
    return sort;
  }

  public void setSort(final List<Sort> sort) {
    this.sort = sort;
  }

  public String getQuery() {
    return query;
  }

  public void setQuery(final String query) {
    this.query = query;
  }

  public List<Filter> getFilter() {
    return filter;
  }

  public boolean isFormatSearch() {
    return formatSearch;
  }

  public void setFormatSearch(final boolean formatSearch) {
    this.formatSearch = formatSearch;
  }

  @Override
  public String toString() {
    return "StoreLoadParameters{" +
        "page=" + page +
        ", start=" + start +
        ", limit=" + limit +
        ", sort=" + sort +
        ", filter=" + filter +
        ", formatSearch=" + formatSearch +
        '}';
  }

  public static class Filter
  {
    private String property;

    private String value;

    public String getProperty() {
      return property;
    }

    public Filter property(final String property) {
      this.property = property;
      return this;
    }

    public void setProperty(final String property) {
      this.property = property;
    }

    public String getValue() {
      return value;
    }

    public Filter value(final String value) {
      this.value = value;
      return this;
    }

    public void setValue(final String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return "Filter{" +
          "property='" + property + '\'' +
          ", value='" + value + '\'' +
          '}';
    }
  }

  public static class Sort
  {
    private String property;

    private String direction;

    public Sort() {
    }

    public Sort(final String property, final String direction) {
      this.property = property;
      this.direction = direction;
    }

    public String getProperty() {
      return property;
    }

    public void setProperty(final String property) {
      this.property = property;
    }

    public String getDirection() {
      return direction;
    }

    public void setDirection(final String value) {
      this.direction = value;
    }

    @Override
    public String toString() {
      return "Sort{" +
          "property='" + property + '\'' +
          ", direction='" + direction + '\'' +
          '}';
    }
  }
}
