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
package org.sonatype.nexus.security.internal.uploadermetadata;

import org.sonatype.nexus.security.config.MemorySecurityConfiguration;
import org.sonatype.nexus.security.config.SecurityConfiguration;
import org.sonatype.nexus.security.config.SecurityContributorSupport;

import org.springframework.stereotype.Component;

@Component
public class UploaderMetadataSecurityContributor
    extends SecurityContributorSupport
{
  public static final String UPLOADER_METADATA_DOMAIN = "uploader-metadata";

  public static final String UPLOADER_METADATA_READ_PRIV_ID = "nx-uploader-metadata-read";

  public static final String UPLOADER_METADATA_READ_PERMISSION = "nexus:" + UPLOADER_METADATA_DOMAIN + ":read";

  @Override
  public SecurityConfiguration getContribution() {
    MemorySecurityConfiguration config = new MemorySecurityConfiguration();
    config.addPrivilege(
        createApplicationPrivilege(
            UPLOADER_METADATA_READ_PRIV_ID,
            READ_DESCRIPTION_BASE + "Uploader Metadata",
            UPLOADER_METADATA_DOMAIN,
            ACTION_READ));
    return config;
  }
}
