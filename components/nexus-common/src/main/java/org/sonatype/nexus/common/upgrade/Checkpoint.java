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
package org.sonatype.nexus.common.upgrade;

/**
 * Checkpoints data/resources for a specific model/schema before upgrade starts.
 * 
 * @since 3.1
 */
public interface Checkpoint
{
  /**
   * Creates a new checkpoint for the installed version.
   */
  void begin(String version) throws Exception;

  /**
   * Commits the current checkpoint.
   */
  void commit() throws Exception;

  /**
   * Reverts to the previous checkpoint.
   */
  void rollback() throws Exception;

  /**
   * Finishes the checkpoint after the entire upgrade was successfully committed, i.e. all checkpoints succeeded and no
   * {@link #rollback()} will happen. This provides an opportunity to delete temporary/backup files. Any exception
   * thrown will be ignored and does not fail the upgrade.
   */
  void end();
}
