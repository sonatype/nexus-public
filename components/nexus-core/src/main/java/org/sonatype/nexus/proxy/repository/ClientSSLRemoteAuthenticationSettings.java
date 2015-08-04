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
package org.sonatype.nexus.proxy.repository;

import java.io.File;

public class ClientSSLRemoteAuthenticationSettings
    implements RemoteAuthenticationSettings
{
  private final File trustStore;

  private final String trustStorePassword;

  private final File keyStore;

  private final String keyStorePassword;

  public ClientSSLRemoteAuthenticationSettings(File trustStore, String trustStorePassword, File keyStore,
                                               String keyStorePassword)
  {
    this.trustStore = trustStore;

    this.trustStorePassword = trustStorePassword;

    this.keyStore = keyStore;

    this.keyStorePassword = keyStorePassword;
  }

  public File getTrustStore() {
    return trustStore;
  }

  public String getTrustStorePassword() {
    return trustStorePassword;
  }

  public File getKeyStore() {
    return keyStore;
  }

  public String getKeyStorePassword() {
    return keyStorePassword;
  }
}
