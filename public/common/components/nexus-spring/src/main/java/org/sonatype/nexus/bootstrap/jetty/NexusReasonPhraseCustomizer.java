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
package org.sonatype.nexus.bootstrap.jetty;

import java.nio.ByteBuffer;

import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpStream;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.Callback;

/**
 * Restores custom HTTP/1.1 reason-phrase support using only public Jetty 12 API (STL-476).
 *
 * <p>
 * Jetty 12 removed the ability to set a custom HTTP/1.1 reason phrase. Nexus relies on it so that
 * error/quarantine responses carry a descriptive status line for HTTP/1.1 clients (e.g. Maven 3.9.x, npm),
 * mirrored in the body for HTTP/2 (NEXUS-53528). This previously required a source fork of several internal
 * Jetty classes ({@code public/common/components/thirdparty/jetty-modifications}); this customizer replaces
 * that fork.
 *
 * <p>
 * A producer that wants a custom reason phrase sets the {@link #REASON_PHRASE_HEADER} header on the
 * response (its value is the reason phrase). This customizer, registered on the shared
 * {@code HttpConfiguration} in {@code jetty.xml}, installs a per-request {@link HttpStream.Wrapper} that, at
 * response commit, moves that header value into the {@link MetaData.Response} reason (and strips the header)
 * so Jetty's HTTP/1.1 generator serializes it on the status line. HTTP/2 has no reason phrase, so the value
 * is ignored there (per RFC).
 *
 * <p>
 * The header is carried on the response (not a request attribute) because that is the object the stream
 * wrapper sees at commit time, avoiding servlet-vs-core request-attribute scoping issues.
 */
public class NexusReasonPhraseCustomizer
    implements HttpConfiguration.Customizer
{
  /**
   * Response header whose value becomes the HTTP/1.1 reason phrase. It is consumed (removed) by this
   * customizer and never sent to the client.
   */
  public static final String REASON_PHRASE_HEADER = "X-Nexus-Reason-Phrase";

  @Override
  public Request customize(final Request request, final HttpFields.Mutable responseHeaders) {
    request.addHttpStreamWrapper(ReasonPhraseStream::new);
    return request;
  }

  /**
   * Moves the {@link #REASON_PHRASE_HEADER} value into the response reason at commit time, when a reason is
   * present and Jetty has not already set one.
   */
  private static final class ReasonPhraseStream
      extends HttpStream.Wrapper
  {
    ReasonPhraseStream(final HttpStream wrapped) {
      super(wrapped);
    }

    @Override
    public void send(
        final MetaData.Request metaRequest,
        MetaData.Response metaResponse,
        final boolean last,
        final ByteBuffer content,
        final Callback callback)
    {
      if (metaResponse != null) {
        HttpFields fields = metaResponse.getHttpFields();
        if (fields != null && fields.contains(REASON_PHRASE_HEADER)) {
          // Always strip the internal sentinel so it never reaches the client, even when it carries no usable value.
          HttpFields stripped = HttpFields.build(fields).remove(REASON_PHRASE_HEADER);
          String reason = sanitizeReasonPhrase(fields.get(REASON_PHRASE_HEADER));
          // A usable sentinel is an explicit request to set the reason phrase, so it always wins over any canonical
          // reason Jetty may have populated (e.g. "Conflict") at the servlet layer; an empty or control-only value
          // falls back to Jetty's default reason.
          String effectiveReason = (reason != null && !reason.isEmpty()) ? reason : metaResponse.getReason();
          metaResponse = new MetaData.Response(
              metaResponse.getStatus(),
              effectiveReason,
              metaResponse.getHttpVersion(),
              stripped,
              metaResponse.getContentLength(),
              metaResponse.getTrailersSupplier());
        }
      }
      super.send(metaRequest, metaResponse, last, content, callback);
    }

    /**
     * Removes CR, LF and other C0 control characters (and DEL) so a producer-supplied value cannot inject
     * extra headers or split the HTTP/1.1 status line: Jetty's generator writes the reason phrase verbatim.
     * A {@code null} value is returned unchanged; a value reduced to empty falls back to Jetty's default reason.
     */
    private static String sanitizeReasonPhrase(final String value) {
      if (value == null) {
        return null;
      }
      StringBuilder sanitized = null;
      for (int i = 0; i < value.length(); i++) {
        char c = value.charAt(i);
        if (c < 0x20 || c == 0x7f) {
          if (sanitized == null) {
            sanitized = new StringBuilder(value.length()).append(value, 0, i);
          }
        }
        else if (sanitized != null) {
          sanitized.append(c);
        }
      }
      return sanitized == null ? value : sanitized.toString();
    }
  }
}
