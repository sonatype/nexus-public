package org.sonatype.nexus.coreui.internal.blobstore;

import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics;

import static com.google.common.base.Preconditions.checkNotNull;

public class InternalBlobStoreApiResponse
{
  private final String name;

  private final String type;

  private final boolean unavailable;

  private final long blobCount;

  private final long totalSizeInBytes;

  private final long availableSpaceInBytes;

  private final boolean unlimited;

  public InternalBlobStoreApiResponse(final BlobStore blobStore) {
    BlobStoreConfiguration configuration = blobStore.getBlobStoreConfiguration();

    if (blobStore.isStarted()) {
      BlobStoreMetrics metrics = blobStore.getMetrics();
      unavailable = metrics.isUnavailable();
      blobCount = metrics.getBlobCount();
      totalSizeInBytes = metrics.getTotalSize();
      availableSpaceInBytes = metrics.getAvailableSpace();
      unlimited = metrics.isUnlimited();
    }
    else {
      unavailable = true;
      blobCount = 0;
      totalSizeInBytes = 0;
      availableSpaceInBytes = 0;
      unlimited = false;
    }

    name = checkNotNull(configuration.getName());
    type = checkNotNull(configuration.getType());
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public boolean isUnavailable() {
    return unavailable;
  }

  public long getBlobCount() {
    return blobCount;
  }

  public long getTotalSizeInBytes() {
    return totalSizeInBytes;
  }

  public long getAvailableSpaceInBytes() {
    return availableSpaceInBytes;
  }

  public boolean isUnlimited() {
    return unlimited;
  }
}
