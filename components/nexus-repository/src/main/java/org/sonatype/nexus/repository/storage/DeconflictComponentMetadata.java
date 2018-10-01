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

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.orient.entity.ConflictState;
import org.sonatype.nexus.orient.entity.DeconflictStepSupport;

import com.orientechnologies.orient.core.record.impl.ODocument;

import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_LAST_UPDATED;

/**
 * Deconflicts component metadata:
 *
 * "last_updated" - book-keeping attribute, we're only interested in the latest time
 *
 * @since 3.14
 */
@Named
@Singleton
public class DeconflictComponentMetadata
    extends DeconflictStepSupport<Component>
{
  @Override
  public ConflictState deconflict(final ODocument storedRecord, final ODocument changeRecord) {
    return pickLatest(storedRecord, changeRecord, P_LAST_UPDATED);
  }
}
