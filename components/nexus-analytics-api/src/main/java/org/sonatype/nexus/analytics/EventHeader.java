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
package org.sonatype.nexus.analytics;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Throwables;

/**
 * Analytics event header.
 *
 * @since 3.0
 */
public class EventHeader
  implements Cloneable, Serializable
{
  private static final long serialVersionUID = 1L;

  /**
   * Format of the event stream (ex. zip-bundle/1)
   */
  private String format;

  /**
   * The product which produced the events (ex. nexus-oss/2.8)
   */
  private String product;

  /**
   * The organization the events belong to (ex. customer license identifier).
   */
  private String organization;

  /**
   * Unique identifier for the server instance that produced events.
   */
  private String node;

  /**
   * Custom data to associate with event stream.
   */
  private Map<String,String> attributes = new HashMap<>();

  public String getFormat() {
    return format;
  }

  public void setFormat(final String format) {
    this.format = format;
  }

  public String getProduct() {
    return product;
  }

  public void setProduct(final String product) {
    this.product = product;
  }

  public String getOrganization() {
    return organization;
  }

  public void setOrganization(final String organization) {
    this.organization = organization;
  }

  public String getNode() {
    return node;
  }

  public void setNode(final String node) {
    this.node = node;
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }

  /**
   * Returns a deeply cloned copy.
   */
  public EventHeader copy() {
    try {
      EventHeader copy = (EventHeader) clone();
      copy.attributes = new HashMap<>(this.attributes);
      return copy;
    }
    catch (CloneNotSupportedException e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "format='" + format + '\'' +
        ", product='" + product + '\'' +
        ", organization='" + organization + '\'' +
        ", node='" + node + '\'' +
        ", attributes=" + attributes +
        '}';
  }
}
