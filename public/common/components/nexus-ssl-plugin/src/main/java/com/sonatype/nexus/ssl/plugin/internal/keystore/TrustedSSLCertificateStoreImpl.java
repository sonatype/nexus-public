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
package com.sonatype.nexus.ssl.plugin.internal.keystore;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.datastore.ConfigStoreSupport;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.transaction.Transactional;

import static com.google.common.base.Preconditions.checkNotNull;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * MyBatis {@link TrustedSSLCertificateStore} implementation.
 */
@Component
@Qualifier("mybatis")
public class TrustedSSLCertificateStoreImpl
    extends ConfigStoreSupport<TrustedSSLCertificateDAO>
    implements TrustedSSLCertificateStore
{
  private final EventManager eventManager;

  @Autowired
  public TrustedSSLCertificateStoreImpl(
      final DataSessionSupplier sessionSupplier,
      final EventManager eventManager)
  {
    super(sessionSupplier);
    this.eventManager = checkNotNull(eventManager);
  }

  @Transactional
  @Override
  public Optional<TrustedSSLCertificate> find(final String alias) {
    Optional<TrustedSSLCertificateData> optional = dao().find(alias);
    return optional.map(data -> data);
  }

  @Transactional
  @Override
  public List<TrustedSSLCertificate> findAll() {
    List<TrustedSSLCertificateData> list = dao().findAll();

    return list.stream()
        .map(data -> (TrustedSSLCertificate) data)
        .toList();
  }

  @Transactional
  @Override
  public void save(final String alias, final String pem) {
    checkNotNull(alias);
    checkNotNull(pem);
    TrustedSSLCertificateData data = new TrustedSSLCertificateData(alias, pem);
    dao().save(data);
    postEvent(alias);
  }

  @Transactional
  @Override
  public void delete(final String alias) {
    checkNotNull(alias);
    dao().delete(alias);
    postEvent(alias);
  }

  private void postEvent(final String alias) {
    // trigger invalidation of TrustStoreImpl context
    eventManager.post((TrustedSSLCertificateDataEvent) () -> alias);
  }
}
