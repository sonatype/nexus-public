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

  private String normalisedVersion;

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

  //Custom format attributes
  private String formatField1;

  private String formatField2;

  private String formatField3;

  private String formatField4;

  private String formatField5;

  // asset's blob uploaderBy property
  private String uploader;

  // asset's blob uploaderByIp property
  private String uploaderIp;

  public SearchTableData() {
  }

  public SearchTableData(final Integer repositoryId, final String format) {
    this(repositoryId, null, null, format);
  }

  public SearchTableData(final Integer repositoryId, final Integer componentId, final String format) {
    this(repositoryId, componentId, null, format);
  }

  public SearchTableData(
      final Integer repositoryId,
      final Integer componentId,
      final Integer assetId,
      final String format)
  {
    this.repositoryId = repositoryId;
    this.componentId = componentId;
    this.assetId = assetId;
    this.format = format;
  }

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

  public void setNormalisedVersion(final String normalisedVersion) {
    this.normalisedVersion = normalisedVersion;
  }

  public String getNormalisedVersion() {
    return normalisedVersion;
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

  public String getFormatField1() {
    return formatField1;
  }

  public void setFormatField1(final String formatField1) {
    this.formatField1 = formatField1;
  }

  public String getFormatField2() {
    return formatField2;
  }

  public void setFormatField2(final String formatField2) {
    this.formatField2 = formatField2;
  }

  public String getFormatField3() {
    return formatField3;
  }

  public void setFormatField3(final String formatField3) {
    this.formatField3 = formatField3;
  }

  public String getFormatField4() {
    return formatField4;
  }

  public void setFormatField4(final String formatField4) {
    this.formatField4 = formatField4;
  }

  public String getFormatField5() {
    return formatField5;
  }

  public void setFormatField5(final String formatField5) {
    this.formatField5 = formatField5;
  }

  public void setUploader(final String uploader) {
    this.uploader = uploader;
  }

  public String getUploader() {
    return uploader;
  }

  public void setUploaderIp(final String uploaderIp) {
    this.uploaderIp = uploaderIp;
  }

  public String getUploaderIp() {
    return uploaderIp;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SearchTableData tableData = (SearchTableData) o;
    return Objects.equals(repositoryId, tableData.repositoryId) &&
        Objects.equals(componentId, tableData.componentId) &&
        Objects.equals(assetId, tableData.assetId) && Objects.equals(format, tableData.format) &&
        Objects.equals(namespace, tableData.namespace) &&
        Objects.equals(componentName, tableData.componentName) &&
        Objects.equals(componentKind, tableData.componentKind) &&
        Objects.equals(version, tableData.version) &&
        Objects.equals(normalisedVersion, tableData.normalisedVersion) &&
        Objects.equals(componentCreated, tableData.componentCreated) &&
        Objects.equals(repositoryName, tableData.repositoryName) &&
        Objects.equals(path, tableData.path) && Objects.equals(contentType, tableData.contentType) &&
        Objects.equals(md5, tableData.md5) && Objects.equals(sha1, tableData.sha1) &&
        Objects.equals(sha256, tableData.sha256) && Objects.equals(sha512, tableData.sha512) &&
        Objects.equals(formatField1, tableData.formatField1) &&
        Objects.equals(formatField2, tableData.formatField2) &&
        Objects.equals(formatField3, tableData.formatField3) &&
        Objects.equals(formatField4, tableData.formatField4) &&
        Objects.equals(formatField5, tableData.formatField5) &&
        Objects.equals(uploader, tableData.uploader) &&
        Objects.equals(uploaderIp, tableData.uploaderIp);
  }

  @Override
  public int hashCode() {
    return Objects.hash(repositoryId, componentId, assetId, format, namespace, componentName, componentKind, version,
        normalisedVersion, componentCreated, repositoryName, path, contentType, md5, sha1, sha256, sha512,
        formatField1, formatField2, formatField3, formatField4, formatField5, uploader, uploaderIp);
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
        .add("normalisedVersion='" + normalisedVersion + "'")
        .add("componentCreated=" + componentCreated)
        .add("repositoryName='" + repositoryName + "'")
        .add("path='" + path + "'")
        .add("contentType='" + contentType + "'")
        .add("md5='" + md5 + "'")
        .add("sha1='" + sha1 + "'")
        .add("sha256='" + sha256 + "'")
        .add("sha512='" + sha512 + "'")
        .add("formatField1='" + formatField1 + "'")
        .add("formatField2='" + formatField2 + "'")
        .add("formatField3='" + formatField3 + "'")
        .add("formatField4='" + formatField4 + "'")
        .add("formatField5='" + formatField5 + "'")
        .add("uploader='" + uploader + "'")
        .add("uploaderIp='" + uploaderIp + "'")
        .toString();
  }
}
