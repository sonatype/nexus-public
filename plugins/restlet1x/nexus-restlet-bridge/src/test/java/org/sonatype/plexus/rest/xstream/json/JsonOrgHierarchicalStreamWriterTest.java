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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.thoughtworks.xstream.XStream;
import org.codehaus.plexus.PlexusTestCase;

public class JsonOrgHierarchicalStreamWriterTest
    extends PlexusTestCase
{

  protected XStream xstream;

  protected void prepare() {
    this.xstream = new XStream(new JsonOrgHierarchicalStreamDriver());

    this.xstream.registerConverter(new PrimitiveKeyedMapConverter(xstream.getMapper()));

    Class[] types = new Class[] {
        OneValued.class,
        CombinedValued.class,
        ThreeValued.class
    };
    this.xstream.allowTypes(types);
    this.xstream.processAnnotations(types);
  }

  protected void setUp()
      throws Exception
  {
    super.setUp();
    prepare();
  }

  protected void serialize(Object o)
      throws IOException
  {
    String result = xstream.toXML(o);

    System.out.println(result);
  }

  public void testList()
      throws Exception
  {
    System.out.println(" == LISTS ==");

    List<String> strings = new ArrayList<String>(3);
    strings.add("oneList");
    strings.add("twoList");
    strings.add("threeList");
    serialize(strings);
  }

  public void testMap()
      throws Exception
  {
    System.out.println(" == MAP ==");

    HashMap map = new HashMap();
    map.put("key", "value");
    map.put("aNumber", 1975);
    map.put("aBoolean", Boolean.TRUE);
    map.put("one-nine-seven-five", 1975);
    serialize(map);

    HashMap complicated = new HashMap();
    complicated.put("simpleKey", "someValue");
    complicated.put("aMap", map);
    Object[] objects = new Object[3];
    objects[0] = "text";
    objects[1] = 1975;
    objects[2] = Boolean.TRUE;
    complicated.put("arrayOfObjects", objects);
    serialize(complicated);
  }

  public void testArray()
      throws Exception
  {
    System.out.println(" == ARRAYS ==");

    String[] strings = new String[2];
    strings[0] = "one";
    strings[1] = "two";
    serialize(strings);

    int[] ints = new int[3];
    ints[0] = 1;
    ints[1] = 2;
    ints[2] = 3;
    serialize(ints);

    Object[] objects = new Object[3];
    objects[0] = "text";
    objects[1] = 1975;
    objects[2] = Boolean.TRUE;
    serialize(objects);
  }

  public void testCustomObjects()
      throws Exception
  {
    System.out.println(" == CUSTOM OBJECTS ==");

    OneValued ovn = new OneValued();
    ovn.stringValue = null;
    serialize(ovn);

    OneValued ov = new OneValued();
    ov.stringValue = "some string value";
    serialize(ov);

    ThreeValued tw = new ThreeValued();
    tw.stringValue = "again some string field";
    tw.intValue = 1975;
    tw.boolValue = true;
    serialize(tw);

    CombinedValued co = new CombinedValued();
    co.stringValue = "custom object";
    int[] ints = new int[3];
    ints[0] = 1;
    ints[1] = 2;
    ints[2] = 3;
    co.ints = ints;

    List<String> strings = new ArrayList<String>(3);
    strings.add("oneList");
    strings.add("twoList");
    strings.add("threeList");
    co.objectsList = strings;

    HashMap map = new HashMap();
    map.put("key", "value");
    map.put("aNumber", 1975);
    co.objectMap = map;

    serialize(co);
  }

}
