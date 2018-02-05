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
package org.sonatype.nexus.ssl.internal;

import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.X509KeyManager;

/**
 * A X509KeyManager that will can be updated.
 *
 * The default implementation is not updated when changes made to a keystore.
 *
 * @see ReloadableX509TrustManager
 * @since 3.0
 */
public class ReloadableX509KeyManager
    implements X509KeyManager
{
  private X509KeyManager delegateKeyManager;

  /**
   * Creates the initial ReloadableX509KeyManager which wraps an existing X509KeyManager.
   *
   * @param delegateKeyManager the X509KeyManager to delegate calls to.
   */
  private ReloadableX509KeyManager(final X509KeyManager delegateKeyManager) {
    this.setDelegateKeyManager(delegateKeyManager);
  }

  @Override
  public String[] getClientAliases(final String s, final Principal[] principals) {
    return delegateKeyManager.getClientAliases(s, principals);
  }

  @Override
  public String chooseClientAlias(final String[] strings, final Principal[] principals, final Socket socket) {
    return delegateKeyManager.chooseClientAlias(strings, principals, socket);
  }

  @Override
  public String[] getServerAliases(final String s, final Principal[] principals) {
    return delegateKeyManager.getServerAliases(s, principals);
  }

  @Override
  public String chooseServerAlias(final String s, final Principal[] principals, final Socket socket) {
    return delegateKeyManager.chooseServerAlias(s, principals, socket);
  }

  @Override
  public X509Certificate[] getCertificateChain(String s) {
    return delegateKeyManager.getCertificateChain(s);
  }

  @Override
  public PrivateKey getPrivateKey(String s) {
    return delegateKeyManager.getPrivateKey(s);
  }

  /**
   * Sets the X509KeyManager which will be used to delegate calls to.
   *
   * @param delegateKeyManager the X509KeyManager which will be used to delegate calls to.
   */
  private void setDelegateKeyManager(final X509KeyManager delegateKeyManager) {
    this.delegateKeyManager = delegateKeyManager;
  }

  /**
   * Finds and replaces the X509KeyManager with a ReloadableX509KeyManager.  If there is more then one, only the first
   * one will be replaced.
   *
   * @param reloadableX509KeyManager an existing ReloadableX509KeyManager, or null if one does not exist.
   * @param keyManagers              an array of KeyManagers that is expected to contain a X509KeyManager.
   * @return a newly create ReloadableX509KeyManager
   * @throws NoSuchAlgorithmException
   *                               thrown if a X509KeyManager cannot be found in the array.
   * @throws IllegalStateException thrown if a ReloadableX509KeyManager is found in the array.
   */
  public static ReloadableX509KeyManager replaceX509KeyManager(ReloadableX509KeyManager reloadableX509KeyManager,
                                                               final KeyManager[] keyManagers)
      throws NoSuchAlgorithmException
  {
    for (int ii = 0; ii < keyManagers.length; ii++) {
      if (ReloadableX509KeyManager.class.isInstance(keyManagers[ii])) {
        throw new IllegalStateException(
            "A ReloadableX509KeyManager has already been set for this KeyManager[]");
      }

      if (X509KeyManager.class.isInstance(keyManagers[ii])) {
        if (reloadableX509KeyManager == null) {
          reloadableX509KeyManager = new ReloadableX509KeyManager((X509KeyManager) keyManagers[ii]);
        }
        else {
          reloadableX509KeyManager.setDelegateKeyManager((X509KeyManager) keyManagers[ii]);
        }

        keyManagers[ii] = reloadableX509KeyManager;
        return reloadableX509KeyManager;
      }
    }

    throw new NoSuchAlgorithmException("No X509KeyManager found in KeyManager[]");
  }
}
