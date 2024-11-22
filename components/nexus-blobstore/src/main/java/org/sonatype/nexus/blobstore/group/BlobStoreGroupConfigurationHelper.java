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
package org.sonatype.nexus.blobstore.group;

import java.util.List;

import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;

import static org.sonatype.nexus.blobstore.group.BlobStoreGroup.CONFIG_KEY;
import static org.sonatype.nexus.blobstore.group.BlobStoreGroup.FILL_POLICY_KEY;
import static org.sonatype.nexus.blobstore.group.BlobStoreGroup.MEMBERS_KEY;

/**
 * Helper for blob store group attributes in {@link BlobStoreConfiguration}.
 *
 * @since 3.14
 */
public class BlobStoreGroupConfigurationHelper
{

  private BlobStoreGroupConfigurationHelper() {
    // Don't instantiate
  }

  public static List<String> memberNames(final BlobStoreConfiguration configuration) {
    return configuration.attributes(CONFIG_KEY).require(MEMBERS_KEY, List.class);
  }

  public static String fillPolicyName(final BlobStoreConfiguration configuration) {
    return configuration.attributes(CONFIG_KEY).require(FILL_POLICY_KEY).toString();
  }
}
