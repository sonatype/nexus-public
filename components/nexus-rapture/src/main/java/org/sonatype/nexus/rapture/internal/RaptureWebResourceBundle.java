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
package org.sonatype.nexus.rapture.internal;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.common.template.TemplateAccessible;
import org.sonatype.nexus.common.template.TemplateHelper;
import org.sonatype.nexus.common.template.TemplateParameters;
import org.sonatype.nexus.rapture.UiPluginDescriptor;
import org.sonatype.nexus.rapture.internal.state.StateComponent;
import org.sonatype.nexus.rapture.ReactFrontendConfiguration;
import org.sonatype.nexus.servlet.ServletHelper;
import org.sonatype.nexus.webresources.GeneratedWebResource;
import org.sonatype.nexus.webresources.WebResource;
import org.sonatype.nexus.webresources.WebResourceBundle;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Rapture {@link WebResourceBundle}.
 *
 * Provides resources:
 * <ul>
 * <li>{@code /index.html}</li>
 * <li>{@code /static/rapture/bootstrap.js}</li>
 * <li>{@code /static/rapture/app.js}</li>
 * </ul>
 *
 * @since 3.0
 */
@Named
@Singleton
public class RaptureWebResourceBundle
    extends ComponentSupport
    implements WebResourceBundle
{
  private final ApplicationVersion applicationVersion;

  private final Provider<HttpServletRequest> servletRequestProvider;

  private final Provider<StateComponent> stateComponentProvider;

  private final TemplateHelper templateHelper;

  private final List<UiPluginDescriptor> pluginDescriptors;

  private final Gson gson;

  private final ReactFrontendConfiguration reactFrontendConfiguration;

  @Inject
  public RaptureWebResourceBundle(final ApplicationVersion applicationVersion,
                                  final Provider<HttpServletRequest> servletRequestProvider,
                                  final Provider<StateComponent> stateComponentProvider,
                                  final TemplateHelper templateHelper,
                                  final List<UiPluginDescriptor> pluginDescriptors,
                                  final ReactFrontendConfiguration reactFrontendConfiguration)
  {
    this.applicationVersion = checkNotNull(applicationVersion);
    this.servletRequestProvider = checkNotNull(servletRequestProvider);
    this.stateComponentProvider = checkNotNull(stateComponentProvider);
    this.templateHelper = checkNotNull(templateHelper);
    this.pluginDescriptors = checkNotNull(pluginDescriptors);
    this.reactFrontendConfiguration = checkNotNull(reactFrontendConfiguration);

    log.info("UI plugin descriptors:");
    for (UiPluginDescriptor descriptor : pluginDescriptors) {
      log.info("  {}", descriptor.getPluginId());
    }

    gson = new GsonBuilder().setPrettyPrinting().create();
  }

  //
  // FIXME: optimize so that we dont duplicate things like isDebug() over and over each request
  // FIXME: for now we simply do a bit more work than is needed :-(
  //

  @Override
  public List<WebResource> getResources() {
    return ImmutableList.of(
        index_html(),
        bootstrap_js(),
        app_js()
    );
  }

  private abstract class TemplateWebResource
      extends GeneratedWebResource
  {
    protected byte[] render(final String template, final TemplateParameters parameters) throws IOException {
      log.trace("Rendering template: {}, with params: {}", template, parameters);
      URL url = getClass().getResource(template);
      return templateHelper.render(url, parameters).getBytes();
    }
  }

  @TemplateAccessible
  public static class TemplateUtil
  {
    /**
     * Helper to return the filename for a URI.
     */
    public String fileName(final URI uri) {
      String path = uri.getPath();
      int i = path.lastIndexOf('/');
      return path.substring(i + 1, path.length());
    }
  }

  /**
   * The index.html resource.
   */
  private WebResource index_html() {
    return new TemplateWebResource()
    {
      @Override
      public String getPath() {
        return "/index.html";
      }

      @Override
      public String getContentType() {
        return HTML;
      }

      @Override
      protected byte[] generate() throws IOException {
        return render("index.vm", new TemplateParameters()
                .set("baseUrl", BaseUrlHolder.get())
                .set("debug", isDebug())
                .set("urlSuffix", generateUrlSuffix())
                .set("styles", getStyles())
                .set("scripts", getScripts())
                .set("util", new TemplateUtil())
        );
      }
    };
  }

  /**
   * The bootstrap.js resource.
   */
  private WebResource bootstrap_js() {
    return new TemplateWebResource()
    {
      @Override
      public String getPath() {
        return "/static/rapture/bootstrap.js";
      }

      @Override
      public String getContentType() {
        return JAVASCRIPT;
      }

      @Override
      protected byte[] generate() throws IOException {
        return render("bootstrap.vm", new TemplateParameters()
                .set("baseUrl", BaseUrlHolder.get())
                .set("debug", isDebug())
                .set("urlSuffix", generateUrlSuffix())
                .set("namespaces", getNamespaces())
        );
      }
    };
  }

  /**
   * The app.js resource.
   */
  private WebResource app_js() {
    return new TemplateWebResource()
    {
      @Override
      public String getPath() {
        return "/static/rapture/app.js";
      }

      @Override
      public String getContentType() {
        return JAVASCRIPT;
      }

      @Override
      protected byte[] generate() throws IOException {
        return render("app.vm", new TemplateParameters()
                .set("baseUrl", BaseUrlHolder.get())
                .set("debug", isDebug())
                .set("state", gson.toJson(getState()))
                .set("pluginConfigs", getPluginConfigs())
        );
      }
    };
  }

  /**
   * Generate URL suffix to use on all requests when loading the index.
   */
  private String generateUrlSuffix() {
    StringBuilder buff = new StringBuilder();
    String version = applicationVersion.getVersion();
    String edition = applicationVersion.getEdition();
    buff.append("_v=").append(version);
    buff.append("&_e=").append(edition);

    // if version is a SNAPSHOT, then append additional timestamp to disable cache
    if (version.endsWith("SNAPSHOT")) {
      buff.append("&_dc=").append(System.currentTimeMillis());
    }

    // when debug, add parameter
    if (isDebug()) {
      buff.append("&debug=true");
    }

    return buff.toString();
  }

  /**
   * Check if ?debug parameter is given on the request.
   */
  private boolean isDebug() {
    HttpServletRequest request = servletRequestProvider.get();
    return ServletHelper.isDebug(request);
  }

  /**
   * Returns the initial state for the application.
   */
  private Map<String, Object> getState() {
    return stateComponentProvider.get().getState(Maps.<String, String>newHashMap());
  }

  /**
   * Find all plugin configs.
   */
  private List<String> getPluginConfigs() {
    List<String> classNames = Lists.newArrayList();
    for (UiPluginDescriptor descriptor : pluginDescriptors) {
      String className = descriptor.getConfigClassName();
      if (className != null) {
        classNames.add(className);
      }
    }
    return classNames;
  }

  /**
   * Determine all plugin namespaces.
   */
  private List<String> getNamespaces() {
    List<String> namespaces = Lists.newArrayList();
    for (UiPluginDescriptor descriptor : pluginDescriptors) {
      String ns = descriptor.getNamespace();
      if (ns != null) {
        namespaces.add(ns);
      }
    }
    return namespaces;
  }

  /**
   * Replaces "{mode}" in given path with either "prod" or "debug".
   */
  private String mode(final String path) {
    String mode = isDebug() ? "debug" : "prod";
    return path.replaceAll("\\{mode\\}", mode);
  }

  /**
   * Generate a URI for the given path.
   */
  private URI uri(final String path) {
    try {
      return new URI(String.format("%s/static/rapture/%s", BaseUrlHolder.get(), path));
    }
    catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Generate the list of CSS styles to include in the index.html.
   */
  private List<URI> getStyles() {
    List<URI> styles = Lists.newArrayList();
    styles.add(uri(mode("resources/loading-{mode}.css")));
    styles.add(uri(mode("resources/baseapp-{mode}.css")));

    // add all plugin styles
    for (UiPluginDescriptor descriptor : pluginDescriptors) {
      if (descriptor.hasStyle()) {
        String path = String.format("resources/%s-{mode}.css", descriptor.getPluginId());
        styles.add(uri(mode(path)));
      }
    }

    return styles;
  }

  /**
   * Generate the list of javascript sources to include in the index.html.
   */
  private List<URI> getScripts() {
    List<URI> scripts = Lists.newArrayList();

    scripts.add(uri(mode("baseapp-{mode}.js")));
    scripts.add(uri(mode("extdirect-{mode}.js")));
    scripts.add(uri("bootstrap.js"));
    scripts.add(uri("d3.v4.min.js"));

    if (reactFrontendConfiguration.isEnabled()) {
      scripts.add(uri("frontend-bundle.js"));
    }

    // add all "prod" plugin scripts if debug is not enabled
    if (!isDebug()) {
      for (UiPluginDescriptor descriptor : pluginDescriptors) {
        if (descriptor.hasScript()) {
          String path = String.format("%s-prod.js", descriptor.getPluginId());
          scripts.add(uri(path));
        }
      }
    }

    scripts.add(uri("app.js"));
    return scripts;
  }
}
