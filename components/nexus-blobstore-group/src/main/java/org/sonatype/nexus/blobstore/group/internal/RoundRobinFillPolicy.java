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

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;
import javax.inject.Named;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.api.BlobStore;

import com.google.common.annotations.VisibleForTesting;

/**
 * {@link FillPolicy} that divides writes to member blob stores evenly based upon a round robin selection.
 *
 * @since 3.next
 */
@Named(RoundRobinFillPolicy.TYPE)
public class RoundRobinFillPolicy
    extends ComponentSupport
    implements FillPolicy
{

  public static final String TYPE = "roundRobin";
  private static final String NAME = "Round Robin";

  private final AtomicInteger sequence = new AtomicInteger();

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  @Nullable
  public BlobStore chooseBlobStore(final BlobStoreGroup blobStoreGroup, final Map<String, String> headers) {
    int index = nextIndex();
    log.trace("Using index {}", index);

    List<BlobStore> members = blobStoreGroup.getMembers();
    if (!members.isEmpty()) {
      return members.get(index % members.size());
    }
    else {
      return null;
    }
  }

  @VisibleForTesting
  int nextIndex() {
    return sequence.getAndUpdate(i -> {
        i += 1;
        return i < 0 ? 0 : i;
    });
  }
}
