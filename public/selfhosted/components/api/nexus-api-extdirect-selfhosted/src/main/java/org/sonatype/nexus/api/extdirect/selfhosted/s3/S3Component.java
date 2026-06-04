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
package org.sonatype.nexus.api.extdirect.selfhosted.s3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.sonatype.nexus.blobstore.s3.internal.AmazonS3Factory;
import org.sonatype.nexus.blobstore.s3.internal.encryption.KMSEncrypter;
import org.sonatype.nexus.blobstore.s3.internal.encryption.NoEncrypter;
import org.sonatype.nexus.blobstore.s3.internal.encryption.S3ManagedEncrypter;
import org.sonatype.nexus.extdirect.DirectComponent;
import org.sonatype.nexus.extdirect.DirectComponentSupport;
import org.sonatype.nexus.api.extdirect.selfhosted.s3.model.S3EncryptionTypeXO;
import org.sonatype.nexus.api.extdirect.selfhosted.s3.model.S3RegionXO;

import software.amazon.awssdk.regions.Region;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.softwarementors.extjs.djn.config.annotations.DirectAction;
import com.softwarementors.extjs.djn.config.annotations.DirectMethod;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Component;

/**
 * S3 {@link DirectComponent}.
 *
 * @since 3.12
 */
@Component
@DirectAction(action = "s3_S3")
public class S3Component
    extends DirectComponentSupport
{
  private static final String DEFAULT_LABEL = "Default";

  private final List<S3RegionXO> regions;

  private final List<S3EncryptionTypeXO> encryptionTypes;

  public S3Component() {
    regions = new ArrayList<>();
    regions.add(new S3RegionXO().withOrder(0).withId(AmazonS3Factory.DEFAULT).withName(DEFAULT_LABEL));

    // Add all AWS SDK 2.x regions
    var regionValues = Region.regions();
    int index = 0;
    for (Region region : regionValues) {
      regions.add(new S3RegionXO()
          .withOrder(index + 1)
          .withId(region.id())
          .withName(region.id()));
      index++;
    }

    this.encryptionTypes = Arrays.asList(
        new S3EncryptionTypeXO().withOrder(0).withId(NoEncrypter.ID).withName(NoEncrypter.NAME),
        new S3EncryptionTypeXO().withOrder(1).withId(S3ManagedEncrypter.ID).withName(S3ManagedEncrypter.NAME),
        new S3EncryptionTypeXO().withOrder(2).withId(KMSEncrypter.ID).withName(KMSEncrypter.NAME));
  }

  public List<S3RegionXO> getRegions() {
    return regions;
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
