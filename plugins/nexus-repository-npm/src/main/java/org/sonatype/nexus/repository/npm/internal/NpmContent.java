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
package org.sonatype.nexus.repository.npm.internal;

import java.util.List;

import org.sonatype.nexus.repository.storage.MissingAssetBlobException;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.payloads.StreamPayload.InputStreamFunction;

import static java.util.Collections.singletonList;

/**
 * NPM focused {@link Content} allowing for setting {@link NpmStreamPayload} fields after creation.
 *
 * @since 3.next
 */
public class NpmContent
    extends Content
{
  private NpmStreamPayload payload;

  public NpmContent(final NpmStreamPayload payload) {
    super(payload);
    this.payload = payload;
  }

  public NpmContent packageId(final String packageId) {
    payload.packageId(packageId);
    return this;
  }

  public NpmContent revId(final String revId) {
    payload.revId(revId);
    return this;
  }

  public NpmContent fieldMatchers(final NpmFieldMatcher fieldMatcher) {
    return fieldMatchers(singletonList(fieldMatcher));
  }

  public NpmContent fieldMatchers(final List<NpmFieldMatcher> fieldMatchers) {
    payload.fieldMatchers(fieldMatchers);
    return this;
  }

  public NpmContent missingBlobInputStreamSupplier(
      final InputStreamFunction<MissingAssetBlobException> missingBlobInputStreamSupplier)
  {
    payload.missingBlobInputStreamSupplier(missingBlobInputStreamSupplier);
    return this;
  }
}
