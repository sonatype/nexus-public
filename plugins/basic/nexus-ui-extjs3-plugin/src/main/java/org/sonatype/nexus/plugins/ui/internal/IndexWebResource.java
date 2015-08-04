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
package org.sonatype.nexus.plugins.ui.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import org.sonatype.nexus.ApplicationStatusSource;
import org.sonatype.nexus.SystemStatus;
import org.sonatype.nexus.plugins.ui.contribution.UiContributor;
import org.sonatype.nexus.plugins.ui.contribution.UiContributor.UiContribution;
import org.sonatype.nexus.web.BaseUrlHolder;
import org.sonatype.nexus.web.DelegatingWebResource;
import org.sonatype.nexus.web.WebResource;
import org.sonatype.nexus.web.WebResource.Prepareable;
import org.sonatype.sisu.goodies.common.ComponentSupport;
import org.sonatype.sisu.goodies.template.TemplateEngine;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Extjs-3 UI {@code index.html} resource.
 *
 * @since 2.8
 */
@Named
@Singleton
public class IndexWebResource
  extends ComponentSupport
  implements WebResource, Prepareable
{
  private final Provider<HttpServletRequest> requestProvider;

  private final ApplicationStatusSource applicationStatusSource;

  private final TemplateEngine templateEngine;

  private final BuildNumberService buildNumberService;

  private final Set<UiContributor> uiContributors;

  @Inject
  public IndexWebResource(final Provider<HttpServletRequest> requestProvider,
                          final ApplicationStatusSource applicationStatusSource,
                          final TemplateEngine templateEngine,
                          final BuildNumberService buildNumberService,
                          final Set<UiContributor> uiContributors)
  {
    this.requestProvider = checkNotNull(requestProvider);
    this.applicationStatusSource = checkNotNull(applicationStatusSource);
    this.templateEngine = checkNotNull(templateEngine);
    this.buildNumberService = checkNotNull(buildNumberService);
    this.uiContributors = checkNotNull(uiContributors);
  }

  @Override
  public String getPath() {
    return "/index.html";
  }

  @Nullable
  @Override
  public String getContentType() {
    return "text/html";
  }

  @Override
  public long getLastModified() {
    return System.currentTimeMillis();
  }

  @Override
  public boolean isCacheable() {
    return false;
  }

  @Override
  public long getSize() {
    throw new UnsupportedOperationException("Preparation required");
  }

  @Override
  public InputStream getInputStream() throws IOException {
    throw new UnsupportedOperationException("Preparation required");
  }

  private byte[] renderTemplate(final String templateName) throws IOException {
    SystemStatus systemStatus = applicationStatusSource.getSystemStatus();

    Map<String, Object> params = Maps.newHashMap();
    params.put("serviceBase", "service/local");
    params.put("contentBase", "content");
    params.put("nexusVersion", systemStatus.getVersion());
    params.put("nexusRoot", BaseUrlHolder.get());
    params.put("appName", systemStatus.getAppName());
    params.put("formattedAppName", systemStatus.getFormattedAppName());

    boolean debugMode = isDebugMode();
    params.put("debug", debugMode);

    List<UiContribution> contributions = Lists.newArrayList();
    for (UiContributor contributor : uiContributors) {
      UiContribution contribution = contributor.contribute(debugMode);
      if (contribution.isEnabled()) {
        contributions.add(contribution);
      }
    }
    params.put("rJsContributions", contributions);
    params.put("buildQualifier", buildNumberService.getBuildNumber());

    URL template = getClass().getResource(templateName);
    checkState(template != null, "Missing template: %s", templateName);

    log.debug("Rendering template: {}", template);
    String content = templateEngine.render(this, template, params);

    return content.getBytes();
  }

  private boolean isDebugMode() {
    String query = requestProvider.get().getQueryString();
    return query != null && query.contains("debug");
  }

  @Override
  public WebResource prepare() throws IOException {
    return new DelegatingWebResource(this)
    {
      private final byte[] content = renderTemplate("index.vm");

      @Override
      public long getSize() {
        return content.length;
      }

      @Override
      public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(content);
      }
    };
  }
}
