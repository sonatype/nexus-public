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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import org.sonatype.nexus.NexusAppTestSupport;
import org.sonatype.nexus.rest.feeds.sources.FeedSource;
import org.sonatype.plexus.rest.resource.PlexusResource;

import com.rometools.rome.feed.synd.SyndFeed;
import junit.framework.Assert;
import org.junit.Test;
import org.restlet.Context;
import org.restlet.data.Request;

public class FeedTest
    extends NexusAppTestSupport
{
  @Test
  public void testFeedSources()
      throws Exception
  {
    Map<String, FeedSource> map = this.getContainer().lookupMap(FeedSource.class);

    System.out.println("map: " + map);

    FeedPlexusResource feedResource = (FeedPlexusResource) this.lookup(PlexusResource.class, FeedPlexusResource.class.getName());

    // need to test the protected method, its a little hacky, but the problem i am trying to test has to do with
    // Plexus loading this class
    // so subclassing to expose this method, sort of get around what i am trying to test.

    // System.out.println( "feedResource: " + feedResource );

    Field feedField = AbstractFeedPlexusResource.class.getDeclaredField("feeds");
    feedField.setAccessible(true);
    Map<String, FeedSource> feeds = (Map<String, FeedSource>) feedField.get(feedResource);

    Assert.assertNotNull(feeds);

    Method getFeedMethod =
        feedResource.getClass().getDeclaredMethod("getFeed", Context.class, Request.class, String.class,
            Integer.class, Integer.class, Map.class);

    SyndFeed feed = (SyndFeed) getFeedMethod.invoke(feedResource, null, null, "brokenArtifacts", null, null, null);

    Assert.assertNotNull(feed);

  }

}
