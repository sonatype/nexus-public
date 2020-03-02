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
 * View response.
 *
 * @see Response.Builder
 * @since 3.0
 */
public class Response
{
  private final AttributesMap attributes;

  private final Headers headers;

  private final Status status;

  private final Payload payload;

  private Response(final AttributesMap attributes,
                   final Headers headers,
                   final Status status,
                   @Nullable final Payload payload)
  {
    this.attributes = attributes;
    this.headers = headers;
    this.status = status;
    this.payload = payload;
  }

  public AttributesMap getAttributes() {
    return attributes;
  }

  public Headers getHeaders() {
    return headers;
  }

  public Status getStatus() {
    return status;
  }

  @Nullable
  public Payload getPayload() {
    return payload;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "status=" + status +
        ", payload=" + payload +
        '}';
  }

  //
  // Builder
  //

  /**
   * {@link Response} builder.
   */
  public static class Builder
  {
    private AttributesMap attributes;

    private Headers headers;

    private Status status;

    private Payload payload;

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

    public Builder status(final Status status) {
      this.status = status;
      return this;
    }

    public Builder payload(final Payload payload) {
      this.payload = payload;
      return this;
    }

    public Builder copy(final Response response) {
      checkNotNull(response);
      attributes = response.getAttributes();
      headers = response.getHeaders();
      status = response.getStatus();
      payload = response.getPayload();
      return this;
    }

    /**
     * Requires {@link #status}.
     */
    public Response build() {
      checkState(status != null, "Missing: status");

      return new Response(attributes(), headers(), status, payload);
    }
  }
}
