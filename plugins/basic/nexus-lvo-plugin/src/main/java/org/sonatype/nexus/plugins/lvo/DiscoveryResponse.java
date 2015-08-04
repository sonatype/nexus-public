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
package org.sonatype.nexus.plugins.lvo;

import java.util.HashMap;
import java.util.Map;

import org.sonatype.plexus.rest.xstream.json.PrimitiveKeyedMapConverter;

import com.thoughtworks.xstream.XStream;

public class DiscoveryResponse
{
  public static final String IS_SUCCESSFUL_KEY = "isSuccessful";

  public static final String VERSION_KEY = "version";

  public static final String URL_KEY = "url";

  private final transient DiscoveryRequest request;

  private Map<String, Object> response;

  public boolean isSuccessful() {
    if (getResponse().containsKey(IS_SUCCESSFUL_KEY)) {
      return (Boolean) getResponse().get(IS_SUCCESSFUL_KEY);
    }
    else {
      return false;
    }
  }

  public void setSuccessful(boolean succesful) {
    getResponse().put(IS_SUCCESSFUL_KEY, succesful);
  }

  public DiscoveryResponse(DiscoveryRequest request) {
    this.request = request;
  }

  public DiscoveryRequest getRequest() {
    return request;
  }

  public String getVersion() {
    return (String) getResponse().get(VERSION_KEY);
  }

  public void setVersion(String version) {
    getResponse().put(VERSION_KEY, version);
  }

  public String getUrl() {
    return (String) getResponse().get(URL_KEY);
  }

  public void setUrl(String url) {
    getResponse().put(URL_KEY, url);
  }

  public Map<String, Object> getResponse() {
    if (response == null) {
      response = new HashMap<String, Object>();
    }

    return response;
  }

  // ugly but workable solution
  public static void configureXStream(XStream x) {
    x.alias("lvoResponse", DiscoveryResponse.class);

    x.registerLocalConverter(DiscoveryResponse.class, "response", new PrimitiveKeyedMapConverter(x.getMapper()));
  }
}
