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

  public Integer getPage() {
    return page;
  }

  public void setPage(final Integer page) {
    this.page = page;
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

  public static class Filter
  {
    private String property;

    private String value;

    public String getProperty() {
      return property;
    }

    public void setProperty(final String property) {
      this.property = property;
    }

    public String getValue() {
      return value;
    }

    public void setValue(final String value) {
      this.value = value;
    }
  }

  public static class Sort
  {
    private String property;

    private String direction;

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
  }
}
