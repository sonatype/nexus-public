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
package org.sonatype.nexus.repository.npm.internal.audit.report;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Model will be serialized into Json representation for npm audit report.
 *
 * @since 3.24
 */
public class ResponseReport
{
  private final List<Action> actions;

  private final Map<String, Advisory> advisories;

  private final List<Object> muted; // NOSONAR we don't care about that field in JSON

  private final Metadata metadata;

  public ResponseReport(
      final List<Action> actions,
      final Map<String, Advisory> advisories,
      final List<Object> muted,
      final Metadata metadata)
  {
    this.actions = actions;
    this.advisories = advisories;
    this.muted = muted;
    this.metadata = metadata;
  }

  public Metadata getMetadata() {
    return metadata;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ResponseReport that = (ResponseReport) o;
    return Objects.equals(actions, that.actions) &&
        Objects.equals(advisories, that.advisories) &&
        Objects.equals(muted, that.muted) &&
        Objects.equals(metadata, that.metadata);
  }

  @Override
  public int hashCode() {
    return Objects.hash(actions, advisories, muted, metadata);
  }
}
