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
package org.sonatype.nexus.repository.search;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Result of a component search
 *
 * @since 3.38
 */
public class ComponentSearchResult
{
  private String id;

  private String repositoryName;

  private String group;

  private String name;

  private String version;

  private String format;

  private OffsetDateTime lastDownloaded;

  private OffsetDateTime lastModified;

  private List<AssetSearchResult> assets;

  private Map<String, Object> annotations = new HashMap<>();

  /**
   * Adds an annotation to the search result, this is an extension point for plugins. The ID must be unique.
   */
  public void addAnnotation(final String id, final Object annotation) {
    checkArgument(!annotations.containsKey(id), "Annotation " + id + " already exists on the component.");
    annotations.put(id, annotation);
  }

  /**
   * Returns the requested annotation if it has been set.
   */
  @SuppressWarnings("unchecked")
  public Object getAnnotation(final String id) {
    return annotations.get(id);
  }

  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public String getRepositoryName() {
    return repositoryName;
  }

  public void setRepositoryName(final String repositoryName) {
    this.repositoryName = repositoryName;
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(final String group) {
    this.group = group;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(final String version) {
    this.version = version;
  }

  public String getFormat() {
    return format;
  }

  public void setFormat(final String format) {
    this.format = format;
  }

  public List<AssetSearchResult> getAssets() {
    return assets != null ? assets : List.of();
  }

  public void setAssets(final List<AssetSearchResult> assets) {
    this.assets = assets;
  }

  /**
   * Represents the latest date a blob from any asset associated with the component was changed.
   */
  public OffsetDateTime getLastModified() {
    return lastModified;
  }

  public void setLastModified(final OffsetDateTime lastModified) {
    this.lastModified = lastModified;
  }

  /**
   * Represents the most recent time any asset associated with this component was downloaded.
   */
  public OffsetDateTime getLastDownloaded() {
    return lastDownloaded;
  }

  public void setLastDownloaded(final OffsetDateTime lastDownloaded) {
    this.lastDownloaded = lastDownloaded;
  }

  public void addAsset(final AssetSearchResult asset) {
    if (assets == null) {
      assets = new ArrayList<>();
    }
    assets.add(asset);
  }

  @Override
  public String toString() {
    return "ComponentSearchResult [id=" + id + ", repositoryName=" + repositoryName + ", group=" + group + ", name="
        + name + ", version=" + version + ", format=" + format + ", lastDownloaded=" + lastDownloaded
        + ", lastModified=" + lastModified + ", assets=" + assets + ", annotations=" + annotations + "]";
  }
}
