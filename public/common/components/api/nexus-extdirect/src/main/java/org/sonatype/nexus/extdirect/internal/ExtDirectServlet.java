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
package org.sonatype.nexus.extdirect.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.annotation.WebInitParam;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import org.sonatype.nexus.common.lifecycle.Lifecycle;
import org.sonatype.nexus.bootstrap.entrypoint.configuration.ApplicationDirectories;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.extdirect.DirectComponent;
import org.sonatype.nexus.security.authc.AntiCsrfHelper;

import com.google.common.collect.Maps;
import com.google.common.collect.Maps.EntryTransformer;
import com.softwarementors.extjs.djn.EncodingUtils;
import com.softwarementors.extjs.djn.api.Registry;
import com.softwarementors.extjs.djn.config.ApiConfiguration;
import com.softwarementors.extjs.djn.config.GlobalConfiguration;
import com.softwarementors.extjs.djn.router.RequestRouter;
import com.softwarementors.extjs.djn.router.dispatcher.Dispatcher;
import com.softwarementors.extjs.djn.router.processor.poll.PollRequestProcessor;
import com.softwarementors.extjs.djn.servlet.DirectJNgineServlet;
import com.softwarementors.extjs.djn.servlet.DirectJNgineServlet.GlobalParameters;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.net.HttpHeaders.X_FRAME_OPTIONS;
import static com.softwarementors.extjs.djn.router.RequestType.FORM_UPLOAD_POST;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SERVICES;
import static org.sonatype.nexus.servlet.XFrameOptions.DENY;

/**
 * Ext.Direct Servlet.
 *
 * @since 3.0
 */
@WebServlet(urlPatterns = ExtDirectServlet.PREFIXED_MOUNT_POINT,
    initParams = {@WebInitParam(name = GlobalParameters.PROVIDERS_URL, value = ExtDirectServlet.MOUNT_POINT),
        @WebInitParam(name = "minify", value = "false"),
        @WebInitParam(name = GlobalParameters.DEBUG, value = "false"),
        @WebInitParam(name = GlobalParameters.JSON_REQUEST_PROCESSOR_THREAD_CLASS,
            value = "org.sonatype.nexus.extdirect.internal.ExtDirectJsonRequestProcessorThread"),
        @WebInitParam(name = GlobalParameters.GSON_BUILDER_CONFIGURATOR_CLASS,
            value = "org.sonatype.nexus.extdirect.internal.ExtDirectGsonBuilderConfigurator")})
@Component
@ManagedLifecycle(phase = SERVICES)
public class ExtDirectServlet
    extends DirectJNgineServlet
    implements Lifecycle, ApplicationContextAware
{
  public static final String PREFIXED_MOUNT_POINT = "/service/extdirect/*";

  public static final String MOUNT_POINT = "service/extdirect";

  private final Set<Class<?>> loaded = new HashSet<>();

  private final ApplicationDirectories directories;

  private ApplicationContext applicationContext;

  private final ExtDirectDispatcher extDirectDispatcher;

  @Autowired
  public ExtDirectServlet(
      final ApplicationDirectories directories,
      final ExtDirectDispatcher extDirectDispatcher,
      @Value("${nexus.security.anticsrftoken.enabled:true}") final boolean antiCsrfTokenEnabled)
  {
    super(antiCsrfTokenEnabled, AntiCsrfHelper.ANTI_CSRF_TOKEN_NAME, /* Don't allow file uploads */ 0);
    this.directories = checkNotNull(directories);
    this.extDirectDispatcher = checkNotNull(extDirectDispatcher);
  }

  @Override
  public void doPost(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
    HttpServletRequest wrappedRequest = wrapRequest(request);

    // Silence warnings about "clickjacking" (even though it doesn't actually apply to API calls)
    // note that we don't apply this logic for FORM_UPLOAD_POST as extjs means of uploading files uses a hidden iframe
    // which a value of DENY will not be allowed to load
    if (StringUtils.isBlank(response.getHeader(X_FRAME_OPTIONS))
        && !FORM_UPLOAD_POST.equals(getFromRequestContentType(wrappedRequest))) {
      response.setHeader(X_FRAME_OPTIONS, DENY);
    }

    super.doPost(wrappedRequest, response);
  }

  /**
   * Wraps the request to normalize null pathInfo to an empty string.
   * DirectJNgine's RequestRouter.isSourceRequest requires non-null pathInfo; fuzzing requests
   * may arrive without a path component (NEXUS-52996).
   */
  protected HttpServletRequest wrapRequest(final HttpServletRequest request) {
    return new HttpServletRequestWrapper(request)
    {
      private BufferedReader reader;

      @Override
      public BufferedReader getReader() throws IOException {
        if (reader == null) {
          try {
            reader = super.getReader();
          }
          catch (IllegalStateException e) {
            // HACK avoid "java.lang.IllegalStateException: STREAMED" which is thrown in case of request.getReader()
            // and request.getInputStream() was already called as in case of getting request parameters
            reader = new RequestBoundReader(new InputStreamReader(getInputStream(), EncodingUtils.UTF8), request);
          }
        }
        return reader;
      }

      @Override
      public String getPathInfo() {
        String pathInfo = super.getPathInfo();
        return pathInfo != null ? pathInfo : "";
      }
    };
  }

  @Override
  public void start() throws Exception {
    if (getServletConfig() != null) {
      // refresh the existing router configuration
      createDirectJNgineRouter(getServletConfig());
    }
  }

  @Override
  public void stop() {
    // no-op
  }

  @Override
  protected List<ApiConfiguration> createApiConfigurationsFromServletConfigurationApi(
      final ServletConfig configuration)
  {
    List<Class<?>> apiClasses = applicationContext.getBeansOfType(DirectComponent.class)
        .values()
        .stream()
        .map(component -> (Class<?>) component.getClass())
        .filter(clazz -> !loaded.contains(clazz))
        .collect(Collectors.toList());

    loaded.addAll(apiClasses);

    File apiFile = new File(directories.getTemporaryDirectory(), "nexus-extdirect/api.js");
    return List.of(
        new ApiConfiguration(
            "nexus",
            apiFile.getName(),
            apiFile.getAbsolutePath(),
            "NX.direct.api",
            "NX.direct",
            apiClasses));
  }

  @Override
  protected Dispatcher createDispatcher(final Class<? extends Dispatcher> cls) {
    return extDirectDispatcher;
  }

  @Override
  protected RequestRouter createRequestRouter(final Registry registry, final GlobalConfiguration globalConfiguration) {
    final Dispatcher dispatcher = createDispatcher(globalConfiguration.getDispatcherClass());
    return new RequestRouter(registry, globalConfiguration, dispatcher)
    {
      @Override
      public void processPollRequest(
          final Reader reader,
          final Writer writer,
          final String pathInfo) throws IOException
      {
        new PollRequestProcessor(registry, dispatcher, globalConfiguration)
        {
          @Override
          // HACK: we determine parameters from request not by reading request content as request content could had
          // been already read exactly for getting the params, case when request content is already empty
          protected Object[] getParameters() {
            if (reader instanceof RequestBoundReader) {
              ServletRequest request = ((RequestBoundReader) reader).getRequest();
              Map<String, String[]> parameterMap = request.getParameterMap();
              Map<String, String> parameters = Maps.newHashMap();
              if (parameterMap != null) {
                parameters = Maps.transformEntries(parameterMap, new EntryTransformer<String, String[], String>()
                {
                  @Override
                  public String transformEntry(@Nullable final String key, @Nullable final String[] values) {
                    return values == null || values.length == 0 ? null : values[0];
                  }
                });
              }
              return new Object[]{parameters};
            }
            return super.getParameters();
          }
        }.process(reader, writer, pathInfo);
      }
    };
  }

  private static class RequestBoundReader
      extends BufferedReader
  {
    private final ServletRequest request;

    public RequestBoundReader(final Reader in, final ServletRequest request) {
      super(in);
      this.request = request;
    }

    private ServletRequest getRequest() {
      return request;
    }
  }

  @Override
  public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }
}
