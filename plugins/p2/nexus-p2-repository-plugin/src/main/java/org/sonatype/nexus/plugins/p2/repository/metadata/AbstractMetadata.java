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
package org.sonatype.nexus.plugins.p2.repository.metadata;

import java.util.LinkedHashMap;
import java.util.Map;

import org.codehaus.plexus.util.xml.Xpp3Dom;

public abstract class AbstractMetadata
{
  protected final Xpp3Dom dom;

  protected AbstractMetadata(final Xpp3Dom dom) {
    this.dom = dom;
  }

  protected AbstractMetadata(final AbstractMetadata other) {
    dom = new Xpp3Dom(other.dom);
  }

  public Xpp3Dom getDom() {
    return dom;
  }

  public static void removeChild(final Xpp3Dom dom, final String name) {
    Xpp3Dom[] children = dom.getChildren();
    for (int i = 0; i < children.length; ) {
      if (name.equals(children[i].getName())) {
        dom.removeChild(i);
        children = dom.getChildren();
      }
      else {
        i++;
      }
    }
  }

  public LinkedHashMap<String, String> getProperties() {
    final LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();

    final Xpp3Dom propertiesDom = dom.getChild("properties");

    if (propertiesDom != null) {
      for (final Xpp3Dom propertyDom : propertiesDom.getChildren("property")) {
        result.put(propertyDom.getAttribute("name"), propertyDom.getAttribute("value"));
      }
    }

    return result;
  }

  public void setProperties(final LinkedHashMap<String, String> properties) {
    removeChild(dom, "properties");

    final Xpp3Dom propertiesDom = new Xpp3Dom("properties");

    for (final Map.Entry<String, String> property : properties.entrySet()) {
      final Xpp3Dom propertyDom = new Xpp3Dom("property");
      propertyDom.setAttribute("name", property.getKey());
      propertyDom.setAttribute("value", property.getValue());
      propertiesDom.addChild(propertyDom);
    }

    propertiesDom.setAttribute("size", Integer.toString(properties.size()));
    dom.addChild(propertiesDom);
  }

}
