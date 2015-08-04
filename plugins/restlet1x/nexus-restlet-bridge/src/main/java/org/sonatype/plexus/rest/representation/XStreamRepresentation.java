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
package org.sonatype.plexus.rest.representation;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import org.restlet.data.CharacterSet;
import org.restlet.data.Language;
import org.restlet.data.MediaType;
import org.restlet.resource.StringRepresentation;

/**
 * A String representation powered by XStream. This Representation needs XStream instance in constructor, and it is
 * best
 * if you share your (threadsafe) XStream instance in restlet Application's Context, for example.
 *
 * @author cstamas
 */
public class XStreamRepresentation
    extends StringRepresentation
{
  private XStream xstream;

  public XStreamRepresentation(XStream xstream, String text, MediaType mt, Language language,
                               CharacterSet characterSet)
  {
    super(text, mt, language, characterSet);

    this.xstream = xstream;
  }

  public XStreamRepresentation(XStream xstream, String text, MediaType mt, Language language) {
    this(xstream, text, mt, language, CharacterSet.UTF_8);
  }

  public XStreamRepresentation(XStream xstream, String text, MediaType mt) {
    this(xstream, text, mt, null);
  }

  public Object getPayload(Object root)
      throws XStreamException
  {
    // TODO: A BIG HACK FOLLOWS, UNTIL WE DO NOT RESOLVE XSTREAM HINTING!
    // In case of JSON reading (since JSON is not self-describing), we are adding
    // and "enveloping" object manually to incoming data.
    if (MediaType.APPLICATION_JSON.equals(getMediaType(), true)) {
      // it is JSON, applying hack, adding "envelope" object
      StringBuffer sb =
          new StringBuffer("{ \"").append(root.getClass().getName()).append("\" : ").append(getText()).append(
              " }");

      return xstream.fromXML(sb.toString(), root);
    }
    else {
      // it is XML or something else
      return xstream.fromXML(getText(), root);
    }
  }

  public void setPayload(Object object) {
    setText(xstream.toXML(object));
  }
}
