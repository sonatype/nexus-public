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
package org.sonatype.nexus.internal.app;

import java.util.Map;

import org.sonatype.nexus.capability.CapabilityConfigurationSupport;
import org.sonatype.nexus.capability.UniquePerCapabilityType;
import org.sonatype.nexus.validation.group.Create;

import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link BaseUrlCapability} configuration.
 *
 * @since 3.0
 */
@UniquePerCapabilityType(value = BaseUrlCapabilityDescriptor.TYPE_ID, groups = Create.class)
public class BaseUrlCapabilityConfiguration
    extends CapabilityConfigurationSupport
{
  public static final String URL = "url";

  @NotBlank
  @URL
  private String url;

  public BaseUrlCapabilityConfiguration(final Map<String,String> properties) {
    checkNotNull(properties);
    this.url = properties.get(URL);
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(final String url) {
    this.url = url;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "url='" + url + '\'' +
        '}';
  }
}
