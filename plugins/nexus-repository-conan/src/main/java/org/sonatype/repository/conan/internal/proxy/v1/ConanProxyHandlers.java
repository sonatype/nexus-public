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
package org.sonatype.repository.conan.internal.proxy.v1;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.property.SystemPropertiesHelper;
import org.sonatype.nexus.repository.httpclient.HttpClientFacet;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.proxy.ProxyHandler;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Parameters;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.payloads.StringPayload;
import org.sonatype.repository.conan.internal.ConanSystemProperties;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import static org.sonatype.nexus.repository.http.HttpStatus.OK;
import static org.sonatype.nexus.repository.view.ContentTypes.APPLICATION_JSON;
import static org.sonatype.nexus.repository.view.Status.success;

/**
 * @since 3.28
 */
@Named
@Singleton
public class ConanProxyHandlers
{
  public static final int DEFAULT_SEARCH_TIMEOUT = 120000;

  @Inject
  public ProxyHandler proxyHandler;

  public final Handler searchQueryHandler = context -> {
    Parameters parameters = context.getRequest().getParameters();
    List<NameValuePair> listParameters = StreamSupport
        .stream(parameters.spliterator(), false)
        .map(entry -> new BasicNameValuePair(entry.getKey(), entry.getValue()))
        .collect(Collectors.toList());
    String path = context.getRequest().getPath().substring(1);
    URI remoteUrl = context.getRepository().facet(ProxyFacet.class)
        .getRemoteUrl();
    URI nextUri = remoteUrl.resolve(path);
    URI requestUri = new URIBuilder(nextUri).addParameters(listParameters).build();

    HttpClient httpClient = context.getRepository()
        .facet(HttpClientFacet.class)
        .getHttpClient();
    HttpGet httpGet = new HttpGet(requestUri);

    int timeout = SystemPropertiesHelper.getInteger(ConanSystemProperties.PROXY_SEARCH_TIMEOUT, DEFAULT_SEARCH_TIMEOUT);
    RequestConfig params = RequestConfig.custom()
        .setSocketTimeout(timeout)
        .build();
    httpGet.setConfig(params);

    HttpResponse response = httpClient.execute(httpGet);
    HttpEntity entity = response.getEntity();
    String JSON = EntityUtils.toString(entity);

    return new Response.Builder()
        .status(success(OK))
        .payload(new StringPayload(JSON, APPLICATION_JSON))
        .build();
  };
}
