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
package org.sonatype.nexus.bundle.launcher.support;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * TODO
 *
 * @since 2.0
 */
public class RequestUtils
{

  private static final Logger LOG = LoggerFactory.getLogger(RequestUtils.class);

  public static boolean isNexusRESTStarted(final String nexusBaseURI) {
    final String serviceStatusURI = checkNotNull(nexusBaseURI).endsWith("/")
        ? nexusBaseURI + "service/local/status"
        : nexusBaseURI + "/service/local/status";

    final HttpParams params = new BasicHttpParams();
    HttpConnectionParams.setConnectionTimeout(params, 2000);
    HttpConnectionParams.setSoTimeout(params, 2000);

    final DefaultHttpClient client = new DefaultHttpClient(params);
    client.getCredentialsProvider().setCredentials(
        AuthScope.ANY,
        new UsernamePasswordCredentials("admin", "admin123")
    );
    final HttpGet request = new HttpGet(serviceStatusURI);
    try {
      final HttpResponse response = client.execute(request);
      final int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != 200) {
        LOG.debug("Nexus Status Check: Returned status: " + statusCode);
        return false;
      }
      final String responseAsString = new BasicResponseHandler().handleResponse(response);
      if (responseAsString == null || !responseAsString.contains("<state>STARTED</state>")) {
        LOG.debug("Nexus Status Check: Invalid system state. Status: " + responseAsString);
        return false;
      }
    }
    catch (IOException e) {
      LOG.debug("Nexus Status Check: Failed with: " + e.getMessage());
      return false;
    }
    finally {
      HttpClientUtils.closeQuietly(client);
    }
    return true;
  }

}
