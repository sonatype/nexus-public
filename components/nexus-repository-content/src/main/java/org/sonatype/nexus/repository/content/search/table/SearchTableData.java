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
package org.sonatype.nexus.repository.content.search.table;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.StringJoiner;

public class SearchTableData
{
  //PK section
  //A PK from repository table
  private Integer repositoryId;

  //A PK from *_component tables
  private Integer componentId;

  //A PK from *_asset tables
  private Integer assetId;

  //A component type: raw, npm, maven2, etc.
  private String format;

  //Data section
  //namespace column from *_component table
  private String namespace;

  //name column from *_component table
  private String componentName;

  //kind column from *_component table
  private String componentKind;

  //version column from *_component table
  private String version;

  //created column from *_component table
  private OffsetDateTime componentCreated;

  //name column from repository table
  private String repositoryName;

  //path column from *_asset table
  private String path;

  //content_type field from *_asset_blob table
  private String contentType;

  //parsed md5 from checksums column *_asset_blob table
  private String md5;

  //parsed sha1 from checksums column *_asset_blob table
  private String sha1;

  //parsed sha256 from checksums column *_asset_blob table
  private String sha256;

  //parsed sha512 from checksums column *_asset_blob table
  private String sha512;

  public Integer getRepositoryId() {
    return repositoryId;
  }

  public void setRepositoryId(final Integer repositoryId) {
    this.repositoryId = repositoryId;
  }

  public Integer getComponentId() {
    return componentId;
  }

  public void setComponentId(final Integer componentId) {
    this.componentId = componentId;
  }

  public Integer getAssetId() {
    return assetId;
  }

  public void setAssetId(final Integer assetId) {
    this.assetId = assetId;
  }

  public String getFormat() {
    return format;
  }

  public void setFormat(final String format) {
    this.format = format;
  }

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(final String namespace) {
    this.namespace = namespace;
  }

  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(final String componentName) {
    this.componentName = componentName;
  }

  public String getComponentKind() {
    return componentKind;
  }

  public void setComponentKind(final String componentKind) {
    this.componentKind = componentKind;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(final String version) {
    this.version = version;
  }

  public OffsetDateTime getComponentCreated() {
    return componentCreated;
  }

  public void setComponentCreated(final OffsetDateTime componentCreated) {
    this.componentCreated = componentCreated;
  }

  public String getRepositoryName() {
    return repositoryName;
  }

  public void setRepositoryName(final String repositoryName) {
    this.repositoryName = repositoryName;
  }

  public String getPath() {
    return path;
  }

  public void setPath(final String path) {
    this.path = path;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(final String contentType) {
    this.contentType = contentType;
  }

  public String getMd5() {
    return md5;
  }

  public void setMd5(final String md5) {
    this.md5 = md5;
  }

  public String getSha1() {
    return sha1;
  }

  public void setSha1(final String sha1) {
    this.sha1 = sha1;
  }

  public String getSha256() {
    return sha256;
  }

  public void setSha256(final String sha256) {
    this.sha256 = sha256;
  }

  public String getSha512() {
    return sha512;
  }

  public void setSha512(final String sha512) {
    this.sha512 = sha512;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SearchTableData that = (SearchTableData) o;
    return Objects.equals(repositoryId, that.repositoryId) &&
        Objects.equals(componentId, that.componentId) && Objects.equals(assetId, that.assetId) &&
        Objects.equals(format, that.format) && Objects.equals(namespace, that.namespace) &&
        Objects.equals(componentName, that.componentName) &&
        Objects.equals(componentKind, that.componentKind) && Objects.equals(version, that.version) &&
        Objects.equals(componentCreated, that.componentCreated) &&
        Objects.equals(repositoryName, that.repositoryName) && Objects.equals(path, that.path) &&
        Objects.equals(contentType, that.contentType) && Objects.equals(md5, that.md5) &&
        Objects.equals(sha1, that.sha1) && Objects.equals(sha256, that.sha256) &&
        Objects.equals(sha512, that.sha512);
  }

  @Override
  public int hashCode() {
    return Objects.hash(repositoryId, componentId, assetId, format, namespace, componentName, componentKind, version,
        componentCreated, repositoryName, path, contentType, md5, sha1, sha256, sha512);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", SearchTableData.class.getSimpleName() + "[", "]")
        .add("repositoryId=" + repositoryId)
        .add("componentId=" + componentId)
        .add("assetId=" + assetId)
        .add("format='" + format + "'")
        .add("namespace='" + namespace + "'")
        .add("componentName='" + componentName + "'")
        .add("componentKind='" + componentKind + "'")
        .add("version='" + version + "'")
        .add("componentCreated=" + componentCreated)
        .add("repositoryName='" + repositoryName + "'")
        .add("path='" + path + "'")
        .add("contentType='" + contentType + "'")
        .add("md5='" + md5 + "'")
        .add("sha1='" + sha1 + "'")
        .add("sha256='" + sha256 + "'")
        .add("sha512='" + sha512 + "'")
        .toString();
  }
}
