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
package org.sonatype.repository.helm.internal.metadata;

import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;

/**
 * Represents a chart entry in index.yaml
 *
 * @since 3.next
 */
public class ChartEntry
{
  private String description;
  private String name;
  private String version;
  private DateTime created;
  private String appVersion;
  private String digest;
  private String icon;
  private List<String> urls;
  private List<String> sources;
  private List<Map<String, String>> maintainers;

  public String getName() { return this.name; }

  public void setName(final String name) { this.name = name; }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(final String version) {
    this.version = version;
  }

  public DateTime getCreated() {
    return created;
  }

  public void setCreated(final DateTime created) {
    this.created = created;
  }

  public String getAppVersion() {
    return appVersion;
  }

  public void setAppVersion(final String appVersion) {
    this.appVersion = appVersion;
  }

  public String getDigest() {
    return digest;
  }

  public void setDigest(final String digest) {
    this.digest = digest;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(final String icon) {
    this.icon = icon;
  }

  public List<String> getUrls() {
    return urls;
  }

  public void setUrls(final List<String> urls) {
    this.urls = urls;
  }

  public List<String> getSources() {
    return sources;
  }

  public void setSources(final List<String> sources) {
    this.sources = sources;
  }

  public List<Map<String, String>> getMaintainers() {
    return maintainers;
  }

  public void setMaintainers(final List<Map<String, String>> maintainers) {
    this.maintainers = maintainers;
  }
}
