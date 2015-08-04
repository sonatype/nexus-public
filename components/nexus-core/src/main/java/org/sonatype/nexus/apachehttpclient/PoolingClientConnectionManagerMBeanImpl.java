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
package org.sonatype.nexus.apachehttpclient;

import java.util.concurrent.TimeUnit;

import javax.management.StandardMBean;

import com.google.common.base.Preconditions;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

/**
 * Default {@link PoolingClientConnectionManagerMBean} implementation.
 *
 * @since 2.2
 */
class PoolingClientConnectionManagerMBeanImpl
    extends StandardMBean
    implements PoolingClientConnectionManagerMBean
{

  private final PoolingHttpClientConnectionManager connMgr;

  PoolingClientConnectionManagerMBeanImpl(final PoolingHttpClientConnectionManager connMgr) {
    super(PoolingClientConnectionManagerMBean.class, false);

    this.connMgr = Preconditions.checkNotNull(connMgr);
  }

  @Override
  public int getMaxTotal() {
    return connMgr.getMaxTotal();
  }

  @Override
  public int getDefaultMaxPerRoute() {
    return connMgr.getDefaultMaxPerRoute();
  }

  @Override
  public int getLeased() {
    return connMgr.getTotalStats().getLeased();
  }

  @Override
  public int getPending() {
    return connMgr.getTotalStats().getPending();
  }

  @Override
  public int getAvailable() {
    return connMgr.getTotalStats().getAvailable();
  }

  @Override
  public int getMax() {
    return connMgr.getTotalStats().getMax();
  }

  @Override
  public void closeIdleConnections(final long idleTimeoutInMillis) {
    connMgr.closeIdleConnections(idleTimeoutInMillis, TimeUnit.MILLISECONDS);
  }

  @Override
  public void closeExpiredConnections() {
    connMgr.closeExpiredConnections();
  }

  @Override
  public void setMaxTotal(final int max) {
    connMgr.setMaxTotal(max);
  }

  @Override
  public void setDefaultMaxPerRoute(final int max) {
    connMgr.setDefaultMaxPerRoute(max);
  }

}
