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
package org.sonatype.nexus.test.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

import org.sonatype.nexus.integrationtests.RequestFacade;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.junit.Assert;
import org.restlet.data.Method;
import org.restlet.data.Response;

public class FeedUtil
{
  private static final String FEED_URL_PART = "service/local/feeds/";

  public static SyndFeed getFeed(String feedId)
      throws IllegalArgumentException, FeedException, IOException
  {
    return getFeed(feedId, null, null, null);
  }

  public static SyndFeed getFeed(String feedId, int from, int count)
      throws IllegalArgumentException, FeedException, IOException
  {
    return getFeed(feedId, from, count, null);
  }

  public static SyndFeed getFeed(final String feedId, final Integer from, final Integer count,
                                 final Map<String, String> params)
      throws IllegalArgumentException, FeedException, IOException
  {
    final StringBuilder sb = new StringBuilder();
    sb.append("?_dc=" + System.currentTimeMillis());
    if (from != null) {
      sb.append("&from=" + from);
    }
    if (count != null) {
      sb.append("&count=" + count);
    }
    if (params != null && !params.isEmpty()) {
      for (Map.Entry<String, String> entry : params.entrySet()) {
        sb.append("&" + entry.getKey());
        if (entry.getValue() != null) {
          sb.append("=" + entry.getValue());
        }
      }
    }
    final Response response =
        RequestFacade.sendMessage(FEED_URL_PART + feedId + sb.toString(), Method.GET);
    final String text = response.getEntity().getText();
    Assert.assertTrue("Unexpected response: " + text, response.getStatus().isSuccess());

    return new SyndFeedInput().build(new XmlReader(new ByteArrayInputStream(text.getBytes())));
  }
}
