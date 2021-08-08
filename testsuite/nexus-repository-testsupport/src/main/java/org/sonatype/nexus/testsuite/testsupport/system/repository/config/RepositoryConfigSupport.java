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
package org.sonatype.nexus.testsuite.testsupport.system.repository.config;

import static org.sonatype.nexus.blobstore.api.BlobStoreManager.DEFAULT_BLOBSTORE_NAME;

public abstract class RepositoryConfigSupport<THIS>
    implements RepositoryConfig<THIS>
{
  private String name;

  private String blobstore = DEFAULT_BLOBSTORE_NAME;

  private Boolean online = true;

  private Boolean strictContentTypeValidation = true;

  @Override
  public THIS withName(final String name) {
    this.name = name;
    return toTHIS();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public THIS withBlobstore(final String blobstore) {
    this.blobstore = blobstore;
    return toTHIS();
  }

  @Override
  public String getBlobstore() {
    return blobstore;
  }

  @Override
  public THIS withOnline(final Boolean online) {
    this.online = online;
    return toTHIS();
  }

  @Override
  public Boolean isOnline() {
    return online;
  }

  @Override
  public THIS withStrictContentTypeValidation(final Boolean strictContentTypeValidation) {
    this.strictContentTypeValidation = strictContentTypeValidation;
    return toTHIS();
  }

  @Override
  public Boolean isStrictContentTypeValidation() {
    return strictContentTypeValidation;
  }

  protected THIS toTHIS() {
    return (THIS) this;
  }
}
