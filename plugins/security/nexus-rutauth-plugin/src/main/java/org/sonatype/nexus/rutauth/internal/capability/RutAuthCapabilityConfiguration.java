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
package org.sonatype.nexus.rutauth.internal.capability;

import java.util.Map;

import org.sonatype.nexus.capability.support.CapabilityConfigurationSupport;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.NonNls;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Configuration adapter for {@link RutAuthCapability}.
 *
 * @since 2.7
 */
public class RutAuthCapabilityConfiguration
    extends CapabilityConfigurationSupport
{

  @NonNls
  public static final String HTTP_HEADER = "httpHeader";

  @NonNls
  public static final String DEFAULT_HTTP_HEADER = "REMOTE_USER";

  private String httpHeader;

  public RutAuthCapabilityConfiguration(final Map<String, String> properties) {
    checkNotNull(properties);
    this.httpHeader = properties.get(HTTP_HEADER);
    if (Strings.isNullOrEmpty(httpHeader)) {
      this.httpHeader = DEFAULT_HTTP_HEADER;
    }
  }

  public String getHttpHeader() {
    return httpHeader;
  }

  public RutAuthCapabilityConfiguration withHttpHeader(String httpHeader) {
    checkArgument(!Strings.isNullOrEmpty(httpHeader), "httpHeader cannot be null or empty");
    this.httpHeader = httpHeader;
    return this;
  }

  public Map<String, String> asMap() {
    final Map<String, String> props = Maps.newHashMap();
    props.put(HTTP_HEADER, httpHeader);
    return props;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" + "httpHeader=" + httpHeader + "}";
  }
}
