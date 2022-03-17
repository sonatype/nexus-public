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
package org.sonatype.plexus.rest.xstream.xml;

import java.util.Arrays;

import org.sonatype.plexus.rest.xstream.xml.test.BaseDataObjectConverter;
import org.sonatype.plexus.rest.xstream.xml.test.DataObject1;
import org.sonatype.plexus.rest.xstream.xml.test.DataObject2;
import org.sonatype.plexus.rest.xstream.xml.test.TopLevelObject;
import org.sonatype.plexus.rest.xstream.xml.test.TopLevelObjectConverter;

import com.thoughtworks.xstream.XStream;
import junit.framework.Assert;
import junit.framework.TestCase;
import org.codehaus.plexus.util.StringUtils;

public class RunTest
    extends TestCase
{

  public void testRunExampleProblem() {
    // a few objects to parse
    TopLevelObject topLevel1 = new TopLevelObject();
    topLevel1.setId("ID1");

    DataObject1 data1 = new DataObject1();
    data1.setType("type-one");
    data1.setDataObjectField1("value1");
    data1.setDataObjectField2("value2");
    data1.setOtherField("otherField1");
    topLevel1.setData(data1);

    // another object with different subclass
    TopLevelObject topLevel2 = new TopLevelObject();
    topLevel2.setId("ID2");

    DataObject2 data2 = new DataObject2();
    data2.setType("type-two");
    data2.setDataObjectField3("value3");
    data2.setDataObjectField4("value3");
    data2.setOtherField("otherField2");
    topLevel2.setData(data2);

    // configure xstream
    XStream xstream = new XStream(new LookAheadXppDriver());
    // XStream xstream = new XStream( new XppDriver() );
    xstream.alias("top", TopLevelObject.class);
    xstream.registerConverter(new TopLevelObjectConverter(xstream.getMapper(), xstream.getReflectionProvider()));
    xstream.registerConverter(new BaseDataObjectConverter(xstream.getMapper(), xstream.getReflectionProvider()));
    Class[] types = new Class[] { DataObject1.class, DataObject2.class, TopLevelObject.class };
    xstream.allowTypes(types);
    xstream.processAnnotations(types);

    // xstream.aliasAttribute( "type", "class" );
    // xstream.alias( "type-one", DataObject1.class );

    // to XML
    String xml = xstream.toXML(topLevel1);
    System.out.println("xml:\n" + xml);

    // check XML
    Assert.assertFalse("XML String should not be empty: ", StringUtils.isEmpty(xml));
    Assert.assertTrue("XML should not contain attributes on data node.", xml.contains("<data>"));

    // from XML
    TopLevelObject result = (TopLevelObject) xstream.fromXML(xml);
    Assert.assertTrue("Expected data to be instance of DataObject1", result.getData() instanceof DataObject1);

    // again with another subclass
    xml = xstream.toXML(topLevel2);

    // check XML
    Assert.assertFalse("XML String should not be empty: ", StringUtils.isEmpty(xml));
    Assert.assertTrue("XML should not contain attributes on data node.", xml.contains("<data>"));

    result = (TopLevelObject) xstream.fromXML(xml);
    Assert.assertTrue("Expected data to be instance of DataObject2", result.getData() instanceof DataObject2);
  }

  public void testAttributes() {
    // a few objects to parse
    TopLevelObject topLevel1 = new TopLevelObject();
    topLevel1.setId("ID1");

    DataObject1 data1 = new DataObject1();
    data1.setType("type-one");
    data1.setDataObjectField1("value1");
    data1.setDataObjectField2("value2");
    data1.setOtherField("otherField1");
    data1.setDataList(Arrays.asList(new String[]{"one", "two", "three", "four"}));
    topLevel1.setData(data1);

    // configure xstream
    XStream xstream = new XStream(new LookAheadXppDriver());
    // XStream xstream = new XStream( new XppDriver() );
    xstream.alias("top", TopLevelObject.class);
    xstream.registerConverter(new TopLevelObjectConverter(xstream.getMapper(), xstream.getReflectionProvider()));
    xstream.registerConverter(new BaseDataObjectConverter(xstream.getMapper(), xstream.getReflectionProvider()));
    Class[] types = new Class[] { DataObject1.class, TopLevelObject.class };
    xstream.allowTypes(types);
    xstream.processAnnotations(types);

    // xstream.aliasAttribute( "type", "class" );
    // xstream.alias( "type-one", DataObject1.class );

    // to XML
    String xml1 = xstream.toXML(topLevel1);
    System.out.println("xml:\n" + xml1);

    // check XML
    Assert.assertFalse("XML String should not be empty: ", StringUtils.isEmpty(xml1));
    Assert.assertTrue("XML should not contain attributes on data node.", xml1.contains("<data>"));

    // from XML
    TopLevelObject result = (TopLevelObject) xstream.fromXML(xml1);
    Assert.assertTrue("Expected data to be instance of DataObject1", result.getData() instanceof DataObject1);

    // now back to xml so we can compare the 2
    String xml2 = xstream.toXML(result);

    Assert.assertEquals("Expected xml strings to be equal", xml1, xml2);
  }

}
