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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.sonatype.nexus.common.collect.AttributesMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * View request.
 *
 * @see Request.Builder
 * @since 3.0
 */
public class Request
{
  private final AttributesMap attributes;

  private final Headers headers;

  private final String action;

  private final String path;

  private final Parameters parameters;

  private final Payload payload;

  private final boolean multipart;

  private final Iterable<PartPayload> multiPayloads;

  private Request(final AttributesMap attributes,
                  final Headers headers,
                  final String action,
                  final String path,
                  final Parameters parameters,
                  @Nullable final Payload payload,
                  final boolean multipart,
                  @Nullable final Iterable<PartPayload> multiPayloads)
  {
    this.attributes = checkNotNull(attributes);
    this.headers = checkNotNull(headers);
    this.action = checkNotNull(action);
    this.path = checkNotNull(path);
    this.parameters = checkNotNull(parameters);
    this.payload = payload;
    this.multipart = multipart;
    this.multiPayloads = multiPayloads;
  }

  public AttributesMap getAttributes() {
    return attributes;
  }

  public Headers getHeaders() {
    return headers;
  }

  public String getAction() {
    return action;
  }

  public String getPath() {
    return path;
  }

  public Parameters getParameters() {
    return parameters;
  }

  @Nullable
  public Payload getPayload() {
    return payload;
  }

  public boolean isMultipart() {
    return multipart;
  }

  @Nullable
  public Iterable<PartPayload> getMultiparts() {
    return multiPayloads;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "action='" + action + '\'' +
        ", path='" + path + '\'' +
        ", parameters=" + parameters +
        ", payload=" + payload +
        ", multipart=" + multipart +
        '}';
  }

  //
  // Builder
  //

  /**
   * {@link Request} builder.
   */
  public static class Builder
  {
    private AttributesMap attributes;

    private Headers headers;

    private String action;

    private String path;

    private Parameters parameters;

    private Payload payload;

    private boolean multipart;

    private Iterable<PartPayload> multiparts;

    public Builder attributes(final AttributesMap attributes) {
      this.attributes = attributes;
      return this;
    }

    @Nonnull
    public AttributesMap attributes() {
      if (attributes == null) {
        attributes = new AttributesMap();
      }
      return attributes;
    }

    public Builder attribute(final String name, final Object value) {
      attributes().set(name, value);
      return this;
    }

    public Builder headers(final Headers headers) {
      this.headers = headers;
      return this;
    }

    @Nonnull
    public Headers headers() {
      if (headers == null) {
        headers = new Headers();
      }
      return headers;
    }

    public Builder header(final String name, final String... values) {
      headers().set(name, values);
      return this;
    }

    public Builder action(final String action) {
      this.action = action;
      return this;
    }

    public Builder path(final String path) {
      this.path = path;
      return this;
    }

    public Builder parameters(final Parameters parameters) {
      this.parameters = parameters;
      return this;
    }

    @Nonnull
    public Parameters parameters() {
      if (parameters == null) {
        parameters = new Parameters();
      }
      return parameters;
    }

    public Builder parameter(final String name, final String... values) {
      parameters().set(name, values);
      return this;
    }

    public Builder payload(final Payload payload) {
      this.payload = payload;
      return this;
    }

    public Builder multipart(final boolean multipart) {
      this.multipart = multipart;
      return this;
    }

    public Builder multiparts(final Iterable<PartPayload> multiparts) {
      this.multiparts = multiparts;
      this.multipart = multiparts != null;
      return this;
    }

    public Builder copy(final Request request) {
      checkNotNull(request);
      attributes = request.getAttributes();
      headers = request.getHeaders();
      action = request.getAction();
      path = request.getPath();
      parameters = request.getParameters();
      payload = request.getPayload();
      multipart = request.isMultipart();
      multiparts = request.getMultiparts();
      return this;
    }

    /**
     * Requires {@link #action} and {@link #path}.
     */
    public Request build() {
      checkState(action != null, "Missing: action");
      checkState(path != null, "Missing: path");

      return new Request(attributes(), headers(), action, path, parameters(), payload, multipart, multiparts);
    }
  }
}
