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

import org.apache.shiro.subject.Subject;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link SubjectProvider} implementation that always returns the "fixed" subject.
 *
 * @author cstamas
 * @since 2.6
 */
public class FixedSubjectProvider
    implements SubjectProvider
{
  private final Subject subject;

  public FixedSubjectProvider(final Subject subject) {
    this.subject = checkNotNull(subject);
  }

  @Override
  public Subject getSubject() {
    return subject;
  }
}
