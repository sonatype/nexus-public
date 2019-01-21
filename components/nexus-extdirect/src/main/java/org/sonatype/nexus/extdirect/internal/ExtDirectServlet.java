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
import java.io.StringWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletConfig;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.goodies.lifecycle.Lifecycle;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.extdirect.DirectComponent;
import org.sonatype.nexus.security.authc.AntiCsrfHelper;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Maps.EntryTransformer;
import com.google.inject.Key;
import com.softwarementors.extjs.djn.EncodingUtils;
import com.softwarementors.extjs.djn.api.Registry;
import com.softwarementors.extjs.djn.config.ApiConfiguration;
import com.softwarementors.extjs.djn.config.GlobalConfiguration;
import com.softwarementors.extjs.djn.router.RequestRouter;
import com.softwarementors.extjs.djn.router.dispatcher.Dispatcher;
import com.softwarementors.extjs.djn.router.processor.poll.PollRequestProcessor;
import com.softwarementors.extjs.djn.router.processor.standard.form.FormPostRequestData;
import com.softwarementors.extjs.djn.router.processor.standard.form.upload.FileUploadException;
import com.softwarementors.extjs.djn.servlet.DirectJNgineServlet;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.sisu.BeanEntry;
import org.eclipse.sisu.inject.BeanLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SERVICES;

/**
 * Ext.Direct Servlet.
 *
 * @since 3.0
 */
@Named
@ManagedLifecycle(phase = SERVICES)
@Singleton
public class ExtDirectServlet
    extends DirectJNgineServlet
    implements Lifecycle
{
  private static final Logger log = LoggerFactory.getLogger(ExtDirectServlet.class);

  private final ApplicationDirectories directories;

  private final BeanLocator beanLocator;

  private final ExtDirectDispatcher extDirectDispatcher;

  @Inject
  public ExtDirectServlet(final ApplicationDirectories directories,
                          final BeanLocator beanLocator,
                          final ExtDirectDispatcher extDirectDispatcher,
                          @Named("${nexus.security.anticsrftoken.enabled:-true}") final boolean antiCsrfTokenEnabled)
  {
    super(antiCsrfTokenEnabled, AntiCsrfHelper.ANTI_CSRF_TOKEN_NAME, -1);
    this.directories = checkNotNull(directories);
    this.beanLocator = checkNotNull(beanLocator);
    this.extDirectDispatcher = checkNotNull(extDirectDispatcher);
  }

  @Override
  public void doPost(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
    HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request)
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
    };

    try {
      super.doPost(wrappedRequest, response);
    } catch (FileUploadException fileUploadException) {
      try {
        FileItemIterator fileItems = new ServletFileUpload(new DiskFileItemFactory()).getItemIterator(request);
        String tid = getTransactionId(fileItems);

        // Send the error from the exception in a json object so we may capture it on the frontend.
        response.setContentType("text/html");
        response.getWriter().append("<html><body><textarea>{\"tid\":" + tid +
            ",\"action\":\"coreui_Upload\",\"method\":\"doUpload\",\"result\":{\"success\": false,\"message\":\"" +
            escapeHtml(fileUploadException.getMessage()) + "\"},\"type\":\"rpc\"}</textarea></body></html>").flush();
      }
      catch (Exception e) {
        log.warn("Unable to read the ext direct transaction id for upload", e);
        throw fileUploadException;
      }
    }
  }

  private String getTransactionId(final FileItemIterator fileItems)
      throws org.apache.commons.fileupload.FileUploadException, IOException
  {
    String tid = null;
    while (tid == null && fileItems.hasNext()) {
      FileItemStream fileItemStream = fileItems.next();
      if (StringUtils.equals(fileItemStream.getFieldName(), FormPostRequestData.TID_ELEMENT)) {
        StringWriter writer = new StringWriter();
        IOUtils.copy(fileItemStream.openStream(), writer, UTF_8);
        tid = writer.toString();
      }
    }
    return tid;
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
    Iterable<? extends BeanEntry<Annotation, DirectComponent>> entries =
        beanLocator.locate(Key.get(DirectComponent.class));

    List<Class<?>> apiClasses = Lists.newArrayList(
        Iterables.transform(entries, new Function<BeanEntry<Annotation, DirectComponent>, Class<?>>()
        {
          @Nullable
          @Override
          public Class<?> apply(final BeanEntry<Annotation, DirectComponent> input) {
            Class<DirectComponent> implementationClass = input.getImplementationClass();
            log.debug("Registering Ext.Direct component '{}'", implementationClass);
            return implementationClass;
          }
        })
    );
    File apiFile = new File(directories.getTemporaryDirectory(), "nexus-extdirect/api.js");
    return Lists.newArrayList(
        new ApiConfiguration(
            "nexus",
            apiFile.getName(),
            apiFile.getAbsolutePath(),
            "NX.direct.api",
            "NX.direct",
            apiClasses
        )
    );
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
      public void processPollRequest(final Reader reader, final Writer writer, final String pathInfo)
          throws IOException
      {
        new PollRequestProcessor(registry, dispatcher, globalConfiguration)
        {
          @Override
          // HACK: we determine parameters from request not by reading request content as request content could had
          // been already read exactly for getting the params, case when request content is already empty
          protected Object[] getParameters()
          {
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
}
