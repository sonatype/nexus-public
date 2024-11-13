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
package org.sonatype.nexus.kv;

import org.sonatype.nexus.common.event.EventWithSource;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An event fired when a value is set via {@link GlobalKeyValueStore}
 */
public class KeyValueEvent
    extends EventWithSource
{
  private String key;

  private Object value;

  KeyValueEvent() {
    // deserialization
  }

  public KeyValueEvent(final String key, final Object value) {
    this.key = checkNotNull(key);
    this.value = value;
  }

  /**
   * @return the key that was set
   */
  public String getKey() {
    return key;
  }

  /**
   * Note: complex objects will not be preserved for nodes where the event did not originate, for other nodes
   * in the cluster the value will be a Map
   *
   * @return the value, for complex objects in HA
   */
  public Object getValue() {
    return value;
  }

  public void setKey(final String key) {
    this.key = key;
  }

  public void setValue(final Object value) {
    this.value = value;
  }
}
