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
package org.apache.shiro.nexus;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.SessionContext;
import org.apache.shiro.session.mgt.SessionFactory;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.session.mgt.SimpleSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom {@link SessionFactory}.
 *
 * @since 3.0
 */
public class NexusSessionFactory
    extends SimpleSessionFactory
{
  private static final Logger log = LoggerFactory.getLogger(NexusSessionFactory.class);

  /**
   * Ensure {@link SimpleSessionImpl} is used and provides logging.
   */
  @Override
  public Session createSession(final SessionContext initData) {
    log.trace("Creating session w/init-data: {}", initData);

    // duplicated from SimpleSessionFactory, retaining class-hierarchy for sanity
    if (initData != null) {
      String host = initData.getHost();
      if (host != null) {
        return new SimpleSessionImpl(host);
      }
    }
    return new SimpleSessionImpl();
  }

  /**
   * Customized session impl to apply synchronized-treatment to attributes map,
   * and volatile fields for timestamp/expiry state to fix a race on ARM64 where
   * Shiro 2.x plain fields allow stale reads that falsely expire active sessions.
   * See: https://github.com/apache/shiro/pull/2712 (fixed in Shiro 3.0.0)
   *
   * {@link SimpleSession#touch()}, {@link SimpleSession#stop()}, and
   * {@link SimpleSession#expire()} write their respective fields directly rather than
   * through the setters overridden below, so each is also overridden here to route
   * through the volatile setters — otherwise the volatile fields are only ever
   * populated at construction time and never updated by real request activity.
   */
  private static class SimpleSessionImpl
      extends SimpleSession
  {
    private volatile Date lastAccessTime;

    private volatile Date stopTimestamp;

    private volatile boolean expired;

    private volatile long timeout;

    public SimpleSessionImpl() {
      super();
      // Assumes Shiro's SimpleSession() constructor does not call setTimeout() or
      // any other overridable setter — verified against Shiro 2.2.1. If Shiro is
      // upgraded and the constructor gains internal setter calls, these initialisers
      // must be reviewed to ensure the volatile field is populated correctly.
      this.lastAccessTime = super.getLastAccessTime();
      this.stopTimestamp = super.getStopTimestamp();
      this.expired = super.isExpired();
      this.timeout = super.getTimeout();
    }

    public SimpleSessionImpl(final String host) {
      super(host);
      // Same assumption as the no-arg constructor above.
      this.lastAccessTime = super.getLastAccessTime();
      this.stopTimestamp = super.getStopTimestamp();
      this.expired = super.isExpired();
      this.timeout = super.getTimeout();
    }

    @Override
    public Date getLastAccessTime() {
      return lastAccessTime;
    }

    @Override
    public void setLastAccessTime(final Date lastAccessTime) {
      this.lastAccessTime = lastAccessTime;
      super.setLastAccessTime(lastAccessTime);
    }

    @Override
    public Date getStopTimestamp() {
      return stopTimestamp;
    }

    @Override
    public void setStopTimestamp(final Date stopTimestamp) {
      this.stopTimestamp = stopTimestamp;
      super.setStopTimestamp(stopTimestamp);
    }

    @Override
    public boolean isExpired() {
      return expired;
    }

    @Override
    public void setExpired(final boolean expired) {
      this.expired = expired;
      super.setExpired(expired);
    }

    @Override
    public long getTimeout() {
      return timeout;
    }

    @Override
    public void setTimeout(final long timeout) {
      this.timeout = timeout;
      super.setTimeout(timeout);
    }

    /**
     * {@link SimpleSession#touch()} writes {@code lastAccessTime} directly via a private
     * field assignment rather than calling {@link #setLastAccessTime(Date)}, so it never
     * reaches the volatile field above. Overriding here routes every touch through the
     * volatile setter instead.
     */
    @Override
    public void touch() {
      setLastAccessTime(new Date());
    }

    /**
     * {@link SimpleSession#stop()} writes {@code stopTimestamp} directly rather than calling
     * {@link #setStopTimestamp(Date)}. Overriding here routes it through the volatile setter,
     * matching {@link SimpleSession#stop()}'s own idempotency check (only set once).
     */
    @Override
    public void stop() {
      if (getStopTimestamp() == null) {
        setStopTimestamp(new Date());
      }
    }

    /**
     * {@link SimpleSession#expire()} calls {@link #stop()} (already overridden above) but then
     * writes {@code expired} directly rather than calling {@link #setExpired(boolean)}.
     * Overriding here routes it through the volatile setter.
     */
    @Override
    protected void expire() {
      stop();
      setExpired(true);
    }

    /**
     * Work around bug in Shiro which uses a non-synchronized map to back attributes.
     *
     * This appears to only be called by {@link SimpleSession#getAttributesLazy()}.
     */
    @Override
    public void setAttributes(final Map<Object, Object> attributes) {
      super.setAttributes(attributes != null ? Collections.synchronizedMap(attributes) : null);
    }

    /**
     * {@link SimpleSession}'s {@code readObject} restores its own fields via direct field
     * assignment, bypassing this subclass's constructor entirely, so the volatile fields above
     * would otherwise be left at their JVM defaults (null/0/false) after deserialization. Not
     * reachable today — the active session cache is configured with {@code storeByValue(false)},
     * so sessions are never actually serialized — but kept as a safeguard against a future
     * store-by-value, persistent, or clustered cache configuration.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
      in.defaultReadObject();
      this.lastAccessTime = super.getLastAccessTime();
      this.stopTimestamp = super.getStopTimestamp();
      this.expired = super.isExpired();
      this.timeout = super.getTimeout();
    }
  }
}
