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
package org.sonatype.nexus.blobstore.s3.internal.capability;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CustomS3RegionCapabilityTest {
  private CustomS3RegionCapability capability;

  @Before
  public void setUp() {
    capability = new CustomS3RegionCapability();
  }

  @Test
  public void testCreateConfig() {
    Map<String, String> properties = new HashMap<>();
    properties.put("regions", "us-east-1,us-west-2");

    CustomS3RegionCapabilityConfiguration config = capability.createConfig(properties);

    assertNotNull(config);
    assertEquals("us-east-1,us-west-2", config.getRegions());
  }
}
