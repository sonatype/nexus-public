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
package org.sonatype.nexus.repository.rest.api;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.search.AssetSearchResult;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang.StringUtils;

/**
 * Asset transfer object for REST APIs.
 **/
public class AssetXO
{
  private String downloadUrl;

  private String path;

  private String id;

  private String repository;

  private String format;

  private Map<String, String> checksum;

  private String contentType;

  private Date lastModified;

  private Date lastDownloaded;

  private String uploader;

  private String uploaderIp;

  private Long fileSize;

  private Date blobCreated;

  private String blobStoreName;

  @JsonIgnore
  private Map<String, Object> attributes;

  public AssetXO() {
    // empty constructor
  }

  public String getDownloadUrl() {
    return downloadUrl;
  }

  public void setDownloadUrl(final String downloadUrl) {
    this.downloadUrl = downloadUrl;
  }

  public String getPath() {
    return path;
  }

  public void setPath(final String path) {
    this.path = path;
  }

  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public String getRepository() {
    return repository;
  }

  public void setRepository(final String repository) {
    this.repository = repository;
  }

  public String getFormat() {
    return format;
  }

  public void setFormat(final String format) {
    this.format = format;
  }

  public Map<String, String> getChecksum() {
    return checksum;
  }

  public void setChecksum(final Map<String, String> checksum) {
    this.checksum = checksum;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(final String contentType) {
    this.contentType = contentType;
  }

  public Date getLastModified() {
    return lastModified;
  }

  public void setLastModified(final Date lastModified) {
    this.lastModified = lastModified;
  }

  public Date getLastDownloaded() {
    return lastDownloaded;
  }

  public void setLastDownloaded(final Date lastDownloaded) {
    this.lastDownloaded = lastDownloaded;
  }

  public String getUploader() {
    return uploader;
  }

  public void setUploader(final String uploader) {
    this.uploader = uploader;
  }

  public String getUploaderIp() {
    return uploaderIp;
  }

  public void setUploaderIp(final String uploaderIp) {
    this.uploaderIp = uploaderIp;
  }

  public Long getFileSize() {
    return fileSize;
  }

  public void setFileSize(final Long fileSize) {
    this.fileSize = fileSize;
  }

  public Date getBlobCreated() {
    return blobCreated;
  }

  public void setBlobCreated(final Date blobCreated) {
    this.blobCreated = blobCreated;
  }

  public String getBlobStoreName() {
    return blobStoreName;
  }

  public void setBlobStoreName(final String blobStoreName) {
    this.blobStoreName = blobStoreName;
  }

  public void setAttributes(final Map<String, Object> attributes) {
    this.attributes = attributes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AssetXO assetXO = (AssetXO) o;
    return Objects.equals(id, assetXO.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  public static AssetXO from(
      AssetSearchResult asset,
      Repository repository,
      @Nullable Map<String, AssetXODescriptor> assetDescriptors)
  {
    return builder()
        .path(asset.getPath())
        .downloadUrl(repository.getUrl() + '/' + StringUtils.removeStart(asset.getPath(), "/"))
        .id(new RepositoryItemIDXO(repository.getName(), asset.getId()).getValue())
        .repository(repository.getName())
        .checksum(asset.getChecksum())
        .format(asset.getFormat())
        .contentType(asset.getContentType())
        .attributes(getExpandedAttributes(asset.getAttributes(), asset.getFormat(), assetDescriptors))
        .lastModified(asset.getLastModified())
        .lastDownloaded(asset.getLastDownloaded())
        .fileSize(asset.getFileSize())
        .blobCreated(asset.getBlobCreated())
        .uploader(asset.getUploader())
        .uploaderIp(asset.getUploaderIp())
        .build();
  }

  public static AssetXO fromElasticSearchMap(
      Map<String, Object> map,
      Repository repository,
      @Nullable Map<String, AssetXODescriptor> assetDescriptors)
  {
    String path = (String) map.get("name");
    String id = (String) map.get("id");
    Map<String, Object> attributes = (Map<String, Object>) map.getOrDefault("attributes", Map.of());
    Map<String, String> checksum = (Map<String, String>) attributes.get("checksum");
    String format = repository.getFormat().getValue();
    String contentType = (String) map.get("contentType");

    return new AssetXOBuilder()
        .path(path)
        .downloadUrl(repository.getUrl() + '/' + path)
        .id(new RepositoryItemIDXO(repository.getName(), id).getValue())
        .repository(repository.getName())
        .checksum(checksum)
        .format(format)
        .contentType(contentType)
        .attributes(getExpandedAttributes(attributes, format, assetDescriptors))
        .lastModified(calculateLastModified(attributes))
        .build();
  }

  @VisibleForTesting
  static Map<String, Object> getExpandedAttributes(
      Map<String, Object> attributes,
      String format,
      @Nullable Map<String, AssetXODescriptor> assetDescriptors)
  {
    Set<String> exposedAttributeKeys = Optional.ofNullable(assetDescriptors)
        .map(ad -> ad.get(format))
        .map(AssetXODescriptor::listExposedAttributeKeys)
        .orElse(Set.of());

    Map<String, Object> formatAttributes = (Map<String, Object>) attributes.get(format);
    Map<String, Object> exposedAttributes = new HashMap<>();
    if (formatAttributes != null) {
      exposedAttributes.putAll(formatAttributes.entrySet()
          .stream()
          .filter(e -> exposedAttributeKeys.contains(e.getKey()))
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    return Map.of(format, exposedAttributes);
  }

  private static Date calculateLastModified(Map<String, Object> attributes) {
    String lastModifiedString =
        (String) ((Map<String, Object>) attributes.getOrDefault("content", Map.of())).getOrDefault("last_modified",
            null);
    Date lastModified = null;
    if (lastModifiedString != null) {
      try {
        lastModified = new Date(Long.parseLong(lastModifiedString.trim()));
      }
      catch (Exception ignored) {
        // Nothing we can do here for invalid data. It shouldn't happen but date parsing will blow out the results.
      }
    }
    return lastModified;
  }

  public static AssetXOBuilder builder() {
    return new AssetXOBuilder();
  }

  @JsonAnyGetter
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  @Override
  public String toString() {
    return "AssetXO{" +
        "downloadUrl='" + downloadUrl + '\'' +
        ", path='" + path + '\'' +
        ", id='" + id + '\'' +
        ", repository='" + repository + '\'' +
        ", format='" + format + '\'' +
        ", checksum=" + checksum +
        ", contentType='" + contentType + '\'' +
        ", lastModified=" + lastModified +
        ", lastDownloaded=" + lastDownloaded +
        ", uploader='" + uploader + '\'' +
        ", uploaderIp='" + uploaderIp + '\'' +
        ", fileSize=" + fileSize +
        ", blobCreated=" + blobCreated +
        ", blobStoreName='" + blobStoreName + '\'' +
        ", attributes=" + attributes +
        '}';
  }

  // Builder class for AssetXO
  public static class AssetXOBuilder
  {
    private String downloadUrl;

    private String path;

    private String id;

    private String repository;

    private String format;

    private Map<String, String> checksum;

    private String contentType;

    private Date lastModified;

    private Date lastDownloaded;

    private String uploader;

    private String uploaderIp;

    private Long fileSize;

    private Date blobCreated;

    private String blobStoreName;

    private Map<String, Object> attributes;

    public AssetXOBuilder downloadUrl(String downloadUrl) {
      this.downloadUrl = downloadUrl;
      return this;
    }

    public AssetXOBuilder path(String path) {
      this.path = path;
      return this;
    }

    public AssetXOBuilder id(String id) {
      this.id = id;
      return this;
    }

    public AssetXOBuilder repository(String repository) {
      this.repository = repository;
      return this;
    }

    public AssetXOBuilder format(String format) {
      this.format = format;
      return this;
    }

    public AssetXOBuilder checksum(Map<String, String> checksum) {
      this.checksum = checksum;
      return this;
    }

    public AssetXOBuilder contentType(String contentType) {
      this.contentType = contentType;
      return this;
    }

    public AssetXOBuilder lastModified(Date lastModified) {
      this.lastModified = lastModified;
      return this;
    }

    public AssetXOBuilder lastDownloaded(Date lastDownloaded) {
      this.lastDownloaded = lastDownloaded;
      return this;
    }

    public AssetXOBuilder uploader(String uploader) {
      this.uploader = uploader;
      return this;
    }

    public AssetXOBuilder uploaderIp(String uploaderIp) {
      this.uploaderIp = uploaderIp;
      return this;
    }

    public AssetXOBuilder fileSize(Long fileSize) {
      this.fileSize = fileSize;
      return this;
    }

    public AssetXOBuilder blobCreated(Date blobCreated) {
      this.blobCreated = blobCreated;
      return this;
    }

    public AssetXOBuilder blobStoreName(String blobStoreName) {
      this.blobStoreName = blobStoreName;
      return this;
    }

    public AssetXOBuilder attributes(Map<String, Object> attributes) {
      this.attributes = attributes;
      return this;
    }

    public AssetXO build() {
      AssetXO assetXO = new AssetXO();
      assetXO.setDownloadUrl(this.downloadUrl);
      assetXO.setPath(this.path);
      assetXO.setId(this.id);
      assetXO.setRepository(this.repository);
      assetXO.setFormat(this.format);
      assetXO.setChecksum(this.checksum);
      assetXO.setContentType(this.contentType);
      assetXO.setLastModified(this.lastModified);
      assetXO.setLastDownloaded(this.lastDownloaded);
      assetXO.setUploader(this.uploader);
      assetXO.setUploaderIp(this.uploaderIp);
      assetXO.setFileSize(this.fileSize);
      assetXO.setBlobCreated(this.blobCreated);
      assetXO.setBlobStoreName(this.blobStoreName);
      assetXO.setAttributes(this.attributes);
      return assetXO;
    }
  }
}
