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
package org.sonatype.nexus.blobstore.api.metrics;

/**
 * DB Entity for db based blobstore metric storage
 */
public class BlobStoreMetricsEntity
{
  // for mybatis
  private String id;

  /*
   * Name of the blobstore these metrics belong to.
   */
  private String blobStoreName;

  /*
   * Total size (in bytes) this blobstore uses.
   */
  private long totalSize = 0L;

  /*
   * The number of blobs in this blobstore.
   */
  private long blobCount = 0L;

  /*
   * Total amount of bytes uploaded.
   *
   * -Note this is an operation metric and gets reset each push to the remote.
   */
  private long uploadBlobSize = 0L;

  /*
   * The number of successful upload requests.
   *
   * -Note this is an operation metric and gets reset each push to the remote.
   */
  private long uploadSuccessfulRequests = 0L;

  /*
   * Time spent uploading to the blobstore.
   *
   * -Note this is an operation metric and gets reset each push to the remote.
   */
  private long uploadTimeOnRequests = 0L;

  /*
   * Number of upload errors.
   *
   * -Note this is an operation metric and gets reset each push to the remote.
   */
  private long uploadErrorRequests = 0L;

  /*
   * Total amount of bytes downloaded.
   *
   * -Note this is an operation metric and gets reset each push to the remote.
   */
  private long downloadBlobSize = 0L;

  /*
   * The number of successful download requests.
   *
   * -Note this is an operation metric and gets reset each push to the remote.
   */
  private long downloadSuccessfulRequests = 0L;

  /*
   * Time spent downloading from the blobstore.
   *
   * -Note this is an operation metric and gets reset each push to the remote.
   */
  private long downloadTimeOnRequests = 0L;

  /*
   * Number of download errors.
   *
   * -Note this is an operation metric and gets reset each push to the remote.
   */
  private long downloadErrorRequests = 0L;

  public String getBlobStoreName() {
    return blobStoreName;
  }

  public BlobStoreMetricsEntity setBlobStoreName(final String blobStoreName) {
    this.blobStoreName = blobStoreName;
    return this;
  }

  public long getTotalSize() {
    return totalSize;
  }

  public BlobStoreMetricsEntity setTotalSize(final long totalSize) {
    this.totalSize = totalSize;
    return this;
  }

  public long getBlobCount() {
    return blobCount;
  }

  public BlobStoreMetricsEntity setBlobCount(final long blobCount) {
    this.blobCount = blobCount;
    return this;
  }

  public long getUploadBlobSize() {
    return uploadBlobSize;
  }

  public BlobStoreMetricsEntity setUploadBlobSize(final long uploadBlobSize) {
    this.uploadBlobSize = uploadBlobSize;
    return this;
  }

  public long getUploadSuccessfulRequests() {
    return uploadSuccessfulRequests;
  }

  public BlobStoreMetricsEntity setUploadSuccessfulRequests(final long uploadSuccessfulRequests) {
    this.uploadSuccessfulRequests = uploadSuccessfulRequests;
    return this;
  }

  public long getUploadTimeOnRequests() {
    return uploadTimeOnRequests;
  }

  public BlobStoreMetricsEntity setUploadTimeOnRequests(final long uploadTimeOnRequests) {
    this.uploadTimeOnRequests = uploadTimeOnRequests;
    return this;
  }

  public long getUploadErrorRequests() {
    return uploadErrorRequests;
  }

  public BlobStoreMetricsEntity setUploadErrorRequests(final long uploadErrorRequests) {
    this.uploadErrorRequests = uploadErrorRequests;
    return this;
  }

  public long getDownloadBlobSize() {
    return downloadBlobSize;
  }

  public BlobStoreMetricsEntity setDownloadBlobSize(final long downloadBlobSize) {
    this.downloadBlobSize = downloadBlobSize;
    return this;
  }

  public long getDownloadSuccessfulRequests() {
    return downloadSuccessfulRequests;
  }

  public BlobStoreMetricsEntity setDownloadSuccessfulRequests(final long downloadSuccessfulRequests) {
    this.downloadSuccessfulRequests = downloadSuccessfulRequests;
    return this;
  }

  public long getDownloadTimeOnRequests() {
    return downloadTimeOnRequests;
  }

  public BlobStoreMetricsEntity setDownloadTimeOnRequests(final long downloadTimeOnRequests) {
    this.downloadTimeOnRequests = downloadTimeOnRequests;
    return this;
  }

  public long getDownloadErrorRequests() {
    return downloadErrorRequests;
  }

  public BlobStoreMetricsEntity setDownloadErrorRequests(final long downloadErrorRequests) {
    this.downloadErrorRequests = downloadErrorRequests;
    return this;
  }
}
