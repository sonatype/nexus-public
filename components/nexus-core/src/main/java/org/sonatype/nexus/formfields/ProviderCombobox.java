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
package org.sonatype.nexus.formfields;

import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Maps;
import org.codehaus.plexus.util.StringUtils;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * A combo box {@link FormField} that delegates retrieval of id/name entries to a named provider.
 *
 * @since 2.7
 */
public class ProviderCombobox
    extends Combobox<String>
{

  private String name;

  private Map<String, String> params;

  public ProviderCombobox(String id, String label, String helpText, boolean required,
                          String regexValidation)
  {
    super(id, label, helpText, required, regexValidation);
    params = Maps.newHashMap();
  }

  public ProviderCombobox(String id, String label, String helpText, boolean required) {
    this(id, label, helpText, required, null);
  }

  /**
   * Name of provider.
   */
  public ProviderCombobox named(final String name) {
    checkArgument(StringUtils.isNotEmpty(name), "name cannot be empty");
    checkArgument(!name.trim().startsWith("/"), "name cannot start with '/'");
    this.name = name.trim();
    return this;
  }

  /**
   * Adds a query parameter that will be forwarded to provider.
   */
  public ProviderCombobox withParam(final String name, final String value) {
    checkArgument(StringUtils.isNotEmpty(name), "name cannot be empty");
    checkArgument(StringUtils.isNotEmpty(value), "value cannot be empty");
    this.params.put(name, value);
    return this;
  }

  @Override
  public String getStorePath() {
    checkState(name != null, "Name of store cannot be null");
    StringBuilder sb = new StringBuilder();
    for (Entry<String, String> entry : params.entrySet()) {
      if (sb.length() > 0) {
        sb.append("&");
      }
      sb.append(entry.getKey()).append("=").append(entry.getValue());
    }
    if (sb.length() > 0) {
      sb.insert(0, "?");
    }
    sb.insert(0, siestaStore("/capabilities/stores/provider/" + name));

    return sb.toString();
  }
}
