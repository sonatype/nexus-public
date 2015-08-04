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

import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.osgi.service.obr.Capability;
import org.osgi.service.obr.Requirement;


/**
 * Implements the Requirement interface.
 *
 * @version $Revision: 44 $
 */
public class RequirementImpl
    implements Requirement
{
  int id;

  String name;

  String filter = "()";

  FilterImpl _filter;

  String comment;

  boolean optional;

  boolean multiple;

  boolean extend;

  /**
   * Create a requirement with the given name.
   */
  public RequirementImpl(String name) {
    this.name = name;
  }


  /**
   * Parse the requirement from the pull parser.
   */
  public RequirementImpl(XmlPullParser parser) throws IOException, XmlPullParserException {
    parser.require(XmlPullParser.START_TAG, null, null);
    name = parser.getAttributeValue(null, "name");
    filter = parser.getAttributeValue(null, "filter");

    String opt = parser.getAttributeValue(null, "optional");
    String mul = parser.getAttributeValue(null, "multiple");
    String ext = parser.getAttributeValue(null, "extend");
    optional = "true".equalsIgnoreCase(opt);
    multiple = "true".equalsIgnoreCase(mul);
    extend = "true".equalsIgnoreCase(ext);


    StringBuffer sb = new StringBuffer();
    while (parser.next() == XmlPullParser.TEXT) {
      sb.append(parser.getText());
    }
    if (sb.length() > 0) {
      setComment(sb.toString().trim());
    }

    parser.require(XmlPullParser.END_TAG, null, null);
  }

  public void setFilter(String filter) {
    this.filter = filter;
    _filter = null;
  }

  public String getFilter() {
    return filter;
  }

  public Tag toXML(String name) {
    Tag tag = toXML(this);
    tag.rename(name);
    return tag;
  }


  public String getName() {
    return name;
  }

  public boolean isSatisfied(Capability capability) {
    if (_filter == null) {
      _filter = new FilterImpl(filter);
    }

    boolean result = _filter.match(capability.getProperties());
    return result;
  }

  public String toString() {
    return name + " " + filter;
  }


  public String getComment() {
    return comment;
  }


  public void setComment(String comment) {
    this.comment = comment;
  }


  public static Tag toXML(Requirement requirement) {
    Tag req = new Tag("require");
    req.addAttribute("name", requirement.getName());
    req.addAttribute("filter", requirement.getFilter());

    req.addAttribute("optional", requirement.isOptional() + "");
    req.addAttribute("multiple", requirement.isMultiple() + "");
    req.addAttribute("extend", requirement.isExtend() + "");

    if (requirement.getComment() != null) {
      req.addContent(requirement.getComment());
    }

    return req;
  }


  public boolean isMultiple() {
    return multiple;
  }


  public boolean isOptional() {
    return optional;
  }


  public void setOptional(boolean b) {
    optional = b;
  }

  public void setMultiple(boolean b) {
    multiple = b;
  }


  public boolean equals(Object o) {
    if (!(o instanceof Requirement)) {
      return false;
    }

    Requirement r2 = (Requirement) o;
    return filter.equals(r2.getFilter()) && name.equals(r2.getName());
  }

  public int hashCode() {
    return filter.hashCode() ^ name.hashCode();
  }

  public boolean isExtend() {
    return extend;
  }

  public void setExtend(boolean extend) {
    this.extend = extend;
  }
}
