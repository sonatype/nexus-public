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
package org.sonatype.nexus.proxy.attributes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.sonatype.nexus.proxy.attributes.internal.DefaultAttributes;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

public class MarshallerTest
{
  protected void doTest(final Marshaller marshaller)
      throws IOException
  {
    doTestSimpleRoundtrip(marshaller);
    doTestUnparseableRoundtrip(marshaller);
  }

  protected void doTestSimpleRoundtrip(final Marshaller marshaller)
      throws IOException
  {
    // simple roundtrip
    final Attributes attributes1 = new DefaultAttributes();
    attributes1.put("foo", "fooVal");
    attributes1.put("bar", "barVal");
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    marshaller.marshal(attributes1, bos);
    final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
    final Attributes attributes2 = marshaller.unmarshal(bis);
    // size same
    assertThat(attributes2.asMap().size(), equalTo(attributes1.asMap().size()));
    // as maps are equal
    assertThat(attributes2.asMap(), equalTo(attributes1.asMap()));
    // but we deal with two different instances
    assertThat(System.identityHashCode(attributes2), not(equalTo(System.identityHashCode(attributes1))));
  }

  protected void doTestUnparseableRoundtrip(final Marshaller marshaller)
      throws IOException
  {
    // simple roundtrip
    final Attributes attributes1 = new DefaultAttributes();
    attributes1.put("foo", "fooVal");
    attributes1.put("bar", "barVal");
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    marshaller.marshal(attributes1, bos);

    // break the data
    final byte[] data = bos.toByteArray();
    data[0] = 'a';
    data[1] = 'b';
    data[2] = 'c';

    final ByteArrayInputStream bis = new ByteArrayInputStream(data);
    try {
      final Attributes attributes2 = marshaller.unmarshal(bis);
      throw new AssertionError("Unmarshal should fail and throw InvalidInputException!");
    }
    catch (InvalidInputException e) {
      // good
    }
  }

  // ==

  @Test
  public void testJacksonJSON()
      throws IOException
  {
    doTest(new JacksonJSONMarshaller());
  }
}
