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

import org.sonatype.nexus.rest.index.MIndexerXStreamConfiguratorLightweight;
import org.sonatype.nexus.rest.model.XStreamConfigurator;
import org.sonatype.plexus.rest.xstream.json.JsonOrgHierarchicalStreamDriver;
import org.sonatype.plexus.rest.xstream.json.PrimitiveKeyedMapConverter;
import org.sonatype.plexus.rest.xstream.xml.LookAheadXppDriver;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.basic.StringConverter;

/**
 * XStream factory for Nexus Core. It gives away a preconfigured XStream to communicate with Core REST Resources.
 *
 * @author cstamas
 */
public class XStreamFactory
{

  public static XStream getXmlXStream() {
    XStream xmlXStream = new XStream(new LookAheadXppDriver());
    initXStream(xmlXStream);

    return xmlXStream;
  }

  public static XStream getJsonXStream() {
    XStream jsonXStream = new XStream(new JsonOrgHierarchicalStreamDriver());

    // for JSON, we use a custom converter for Maps
    jsonXStream.registerConverter(new PrimitiveKeyedMapConverter(jsonXStream.getMapper()));

    initXStream(jsonXStream);
    return jsonXStream;
  }

  private static void initXStream(XStream xstream) {
    XStreamConfigurator.configureXStream(xstream);
    MIndexerXStreamConfiguratorLightweight.configureXStream(xstream);

    // Nexus replaces the String converter with one that escape HTML, we do NOT want that on the IT client.
    xstream.registerConverter(new StringConverter());

    // We only do this because it is test code
    xstream.allowTypesByWildcard(new String[] {"**"});
  }
}
