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
package org.sonatype.nexus.rest.feeds;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedOutput;
import org.restlet.data.MediaType;
import org.restlet.resource.StreamRepresentation;

/**
 * A restlet.org representation that represents an RSS feed. Representations are instantaniated per request, hance the
 * RssConverter should be stored somewhere to be reusable (like in restlet Context) since it is thread-safe.
 *
 * @author cstamas
 */
public class FeedRepresentation
    extends StreamRepresentation
{
  public static final MediaType RSS_MEDIA_TYPE = new MediaType("application/rss+xml", "RSS syndication documents");

  public static final MediaType ATOM_MEDIA_TYPE = MediaType.APPLICATION_ATOM_XML;

  private SyndFeed feed;

  public FeedRepresentation(MediaType mediaType, SyndFeed feed) {
    super(mediaType);

    this.feed = feed;
  }

  @Override
  public InputStream getStream()
      throws IOException
  {
    return null;
  }

  public void write(OutputStream outputStream)
      throws IOException
  {
    try {
      Writer w = new OutputStreamWriter(outputStream);

      SyndFeedOutput output = new SyndFeedOutput();

      output.output(feed, w);
    }
    catch (FeedException e) {
      throw new RuntimeException("Got exception while generating feed!", e);
    }
  }
}
