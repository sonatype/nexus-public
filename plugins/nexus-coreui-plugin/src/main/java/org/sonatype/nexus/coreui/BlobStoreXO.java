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
package org.sonatype.nexus.coreui;

import java.util.Map;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

import org.sonatype.nexus.validation.group.Create;

import com.fasterxml.jackson.annotation.JsonSetter;

import static org.sonatype.nexus.blobstore.BlobStoreSupport.MAX_NAME_LENGTH;
import static org.sonatype.nexus.blobstore.BlobStoreSupport.MIN_NAME_LENGTH;

/**
 * @since 3.0
 */
public class BlobStoreXO
{
  @NotEmpty
  @UniqueBlobStoreName(groups = Create.class)
  @Size(min = MIN_NAME_LENGTH, max = MAX_NAME_LENGTH)
  private String name;

  @NotEmpty
  private String type;

  private boolean isQuotaEnabled;

  private String quotaType;

  @Min(0L)
  private Long quotaLimit;

  @NotEmpty
  private Map<String, Map<String, Object>> attributes;

  @Min(0L)
  private long blobCount;

  @Min(0L)
  private long totalSize;

  @Min(0L)
  private long availableSpace;

  @Min(0L)
  private long repositoryUseCount;

  private boolean unlimited;

  /**
   * @since 3.19
   */
  private boolean unavailable;

  @Min(0L)
  private long blobStoreUseCount;

  private boolean inUse;

  private boolean convertable;

  /**
   * @since 3.29
   */
  private int taskUseCount;

  /**
   * the name of the group to which this blob store belongs, or null if not in a group
   * 
   * @since 3.15
   */
  private String groupName;

  public Map<String, Map<String, Object>> getAttributes() {
    return attributes;
  }

  public long getAvailableSpace() {
    return availableSpace;
  }

  public long getBlobCount() {
    return blobCount;
  }

  public long getBlobStoreUseCount() {
    return blobStoreUseCount;
  }

  public String getGroupName() {
    return groupName;
  }

  public boolean isQuotaEnabled() {
    return isQuotaEnabled;
  }

  public String getName() {
    return name;
  }

  public Long getQuotaLimit() {
    return quotaLimit;
  }

  public String getQuotaType() {
    return quotaType;
  }

  public long getRepositoryUseCount() {
    return repositoryUseCount;
  }

  public int getTaskUseCount() {
    return taskUseCount;
  }

  public long getTotalSize() {
    return totalSize;
  }

  public String getType() {
    return type;
  }

  public boolean isConvertable() {
    return convertable;
  }

  public boolean isInUse() {
    return inUse;
  }

  public boolean isUnavailable() {
    return unavailable;
  }

  public boolean isUnlimited() {
    return unlimited;
  }

  @JsonSetter("attributes")
  public BlobStoreXO withAttributes(final Map<String, Map<String, Object>> attributes) {
    this.attributes = attributes;
    return this;
  }

  @JsonSetter("availableSpace")
  public BlobStoreXO withAvailableSpace(final long availableSpace) {
    this.availableSpace = availableSpace;
    return this;
  }

  @JsonSetter("blobCount")
  public BlobStoreXO withBlobCount(final long blobCount) {
    this.blobCount = blobCount;
    return this;
  }

  @JsonSetter("blobStoreUseCount")
  public BlobStoreXO withBlobStoreUseCount(final long blobStoreUseCount) {
    this.blobStoreUseCount = blobStoreUseCount;
    return this;
  }

  @JsonSetter("convertable")
  public BlobStoreXO withConvertable(final boolean convertable) {
    this.convertable = convertable;
    return this;
  }

  @JsonSetter("groupName")
  public BlobStoreXO withGroupName(final String groupName) {
    this.groupName = groupName;
    return this;
  }

  @JsonSetter("inUse")
  public BlobStoreXO withInUse(final boolean inUse) {
    this.inUse = inUse;
    return this;
  }

  @JsonSetter("isQuotaEnabled")
  public BlobStoreXO withIsQuotaEnabled(final boolean isQuotaEnabled) {
    this.isQuotaEnabled = isQuotaEnabled;
    return this;
  }

  @JsonSetter("isQuotaEnabled")
  public BlobStoreXO withIsQuotaEnabled(final String isQuotaEnabled) {
    this.isQuotaEnabled = isQuotaEnabled != null && ("true".equalsIgnoreCase(isQuotaEnabled)
        || "on".equalsIgnoreCase(isQuotaEnabled) || "1".equalsIgnoreCase(isQuotaEnabled));
    return this;
  }

  @JsonSetter("name")
  public BlobStoreXO withName(final String name) {
    this.name = name;
    return this;
  }

  @JsonSetter("quotaLimit")
  public BlobStoreXO withQuotaLimit(final Long quotaLimit) {
    this.quotaLimit = quotaLimit;
    return this;
  }

  @JsonSetter("quotaType")
  public BlobStoreXO withQuotaType(final String quotaType) {
    this.quotaType = quotaType;
    return this;
  }

  @JsonSetter("repositoryUseCount")
  public BlobStoreXO withRepositoryUseCount(final long repositoryUseCount) {
    this.repositoryUseCount = repositoryUseCount;
    return this;
  }

  @JsonSetter("taskUseCount")
  public BlobStoreXO withTaskUseCount(final int taskUseCount) {
    this.taskUseCount = taskUseCount;
    return this;
  }

  @JsonSetter("totalSize")
  public BlobStoreXO withTotalSize(final long totalSize) {
    this.totalSize = totalSize;
    return this;
  }

  @JsonSetter("type")
  public BlobStoreXO withType(final String type) {
    this.type = type;
    return this;
  }

  @JsonSetter("unavailable")
  public BlobStoreXO withUnavailable(final boolean unavailable) {
    this.unavailable = unavailable;
    return this;
  }

  @JsonSetter("unlimited")
  public BlobStoreXO withUnlimited(final boolean unlimited) {
    this.unlimited = unlimited;
    return this;
  }
}
