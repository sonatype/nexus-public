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
package org.sonatype.nexus.repository.view;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.function.UnaryOperator;

import javax.annotation.Nonnull;

import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.view.Request.Builder;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.sonatype.nexus.repository.view.ViewUtils.copyLocalContextAttributes;

/**
 * View context.
 *
 * @since 3.0
 */
public class Context
{
  private static final Logger log = LoggerFactory.getLogger(Context.class);

  private final AttributesMap attributes = new AttributesMap();

  private final Repository repository;

  private final Request request;

  private ListIterator<Handler> handlers;

  public Context(
      final Repository repository,
      final Request request)
  {
    this.repository = checkNotNull(repository);
    this.request = checkNotNull(request);
  }

  public AttributesMap getAttributes() {
    return attributes;
  }

  public Repository getRepository() {
    return repository;
  }

  public Request getRequest() {
    return request;
  }

  /**
   * Invokes the next handler in the handler chain.
   *
   * Interceptor-style handlers should invoke this from their {@link Handler#handle(Context)} method and return the
   * result.
   */
  @Nonnull
  public Response proceed() throws Exception {
    checkState(handlers != null, "Context not started");
    checkState(handlers.hasNext(), "End of handler chain");

    // Invoke next handler
    Handler handler = handlers.next();
    try {
      log.debug("Proceeding: {}", handler);
      return handler.handle(this);
    }
    finally {
      // retain handler position in-case of re-proceed
      if (handlers.hasPrevious()) {
        handlers.previous();
      }
    }
  }

  /**
   * Add an additional handler to the context, immediately after the current handler.
   */
  public void insertHandler(final Handler handler) {
    checkNotNull(handler);
    // Insert the handler so that the next proceed() call will encounter it
    handlers.add(handler);
    handlers.previous();
  }

  /**
   * Builds a context that contains a request that can be "replayed" with its post body content. We need to have a new
   * request instance with a payload we can read multiple times.
   */
  public Context replayable() throws IOException {
    Payload payload = checkNotNull(request.getPayload());
    try (InputStream in = payload.openInputStream()) {
      byte[] content = IOUtils.toByteArray(in);
      Context replayableContext = new Context(repository,
          new Builder()
              .attributes(request.getAttributes())
              .headers(request.getHeaders())
              .action(request.getAction())
              .path(request.getPath())
              .parameters(request.getParameters())
              .payload(new BytesPayload(content, payload.getContentType()))
              .build());
      copyLocalContextAttributes(this, replayableContext);

      return replayableContext;
    }
  }

  /**
   * Builds a context that contains a request that can be "replayed" with its post body content. We need to have a new
   * request instance with a payload we can read multiple times.
   */
  public Context copy(
      final UnaryOperator<AttributesMap> attributesCustomizer,
      final UnaryOperator<Request.Builder> requestCustomizer) throws IOException
  {
    final Request.Builder builder = new Builder()
        .attributes(request.getAttributes())
        .headers(request.getHeaders())
        .action(request.getAction())
        .path(request.getPath())
        .parameters(request.getParameters());

    if (request.getPayload() != null) {
      final Payload payload = request.getPayload();
      try (InputStream in = payload.openInputStream()) {
        byte[] content = IOUtils.toByteArray(in);
        builder.payload(new BytesPayload(content, payload.getContentType()));
      }
    }

    final Request newRequest = requestCustomizer.apply(builder).build();

    final Context contextCopy = new Context(repository, newRequest);

    final AttributesMap newAttributes = attributesCustomizer.apply(attributes);
    contextCopy.getAttributes().clear();
    newAttributes.forEach(e -> contextCopy.getAttributes().set(e.getKey(), e.getValue()));

    return contextCopy;
  }

  //
  // Framework internal
  //

  /**
   * Start route.
   */
  @Nonnull
  Response start(final Route route) throws Exception {
    checkNotNull(route);
    checkState(handlers == null, "Already started");
    log.debug("Starting: {}", route);
    // Copy the handler list to allow modification
    this.handlers = new ArrayList<>(route.getHandlers()).listIterator();
    return proceed();
  }
}
