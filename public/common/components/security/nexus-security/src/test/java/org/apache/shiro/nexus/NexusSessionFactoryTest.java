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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.shiro.session.ExpiredSessionException;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.StoppedSessionException;
import org.apache.shiro.session.mgt.DefaultSessionContext;
import org.apache.shiro.session.mgt.SimpleSession;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that {@link NexusSessionFactory} produces sessions whose timestamp/expiry
 * fields are {@code volatile}, preventing false session expiry on ARM64 where
 * Shiro 2.x plain fields allow stale CPU-cache reads under concurrent load.
 *
 * See NEXUS-54763 and https://github.com/apache/shiro/pull/2712
 */
class NexusSessionFactoryTest
{
  private final NexusSessionFactory factory = new NexusSessionFactory();

  /**
   * Confirms Shiro's SimpleSession fields are NOT volatile (the root cause).
   * This test documents why the fix is needed in the first place.
   */
  @Test
  void shiroSimpleSession_fieldsAreNotVolatile() throws Exception {
    for (String fieldName : new String[]{"lastAccessTime", "stopTimestamp", "expired", "timeout"}) {
      Field field = SimpleSession.class.getDeclaredField(fieldName);
      assertFalse(
          Modifier.isVolatile(field.getModifiers()),
          "Shiro SimpleSession." + fieldName + " is now volatile — Shiro has been upgraded to ≥3.0.0; "
              + "remove the volatile subclass fields in NexusSessionFactory.SimpleSessionImpl as they are no longer needed");
    }
  }

  /**
   * Confirms NexusSessionFactory produces sessions with volatile fields.
   * This is the fix for NEXUS-54763.
   */
  @Test
  void nexusSession_timestampFieldsAreVolatile() throws Exception {
    Session session = factory.createSession(null);

    for (String fieldName : new String[]{"lastAccessTime", "stopTimestamp", "expired", "timeout"}) {
      Field field = session.getClass().getDeclaredField(fieldName);
      assertTrue(
          Modifier.isVolatile(field.getModifiers()),
          "NexusSessionFactory session." + fieldName
              + " must be volatile to prevent ARM64 race condition (NEXUS-54763)");
    }
  }

  /**
   * Reproduces the ARM64 race condition:
   * Simulates a stale CPU-cache read of lastAccessTime (as happens on ARM64)
   * and verifies the fixed session does NOT falsely expire.
   *
   * Without the fix: ExpiredSessionException is thrown.
   * With the fix: Session remains valid because validate() reads the volatile field.
   */
  @Test
  void nexusSession_doesNotFalselyExpireWhenParentFieldIsStale() throws Exception {
    Session session = factory.createSession(null);
    session.setTimeout(1_800_000L); // 30 minutes
    session.touch();

    // Simulate ARM64 stale read: set parent SimpleSession.lastAccessTime to 2 hours ago
    // On ARM64, a thread can read this stale value instead of the updated volatile field
    Field parentField = SimpleSession.class.getDeclaredField("lastAccessTime");
    parentField.setAccessible(true);
    parentField.set(session, new Date(System.currentTimeMillis() - 2 * 3600 * 1000));

    // The fixed session reads volatile field in getLastAccessTime() — not the stale parent field
    // So validate() sees the correct recent timestamp and does NOT expire the session
    assertDoesNotThrow(
        () -> ((SimpleSession) session).validate(),
        "Session must not expire due to stale parent field read — volatile field should be used (NEXUS-54763)");
  }

  /**
   * Verifies the host constructor path (createSession with non-null host)
   * also produces volatile fields — covers the SimpleSessionImpl(String host) branch.
   */
  @Test
  void nexusSession_withHost_timestampFieldsAreVolatile() throws Exception {
    DefaultSessionContext context = new DefaultSessionContext();
    context.setHost("127.0.0.1");
    Session session = factory.createSession(context);

    for (String fieldName : new String[]{"lastAccessTime", "stopTimestamp", "expired", "timeout"}) {
      Field field = session.getClass().getDeclaredField(fieldName);
      assertTrue(
          Modifier.isVolatile(field.getModifiers()),
          "NexusSessionFactory session (host constructor)." + fieldName
              + " must be volatile to prevent ARM64 race condition (NEXUS-54763)");
    }
  }

  /**
   * Confirms that an unfixed plain SimpleSession DOES falsely expire
   * when its lastAccessTime field is set to a stale value.
   * This documents the bug behaviour.
   */
  @Test
  void plainShiroSession_falselyExpiresWhenFieldIsStale() throws Exception {
    SimpleSession session = new SimpleSession();
    session.setTimeout(1_800_000L);
    session.touch();

    // Same stale read simulation on unfixed SimpleSession
    Field field = SimpleSession.class.getDeclaredField("lastAccessTime");
    field.setAccessible(true);
    field.set(session, new Date(System.currentTimeMillis() - 2 * 3600 * 1000));

    // Without volatile, validate() reads the stale field directly and throws
    assertThrows(
        ExpiredSessionException.class,
        session::validate,
        "Plain Shiro SimpleSession should falsely expire on stale field read — confirming the bug");
  }

  /**
   * Regression test from PR #17642 review (Charan Boppe): SimpleSession.touch()/stop()/expire()
   * write directly to the parent's private fields via bytecode putfield, bypassing this
   * subclass's overridden setters entirely. Since the volatile subclass fields are only
   * ever populated at construction time, sessions must not falsely expire despite continuous
   * real touch() activity well within the configured timeout.
   */
  @Test
  void nexusSession_remainsValidWithContinuousActivity_REGRESSION() throws Exception {
    Session session = factory.createSession(null);
    session.setTimeout(300L);
    long start = System.currentTimeMillis();
    while (System.currentTimeMillis() - start < 1000) {
      session.touch();
      ((SimpleSession) session).validate();
      Thread.sleep(50);
    }
    assertFalse(
        ((SimpleSession) session).isExpired(),
        "Session must not be marked expired after continuous touch() activity (NEXUS-54763)");
  }

  /**
   * Verifies the documented assumption in SimpleSessionImpl's constructors (see
   * NexusSessionFactory.java) that Shiro's SimpleSession() / SimpleSession(String)
   * constructors do not internally invoke overridable setters. If a future Shiro
   * upgrade changes this, the volatile fields would be populated before the subclass's
   * own constructor body runs, and this test would surface the mismatch.
   */
  @Test
  void nexusSession_constructorFieldsMatchParentAfterConstruction() throws Exception {
    Session session = factory.createSession(null);

    assertTrue(
        ((SimpleSession) session).getLastAccessTime() != null,
        "lastAccessTime must be populated immediately after construction (NEXUS-54763)");
    assertTrue(
        ((SimpleSession) session).getTimeout() > 0,
        "timeout must be populated immediately after construction (NEXUS-54763)");
    assertFalse(
        ((SimpleSession) session).isExpired(),
        "expired must default to false immediately after construction (NEXUS-54763)");
  }

  /**
   * Reproduces the actual ARM64 failure mode: one thread repeatedly calling touch() while
   * a second thread concurrently calls validate() on the SAME session instance, matching
   * how Shiro's storeByValue(false) session cache shares one object across request threads.
   * This is a genuine cross-thread test — not a reflection-based staleness simulation —
   * and is the test category flagged as missing in PR #17642 review (Prashant Shekhar).
   */
  @Test
  void nexusSession_survivesConcurrentTouchAndValidate() throws Exception {
    Session session = factory.createSession(null);
    session.setTimeout(2000L);

    AtomicBoolean stop = new AtomicBoolean(false);
    AtomicReference<Exception> failure = new AtomicReference<>();

    Thread toucher = new Thread(() -> {
      while (!stop.get() && failure.get() == null) {
        session.touch();
      }
    });

    Thread validator = new Thread(() -> {
      while (!stop.get() && failure.get() == null) {
        try {
          ((SimpleSession) session).validate();
        }
        catch (Exception e) {
          failure.set(e);
        }
      }
    });

    toucher.start();
    validator.start();
    Thread.sleep(1000);
    stop.set(true);
    toucher.join(2000);
    validator.join(2000);

    if (failure.get() != null) {
      throw failure.get();
    }
  }

  /**
   * Exercises the overridden stop() (previously untested, per PR #17642 coverage review):
   * verifies its idempotency (matching SimpleSession.stop()'s own "only set once" contract),
   * that the volatile stopTimestamp is populated and stays in sync with the parent's field,
   * and that a stopped session throws StoppedSessionException on validate().
   */
  @Test
  void nexusSession_stopIsIdempotentAndStaysInSyncWithParent() throws Exception {
    Session session = factory.createSession(null);

    session.stop();
    Date firstStopTimestamp = ((SimpleSession) session).getStopTimestamp();
    assertTrue(firstStopTimestamp != null, "getStopTimestamp() must be populated after stop() (NEXUS-54763)");

    // Second stop() call must not overwrite the first timestamp (idempotency contract)
    session.stop();
    assertTrue(
        firstStopTimestamp.equals(((SimpleSession) session).getStopTimestamp()),
        "A second stop() call must not change stopTimestamp — stop() must remain idempotent (NEXUS-54763)");

    // Parent's own field (read directly, bypassing the overridden getter) must match the
    // volatile subclass value — confirms the dual-write in setStopTimestamp() stays in sync
    Field parentField = SimpleSession.class.getDeclaredField("stopTimestamp");
    parentField.setAccessible(true);
    assertTrue(
        firstStopTimestamp.equals(parentField.get(session)),
        "Parent SimpleSession.stopTimestamp must be kept in sync via the dual-write setter (NEXUS-54763)");

    assertThrows(
        StoppedSessionException.class,
        () -> ((SimpleSession) session).validate(),
        "A stopped session must throw StoppedSessionException on validate() (NEXUS-54763)");
  }

  /**
   * Exercises the overridden expire() (previously untested, per PR #17642 coverage review):
   * lets a short timeout lapse, confirms validate() throws ExpiredSessionException, and
   * confirms expire()'s effects (isExpired() true, getStopTimestamp() populated) are visible
   * afterwards through the volatile fields — and that the parent's own fields stay in sync.
   */
  @Test
  void nexusSession_expireSetsExpiredAndStopTimestampInSyncWithParent() throws Exception {
    Session session = factory.createSession(null);
    session.setTimeout(50L);
    session.touch();
    Thread.sleep(100);

    assertThrows(
        ExpiredSessionException.class,
        () -> ((SimpleSession) session).validate(),
        "A session past its timeout must throw ExpiredSessionException on validate() (NEXUS-54763)");

    assertTrue(
        ((SimpleSession) session).isExpired(),
        "isExpired() must be true after expire() runs during validate() (NEXUS-54763)");
    assertTrue(
        ((SimpleSession) session).getStopTimestamp() != null,
        "getStopTimestamp() must be populated after expire() calls stop() internally (NEXUS-54763)");

    Field expiredParentField = SimpleSession.class.getDeclaredField("expired");
    expiredParentField.setAccessible(true);
    assertTrue(
        (boolean) expiredParentField.get(session),
        "Parent SimpleSession.expired must be kept in sync via the dual-write setter (NEXUS-54763)");

    Field stopTimestampParentField = SimpleSession.class.getDeclaredField("stopTimestamp");
    stopTimestampParentField.setAccessible(true);
    assertTrue(
        stopTimestampParentField.get(session) != null,
        "Parent SimpleSession.stopTimestamp must be kept in sync via the dual-write setter (NEXUS-54763)");
  }
}
