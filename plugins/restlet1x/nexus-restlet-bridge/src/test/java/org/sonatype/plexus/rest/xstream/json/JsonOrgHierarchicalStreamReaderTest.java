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
package org.sonatype.plexus.rest.xstream.json;

import java.io.IOException;

import com.thoughtworks.xstream.XStream;
import org.codehaus.plexus.PlexusTestCase;

public class JsonOrgHierarchicalStreamReaderTest
    extends PlexusTestCase
{

  protected XStream xstream;

  protected void prepare()
      throws IOException
  {
    this.xstream = new XStream(new JsonOrgHierarchicalStreamDriver());

    this.xstream.registerConverter(new PrimitiveKeyedMapConverter(xstream.getMapper()));

    this.xstream.processAnnotations(new Class[] {
        OneValued.class,
        CombinedValued.class,
        ThreeValued.class
    });
  }

  protected void setUp()
      throws Exception
  {
    super.setUp();

    prepare();
  }

  protected Object deserialize(String o)
      throws IOException
  {
    return xstream.fromXML(o);
  }

  protected void deserialize(String o, Object root)
      throws IOException
  {
    xstream.fromXML(o, root);
  }

  public void testStringBoolean()
      throws IOException
  {
    OneValued one = new OneValued();
    deserialize("{ \"org.sonatype.plexus.rest.xstream.json.OneValued\" : { \"stringValue\" : \"true\" }}", one);
    assertEquals("true", one.stringValue);
  }

  public void testSimple()
      throws IOException
  {
    OneValued one = new OneValued();
    deserialize("{ \"org.sonatype.plexus.rest.xstream.json.OneValued\" : { \"stringValue\" : \"something\" }}", one);
    assertEquals("something", one.stringValue);

    ThreeValued three = new ThreeValued();
    deserialize(
        "{ \"org.sonatype.plexus.rest.xstream.json.OneValued\" : { \"stringValue\" : \"something\", \"boolValue\" : true, \"intValue\" : 1975 }}",
        three);
    assertEquals("something", three.stringValue);
    assertEquals(true, three.boolValue);
    assertEquals(1975, three.intValue);

  }

  public void testCombined()
      throws IOException
  {
    CombinedValued co = new CombinedValued();
    deserialize(
        "{ \"org.sonatype.plexus.rest.xstream.json.CombinedValued\" : {\"stringValue\":\"custom object\",\"ints\":[1,2,3],\"objectsList\":[\"oneList\",\"twoList\",\"threeList\"],\"objectMap\":{\"key\":\"value\",\"aNumber\":1975}}}",
        co);
    // deserialize(
    // "{ \"org.sonatype.plexus.rest.channel.json.CombinedValued\" : {\"stringValue\":\"custom
    // object\",\"ints\":[1,2,3],\"objectsList\":[\"oneList\",\"twoList\",\"threeList\"],\"objectMap\":{\"entry\":{\"string\":\"key\",\"string\":\"value\"},\"entry\":{\"string\":\"aNumber\",\"int\":1975}}}}",
    // co );

    assertEquals("custom object", co.stringValue);
    assertEquals(3, co.ints.length);
    assertEquals(1, co.ints[0]);
    assertEquals(2, co.ints[1]);
    assertEquals(3, co.ints[2]);
    assertEquals(3, co.objectsList.size());
    assertEquals("oneList", co.objectsList.get(0));
    assertEquals("twoList", co.objectsList.get(1));
    assertEquals("threeList", co.objectsList.get(2));

    // XXX: we have a bug here
    // we should avoid Maps serialized like in second example in JSON (using xstream default converter)!
    // XXX maps works only with PrimitiveKeyedMapConverter registered with XStream
    // XXX test will fail if using default XStream converter!
    assertEquals(2, co.objectMap.size());
    assertEquals("value", co.objectMap.get("key"));
    assertEquals(1975, co.objectMap.get("aNumber"));
  }

  public void testCombinedWithNullValues()
      throws IOException
  {
    CombinedValued co = new CombinedValued();
    deserialize(
        "{ \"org.sonatype.plexus.rest.xstream.json.CombinedValued\" : {\"stringValue\":null,\"ints\":[1,2,3],\"objectsList\":[\"oneList\",\"twoList\",\"threeList\"]}}",
        co);

    // string value should be null
    assertEquals(null, co.stringValue);
    assertEquals(3, co.ints.length);
    assertEquals(1, co.ints[0]);
    assertEquals(2, co.ints[1]);
    assertEquals(3, co.ints[2]);
    assertEquals(3, co.objectsList.size());
    assertEquals("oneList", co.objectsList.get(0));
    assertEquals("twoList", co.objectsList.get(1));
    assertEquals("threeList", co.objectsList.get(2));

    // XXX: we have a bug here
    // we should avoid Maps serialized like in second example in JSON (using xstream default converter)!
    // XXX maps works only with PrimitiveKeyedMapConverter registered with XStream
    // XXX test will fail if using default XStream converter!
    assertEquals(null, co.objectMap);

    co = new CombinedValued();
    deserialize(
        "{ \"org.sonatype.plexus.rest.xstream.json.CombinedValued\" : {\"stringValue\":\"hyy\",\"ints\":[],\"objectsList\":[null,\"twoList\",\"threeList\"]}}",
        co);

    // string value should be null
    assertEquals("hyy", co.stringValue);
    assertEquals(0, co.ints.length);
    assertEquals(3, co.objectsList.size());
    assertEquals(null, co.objectsList.get(0));
    assertEquals("twoList", co.objectsList.get(1));
    assertEquals("threeList", co.objectsList.get(2));

    // XXX: we have a bug here
    // we should avoid Maps serialized like in second example in JSON (using xstream default converter)!
    // XXX maps works only with PrimitiveKeyedMapConverter registered with XStream
    // XXX test will fail if using default XStream converter!
    assertEquals(null, co.objectMap);

    co = new CombinedValued();
    deserialize(
        "{ \"org.sonatype.plexus.rest.xstream.json.CombinedValued\" : {\"stringValue\":null,\"ints\":[],\"objectsList\":[\"oneList\",\"twoList\",\"threeList\"]}}",
        co);

    // string value should be null
    assertEquals(null, co.stringValue);
    assertEquals(0, co.ints.length);
    assertEquals(3, co.objectsList.size());
    assertEquals("oneList", co.objectsList.get(0));
    assertEquals("twoList", co.objectsList.get(1));
    assertEquals("threeList", co.objectsList.get(2));

    // XXX: we have a bug here
    // we should avoid Maps serialized like in second example in JSON (using xstream default converter)!
    // XXX maps works only with PrimitiveKeyedMapConverter registered with XStream
    // XXX test will fail if using default XStream converter!
    assertEquals(null, co.objectMap);
  }

}
