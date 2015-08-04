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
package org.sonatype.nexus.plugins.rrb;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.sonatype.nexus.apachehttpclient.Hc4Provider;
import org.sonatype.nexus.plugins.rrb.parsers.ArtifactoryRemoteRepositoryParser;
import org.sonatype.nexus.plugins.rrb.parsers.HtmlRemoteRepositoryParser;
import org.sonatype.nexus.plugins.rrb.parsers.RemoteRepositoryParser;
import org.sonatype.nexus.plugins.rrb.parsers.S3RemoteRepositoryParser;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.storage.remote.http.QueryStringBuilder;

import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.BasicHttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class for retrieving directory data from remote repository. This class is not thread-safe!
 */
public class MavenRepositoryReader
{

  private final Logger logger = LoggerFactory.getLogger(MavenRepositoryReader.class);

  private final HttpClient client;

  private final QueryStringBuilder queryStringBuilder;

  private ProxyRepository proxyRepository;

  private String remotePath;

  private String remoteUrl;

  private String localUrl;

  private String id;

  public MavenRepositoryReader(final HttpClient client, final QueryStringBuilder queryStringBuilder) {
    this.client = checkNotNull(client);
    this.queryStringBuilder = checkNotNull(queryStringBuilder);
  }

  /**
   * @param remotePath remote path added to the URL
   * @param localUrl   url to the local resource service
   * @return a list containing the remote data
   */
  public List<RepositoryDirectory> extract(String remotePath, String localUrl, final ProxyRepository proxyRepository,
                                           String id)
  {
    logger.debug("remotePath={}", remotePath);
    this.remotePath = remotePath;
    this.localUrl = localUrl;
    this.proxyRepository = proxyRepository;

    this.id = id;

    // NEXUS-5811: check first if we have an valid URL as remote path. If we do we use that one, else we have a relative
    // path and we append it to proxy repository URL
    try {
      new URL(remotePath);
      remoteUrl = remotePath;
    }
    catch (MalformedURLException e) {
      String baseRemoteUrl = proxyRepository.getRemoteUrl();

      if (!baseRemoteUrl.endsWith("/") && !remotePath.startsWith("/")) {
        remoteUrl = baseRemoteUrl + "/" + remotePath;
      }
      else {
        remoteUrl = baseRemoteUrl + remotePath;
      }
    }

    StringBuilder html = getContent();
    if (logger.isDebugEnabled()) {
      logger.trace(html.toString());
    }
    return parseResult(html);
  }

  private ArrayList<RepositoryDirectory> parseResult(StringBuilder indata) {
    RemoteRepositoryParser parser = null;
    String baseUrl = "";
    if (proxyRepository != null) {
      baseUrl = proxyRepository.getRemoteUrl();
    }

    if (indata.indexOf("<html ") != -1) {
      // if title="Artifactory" then it is an Artifactory repo...
      if (indata.indexOf("title=\"Artifactory\"") != -1) {
        logger.debug("is Artifactory repository");
        parser = new ArtifactoryRemoteRepositoryParser(remotePath, localUrl, id, baseUrl);
      }
      else {
        logger.debug("is html repository");
        parser = new HtmlRemoteRepositoryParser(remotePath, localUrl, id, baseUrl);
      }
    }
    else if (indata.indexOf("xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\"") != -1
        || (indata.indexOf("<?xml") != -1 && responseContainsError(indata))) {
      logger.debug("is S3 repository");
      if (responseContainsError(indata) && !responseContainsAccessDenied(indata)) {
        logger.debug("response from S3 repository contains error, need to find rootUrl");
        remoteUrl = findcreateNewUrl(indata);
        indata = getContent();
      }
      else if (responseContainsError(indata) && responseContainsAccessDenied(indata)) {
        logger.debug("response from S3 repository contains access denied response");
        indata = new StringBuilder();
      }

      parser =
          new S3RemoteRepositoryParser(remotePath, localUrl, id, baseUrl.replace(findRootUrl(indata), ""));
    }
    else {
      logger.debug("Found no matching parser, using default html parser");

      parser = new HtmlRemoteRepositoryParser(remotePath, localUrl, id, baseUrl);
    }
    return parser.extractLinks(indata);
  }

  private String maybeAppendQueryString(final String url) {
    String queryString = queryStringBuilder.getQueryString(proxyRepository);

    // if there is no query-string ot append, simply return what we've been handed
    if (StringUtils.isEmpty(queryString)) {
      return url;
    }

    // else sort out the seperator to use and append the query-string
    String sep = url.contains("?") ? "&" : "?";
    return url + sep + queryString;
  }

  private String findcreateNewUrl(StringBuilder indata) {
    logger.debug("indata={}", indata.toString());
    String key = extracktKey(indata);
    String newUrl = "";
    if (!key.equals("")) {
      newUrl = findRootUrl(indata);
      newUrl += "?prefix=" + key;
    }
    if (!newUrl.endsWith("/")) {
      newUrl += "/";
    }

    newUrl = maybeAppendQueryString(newUrl);

    logger.debug("newUrl={}", newUrl);
    return newUrl;
  }

  private String findRootUrl(StringBuilder indata) {
    int end = remoteUrl.indexOf(extracktKey(indata));
    if (end > 0) {
      String newUrl = remoteUrl.substring(0, end);
      if (newUrl.indexOf('?') != -1) {
        newUrl = newUrl.substring(0, newUrl.indexOf('?'));
      }
      return newUrl;
    }
    return remoteUrl;
  }

  private String extracktKey(StringBuilder indata) {
    String key = "";
    int start = indata.indexOf("<Key>");
    int end = indata.indexOf("</Key>");
    if (start > 0 && end > start) {
      key = indata.substring(start + 5, end);
    }
    return key;
  }

  /**
   * Used to detect error in S3 response.
   */
  private boolean responseContainsError(StringBuilder indata) {
    return indata.indexOf("<Error>") != -1 || indata.indexOf("<error>") != -1;
  }

  /**
   * Used to detect access denied in S3 response.
   */
  private boolean responseContainsAccessDenied(StringBuilder indata) {
    return indata.indexOf("<Code>AccessDenied</Code>") != -1 || indata.indexOf("<code>AccessDenied</code>") != -1;
  }

  private StringBuilder getContent() {
    StringBuilder buff = new StringBuilder();

    String sep = remoteUrl.contains("?") ? "&" : "?";
    String url = remoteUrl + sep + "delimiter=/";
    url = maybeAppendQueryString(url);

    HttpGet method = new HttpGet(url);
    try {
      logger.debug("Requesting: {}", method);

      final BasicHttpContext httpContext = new BasicHttpContext();
      httpContext.setAttribute(Hc4Provider.HTTP_CTX_KEY_REPOSITORY, proxyRepository);

      final HttpResponse response = client.execute(method, httpContext);

      int statusCode = response.getStatusLine().getStatusCode();
      logger.debug("Status code: {}", statusCode);

      try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {
        String line;
        while ((line = reader.readLine()) != null) {
          buff.append(line).append("\n");
        }
      }

      // HACK: Deal with S3 edge-case
      // here is the deal, For reasons I do not understand, S3 comes back with an empty response (and a 200),
      // stripping off the last '/' returns the error we are looking for (so we can do a query)
      Header serverHeader = response.getFirstHeader(HttpHeaders.SERVER);
      if (buff.length() == 0 && serverHeader != null &&
          serverHeader.getValue().equalsIgnoreCase("AmazonS3") &&
          remoteUrl.endsWith("/")) {
        remoteUrl = remoteUrl.substring(0, remoteUrl.length() - 1);
        return getContent();
      }

      return buff;
    }
    catch (Exception e) {
      logger.warn("Failed to get directory listing content", e);
    }

    return buff;
  }
}
