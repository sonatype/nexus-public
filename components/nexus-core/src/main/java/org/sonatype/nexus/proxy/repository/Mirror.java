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
package org.sonatype.nexus.proxy.repository;

import org.codehaus.plexus.util.StringUtils;

public class Mirror
{
  private String id;

  private String url;

  private String mirrorOfUrl;

  public Mirror(String id, String url) {
    setId(id);
    setUrl(url);
  }

  public Mirror(String id, String url, String mirrorOfUrl) {
    setId(id);
    setUrl(url);
    setMirrorOfUrl(mirrorOfUrl);
  }

  public String getId() {
    return id;
  }

  public String getUrl() {
    return url;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  // ==

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || (o.getClass() != this.getClass())) {
      return false;
    }

    Mirror other = (Mirror) o;

    return StringUtils.equals(getId(), other.getId()) && StringUtils.equals(getUrl(), other.getUrl());
  }

  public int hashCode() {
    int result = 7;

    result = 31 * result + (id == null ? 0 : id.hashCode());

    result = 31 * result + (url == null ? 0 : url.hashCode());

    return result;
  }

  public void setMirrorOfUrl(String mirrorOfUrl) {
    this.mirrorOfUrl = mirrorOfUrl;
  }

  public String getMirrorOfUrl() {
    return mirrorOfUrl;
  }
}
