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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.inject.Provider;
import jakarta.validation.ValidationException;

import org.sonatype.nexus.rest.ValidationErrorsException;

import org.sonatype.nexus.common.i18n.I18N;
import org.sonatype.nexus.common.i18n.MessageBundle;
import org.sonatype.nexus.blobstore.BlobStoreDescriptor;
import org.sonatype.nexus.blobstore.BlobStoreDescriptorSupport;
import org.sonatype.nexus.blobstore.SelectOption;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaService;
import org.sonatype.nexus.blobstore.s3.internal.capability.CustomS3RegionCapability;
import org.sonatype.nexus.blobstore.s3.internal.capability.CustomS3RegionCapabilityConfiguration;
import org.sonatype.nexus.blobstore.s3.internal.capability.CustomS3RegionCapabilityDescriptor;
import org.sonatype.nexus.blobstore.s3.internal.encryption.KMSEncrypter;
import org.sonatype.nexus.blobstore.s3.internal.encryption.NoEncrypter;
import org.sonatype.nexus.blobstore.s3.internal.encryption.S3ManagedEncrypter;
import org.sonatype.nexus.capability.CapabilityReferenceFilterBuilder;
import org.sonatype.nexus.capability.CapabilityRegistry;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.validation.ssrf.AntiSsrfService;

import software.amazon.awssdk.regions.Region;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.sonatype.nexus.blobstore.s3.S3BlobStoreConfigurationHelper.BUCKET_KEY;
import static org.sonatype.nexus.blobstore.s3.S3BlobStoreConfigurationHelper.BUCKET_PREFIX_KEY;
import static org.sonatype.nexus.blobstore.s3.S3BlobStoreConfigurationHelper.CONFIG_KEY;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.ENDPOINT_KEY;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.SECRET_ACCESS_KEY_KEY;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.SESSION_TOKEN_KEY;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * A {@link BlobStoreDescriptor} for {@link S3BlobStore}.
 *
 * @since 3.6.1
 */
@AvailabilityVersion(from = "1.0")
@Component
@Qualifier(S3BlobStoreDescriptor.TYPE)
public class S3BlobStoreDescriptor
    extends BlobStoreDescriptorSupport
{
  private static final String DEFAULT_LABEL = "Default";

  public static final String TYPE = "S3";

  private final Provider<CapabilityRegistry> capabilityRegistryProvider;

  private final AntiSsrfService antiSsrfService;

  private interface Messages
      extends MessageBundle
  {
    @DefaultMessage("S3")
    String name();
  }

  private static final Messages messages = I18N.create(Messages.class);

  private final BlobStoreManager blobStoreManager;

  @Autowired
  public S3BlobStoreDescriptor(
      final BlobStoreQuotaService quotaService,
      @Lazy final BlobStoreManager blobStoreManager,
      final Provider<CapabilityRegistry> capabilityRegistryProvider,
      final AntiSsrfService antiSsrfService)
  {
    super(quotaService);
    this.blobStoreManager = checkNotNull(blobStoreManager);
    this.capabilityRegistryProvider = checkNotNull(capabilityRegistryProvider);
    this.antiSsrfService = checkNotNull(antiSsrfService);
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
    validateEndpoint(config);
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
    return initializeSelectOptions();
  }

  @Override
  public List<String> getSensitiveConfigurationFields() {
    return List.of(SECRET_ACCESS_KEY_KEY, SESSION_TOKEN_KEY);
  }

  private Map<String, List<SelectOption>> initializeSelectOptions() {
    return ImmutableMap
        .of("regions", getRegionOptions(), "encryptionTypes", getEncryptionTypes());
  }

  protected List<SelectOption> getRegionOptions() {
    if (isCustomS3RegionCapabilityEnabled()) {
      return getCustomS3RegionOptions();
    }

    return Stream
        .concat(Stream.of(new SelectOption(AmazonS3Factory.DEFAULT, DEFAULT_LABEL)), Region.regions()
            .stream()
            .map(region -> new SelectOption(region.id(), region.id())))
        .collect(ImmutableList.toImmutableList());
  }

  protected boolean isCustomS3RegionCapabilityEnabled() {
    return !capabilityRegistryProvider.get()
        .get(
            CapabilityReferenceFilterBuilder.capabilities()
                .withType(CapabilityType.capabilityType(CustomS3RegionCapabilityDescriptor.TYPE_ID))
                .enabled())
        .isEmpty();
  }

  private List<SelectOption> getCustomS3RegionOptions() {
    return capabilityRegistryProvider.get()
        .get(capabilityReference -> capabilityReference.capability() instanceof CustomS3RegionCapability)
        .stream()
        .map(capabilityReference -> capabilityReference.capabilityAs(CustomS3RegionCapability.class))
        .findFirst()
        .map(CustomS3RegionCapability::getConfig)
        .map(CustomS3RegionCapabilityConfiguration::getRegionsList)
        .orElse(Collections.emptyList());
  }

  private List<SelectOption> getEncryptionTypes() {
    return new Builder<SelectOption>().add(new SelectOption(NoEncrypter.ID, NoEncrypter.NAME))
        .add(new SelectOption(S3ManagedEncrypter.ID, S3ManagedEncrypter.NAME))
        .add(new SelectOption(KMSEncrypter.ID, KMSEncrypter.NAME))
        .build();
  }

  private String trimAndCollapseSlashes(final String prefix) {
    return Optional.ofNullable(prefix)
        .filter(StringUtils::isNotBlank)
        .map(s -> StringUtils.strip(s, "/ "))
        .map(s -> s.replaceAll("/+", "/"))
        .orElse(prefix);
  }

  private void validateOverlappingBucketWithConfiguration(
      final BlobStoreConfiguration newConfig, // NOSONAR
      final BlobStoreConfiguration existingConfig)
  {
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

  private void validateEndpoint(final BlobStoreConfiguration config) {
    String endpoint = config.attributes(CONFIG_KEY).get(ENDPOINT_KEY, String.class);
    if (StringUtils.isBlank(endpoint)) {
      return;
    }

    try {
      URI endpointUri = new URI(endpoint);
      String host = endpointUri.getHost();
      if (host != null) {
        antiSsrfService.validateHostWithoutCache(host);
      }
    }
    catch (URISyntaxException e) {
      throw new ValidationErrorsException("Invalid S3 endpoint URL format: " + endpoint);
    }
  }

  private boolean prefixesOverlap(final String prefix1, final String prefix2) {
    String prefix1WithDelimiters = ("/" + prefix1 + "/").replaceAll("//", "/");
    String prefix2WithDelimiters = ("/" + prefix2 + "/").replaceAll("//", "/");
    return prefix1WithDelimiters.startsWith(prefix2WithDelimiters) ||
        prefix2WithDelimiters.startsWith(prefix1WithDelimiters);
  }
}
