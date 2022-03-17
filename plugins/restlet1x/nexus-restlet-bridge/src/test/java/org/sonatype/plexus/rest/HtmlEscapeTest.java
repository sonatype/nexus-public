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
package org.sonatype.plexus.rest;

import org.sonatype.plexus.rest.dto.Bbb;
import org.sonatype.plexus.rest.dto.One;
import org.sonatype.plexus.rest.dto.Two;
import org.sonatype.plexus.rest.xstream.HtmlEscapeStringConverter;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.basic.StringConverter;
import junit.framework.Assert;
import org.codehaus.plexus.PlexusTestCase;

public class HtmlEscapeTest
    extends PlexusTestCase
{
  public void testEscape() {
    XStream xstream = new XStream();
    xstream.registerConverter(new HtmlEscapeStringConverter());
    xstream.allowTypes(new Class[]{Two.class});

    Bbb bbb = new Bbb();
    bbb.setaValue("aaa-value");
    bbb.setbValue("bbb-value");

    Two twoObject = new Two();
    twoObject.setOneValue("one-value");
    twoObject.setTwoValue("interesting image: <img src=\"http://something.com/\" />");
    twoObject.setBbb(bbb);

    String stringedTwo = xstream.toXML(twoObject);
    Two copyOfTwo = (Two) xstream.fromXML(stringedTwo);

    Assert.assertEquals("interesting image: &lt;img src=&quot;http://something.com/&quot; /&gt;",
        copyOfTwo.getTwoValue());
  }

  public void testFieldThatIsNotEscaped() {
    XStream xstream = new XStream();
    xstream.registerConverter(new HtmlEscapeStringConverter());
    xstream.registerLocalConverter(One.class, "oneValue", new StringConverter());
    xstream.allowTypes(new Class[]{Two.class});

    // now make one field allow html characters
    Bbb bbb = new Bbb();
    bbb.setaValue("aaa-value");
    bbb.setbValue("bbb-value");

    Two twoObject = new Two();
    twoObject.setOneValue("allow html: <img src=\"http://something.com/\" />");
    twoObject.setTwoValue("interesting image: <img src=\"http://something.com/\" />");
    twoObject.setBbb(bbb);

    String stringedTwo = xstream.toXML(twoObject);
    Two copyOfTwo = (Two) xstream.fromXML(stringedTwo);

    Assert.assertEquals("interesting image: &lt;img src=&quot;http://something.com/&quot; /&gt;",
        copyOfTwo.getTwoValue());
    Assert.assertEquals("allow html: <img src=\"http://something.com/\" />", copyOfTwo.getOneValue());

  }

}
