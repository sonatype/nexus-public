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
package org.sonatype.nexus.repository.httpbridge.internal.describe;

import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.util.StringMultimap;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.Status;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.primitives.Primitives;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.nullToEmpty;

/**
 * Helper to render description information.
 *
 * @since 3.0
 */
@Named
@Singleton
public class DescriptionHelper
{
  public void describeRequest(final Description desc, final Request request) {
    desc.topic("Request");

    desc.addTable("Details", ImmutableMap.<String, Object>builder()
            .put("Action", request.getAction())
            .put("path", request.getPath()).build()
    );

    desc.addTable("Parameters", toMap(request.getParameters()));
    desc.addTable("Headers", toMap(request.getHeaders()));
    desc.addTable("Attributes", toMap(request.getAttributes()));

    if (request.isMultipart()) {
      Iterable<Payload> parts = request.getMultiparts();
      checkState(parts != null);
      for (Payload payload : parts) {
        desc.addTable("Payload", toMap(payload));
      }
    }
    else {
      if (request.getPayload() != null) {
        desc.addTable("Payload", toMap(request.getPayload()));
      }
    }
  }

  public void describeResponse(final Description desc, final Response response) {
    desc.topic("Response");

    final Status status = response.getStatus();
    desc.addTable("Status", ImmutableMap.of(
        "Code", (Object) status.getCode(),
        "Message", nullToEmpty(status.getMessage())
    ));

    desc.addTable("Headers", toMap(response.getHeaders()));
    desc.addTable("Attributes", toMap(response.getAttributes()));

    Payload payload = response.getPayload();
    if (payload != null) {
      desc.addTable("Payload", toMap(payload));
    }
  }

  private ImmutableMap<String, Object> toMap(final Payload payload) {
    return ImmutableMap.<String, Object>of(
        "Content-Type", nullToEmpty(payload.getContentType()),
        "Size", payload.getSize()
    );
  }

  public void describeException(final Description d, final Exception e) {
    d.topic("Exception during handler processing");

    for (Throwable cause : Throwables.getCausalChain(e)) {
      d.addTable(cause.getClass().getName(),
          ImmutableMap.<String, Object>of("Message", nullToEmpty(cause.getMessage())));
    }
  }

  private Map<String, Object> toMap(final Iterable<Entry<String, Object>> entries) {
    Map<String, Object> table = Maps.newHashMap();
    for (Entry<String, Object> entry : entries) {
      table.put(entry.getKey(), convert(entry.getValue()));
    }
    return table;
  }

  private Map<String, Object> toMap(final StringMultimap headers) {
    Map<String, Object> table = Maps.newHashMap();
    final Iterable<Entry<String, String>> entries = headers.entries();
    for (Entry<String, String> e : entries) {
      table.put(e.getKey(), e.getValue());
    }
    return table;
  }

  /**
   * Helper to convert value to string unless its a char-sequence or primitive/boxed-type.
   *
   * This is to help keep rendering JSON simple, and avoid side-effect with getter invocations when rendering.
   */
  private Object convert(final Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof CharSequence) {
      return value.toString();
    }
    if (value.getClass().isPrimitive() || Primitives.isWrapperType(value.getClass())) {
      return value;
    }
    return String.valueOf(value);
  }
}
