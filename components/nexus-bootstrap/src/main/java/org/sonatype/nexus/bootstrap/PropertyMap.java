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
package org.sonatype.nexus.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

// TODO Copy this to goodies-common, though has to be duplicated here

/**
 * Properties-like map with appropriate generics signature.
 *
 * @since 2.8
 */
public class PropertyMap
  extends HashMap<String,String>
{
  public PropertyMap() {
    super();
  }

  public PropertyMap(final Map<String, String> map) {
    super(map);
  }

  /**
   * @since 2.8.1
   */
  public PropertyMap(final Properties properties) {
    putAll(properties);
  }

  public void putAll(final Properties props) {
    for (Object key : props.keySet()) {
      put(key.toString(), String.valueOf(props.get(key)));
    }
  }

  public String get(final String key, final String defaultValue) {
    String value = super.get(key);
    if (value == null) {
      return defaultValue;
    }
    return value;
  }

  public void load(final InputStream input) throws IOException {
    Properties p = new Properties();
    p.load(input);
    putAll(p);
  }

  public void load(final URL url) throws IOException {
    try (InputStream input = url.openStream()) {
      load(input);
    }
  }

  /**
   * Returns list of sorted keys.
   */
  public List<String> keys() {
    List<String> keys = new ArrayList<>(keySet());
    Collections.sort(keys);
    return Collections.unmodifiableList(keys);
  }
}
