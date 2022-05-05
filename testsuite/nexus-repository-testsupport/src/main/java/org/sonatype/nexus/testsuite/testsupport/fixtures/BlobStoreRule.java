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
package org.sonatype.nexus.testsuite.testsupport.fixtures;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.file.FileBlobStore;
import org.sonatype.nexus.blobstore.group.BlobStoreGroup;
import org.sonatype.nexus.blobstore.group.internal.WriteToFirstMemberFillPolicy;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.io.DirectoryHelper;

import com.google.common.collect.Streams;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Boolean.TRUE;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.sonatype.nexus.blobstore.api.BlobStoreManager.DEFAULT_BLOBSTORE_NAME;
import static org.sonatype.nexus.blobstore.file.FileBlobStore.PATH_KEY;

/**
 * @since 3.20
 */
@Named
@Singleton
public class BlobStoreRule
    extends ExternalResource
{
  private static final Logger log = LoggerFactory.getLogger(BlobStoreRule.class);

  protected final Provider<BlobStoreManager> blobStoreManagerProvider;

  protected final List<String> blobStoreNames = new ArrayList<>();

  protected final List<String> blobStoreGroupNames = new ArrayList<>();

  private static final String S3_TYPE = "S3";

  private static final String S3_SERVICE_ENDPOINT = System.getProperty("mock.s3.service.endpoint");

  public BlobStoreRule(final Provider<BlobStoreManager> blobStoreManagerProvider) {
    this.blobStoreManagerProvider = blobStoreManagerProvider;
  }

  @Inject
  public BlobStoreRule(final BlobStoreManager blobStoreManager) {
    this.blobStoreManagerProvider = () -> blobStoreManager;
  }

  public BlobStore createFile(final String name) throws Exception {
    BlobStoreConfiguration config = blobStoreManagerProvider.get().newConfiguration();
    config.setName(name);
    config.setType(FileBlobStore.TYPE);
    config.attributes(FileBlobStore.CONFIG_KEY).set(PATH_KEY, name);
    BlobStore blobStore = blobStoreManagerProvider.get().create(config);
    blobStoreNames.add(name);
    return blobStore;
  }

  public BlobStore createFile(final String name, String path) throws Exception {
    BlobStoreConfiguration config = blobStoreManagerProvider.get().newConfiguration();
    config.setName(name);
    config.setType(FileBlobStore.TYPE);
    config.attributes(FileBlobStore.CONFIG_KEY).set(PATH_KEY, path);
    BlobStore blobStore = blobStoreManagerProvider.get().create(config);
    blobStoreNames.add(name);
    return blobStore;
  }

  public BlobStore createGroup(final String name, final String... members) throws Exception {
    BlobStoreConfiguration config = blobStoreManagerProvider.get().newConfiguration();
    config.setName(name);
    config.setType(BlobStoreGroup.TYPE);
    config.attributes(BlobStoreGroup.CONFIG_KEY).set(BlobStoreGroup.MEMBERS_KEY, asList(members));
    config.attributes(BlobStoreGroup.CONFIG_KEY).set(BlobStoreGroup.FILL_POLICY_KEY, WriteToFirstMemberFillPolicy.TYPE);
    BlobStore blobStore = blobStoreManagerProvider.get().create(config);
    blobStoreGroupNames.add(name);
    return blobStore;
  }

  public BlobStore createS3(final String blobStoreName, final String bucketName, final String prefix)
      throws Exception
  {
    BlobStoreConfiguration configuration = blobStoreManagerProvider.get().newConfiguration();
    configuration.setType(S3_TYPE);
    configuration.setName(blobStoreName);
    final NestedAttributesMap bucketAttributes = configuration.attributes(S3_TYPE.toLowerCase());
    bucketAttributes.set("region", "us-east-1");
    bucketAttributes.set("bucket", bucketName);
    bucketAttributes.set("prefix", prefix);
    bucketAttributes.set("expiration", 5);
    bucketAttributes.set("endpoint", S3_SERVICE_ENDPOINT);
    bucketAttributes.set("forcePathStyle".toLowerCase(), TRUE.toString());
    return blobStoreManagerProvider.get().create(configuration);
  }

  public BlobStore get(final String name) {
    return blobStoreManagerProvider.get().get(name);
  }

  /**
   * Delete all blobstores except the default ones
   */
  public void deleteAllBlobstoresExceptDefault() {
    Streams.stream(blobStoreManagerProvider.get().browse()).forEach(blobStore -> {
      final String name = blobStore.getBlobStoreConfiguration().getName();
      if (!name.equals(DEFAULT_BLOBSTORE_NAME)) {
        try {
          blobStoreManagerProvider.get().forceDelete(name);
        }
        catch (Exception e) {
          log.error("Failed to remove blobstore {}");
        }
      }
    });

  }

  @Override
  public void after() {
    blobStoreGroupNames.forEach(blobStoreGroupName -> {
      try {
        blobStoreManagerProvider.get().delete(blobStoreGroupName);
      }
      catch (Exception e) {
        log.error("Failed to remove blobstore group {}", blobStoreGroupName, e);
      }
    });
    blobStoreNames.forEach(blobStoreName -> {
      try {
        BlobStore blobStore = blobStoreManagerProvider.get().get(blobStoreName);
        if (blobStore == null) {
          log.warn("Blobstore {} not found, will not delete", blobStoreName);
        }
        else {
          cleanBlobstoreContent(blobStore);
          blobStoreManagerProvider.get().delete(blobStoreName);
        }
      }
      catch (Exception e) {
        log.error("Failed to remove blobstore {}", blobStoreName, e);
      }
    });
    blobStoreGroupNames.clear();
    blobStoreNames.clear();
  }

  public void cleanBlobstore(final String name) {
    BlobStore blobStore = blobStoreManagerProvider.get().get(name);

    if (blobStore == null) {
      log.error("Unable to remove blobstore {}, not found.", name);
    }
    else {
      try {
        log.info("Cleaning blobstore {} content", name);
        cleanBlobstoreContent(blobStore);
        blobStore.flushMetrics();
        log.info("Finished cleaning blobstore content");
      }
      catch (IOException e) {
        log.error("Failed to clean blobstore {}.", name, e);
      }
    }
  }

  private void cleanBlobstoreContent(final BlobStore blobstore) {
    try {
      log.info("Deleting all Blobids from blobstore {}", blobstore.getBlobStoreConfiguration().getName());
      blobstore.getBlobIdStream().filter(Objects::nonNull).forEach(blobId -> {
        try {
          blobstore.deleteHard(blobId);
        }
        catch (UncheckedIOException e) {
          if (e.getCause() instanceof FileNotFoundException) {
            log.trace("Attempt to delete file that doesn't exist, just ignore and move on.", e);
          }
          else {
            throw e;
          }
        }
      });

      //just in case, dump anything else
      if (blobstore instanceof FileBlobStore) {
        FileBlobStore fileBlobStore = (FileBlobStore) blobstore;
        DirectoryHelper.emptyIfExists(fileBlobStore.getContentDir());
      }
      log.info("Completed deleting all Blobids from blobstore {}", blobstore.getBlobStoreConfiguration().getName());
    }
    catch (Exception e) {
      log.error("Failed to remove content from blobstore {}.", blobstore.getBlobStoreConfiguration().getName(), e);
    }
  }
}
