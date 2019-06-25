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
package org.sonatype.nexus.blobstore.s3.internal;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.ValidationException;

import org.sonatype.goodies.i18n.I18N;
import org.sonatype.goodies.i18n.MessageBundle;
import org.sonatype.nexus.blobstore.BlobStoreDescriptorSupport;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaService;
import org.sonatype.nexus.formfields.FormField;

import org.apache.commons.lang.StringUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.ACCESS_KEY_ID_KEY;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.ASSUME_ROLE_KEY;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.BUCKET_KEY;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.BUCKET_PREFIX_KEY;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.CONFIG_KEY;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.ENDPOINT_KEY;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.EXPIRATION_KEY;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.FORCE_PATH_STYLE_KEY;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.REGION_KEY;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.SECRET_ACCESS_KEY_KEY;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.SESSION_TOKEN_KEY;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.SIGNERTYPE_KEY;


/**
 * A {@link BlobStoreDescriptor} for {@link S3BlobStore}.
 *
 * @since 3.6.1
 */
@Named(S3BlobStoreDescriptor.TYPE)
public class S3BlobStoreDescriptor
    extends BlobStoreDescriptorSupport
{
  public static final String TYPE = "S3";

  private interface Messages
      extends MessageBundle
  {
    @DefaultMessage("S3")
    String name();
  }

  private static final Messages messages = I18N.create(Messages.class);

  private final BlobStoreManager blobStoreManager;

  @Inject
  public S3BlobStoreDescriptor(final BlobStoreQuotaService quotaService,
                               final BlobStoreManager blobStoreManager) {
    super(quotaService);
    this.blobStoreManager = checkNotNull(blobStoreManager);
  }

  @Override
  public String getName() {
    return messages.name();
  }

  @Override
  public List<FormField> getFormFields() {
      return Collections.emptyList();
  }

  @Override
  public String customFormName() {
    return "nx-blobstore-settings-s3";
  }

  @Override
  public void validateConfig(final BlobStoreConfiguration config) {
    super.validateConfig(config);
    for (BlobStore existingBlobStore : blobStoreManager.browse()) {
      validateOverlappingBucketWithConfiguration(config, existingBlobStore.getBlobStoreConfiguration());
    }
  }

  @Override
  public void sanitizeConfig(final BlobStoreConfiguration config) {
    String bucketPrefix = config.attributes(CONFIG_KEY).get(BUCKET_PREFIX_KEY, String.class, "");
    config.attributes(CONFIG_KEY).set(BUCKET_PREFIX_KEY, trimAndCollapseSlashes(bucketPrefix));
  }

  private String trimAndCollapseSlashes(final String prefix) {
    return Optional.ofNullable(prefix)
          .filter(StringUtils::isNotBlank)
          .map(s -> StringUtils.strip(s, "/ "))
          .map(s -> s.replaceAll("/+", "/"))
          .orElse(prefix);
  }

  private void validateOverlappingBucketWithConfiguration(final BlobStoreConfiguration newConfig, // NOSONAR
                                                          final BlobStoreConfiguration existingConfig) {
    String newName = newConfig.getName();
    String newBucket = newConfig.attributes(CONFIG_KEY).get(BUCKET_KEY, String.class, "");
    String newPrefix = newConfig.attributes(CONFIG_KEY).get(BUCKET_PREFIX_KEY, String.class, "");
    String newEndpoint = newConfig.attributes(CONFIG_KEY).get(ENDPOINT_KEY, String.class, "");

    if (!existingConfig.getName().equals(newName) && existingConfig.getType().equals(S3BlobStore.TYPE)) {
      String existingBucket = existingConfig.attributes(CONFIG_KEY).get(BUCKET_KEY, String.class, "");
      String existingPrefix = existingConfig.attributes(CONFIG_KEY).get(BUCKET_PREFIX_KEY, String.class, "");
      String existingEndpoint = existingConfig.attributes(CONFIG_KEY).get(ENDPOINT_KEY, String.class, "");
      if (newBucket.equals(existingBucket) &&
          newEndpoint.equals(existingEndpoint) &&
          prefixesOverlap(existingPrefix, newPrefix)) {
        String message = format("Blob Store '%s' is already using bucket '%s'", existingConfig.getName(),
            existingBucket);
        if (!newPrefix.isEmpty() || !existingPrefix.isEmpty()) {
          message = message + format(" with prefix '%s'", existingPrefix);
        }
        if (!newEndpoint.isEmpty() || !existingEndpoint.isEmpty()) {
          message = message + format(" on endpoint '%s'", existingEndpoint);
        }
        throw new ValidationException(message);
      }
    }
  }

  private boolean prefixesOverlap(final String prefix1, final String prefix2) {
    String prefix1WithDelimiters = ("/" + prefix1 + "/").replaceAll("//", "/");
    String prefix2WithDelimiters = ("/" + prefix2 + "/").replaceAll("//", "/");
    return
        prefix1WithDelimiters.startsWith(prefix2WithDelimiters) ||
        prefix2WithDelimiters.startsWith(prefix1WithDelimiters);
  }
}
