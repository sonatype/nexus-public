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
package org.sonatype.nexus.blobstore.group.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;
import javax.inject.Named;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.group.BlobStoreGroup;
import org.sonatype.nexus.blobstore.group.FillPolicy;

import com.google.common.annotations.VisibleForTesting;

import static java.util.Collections.rotate;

/**
 * {@link FillPolicy} that divides writes to member blob stores evenly based upon a round robin selection.
 *
 * @since 3.14
 */
@Named(RoundRobinFillPolicy.TYPE)
public class RoundRobinFillPolicy
    extends ComponentSupport
    implements FillPolicy
{
  public static final String TYPE = "roundRobin";

  protected static final String NAME = "Round Robin";

  private AtomicInteger sequence = new AtomicInteger();

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  @Nullable
  public BlobStore chooseBlobStore(final BlobStoreGroup blobStoreGroup, final Map<String, String> headers) {
    return nextMember(blobStoreGroup.getMembers());
  }

  /**
   * Retrieves the next writable member in the group
   *
   * @param members of the BlobStoreGroup
   * @return the first writable {@link BlobStore} or null if none are writable
   */
  @Nullable
  private BlobStore nextMember(final List<BlobStore> members)
  {
    if (members.isEmpty()) {
      return null;
    }
    final int index = nextIndex() % members.size();
    log.trace("Using index {}", index);

    ArrayList<BlobStore> rotatedMembers = new ArrayList<>(members);
    rotate(rotatedMembers, index);
    return rotatedMembers.stream()
        .filter(BlobStore::isWritable)
        .filter(BlobStore::isStorageAvailable)
        .findFirst()
        .orElse(null);
  }

  @VisibleForTesting
  int nextIndex() {
    return sequence.getAndUpdate(i -> {
      i += 1;
      return i < 0 ? 0 : i;
    });
  }
}
