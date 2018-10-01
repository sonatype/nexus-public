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
package org.sonatype.nexus.orient.entity;

import org.sonatype.nexus.common.entity.Entity;

import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 * Represents a specific step in deconflicting differences between entity records.
 *
 * @since 3.14
 */
public interface DeconflictStep<T extends Entity>
{
  /**
   * Attempts to deconflict differences between specific parts of the given records.
   * This may result in either record changing in-memory as differences are resolved
   * depending which record is seen as having the authoritative content for this step.
   *
   * The returned {@link ConflictState} should reflect which records were changed.
   *
   * For example: if the incoming change was seen as authoritative (ie. its data was
   * folded into the stored record) you'd return ALLOW. Whereas if the stored record
   * was authoritative (ie. it was merged into the incoming change) return MERGE.
   *
   * If it's obvious that deconfliction cannot proceed then you should return DENY.
   */
  ConflictState deconflict(ODocument storedRecord, ODocument changeRecord);
}
