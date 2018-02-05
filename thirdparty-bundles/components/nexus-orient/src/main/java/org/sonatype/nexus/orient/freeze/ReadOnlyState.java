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
package org.sonatype.nexus.orient.freeze;

/**
 * Summary object representing read-only state.
 *
 * @since 3.6
 */
public interface ReadOnlyState
{
  /**
   * @return true if read-only is set
   */
  boolean isFrozen();

  /**
   * @return a summary of why read-only is in effect; will be an empty string if {@link #isFrozen()} returns false
   */
  String getSummaryReason();

  /**
   * @return true if read-only was initiated by a system task; false if initiated by a user (via REST or UI)
   */
  boolean isSystemInitiated();
}
