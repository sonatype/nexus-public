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

import javax.annotation.Nullable;
import javax.inject.Named;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.api.BlobStore;

/**
 * Default {@link FillPolicy}.
 *
 * @since 3.next
 */
@Named
public class DefaultFillPolicy
    extends ComponentSupport
    implements FillPolicy
{

  @Override
  @Nullable
  public BlobStore chooseBlobStore(final BlobStoreGroup blobStoreGroup, final Map<String, String> headers) {
    List<BlobStore> members = blobStoreGroup.getMembers();
    if (!members.isEmpty()) {
      return members.get(0);
    }
    else {
      return null;
    }
  }
}
