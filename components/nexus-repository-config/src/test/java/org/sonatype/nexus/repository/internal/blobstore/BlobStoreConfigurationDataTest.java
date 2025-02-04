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
package org.sonatype.nexus.repository.internal.blobstore;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import static org.sonatype.nexus.repository.internal.blobstore.BlobStoreConfigurationData.ACCESS_KEY_ID;
import static org.sonatype.nexus.repository.internal.blobstore.BlobStoreConfigurationData.SECRET_ACCESS_KEY;
import static org.sonatype.nexus.repository.internal.blobstore.BlobStoreConfigurationData.S_3;

public class BlobStoreConfigurationDataTest
    extends TestCase
{
  public static final String SOLDO_NEXUS_PRO_BLOBSTORE = "soldo-nexus-pro-blobstore";

  public static final String SOLDO_NEXUS_PRO_HA_BLOBSTORE = "soldo-nexus-pro-ha-blobstore";

  public static final String BUCKET = "bucket";

  public static final String REGION = "region";

  public static final String EU_WEST_1 = "eu-west-1";

  public static final String CRITICAL_ACCESS_KEY_ID = "criticalAccessKeyId";

  public static final String CRITICAL_SECRET_ACCESS_KEY = "criticalSecretAccessKey";

  public void testTestToString_NoCriticalInfoReturned() {

    BlobStoreConfigurationData blobStoreConfigurationData = new BlobStoreConfigurationData();
    blobStoreConfigurationData.setName(SOLDO_NEXUS_PRO_BLOBSTORE);
    blobStoreConfigurationData.setType(S_3);

    Map<String, Object> s3 = new HashMap<>();
    s3.put(BUCKET, SOLDO_NEXUS_PRO_HA_BLOBSTORE);
    s3.put(REGION, EU_WEST_1);
    s3.put(ACCESS_KEY_ID, CRITICAL_ACCESS_KEY_ID);
    s3.put(SECRET_ACCESS_KEY, CRITICAL_SECRET_ACCESS_KEY);

    Map<String, Map<String, Object>> attributes = new HashMap<>();
    attributes.put(S_3, s3);

    blobStoreConfigurationData.setAttributes(attributes);
    String result = blobStoreConfigurationData.toString();

    assertFalse(result.contains(ACCESS_KEY_ID));
    assertFalse(result.contains(SECRET_ACCESS_KEY));
  }
}
