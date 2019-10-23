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
package org.sonatype.nexus.repository.cocoapods.internal.pod.git;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 3.19
 */
public class GitArtifactInfo
{
  private String host;

  private String vendor;

  private String repository;

  private String ref;

  public GitArtifactInfo(final String host, final String vendor, final String repository, @Nullable final String ref) {
    this.host = checkNotNull(host);
    this.vendor = checkNotNull(vendor);
    this.repository = checkNotNull(repository);
    this.ref = ref;
  }

  public String getHost() {
    return host;
  }

  public String getVendor() {
    return vendor;
  }

  public String getRepository() {
    return repository;
  }

  public String getRef() {
    return ref;
  }
}
