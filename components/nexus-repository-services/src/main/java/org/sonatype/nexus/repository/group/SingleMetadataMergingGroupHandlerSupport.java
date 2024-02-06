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
package org.sonatype.nexus.repository.group;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Response;

/**
 * Merges responses from member repositories.
 */
public abstract class SingleMetadataMergingGroupHandlerSupport
    extends MergingGroupHandlerSupport
{
  @Override
  protected Optional<Content> merge(
      final Context context,
      final Map<Repository, Response> successfulResponses,
      final Optional<String> optEtag) throws IOException
  {
    Optional<Content> result = merge(successfulResponses.values()).map(Content::new);

    if (result.isPresent() && optEtag.isPresent()) {
      result.map(Content::getAttributes)
          .ifPresent(attributes -> attributes.set(Content.CONTENT_ETAG, optEtag.get()));

      result = Optional.of(store(context, result.get()));
    }
    else {
      log.debug("Unable to compute lastModified or value");
    }

    return result;
  }

  /**
   * Consume & merge the responses provided to generate the merged metadata.
   *
   * @param successfulResponses a list of successful responses to be merged
   * @return a constructed payload, implementors should not persist the value
   */
  protected abstract Optional<Payload> merge(Collection<Response> successfulResponses);

  /**
   * Persist the content merged for the context
   *
   * @param context the request context
   * @param content the content to persist
   * @return the persisted content
   * @throws IOException
   */
  protected abstract Content store(Context context, Content content) throws IOException;
}
