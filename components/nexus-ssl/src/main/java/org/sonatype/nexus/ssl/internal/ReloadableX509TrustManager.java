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

import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * A X509TrustManager that will can be updated.
 *
 * The default implementation is not updated when changes made to a keystore.
 *
 * Based on work from: http://jcalcote.wordpress.com/2010/06/22/managing-a-dynamic-java-trust-store/
 *
 * @since 3.0
 */
public class ReloadableX509TrustManager
    implements X509TrustManager
{
  private X509TrustManager delegateTrustManager;

  /**
   * Creates the initial ReloadableX509TrustManager which wraps an existing X509TrustManager.
   *
   * @param delegateTrustManager the X509TrustManager to delegate calls to.
   */
  private ReloadableX509TrustManager(final X509TrustManager delegateTrustManager) {
    this.setDelegateTrustManager(delegateTrustManager);
  }

  @Override
  public void checkClientTrusted(final X509Certificate[] x509Certificates, final String s) throws CertificateException {
    delegateTrustManager.checkClientTrusted(x509Certificates, s);
  }

  @Override
  public void checkServerTrusted(final X509Certificate[] x509Certificates, final String s) throws CertificateException {
    delegateTrustManager.checkServerTrusted(x509Certificates, s);
  }

  @Override
  public X509Certificate[] getAcceptedIssuers() {
    return delegateTrustManager.getAcceptedIssuers();
  }

  /**
   * Sets the X509TrustManager which will be used to delegate calls to.
   *
   * @param delegateTrustManager the X509TrustManager which will be used to delegate calls to.
   */
  private void setDelegateTrustManager(final X509TrustManager delegateTrustManager) {
    this.delegateTrustManager = delegateTrustManager;
  }

  /**
   * Finds and replaces the X509TrustManager with a ReloadableX509TrustManager.  If there is more then one, only the
   * first one will be replaced.
   *
   * @param reloadableX509TrustManager an existing ReloadableX509TrustManager, or null if one does not exist.
   * @param trustManagers              an array of TrustManagers that is expected to contain a X509TrustManager.
   * @return a newly create ReloadableX509TrustManager
   * @throws NoSuchAlgorithmException
   *                               thrown if a X509TrustManager cannot be found in the array.
   * @throws IllegalStateException thrown if a ReloadableX509TrustManager is found in the array.
   */
  public static ReloadableX509TrustManager replaceX509TrustManager(ReloadableX509TrustManager reloadableX509TrustManager,
                                                                   final TrustManager[] trustManagers)
      throws NoSuchAlgorithmException
  {
    for (int ii = 0; ii < trustManagers.length; ii++) {
      if (ReloadableX509TrustManager.class.isInstance(trustManagers[ii])) {
        throw new IllegalStateException(
            "A ReloadableX509TrustManager has already been set for this TrustManager[]");
      }

      if (X509TrustManager.class.isInstance(trustManagers[ii])) {
        if (reloadableX509TrustManager == null) {
          reloadableX509TrustManager = new ReloadableX509TrustManager((X509TrustManager) trustManagers[ii]);
        }
        else {
          reloadableX509TrustManager.setDelegateTrustManager((X509TrustManager) trustManagers[ii]);
        }

        trustManagers[ii] = reloadableX509TrustManager;
        return reloadableX509TrustManager;
      }
    }

    throw new NoSuchAlgorithmException("No X509TrustManager found in TrustManager[]");
  }
}
