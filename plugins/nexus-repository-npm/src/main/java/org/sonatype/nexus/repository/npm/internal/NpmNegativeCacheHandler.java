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

import org.sonatype.nexus.repository.cache.NegativeCacheHandler;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.Status;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher.State;

import org.apache.http.HttpStatus;

/**
 * Npm specific handling for negative cache responses.
 *
 * @since 3.0
 */
public class NpmNegativeCacheHandler
    extends NegativeCacheHandler
{
  @Override
  protected Response buildResponse(final Status status, final Context context) {
    if (status.getCode() == HttpStatus.SC_NOT_FOUND) {
      State state = context.getAttributes().require(TokenMatcher.State.class);
      NpmPackageId packageId = NpmHandlers.packageId(state);
      return NpmResponses.packageNotFound(packageId);
    }
    return super.buildResponse(status, context);
  }
}
