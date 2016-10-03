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
package org.sonatype.nexus.internal.node;

import java.io.File;
import java.security.cert.Certificate;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.crypto.internal.CryptoHelperImpl;
import org.sonatype.nexus.ssl.KeyStoreManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests for local {@link NodeAccess}.
 */
@SuppressWarnings("HardCodedStringLiteral")
public class LocalNodeAccessTest
    extends TestSupport
{
  private KeyStoreManager keyStoreManager;

  private NodeAccess nodeAccess;

  @Before
  public void setUp() throws Exception {
    File dir = util.createTempDir("keystores");
    KeyStoreManagerConfigurationImpl config = new KeyStoreManagerConfigurationImpl();
    // use lower strength for faster test execution
    config.setKeyAlgorithmSize(512);
    keyStoreManager = new KeyStoreManagerImpl(new CryptoHelperImpl(), new KeyStoreStorageManagerImpl(dir), config);
    keyStoreManager.generateAndStoreKeyPair("a", "b", "c", "d", "e", "f");

    nodeAccess = new LocalNodeAccess(keyStoreManager);
    nodeAccess.start();
  }

  @After
  public void tearDown() throws Exception {
    if (nodeAccess != null) {
      nodeAccess.stop();
    }
  }

  @Test
  public void idEqualToIdentityCertificate() throws Exception {
    Certificate cert = keyStoreManager.getCertificate();
    assertThat(nodeAccess.getId(), equalTo(NodeIdEncoding.nodeIdForCertificate(cert)));
  }
}
