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
package org.sonatype.nexus.blobstore.s3.internal.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.s3.internal.AmazonS3Factory;
import org.sonatype.nexus.blobstore.s3.internal.encryption.KMSEncrypter;
import org.sonatype.nexus.blobstore.s3.internal.encryption.NoEncrypter;
import org.sonatype.nexus.blobstore.s3.internal.encryption.S3ManagedEncrypter;
import org.sonatype.nexus.extdirect.DirectComponent;
import org.sonatype.nexus.extdirect.DirectComponentSupport;

import com.amazonaws.services.s3.model.Region;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.softwarementors.extjs.djn.config.annotations.DirectAction;
import com.softwarementors.extjs.djn.config.annotations.DirectMethod;
import org.apache.shiro.authz.annotation.RequiresPermissions;

/**
 * S3 {@link DirectComponent}.
 *
 * @since 3.12
 */
@Named
@Singleton
@DirectAction(action = "s3_S3")
public class S3Component
    extends DirectComponentSupport
{
  private static final String DEFAULT_LABEL = "Default";

  private static final String S3_SIGNER = "S3SignerType";

  private static final String S3_V4_SIGNER = "AWSS3V4SignerType";

  private final List<S3RegionXO> regions;

  private final List<S3SignerTypeXO> signerTypes;

  private final List<S3EncryptionTypeXO> encryptionTypes;

  public S3Component() {
    regions = new ArrayList<>();
    regions.add(new S3RegionXO().withOrder(0).withId(AmazonS3Factory.DEFAULT).withName(DEFAULT_LABEL));
    IntStream.range(0, Region.values().length)
        .mapToObj(index -> {
            Region item = Region.values()[index];
            return new S3RegionXO()
                .withOrder(index + 1)
                .withId(item.toAWSRegion().getName())
                .withName(item.toAWSRegion().getName());
        })
        .forEach(s3RegionXO -> regions.add(s3RegionXO));
    this.signerTypes = Arrays.asList(
        new S3SignerTypeXO().withOrder(0).withId(AmazonS3Factory.DEFAULT).withName(DEFAULT_LABEL),
        new S3SignerTypeXO().withOrder(1).withId(S3_SIGNER).withName(S3_SIGNER),
        new S3SignerTypeXO().withOrder(2).withId(S3_V4_SIGNER).withName(S3_V4_SIGNER)
    );

    this.encryptionTypes = Arrays.asList(
        new S3EncryptionTypeXO().withOrder(0).withId(NoEncrypter.ID).withName(NoEncrypter.NAME),
        new S3EncryptionTypeXO().withOrder(1).withId(S3ManagedEncrypter.ID).withName(S3ManagedEncrypter.NAME),
        new S3EncryptionTypeXO().withOrder(2).withId(KMSEncrypter.ID).withName(KMSEncrypter.NAME)
    );
  }

  public List<S3RegionXO> getRegions() {
    return regions;
  }

  public List<S3SignerTypeXO> getSignerTypes() {
    return signerTypes;
  }

  public List<S3EncryptionTypeXO> getEncryptionTypes() {
    return encryptionTypes;
  }

  /**
   * S3 regions
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions("nexus:settings:read")
  public List<S3RegionXO> regions() {
    return regions;
  }

  /**
   * S3 signer types
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions("nexus:settings:read")
  public List<S3SignerTypeXO> signertypes() {
    return signerTypes;
  }

  /**
   * S3 encryption types
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions("nexus:settings:read")
  public List<S3EncryptionTypeXO> encryptionTypes() {
    return encryptionTypes;
  }
}
