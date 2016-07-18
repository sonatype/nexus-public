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
package org.sonatype.nexus.ssl;

import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Collection;
import java.util.List;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.crypto.CryptoHelper;
import org.sonatype.nexus.ssl.internal.ReloadableX509KeyManager;
import org.sonatype.nexus.ssl.internal.ReloadableX509TrustManager;
import org.sonatype.nexus.ssl.internal.geronimo.FileKeystoreInstance;
import org.sonatype.nexus.ssl.internal.geronimo.KeystoreInstance;
import org.sonatype.nexus.ssl.spi.KeyStoreStorage;
import org.sonatype.nexus.ssl.spi.KeyStoreStorageManager;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An implementation of a {@link KeyStoreManager} that stores the trusted certificates and
 * public/private key used for authentication in different key-stores.
 *
 * Current key-store implementation appears to NOT to have case-sensitive alias support (ie. "myKey" = "mykey").
 *
 * @since 3.0
 */
public class KeyStoreManagerImpl
    extends ComponentSupport
    implements KeyStoreManager
{
  private static final String PRIVATE_KEY_STORE_NAME = "private.ks";

  private static final String TRUSTED_KEY_STORE_NAME = "trusted.ks";

  private static final String DEFAULT00_KEY_ALIAS = "__default00";

  @VisibleForTesting
  static final String PRIVATE_KEY_ALIAS = "identity";

  private final CryptoHelper crypto;

  private final KeyStoreManagerConfiguration config;

  private final KeystoreInstance privateKeyStore;

  private final KeystoreInstance trustedKeyStore;

  private ReloadableX509TrustManager reloadableX509TrustManager;

  private ReloadableX509KeyManager reloadableX509KeyManager;

  public KeyStoreManagerImpl(final CryptoHelper crypto,
                             final KeyStoreStorageManager storageManager,
                             final KeyStoreManagerConfiguration config)
  {
    this.crypto = checkNotNull(crypto);
    this.config = checkNotNull(config);

    this.privateKeyStore = initializePrivateKeyStore(storageManager.createStorage(PRIVATE_KEY_STORE_NAME));
    this.trustedKeyStore = initializeTrustedKeyStore(storageManager.createStorage(TRUSTED_KEY_STORE_NAME));
  }

  /**
   * Initializes the key-store with a default key (if not already created), so that the identity key can be removed
   * w/o invalidating the re-loadable key-manager.
   * <p/>
   * This key is never used by anything.  Its only here to prevent the key-store from being empty of keys.
   */
  private KeystoreInstance initializePrivateKeyStore(final KeyStoreStorage storage) {
    log.debug("Initializing private key-store: {}", storage);

    FileKeystoreInstance ks = new FileKeystoreInstance(
        crypto,
        storage,
        PRIVATE_KEY_STORE_NAME,
        config.getPrivateKeyStorePassword(),
        config.getKeyStoreType(),
        ImmutableMap.of(PRIVATE_KEY_ALIAS, config.getPrivateKeyPassword())
    );

    if (!isKeyPairInstalled(ks, DEFAULT00_KEY_ALIAS)) {
      try {
        log.debug("Initializing private key-store");

        ks.generateKeyPair(
            DEFAULT00_KEY_ALIAS,
            config.getPrivateKeyStorePassword(),
            config.getPrivateKeyPassword(),
            config.getKeyAlgorithm(),
            config.getKeyAlgorithmSize(),
            config.getSignatureAlgorithm(),
            config.getCertificateValidity().toDaysI(),
            DEFAULT00_KEY_ALIAS,
            "Nexus",            //NON-NLS
            "Sonatype",         //NON-NLS
            "Silver Spring",    //NON-NLS
            "MD",               //NON-NLS
            "US"                //NON-NLS
        );

        Certificate cert =
            ks.getCertificate(DEFAULT00_KEY_ALIAS, config.getPrivateKeyStorePassword());
        log.trace("Generated default certificate:\n{}", cert);
      }
      catch (KeystoreException e) {
        log.error("Failed to install default certificate", e);
        throw Throwables.propagate(e);
      }
    }

    // List key aliases for sanity
    if (log.isTraceEnabled()) {
      try {
        String[] aliases = ks.listPrivateKeys(config.getPrivateKeyStorePassword());
        if (aliases != null && aliases.length != 0) {
          log.trace("Private key aliases:");
          for (String alias : aliases) {
            log.trace("  {}", alias);
          }
        }
      }
      catch (KeystoreException e) {
        log.error("Failed to list key aliases", e);
        // this probably won't ever happen
      }
    }

    log.debug("Private key-store initialized");

    return ks;
  }

  private KeystoreInstance initializeTrustedKeyStore(final KeyStoreStorage storage) {
    log.debug("Initializing trusted key-store: {}", storage);

    FileKeystoreInstance ks = new FileKeystoreInstance(
        crypto,
        storage,
        TRUSTED_KEY_STORE_NAME,
        config.getTrustedKeyStorePassword(),
        config.getKeyStoreType(),
        ImmutableMap.of(TRUSTED_KEY_STORE_NAME, config.getTrustedKeyStorePassword())
    );

    logTrustedCertificateAliases(ks);

    // FIXME: Log a warning for edge cases when truststore is not empty, it should be (we manage this content via capability),
    // FIXME: ... probably some bugs related to be fixed
    try {
      if (ks.listTrustCertificates(config.getTrustedKeyStorePassword()).length != 0) {
        log.warn("Trusted key-store should have been empty when initialized but was not");
      }
    }
    catch (KeystoreException e) {
      // ignore
    }

    log.debug("Trusted key-store initialized");

    return ks;
  }

  private void logTrustedCertificateAliases(final KeystoreInstance ks) {
    assert ks != null;

    if (log.isTraceEnabled()) {
      try {
        String[] aliases = ks.listTrustCertificates(config.getTrustedKeyStorePassword());
        if (aliases != null && aliases.length != 0) {
          log.trace("Trusted certificate aliases:");
          for (String alias : aliases) {
            log.trace("  {}", alias);
          }
        }
      }
      catch (KeystoreException e) {
        log.error("Failed to list aliases", e);
        // this probably won't ever happen
      }
    }
  }

  @Override
  public TrustManager[] getTrustManagers() throws KeystoreException {
    TrustManager[] trustManagers = trustedKeyStore.getTrustManager(config.getTrustManagerAlgorithm(),
        config.getTrustedKeyStorePassword());

    // important! any time we get the array of trust managers we need to replace the X509TrustManager with the
    // ReloadableX509TrustManager so that changes to the keystore are updated in the TrustManager
    try {
      reloadableX509TrustManager =
          ReloadableX509TrustManager.replaceX509TrustManager(reloadableX509TrustManager, trustManagers);
    }
    catch (NoSuchAlgorithmException e) {
      throw new KeystoreException("A ReloadableX509TrustManager could not be created.", e);
    }
    return trustManagers;
  }

  @Override
  public KeyManager[] getKeyManagers() throws KeystoreException {
    KeyManager[] keyManagers = privateKeyStore.getKeyManager(config.getKeyManagerAlgorithm(), PRIVATE_KEY_ALIAS,
        config.getPrivateKeyStorePassword());

    // important! any time we get the array of key managers we need to replace the X509KeyManager with the
    // ReloadableX509KeyManager so that changes to the keystore are updated in the KeyManager
    try {
      reloadableX509KeyManager =
          ReloadableX509KeyManager.replaceX509KeyManager(reloadableX509KeyManager, keyManagers);
    }
    catch (NoSuchAlgorithmException e) {
      throw new KeystoreException("A ReloadableX509KeyManager could not be created.", e);
    }
    return keyManagers;
  }

  @Override
  public void importTrustCertificate(Certificate certificate, String alias) throws KeystoreException {
    log.debug("Importing trust certificate w/alias: {}", alias);

    if (trustedKeyStore.getCertificate(alias) != null) {
      log.warn("Certificate already exists in trust-store w/alias: {}; replacing certificate", alias);
      trustedKeyStore.deleteEntry(alias, config.getTrustedKeyStorePassword());
    }

    trustedKeyStore.importTrustCertificate(certificate, alias, config.getTrustedKeyStorePassword());

    logTrustedCertificateAliases(trustedKeyStore);

    // update re-loadable bits
    getTrustManagers();
  }

  @Override
  public void importTrustCertificate(String certificateInPEM, String alias)
      throws KeystoreException, CertificateException
  {
    // parse the cert
    Certificate certificate = CertificateUtil.decodePEMFormattedCertificate(certificateInPEM);
    // then import it
    importTrustCertificate(certificate, alias);
  }

  @Override
  public Certificate getTrustedCertificate(String alias) throws KeystoreException {
    return trustedKeyStore.getCertificate(
        checkNotNull(alias, "'alias' cannot be null when looking up a trusted Certificate."), //NON-NLS
        config.getTrustedKeyStorePassword());
  }

  @Override
  public Collection<Certificate> getTrustedCertificates() throws KeystoreException {
    String[] aliases = trustedKeyStore.listTrustCertificates(config.getTrustedKeyStorePassword());
    List<Certificate> certificates = Lists.newArrayListWithCapacity(aliases.length);
    for (String alias : aliases) {
      Certificate cert = trustedKeyStore.getCertificate(alias);
      // FIXME: Work around some strange case not clear why, but alias is reported for non-existent/removed certs
      if (cert == null) {
        log.warn("Trust-store reports it contains certificate for alias '{}' but certificate is null", alias);
        continue;
      }
      certificates.add(cert);
    }

    return certificates;
  }

  @Override
  public void removeTrustCertificate(String alias) throws KeystoreException {
    log.debug("Removing trust certificate w/alias: {}", alias);

    trustedKeyStore.deleteEntry(alias, config.getTrustedKeyStorePassword());

    logTrustedCertificateAliases(trustedKeyStore);

    // update re-loadable bits
    getTrustManagers();
  }

  @Override
  public void generateAndStoreKeyPair(final String commonName,
                                      final String organizationalUnit,
                                      final String organization,
                                      final String locality,
                                      final String state,
                                      final String country)
      throws KeystoreException
  {
    privateKeyStore.generateKeyPair(PRIVATE_KEY_ALIAS,
        config.getPrivateKeyStorePassword(),
        config.getPrivateKeyPassword(),
        config.getKeyAlgorithm(),
        config.getKeyAlgorithmSize(),
        config.getSignatureAlgorithm(),
        config.getCertificateValidity().toDaysI(),
        commonName,
        organizationalUnit,
        organization,
        locality,
        state,
        country);

    // update re-loadable bits
    getKeyManagers();
  }

  private boolean isKeyPairInstalled(final KeystoreInstance ks, final String alias) {
    try {
      ks.getCertificate(alias, config.getPrivateKeyStorePassword());
      return true;
    }
    catch (KeystoreException e) {
      log.trace("Key-pair not installed w/alias: {}", alias);
      return false;
    }
  }

  @Override
  public boolean isKeyPairInitialized() {
    return isKeyPairInstalled(privateKeyStore, PRIVATE_KEY_ALIAS);
  }

  @Override
  public Certificate getCertificate() throws KeystoreException {
    return privateKeyStore.getCertificate(PRIVATE_KEY_ALIAS, config.getPrivateKeyStorePassword());
  }

  @Override
  public PrivateKey getPrivateKey() throws KeystoreException {
    return privateKeyStore.getPrivateKey(PRIVATE_KEY_ALIAS, config.getPrivateKeyStorePassword(),
        config.getPrivateKeyPassword());
  }

  @Override
  public void removePrivateKey() throws KeystoreException {
    privateKeyStore.deleteEntry(PRIVATE_KEY_ALIAS, config.getPrivateKeyStorePassword());
  }
}
