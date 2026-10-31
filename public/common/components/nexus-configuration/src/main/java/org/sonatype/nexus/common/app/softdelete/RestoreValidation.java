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
package org.sonatype.nexus.common.app.softdelete;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

/**
 * Result of validating whether a soft-deleted entity can be restored.
 */
public class RestoreValidation
{
  private final boolean canRestore;

  private final List<String> blockers;

  private final List<String> warnings;

  private RestoreValidation(final boolean canRestore, final List<String> blockers, final List<String> warnings) {
    this.canRestore = canRestore;
    this.blockers = unmodifiableList(blockers);
    this.warnings = unmodifiableList(warnings);
  }

  public boolean isCanRestore() {
    return canRestore;
  }

  public List<String> getBlockers() {
    return blockers;
  }

  public List<String> getWarnings() {
    return warnings;
  }

  public static RestoreValidation ok() {
    return new RestoreValidation(true, emptyList(), emptyList());
  }

  public static RestoreValidation ok(final List<String> warnings) {
    return new RestoreValidation(true, emptyList(), warnings);
  }

  public static RestoreValidation blocked(final List<String> blockers) {
    return new RestoreValidation(false, blockers, emptyList());
  }
}
