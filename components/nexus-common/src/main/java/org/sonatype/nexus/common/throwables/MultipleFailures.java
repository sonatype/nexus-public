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
package org.sonatype.nexus.common.throwables;

import java.util.List;

import javax.annotation.Nullable;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.text.Plural;

import com.google.common.collect.Lists;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Helper to collect and throw multiple {@link Throwable}s.
 *
 * @since 3.0
 */
public class MultipleFailures
  extends ComponentSupport
{
  private final List<Throwable> failures = Lists.newLinkedList();

  public List<Throwable> getFailures() {
    return failures;
  }

  public void add(final Throwable failure) {
    log.trace("Adding: {}", failure);
    failures.add(checkNotNull(failure));
  }

  public class MultipleFailuresException
    extends Exception
  {
    private MultipleFailuresException(@Nullable final String message) {
      super(message);
      for (Throwable failure : failures) {
        addSuppressed(failure);
      }
    }

    @Override
    public String getMessage() {
      StringBuilder buff = new StringBuilder();

      String message = super.getMessage();
      if (message != null) {
        buff.append(message).append("; ");
      }

      Plural.append(buff, failures.size(), "failure");
      return buff.toString();
    }
  }

  /**
   * Maybe throw {@link MultipleFailuresException} if there are any failures with optional message.
   */
  public void maybePropagate(@Nullable final String message) throws MultipleFailuresException {
    if (!failures.isEmpty()) {
      log.trace("Propagating: {}", failures);
      throw new MultipleFailuresException(message);
    }
  }

  /**
   * Maybe throw {@link MultipleFailuresException} if there are any failures.
   */
  public void maybePropagate() throws MultipleFailuresException {
    maybePropagate(null);
  }
}
