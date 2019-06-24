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
package org.sonatype.nexus.datastore.api;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link DataStore} configuration.
 *
 * @since 3.next
 */
public class DataStoreConfiguration
{
  private String name;

  private String type;

  private Map<String, String> attributes;

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = checkNotNull(name);
  }

  public String getType() {
    return type;
  }

  public void setType(final String type) {
    this.type = checkNotNull(type);
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }

  public void setAttributes(final Map<String, String> attributes) {
    this.attributes = checkNotNull(attributes);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "name='" + name + '\'' +
        ", type='" + type + '\'' +
        ", attributes=" + attributes +
        '}';
  }
}
