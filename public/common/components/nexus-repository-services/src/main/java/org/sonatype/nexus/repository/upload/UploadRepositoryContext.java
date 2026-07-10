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
package org.sonatype.nexus.repository.upload;

import org.sonatype.nexus.repository.Repository;

/**
 * Thread-local binding of the {@link Repository} currently being uploaded to. Allows a
 * {@link org.sonatype.nexus.repository.rest.ComponentUploadExtension#validate(ComponentUpload)}
 * implementation — whose interface signature does not carry the repository — to still
 * reach the target repository.
 *
 * Populated by the UI-facing {@code UploadService} and the REST {@code ComponentsResource}
 * immediately before calling {@link UploadManager#handle} and cleared in a finally block
 * afterwards.
 *
 * <h3>API surface caveat</h3>
 * The {@link #set} mutator is package-friendly only by convention — Java does not let us
 * reduce visibility on a static helper without adding an accessor pattern. Treat
 * {@link #set} and {@link #clear} as <b>internal</b> to the upload entry-points
 * ({@code UploadService}, {@code ComponentsResource}). A plugin that overwrites the
 * binding mid-request can steer the policy-enforcement gate at a different repository
 * than the one being written to. Do not call {@link #set} from
 * {@link org.sonatype.nexus.repository.rest.ComponentUploadExtension} or any other
 * extension callback.
 *
 * <h3>Threading constraint — must run on the request-handling thread</h3>
 * This class uses a plain {@link ThreadLocal}, not {@link InheritableThreadLocal}: values
 * are <b>not</b> inherited by child threads. The whole upload chain — {@code set} on the
 * entry point, {@code UploadManager.handle}, {@code ComponentUploadExtension.validate},
 * and {@code clear} in the finally — is required to execute on the same thread.
 * <p>
 * If a future change dispatches any part of upload handling onto a different thread (a
 * virtual-thread executor, an async format handler, an AOP proxy that routes through a
 * pool), {@code get()} will return {@code null} on the worker thread and the policy gate
 * will be silently skipped. {@code UiUploadEnforcementInterceptor} surfaces the missing
 * binding as a WARN log (see {@code UiUploadEnforcementInterceptor.validate}), but the
 * client still gets a 200 because the bypass happens before any verdict is produced.
 * <p>
 * The right long-term fix is to retire this ThreadLocal entirely by adding
 * {@code default void validate(Repository, ComponentUpload)} to
 * {@code ComponentUploadExtension} — see PMQ-HRE-013 in the IQ PM questions doc.
 * Switching to {@code InheritableThreadLocal} is an interim option but only protects
 * threads spawned from the request thread; it does not help worker-pool dispatch.
 */
public final class UploadRepositoryContext
{
  private static final ThreadLocal<Repository> CURRENT = new ThreadLocal<>();

  private UploadRepositoryContext() {
  }

  /**
   * Bind {@code repository} on the current thread. Must be paired with {@link #clear()}
   * in a {@code finally} block. Internal to upload entry-points only — see class
   * Javadoc.
   */
  public static void set(final Repository repository) {
    CURRENT.set(repository);
  }

  public static Repository get() {
    return CURRENT.get();
  }

  public static void clear() {
    CURRENT.remove();
  }
}
