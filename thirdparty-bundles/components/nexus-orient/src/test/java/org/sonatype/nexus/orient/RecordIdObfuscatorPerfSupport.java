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
package org.sonatype.nexus.orient;

import org.sonatype.goodies.testsupport.TestSupport;

import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import org.databene.contiperf.PerfTest;
import org.databene.contiperf.junit.ContiPerfRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Support for {@link RecordIdObfuscator} performance tests.
 */
public abstract class RecordIdObfuscatorPerfSupport
  extends TestSupport
{
  @Rule
  public ContiPerfRule perfRule = new ContiPerfRule();

  private RecordIdObfuscator underTest;

  private ORID rid;

  private OClass type;

  @Before
  public void setUp() throws Exception {
    this.underTest = createTestSubject();

    this.rid = new ORecordId("#9:1");

    this.type = mock(OClass.class);
    when(type.getClusterIds()).thenReturn(new int[] { rid.getClusterId() });

    // prime jvm byte code optimization (maybe, we hope)
    for (int i=0; i<1000; i++) {
      encodeAndDecode();
    }
  }

  protected abstract RecordIdObfuscator createTestSubject() throws Exception;

  @Test
  @PerfTest(invocations = 100000)
  public void encodeAndDecode() throws Exception {
    String encoded = underTest.encode(type, rid);
    ORID decoded = underTest.decode(type, encoded);
  }
}
