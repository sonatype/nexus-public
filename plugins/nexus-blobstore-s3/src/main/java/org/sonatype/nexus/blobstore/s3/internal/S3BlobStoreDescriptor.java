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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.ValidationException;

import org.sonatype.goodies.i18n.I18N;
import org.sonatype.goodies.i18n.MessageBundle;
import org.sonatype.nexus.blobstore.BlobStoreDescriptorSupport;
import org.sonatype.nexus.blobstore.SelectOption;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaService;
import org.sonatype.nexus.blobstore.s3.internal.encryption.KMSEncrypter;
import org.sonatype.nexus.blobstore.s3.internal.encryption.NoEncrypter;
import org.sonatype.nexus.blobstore.s3.internal.encryption.S3ManagedEncrypter;
import org.sonatype.nexus.blobstore.s3.internal.ui.S3Component;
import org.sonatype.nexus.blobstore.s3.internal.ui.S3RegionXO;
import org.sonatype.nexus.formfields.FormField;

import com.amazonaws.services.s3.model.Region;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang.StringUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.BUCKET_KEY;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.BUCKET_PREFIX_KEY;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.CONFIG_KEY;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.ENDPOINT_KEY;


/**
 * A {@link BlobStoreDescriptor} for {@link S3BlobStore}.
 *
 * @since 3.6.1
 */
@Named(S3BlobStoreDescriptor.TYPE)
public class S3BlobStoreDescriptor
    extends BlobStoreDescriptorSupport
{
  private static final String DEFAULT_LABEL = "Default";

  private static final String S3_SIGNER = "S3SignerType";

  private static final String S3_V4_SIGNER = "AWSS3V4SignerType";

  public static final String TYPE = "S3";

  private final Map<String, List<SelectOption>> s3SelectOptions;

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
    s3SelectOptions = intializeSelectOptions();
  }

  @Override
  public String getId() {
    return "s3";
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

  @Override
  public Map<String, List<SelectOption>> getDropDownValues() {
    return s3SelectOptions;
  }

  private Map<String, List<SelectOption>> intializeSelectOptions() {
    return ImmutableMap
        .of("regions", getRegionOptions(), "encryptionTypes", getEncryptionTypes(), "signerTypes", getSignerTypes());
  }

  private List<SelectOption> getRegionOptions() {
    return Stream
        .concat(Stream.of(new SelectOption(AmazonS3Factory.DEFAULT, DEFAULT_LABEL)), Arrays.stream(Region.values())
            .map(region -> new SelectOption(region.toAWSRegion().getName(), region.toAWSRegion().getName())))
        .collect(ImmutableList.toImmutableList());
  }

  private List<SelectOption> getSignerTypes() {
    return new ImmutableList.Builder<SelectOption>().add(new SelectOption(AmazonS3Factory.DEFAULT, DEFAULT_LABEL))
        .add(new SelectOption(S3_SIGNER, S3_SIGNER)).add(new SelectOption(S3_V4_SIGNER, S3_V4_SIGNER)).build();
  }

  private List<SelectOption> getEncryptionTypes() {
    return new ImmutableList.Builder<SelectOption>().add(new SelectOption(NoEncrypter.ID, NoEncrypter.NAME))
        .add(new SelectOption(S3ManagedEncrypter.ID, S3ManagedEncrypter.NAME))
        .add(new SelectOption(KMSEncrypter.ID, KMSEncrypter.NAME)).build();
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
