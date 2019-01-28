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
package org.sonatype.nexus.selector;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.sonatype.nexus.common.entity.AbstractEntity;

/**
 * {@link Selector} configuration.
 *
 * @since 3.0
 */
public class SelectorConfiguration
    extends AbstractEntity
{
  private String name;

  private String type;

  private String description;

  private Map<String,String> attributes;

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(final String type) {
    this.type = type;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public Map<String,String> getAttributes() {
    return attributes;
  }

  public void setAttributes(final Map<String, Object> attributes) {
    this.attributes = new HashMap<>(attributes.size());
    // prevent OrientDB deserialization exceptions by converting value to String - see NEXUS-17850
    attributes.forEach((key, value) -> this.attributes.put(key, value.toString()));
  }

  @Override
  public boolean equals(final Object other) {
    if (other instanceof SelectorConfiguration) {
      SelectorConfiguration o = (SelectorConfiguration) other;
      return Objects.equals(name, o.name) && Objects.equals(type, o.type) && Objects.equals(description, o.description)
          && Objects.equals(attributes, o.attributes);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type, description, attributes);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "name='" + name + '\'' +
        ", type='" + type + '\'' +
        ", description='" + description + '\'' +
        ", attributes='" + attributes + '\'' +
        '}';
  }
}
