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
package org.sonatype.nexus.repository.storage;

/**
 * Write policy.
 *
 * @since 3.0
 */
public enum WritePolicy
{
  /**
   * Asset can be linked with a blob.
   * Asset can be re-linked with another blob.
   * Asset can be unlinked from a blob (blob can be deleted).
   */
  ALLOW,
  /**
   * Asset can be linked with a blob.
   * Asset cannot be re-linked with another blob.
   * Asset can be unlinked from a blob (blob can be deleted).
   */
  ALLOW_ONCE,
  /**
   * Asset cannot be linked with a blob.
   * Asset cannot be re-linked with another blob.
   * Asset cannot be unlinked from a blob.
   */
  DENY;

  /**
   * Returns {@code true} if Create allowed with this policy.
   */
  public boolean checkCreateAllowed() {
    return this != DENY;
  }

  /**
   * Returns {@code true} if Update allowed with this policy.
   */
  public boolean checkUpdateAllowed() {
    return this == ALLOW;
  }

  /**
   * Returns {@code true} if Delete allowed with this policy.
   */
  public boolean checkDeleteAllowed() {
    return this != DENY;
  }
}
