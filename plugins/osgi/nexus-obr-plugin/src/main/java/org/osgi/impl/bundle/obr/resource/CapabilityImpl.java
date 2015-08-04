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
package org.osgi.impl.bundle.obr.resource;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.osgi.service.obr.Capability;


public class CapabilityImpl
    implements Capability
{
  String name;

  Map properties = new TreeMap();

  public CapabilityImpl(String name) {
    this.name = name;
  }

  public CapabilityImpl(XmlPullParser parser) throws IOException, XmlPullParserException {
    parser.require(XmlPullParser.START_TAG, null, "capability");
    name = parser.getAttributeValue(null, "name");
    while (parser.nextTag() == XmlPullParser.START_TAG) {
      if (parser.getName().equals("p")) {
        String name = parser.getAttributeValue(null, "n");
        String value = parser.getAttributeValue(null, "v");
        String type = parser.getAttributeValue(null, "t");
        Object v = value;

        if ("nummeric".equals(type)) {
          v = new Long(value);
        }
        else if ("version".equals(type)) {
          v = new VersionRange(value);
        }
        addProperty(name, v);
      }
      parser.next();
      parser.require(XmlPullParser.END_TAG, null, "p");
    }
    parser.require(XmlPullParser.END_TAG, null, "capability");
  }


  public void addProperty(String key, Object value) {
    List values = (List) properties.get(key);
    if (values == null) {
      values = new ArrayList();
      properties.put(key, values);
    }
    values.add(value);
  }

  public Tag toXML() {
    return toXML(this);
  }

  public static Tag toXML(Capability capability) {
    Tag tag = new Tag("capability");
    tag.addAttribute("name", capability.getName());
    Map properties = capability.getProperties();
    for (Iterator k = properties.keySet().iterator(); k.hasNext(); ) {
      String key = (String) k.next();
      List values = (List) properties.get(key);
      for (Iterator v = values.iterator(); v.hasNext(); ) {
        Object value = v.next();
        Tag p = new Tag("p");
        tag.addContent(p);
        p.addAttribute("n", key);
        if (value != null) {
          p.addAttribute("v", valueString(value));
          String type = null;
          if (value instanceof Number) {
            type = "number";
          }
          else if (value.getClass() == VersionRange.class) {
            type = "version";
          }
          else if (value.getClass().isArray()) {
            type = "set";
          }

          if (type != null) {
            p.addAttribute("t", type);
          }
        }
/*[mcculls] ignore
                                else
					System.out.println("Missing value " + key);
*/
      }
    }
    return tag;
  }


  private static String valueString(Object value) {
    if (value.getClass().isArray()) {
      StringBuffer buf = new StringBuffer();
      for (int i = 0; i < Array.getLength(value); i++) {
        if (i > 0) {
          buf.append(",");
        }
        buf.append(Array.get(value, i).toString());
      }
      return buf.toString();
    }
    else {
      return value.toString();
    }
  }

  public String getName() {
    return name;
  }


  public Map getProperties() {
    return properties;
  }

}
