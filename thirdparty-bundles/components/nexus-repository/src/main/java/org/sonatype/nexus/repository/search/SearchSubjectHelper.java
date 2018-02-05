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
package org.sonatype.nexus.repository.search;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;

import com.google.common.annotations.VisibleForTesting;
import org.apache.shiro.subject.Subject;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Helper class used to temporarily associate a subject with a key for purposes of passing Shiro {@link Subject}s into
 * Elasticsearch. The caller is provided with a {@link SubjectRegistration} that should typically be used as part of a
 * try-with-resources block.
 *
 * @since 3.1
 */
@Named
@Singleton
public class SearchSubjectHelper
    extends ComponentSupport
{
  @VisibleForTesting
  final Map<String, Subject> subjects = new ConcurrentHashMap<>();

  /**
   * Registers the subject, returning a {@link SubjectRegistration}.
   */
  public SubjectRegistration register(final Subject subject) {
    checkNotNull(subject);
    String uuid = UUID.randomUUID().toString();
    if (subjects.putIfAbsent(uuid, subject) != null) {
      throw new IllegalStateException("Duplicate UUID: " + uuid);
    }
    return new SubjectRegistration(uuid);
  }

  /**
   * Gets the subject associated with the specified ID if present, throwing an exception if no subject exists.
   */
  public Subject getSubject(final String subjectId) {
    checkNotNull(subjectId);
    return checkNotNull(subjects.get(subjectId), "No subject for ID %s", subjectId);
  }

  /**
   * Unregisters the subject with the specified ID.
   */
  private void unregister(final String subjectId) {
    checkNotNull(subjectId);
    subjects.remove(subjectId);
  }

  /**
   * {@link AutoCloseable} class demarcating the registration and unregistration of a subject.
   */
  public class SubjectRegistration
      implements AutoCloseable
  {
    private final String id;

    public SubjectRegistration(final String id) {
      this.id = checkNotNull(id);
    }

    public String getId() {
      return id;
    }

    @Override
    public void close() {
      unregister(id);
    }
  }
}
