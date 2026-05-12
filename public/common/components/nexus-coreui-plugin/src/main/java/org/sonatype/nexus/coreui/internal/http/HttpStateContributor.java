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
package org.sonatype.nexus.coreui.internal.http;

import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.sonatype.nexus.httpclient.HttpDefaultsCustomizer;
import org.sonatype.nexus.rapture.StateContributor;

import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Singleton
public class HttpStateContributor
    implements StateContributor
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final HttpDefaultsCustomizer customizer;

  private boolean featureFlag;

  private final String contextPath;

  @Inject
  public HttpStateContributor(
      @Value("${nexus.react.httpSettings:true}") final Boolean featureFlag,
      @Value("${nexus-context-path:/}") final String contextPath,
      final HttpDefaultsCustomizer customizer)
  {
    this.customizer = customizer;
    this.featureFlag = featureFlag;
    this.contextPath = contextPath;
  }

  @Override
  public Map<String, Object> getState() {
    return ImmutableMap.of(
        "nexus.react.httpSettings", featureFlag,
        "requestTimeout", customizer.getRequestTimeout(),
        "retryCount", customizer.getRetryCount(),
        "nexus-context-path", contextPath);
  }
}
