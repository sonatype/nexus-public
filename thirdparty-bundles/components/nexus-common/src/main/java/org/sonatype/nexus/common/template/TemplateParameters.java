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
package org.sonatype.nexus.common.template;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Helper to build template parameters map.
 *
 * @since 3.0
 */
public class TemplateParameters
{
  private final Map<String, Object> params;

  public TemplateParameters(final Map<String, Object> params) {
    this.params = checkNotNull(params);
  }

  public TemplateParameters() {
    this(new HashMap<>());
  }

  public TemplateParameters set(final String key, final Object value) {
    params.put(key, value);
    return this;
  }

  public TemplateParameters setAll(final Map<String, Object> entries) {
    params.putAll(entries);
    return this;
  }

  public Map<String, Object> get() {
    return params;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "params=" + params +
        '}';
  }
}
