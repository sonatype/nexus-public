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
package org.sonatype.nexus.repository.content.search;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.content.SearchResult;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link SearchResult} data backed by the content data store.
 *
 * @since 3.38
 */
public class SearchResultData
    implements SearchResult
{
  private Integer componentId; // NOSONAR: internal id

  private Integer repositoryId; // NOSONAR: internal id

  private NestedAttributesMap attributes = new NestedAttributesMap("attributes", new HashMap<>());

  private OffsetDateTime lastModified;

  private String namespace;

  private String format;

  private String componentName;

  private String version;

  private String normalisedVersion;

  private String repositoryName;

  private List<String> tags;

  @Override
  public Integer componentId() {
    return componentId;
  }

  @Override
  public Integer repositoryId() {
    return repositoryId;
  }

  @Override
  public NestedAttributesMap attributes() {
    return attributes;
  }

  @Override
  public String namespace() {
    return namespace;
  }

  @Override
  public String componentName() {
    return componentName;
  }

  @Override
  public OffsetDateTime lastModified() {
    return lastModified;
  }

  @Override
  public String repositoryName() {
    return repositoryName;
  }

  @Override
  public String version() {
    return version;
  }

  @Override
  public String normalisedVersion() {
    return normalisedVersion;
  }

  @Override
  public List<String> tags() {
    return tags;
  }

  @Override
  public String format() {
    return format;
  }

  public void setAttributes(final NestedAttributesMap attributes) {
    this.attributes = checkNotNull(attributes);
  }

  public void setLastModified(final OffsetDateTime lastModified) {
    this.lastModified = checkNotNull(lastModified);
  }

  public void setComponentId(final Integer componentId) {
    this.componentId = componentId;
  }

  public void setRepositoryId(final int repositoryId) {
    this.repositoryId = repositoryId;
  }

  public void setNamespace(final String namespace) {
    this.namespace = namespace;
  }

  public void setComponentName(final String componentName) {
    this.componentName = componentName;
  }

  public void setVersion(final String version) {
    this.version = version;
  }

  public void setNormalisedVersion(final String normalisedVersion) {
    this.normalisedVersion = normalisedVersion;
  }

  public void setRepositoryName(final String repositoryName) {
    this.repositoryName = repositoryName;
  }

  public void setFormat(final String format) {
    this.format = format;
  }

  public void setTags(final List<String> tags) {
    this.tags = tags;
  }

  @Override
  public String toString() {
    return "SearchResultData{" +
        "componentId=" + componentId +
        ", repositoryId=" + repositoryId +
        ", namespace='" + namespace + '\'' +
        ", format='" + format + '\'' +
        ", componentName='" + componentName + '\'' +
        ", version='" + version + '\'' +
        ", normalisedVersion='" + normalisedVersion + '\'' +
        ", repositoryName='" + repositoryName + '\'' +
        ", lastModified='" + lastModified + '\'' +
        ", tags='" + tags + '\'' +
        '}';
  }
}
