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
package org.sonatype.nexus.cleanup.internal;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.db.DatabaseCheck;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.FeatureFlags.CLEANUP_MAVEN_RETAIN;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.IS_PRERELEASE_KEY;

public class CleanupFeatureCheck extends ComponentSupport
{
  private final DatabaseCheck databaseCheck;

  private final Set<String> retainEnabledSet;

  @Inject
  public CleanupFeatureCheck(
      final DatabaseCheck databaseCheck,
      @Named("${" + CLEANUP_MAVEN_RETAIN + ":-false}") final boolean mavenRetainEnabled)
  {
    this.databaseCheck = checkNotNull(databaseCheck);
    this.retainEnabledSet = new HashSet<>();
    if (mavenRetainEnabled) {
      this.retainEnabledSet.add("maven2");
    }
  }

  public final boolean isRetainSupported(String formatName, Map<String, String> criteria) {
    boolean isPostgres = databaseCheck.isPostgresql();
    boolean retainEnabled = this.retainEnabledSet.contains(formatName);
    boolean isPrerelease = criteria.getOrDefault(IS_PRERELEASE_KEY, "false").equalsIgnoreCase("true");
    log.debug("format={}, isPostgres={}, retainEnabled={}, isPrerelease={}", formatName, isPostgres, retainEnabled, isPrerelease);
    return isPostgres && retainEnabled && isPrerelease;
  }
}
