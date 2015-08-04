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
package org.sonatype.nexus.threads;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.apache.shiro.concurrent.SubjectAwareExecutorService;
import org.apache.shiro.subject.Subject;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A modification of Shiro's {@link org.apache.shiro.concurrent.SubjectAwareExecutorService} that in turn returns
 * always
 * the same, supplied
 * {@link org.apache.shiro.subject.Subject} to bind threads with.
 *
 * @author cstamas
 * @since 2.6
 */
public class NexusExecutorService
    extends SubjectAwareExecutorService
{
  public static NexusExecutorService forFixedSubject(final ExecutorService target, final Subject subject) {
    return new NexusExecutorService(target, new FixedSubjectProvider(subject));
  }

  public static NexusExecutorService forCurrentSubject(final ExecutorService target) {
    return new NexusExecutorService(target, new CurrentSubjectProvider());
  }

  // ==

  private final SubjectProvider subjectProvider;

  public NexusExecutorService(final ExecutorService target, final SubjectProvider subjectProvider) {
    super(checkNotNull(target));
    this.subjectProvider = checkNotNull(subjectProvider);
  }

  /**
   * Override, use our SubjectProvider to get subject from.
   */
  @Override
  protected Subject getSubject() {
    return subjectProvider.getSubject();
  }

  @Override
  protected Runnable associateWithSubject(Runnable r) {
    Subject subject = getSubject();
    return subject.associateWith(new MDCAwareRunnable(r));
  }

  @Override
  protected <T> Callable<T> associateWithSubject(Callable<T> task) {
    Subject subject = getSubject();
    return subject.associateWith(new MDCAwareCallable(task));
  }
}
