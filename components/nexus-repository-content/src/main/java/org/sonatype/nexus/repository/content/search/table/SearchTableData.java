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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

import org.sonatype.nexus.common.collect.NestedAttributesMap;

import org.apache.commons.lang3.StringUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.unmodifiableCollection;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class SearchTableData
{
  //PK section
  //A PK from repository table
  private Integer repositoryId;

  //A PK from *_component tables
  private Integer componentId;

  //A component type: raw, npm, maven2, etc.
  private String format;

  //Data section
  //namespace column from *_component table
  private String namespace;

  //name column from *_component table
  private String componentName;

  private Set<String> aliasComponentNames = new HashSet<>();

  //kind column from *_component table
  private String componentKind;

  //version column from *_component table
  private String version;

  private String normalisedVersion;

  // Largest AssetBlob.created() from the component's assets
  private OffsetDateTime lastModified;

  //name column from repository table
  private String repositoryName;

  // uploaderBy property for asset blobs
  private final Set<String> uploaders = new HashSet<>();

  // uploaderByIp property for asset blobs
  private final Set<String> uploaderIps = new HashSet<>();

  //asset paths
  private final Set<String> paths = new HashSet<>();

  private final List<String> keywords = new ArrayList<>();//Need to maintain order because of ranking

  private final Set<String> md5 = new HashSet<>();

  private final Set<String> sha1 = new HashSet<>();

  private final Set<String> sha256 = new HashSet<>();

  private final Set<String> sha512 = new HashSet<>();

  private final Set<String> formatFieldValues1 = new HashSet<>();

  private final Set<String> formatFieldValues2 = new HashSet<>();

  private final Set<String> formatFieldValues3 = new HashSet<>();

  private final Set<String> formatFieldValues4 = new HashSet<>();

  private final Set<String> formatFieldValues5 = new HashSet<>();

  private final Set<String> formatFieldValues6 = new HashSet<>();

  private final Set<String> formatFieldValues7 = new HashSet<>();

  private NestedAttributesMap attributes = new NestedAttributesMap("attributes", new HashMap<>());

  private boolean prerelease;

  private Integer entityVersion;

  private final Set<String> tags = new HashSet<>();

  public SearchTableData() {
  }

  public SearchTableData(final Integer repositoryId, final String format) {
    this(repositoryId, null, format);
  }

  public SearchTableData(final Integer repositoryId, final Integer componentId, final String format) {
    this.repositoryId = repositoryId;
    this.componentId = componentId;
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

  public Collection<String> getAliasComponentNames() {
    return unmodifiableCollection(aliasComponentNames);
  }

  public void addAliasComponentName(final String aliasComponentName) {
    if (isNotBlank(aliasComponentName)) {
      this.aliasComponentNames.add(aliasComponentName);
    }
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

  public OffsetDateTime getLastModified() {
    return lastModified;
  }

  public void setLastModified(final OffsetDateTime lastModified) {
    this.lastModified = lastModified;
  }

  public String getRepositoryName() {
    return repositoryName;
  }

  public void setRepositoryName(final String repositoryName) {
    this.repositoryName = repositoryName;
  }

  public void addKeywords(final List<String> values) {
    if (values != null) {
      this.keywords.addAll(values.stream().filter(StringUtils::isNotBlank).collect(toList()));
    }
  }

  public void addKeyword(final String value) {
    if (isNotBlank(value)) {
      this.keywords.add(value);
    }
  }

  public Collection<String> getKeywords() {
    return unmodifiableCollection(keywords);
  }

  public void addMd5(final String md5) {
    if (isNotBlank(md5)) {
      this.md5.add(md5);
    }
  }

  public void addSha1(final String sha1) {
    if (isNotBlank(sha1)) {
      this.sha1.add(sha1);
    }
  }

  public void addSha256(final String sha256) {
    if (isNotBlank(sha256)) {
      this.sha256.add(sha256);
    }
  }

  public void addSha512(final String sha512) {
    if (isNotBlank(sha512)) {
      this.sha512.add(sha512);
    }
  }

  public Collection<String> getMd5() {
    return unmodifiableCollection(md5);
  }

  public Collection<String> getSha1() {
    return unmodifiableCollection(sha1);
  }

  public Collection<String> getSha256() {
    return unmodifiableCollection(sha256);
  }

  public Collection<String> getSha512() {
    return unmodifiableCollection(sha512);
  }

  public void addFormatFieldValue1(final String formatFieldValue1) {
    if (isNotBlank(formatFieldValue1)) {
      this.formatFieldValues1.add(formatFieldValue1);
    }
  }

  public Collection<String> getFormatFieldValues1() {
    return unmodifiableCollection(formatFieldValues1);
  }

  public void addFormatFieldValue2(final String formatFieldValue2) {
    if (isNotBlank(formatFieldValue2)) {
      this.formatFieldValues2.add(formatFieldValue2);
    }
  }

  public Collection<String> getFormatFieldValues2() {
    return unmodifiableCollection(formatFieldValues2);
  }

  public void addFormatFieldValue3(final String formatFieldValue3) {
    if (isNotBlank(formatFieldValue3)) {
      this.formatFieldValues3.add(formatFieldValue3);
    }
  }

  public Collection<String> getFormatFieldValues3() {
    return unmodifiableCollection(formatFieldValues3);
  }

  public void addFormatFieldValue4(final String formatFieldValues4) {
    if (isNotBlank(formatFieldValues4)) {
      this.formatFieldValues4.add(formatFieldValues4);
    }
  }

  public Collection<String> getFormatFieldValues4() {
    return unmodifiableCollection(formatFieldValues4);
  }

  public void addFormatFieldValue5(final String formatFieldValue5) {
    if (isNotBlank(formatFieldValue5)) {
      this.formatFieldValues5.add(formatFieldValue5);
    }
  }

  public Collection<String> getFormatFieldValues5() {
    return unmodifiableCollection(formatFieldValues5);
  }

  public void addFormatFieldValue6(final String formatFieldValues6) {
    if (isNotBlank(formatFieldValues6)) {
      this.formatFieldValues6.add(formatFieldValues6);
    }
  }

  public Collection<String> getFormatFieldValues6() {
    return unmodifiableCollection(formatFieldValues6);
  }

  public void addFormatFieldValue7(final String formatFieldValues7) {
    if (isNotBlank(formatFieldValues7)) {
      this.formatFieldValues7.add(formatFieldValues7);
    }
  }

  public Collection<String> getFormatFieldValues7() {
    return unmodifiableCollection(formatFieldValues7);
  }

  public NestedAttributesMap attributes() {
    return attributes;
  }

  public void setAttributes(final NestedAttributesMap attributes) {
    this.attributes = checkNotNull(attributes);
  }

  public void addUploader(final String uploader) {
    if (isNotBlank(uploader)) {
      this.uploaders.add(uploader);
    }
  }

  public Collection<String> getUploaders() {
    return unmodifiableCollection(uploaders);
  }

  public void addUploaderIp(final String uploaderIp) {
    if (isNotBlank(uploaderIp)) {
      this.uploaderIps.add(uploaderIp);
    }
  }

  public Collection<String> getUploaderIps() {
    return unmodifiableCollection(uploaderIps);
  }

  public void addPath(final String path) {
    if (isNotBlank(path)) {
      this.paths.add(path);
    }
  }

  public Collection<String> getPaths() {
    return unmodifiableCollection(paths);
  }

  public boolean isPrerelease() {
    return prerelease;
  }

  public void setPrerelease(final boolean prerelease) {
    this.prerelease = prerelease;
  }

  public void setEntityVersion(final Integer entityVersion) {
    this.entityVersion = entityVersion;
  }

  public Integer getEntityVersion() {
    return entityVersion;
  }

  public void setTags(final Collection<String> values) {
    if (!values.isEmpty()) {
      this.tags.addAll(values);
    }
  }

  public Collection<String> getTags() {
    return unmodifiableCollection(tags);
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
        Objects.equals(format, tableData.format) &&
        Objects.equals(namespace, tableData.namespace) &&
        Objects.equals(componentName, tableData.componentName) &&
        Objects.equals(aliasComponentNames, tableData.aliasComponentNames) &&
        Objects.equals(componentKind, tableData.componentKind) &&
        Objects.equals(version, tableData.version) &&
        Objects.equals(normalisedVersion, tableData.normalisedVersion) &&
        Objects.equals(lastModified, tableData.lastModified) &&
        Objects.equals(repositoryName, tableData.repositoryName) &&
        Objects.equals(prerelease, tableData.prerelease) &&
        Objects.equals(uploaders, tableData.uploaders) &&
        Objects.equals(uploaderIps, tableData.uploaderIps) &&
        Objects.equals(paths, tableData.paths) &&
        Objects.equals(keywords, tableData.keywords) &&
        Objects.equals(md5, tableData.md5) &&
        Objects.equals(sha1, tableData.sha1) &&
        Objects.equals(sha256, tableData.sha256) &&
        Objects.equals(sha512, tableData.sha512) &&
        Objects.equals(entityVersion, tableData.entityVersion) &&
        Objects.equals(formatFieldValues1, tableData.formatFieldValues1) &&
        Objects.equals(formatFieldValues2, tableData.formatFieldValues2) &&
        Objects.equals(formatFieldValues3, tableData.formatFieldValues3) &&
        Objects.equals(formatFieldValues4, tableData.formatFieldValues4) &&
        Objects.equals(formatFieldValues5, tableData.formatFieldValues5) &&
        Objects.equals(formatFieldValues6, tableData.formatFieldValues6) &&
        Objects.equals(formatFieldValues7, tableData.formatFieldValues7) &&
        Objects.equals(attributes, tableData.attributes) &&
        Objects.equals(tags, tableData.tags);
  }

  @Override
  public int hashCode() {
    return Objects.hash(repositoryId, componentId, format, namespace, componentName, aliasComponentNames, componentKind,
        version, normalisedVersion, lastModified, repositoryName,
        prerelease, uploaders, uploaderIps, paths, keywords, md5, sha1, sha256, sha512, entityVersion,
        formatFieldValues1, formatFieldValues2, formatFieldValues3, formatFieldValues4, formatFieldValues5,
        formatFieldValues6, formatFieldValues7, attributes, tags);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", SearchTableData.class.getSimpleName() + "[", "]")
        .add("repositoryId=" + repositoryId)
        .add("componentId=" + componentId)
        .add("format='" + format + "'")
        .add("namespace='" + namespace + "'")
        .add("componentName='" + componentName + "'")
        .add("aliasComponentNames='" + aliasComponentNames + "'")
        .add("componentKind='" + componentKind + "'")
        .add("version='" + version + "'")
        .add("normalisedVersion='" + normalisedVersion + "'")
        .add("lastModified=" + lastModified)
        .add("repositoryName='" + repositoryName + "'")
        .add("prerelease=" + prerelease)
        .add("uploader='" + uploaders + "'")
        .add("uploaderIp='" + uploaderIps + "'")
        .add("paths='" + paths + "'")
        .add("keywords='" + keywords + "'")
        .add("md5='" + md5 + "'")
        .add("sha1='" + sha1 + "'")
        .add("sha256='" + sha256 + "'")
        .add("sha512='" + sha512 + "'")
        .add("entityVersion='" + entityVersion + "'")
        .add("formatFieldValues1='" + formatFieldValues1 + "'")
        .add("formatFieldValues2='" + formatFieldValues2 + "'")
        .add("formatFieldValues3='" + formatFieldValues3 + "'")
        .add("formatFieldValues4='" + formatFieldValues4 + "'")
        .add("formatFieldValues5='" + formatFieldValues5 + "'")
        .add("formatFieldValues6='" + formatFieldValues6 + "'")
        .add("formatFieldValues7='" + formatFieldValues7 + "'")
        .add("attributes='" + attributes + "'")
        .add("tags='" + tags + "'")
        .toString();
  }
}
