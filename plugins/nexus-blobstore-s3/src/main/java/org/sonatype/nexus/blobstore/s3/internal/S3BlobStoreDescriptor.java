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

import java.util.Arrays;
import java.util.List;

import javax.inject.Named;

import org.sonatype.goodies.i18n.I18N;
import org.sonatype.goodies.i18n.MessageBundle;
import org.sonatype.nexus.blobstore.BlobStoreDescriptor;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.NumberTextFormField;
import org.sonatype.nexus.formfields.PasswordFormField;
import org.sonatype.nexus.formfields.StringTextFormField;

/**
 * A {@link BlobStoreDescriptor} for {@link S3BlobStore}.
 *
 * @since 3.7
 */
@Named(S3BlobStoreDescriptor.TYPE)
public class S3BlobStoreDescriptor
    implements BlobStoreDescriptor
{
  public static final String TYPE = "S3";

  private interface Messages
      extends MessageBundle
  {
    @DefaultMessage("S3 (Experimental)")
    String name();

    @DefaultMessage("Bucket")
    String bucketLabel();

    @DefaultMessage("S3 Bucket Name")
    String bucketHelp();

    @DefaultMessage("Access Key ID")
    String accessKeyIdLabel();

    @DefaultMessage("AWS Access Key ID")
    String accessKeyIdHelp();

    @DefaultMessage("Secret Access Key")
    String secretAccessKeyLabel();

    @DefaultMessage("AWS Secret Access Key")
    String secretAccessKeyHelp();

    @DefaultMessage("Session Token")
    String sessionTokenLabel();

    @DefaultMessage("STS Session Token")
    String sessionTokenHelp();

    @DefaultMessage("Assume Role ARN")
    String assumeRoleLabel();

    @DefaultMessage("Optional ARN for Role to Assume")
    String assumeRoleHelp();

    @DefaultMessage("Region")
    String regionLabel();

    @DefaultMessage("AWS Region")
    String regionHelp();

    @DefaultMessage("Endpoint URL")
    String endpointLabel();

    @DefaultMessage("AWS Endpoint URL")
    String endpointHelp();

    @DefaultMessage("Expiration Days")
    String expirationLabel();

    @DefaultMessage("How many days until deleted blobs are finally removed from the S3 bucket")
    String expirationHelp();
  }

  private static final Messages messages = I18N.create(Messages.class);

  private final FormField bucket;
  private final FormField accessKeyId;
  private final FormField secretAccessKey;
  private final FormField sessionToken;
  private final FormField assumeRole;
  private final FormField region;
  private final FormField endpoint;
  private final FormField expiration;

  public S3BlobStoreDescriptor() {
    this.bucket = new StringTextFormField(
        S3BlobStore.BUCKET_KEY,
        messages.bucketLabel(),
        messages.bucketHelp(),
        FormField.MANDATORY
    );
    this.accessKeyId = new StringTextFormField(
        S3BlobStore.ACCESS_KEY_ID_KEY,
        messages.accessKeyIdLabel(),
        messages.accessKeyIdHelp(),
        FormField.OPTIONAL
    );
    this.secretAccessKey = new PasswordFormField(
        S3BlobStore.SECRET_ACCESS_KEY_KEY,
        messages.secretAccessKeyLabel(),
        messages.secretAccessKeyHelp(),
        FormField.OPTIONAL
    );
    this.assumeRole = new StringTextFormField(
        S3BlobStore.ASSUME_ROLE_KEY,
        messages.assumeRoleLabel(),
        messages.assumeRoleHelp(),
        FormField.OPTIONAL
    );
    this.sessionToken = new StringTextFormField(
        S3BlobStore.SESSION_TOKEN_KEY,
        messages.sessionTokenLabel(),
        messages.sessionTokenHelp(),
        FormField.OPTIONAL
    );
    this.region = new StringTextFormField(
        S3BlobStore.REGION_KEY,
        messages.regionLabel(),
        messages.regionHelp(),
        FormField.OPTIONAL
    );
    this.endpoint = new StringTextFormField(
        S3BlobStore.ENDPOINT_KEY,
        messages.endpointLabel(),
        messages.endpointHelp(),
        FormField.OPTIONAL
    );
    this.expiration = new NumberTextFormField(
        S3BlobStore.EXPIRATION_KEY,
        messages.expirationLabel(),
        messages.expirationHelp(),
        FormField.OPTIONAL)
        .withInitialValue(S3BlobStore.DEFAULT_EXPIRATION_IN_DAYS)
        .withMinimumValue(1);
  }

  @Override
  public String getName() {
    return messages.name();
  }

  @Override
  public List<FormField> getFormFields() {
      return Arrays.asList(bucket, accessKeyId, secretAccessKey, sessionToken, assumeRole, region, endpoint, expiration);
  }
}
