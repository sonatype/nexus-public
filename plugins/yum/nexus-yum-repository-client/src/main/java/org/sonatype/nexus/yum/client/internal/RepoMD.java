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
package org.sonatype.nexus.yum.client.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since yum 3.0
 */
public class RepoMD
{

  private static final Logger log = LoggerFactory.getLogger(RepoMD.class);

  private final Map<String, String> locations;

  public RepoMD(final InputStream in) {
    locations = parse(in);
    log.debug("Locations: {}", locations);
  }

  public RepoMD(final File file) {
    BufferedInputStream in = null;
    try {
      in = new BufferedInputStream(new FileInputStream(file));
      locations = parse(in);
      log.debug("Locations: {}", locations);
    }
    catch (FileNotFoundException e) {
      throw Throwables.propagate(e);
    }
    finally {
      IOUtils.closeQuietly(in);
    }
  }

  private static Map<String, String> parse(final InputStream in) {
    try {
      final Map<String, String> locations = Maps.newHashMap();
      final Xpp3Dom dom = Xpp3DomBuilder.build(new XmlStreamReader(in));

      for (final Xpp3Dom data : dom.getChildren("data")) {
        final Xpp3Dom location = data.getChild("location");
        final String type = data.getAttribute("type");
        final String href = location.getAttribute("href");

        locations.put(type, href);
      }
      return locations;
    }
    catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  public Collection<String> getLocations() {
    return locations.values();
  }

  public String getLocation(final String type) {
    return locations.get(type);
  }

  public String getPrimaryLocation() {
    return getLocation("primary");
  }

}
