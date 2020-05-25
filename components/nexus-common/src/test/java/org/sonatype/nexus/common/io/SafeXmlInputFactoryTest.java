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
package org.sonatype.nexus.common.io;

import javax.xml.stream.XMLInputFactory;

import org.sonatype.nexus.common.io.SafeXmlInputFactory;

import org.junit.Test;

import static javax.xml.stream.XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES;
import static javax.xml.stream.XMLInputFactory.SUPPORT_DTD;
import static org.junit.Assert.assertFalse;

public class SafeXmlInputFactoryTest
{
  @Test
  public void shouldNotSupportDocTypeDefinitions() {
    XMLInputFactory xmlInputFactory = SafeXmlInputFactory.xmlInputFactory();

    Object supportDtd = xmlInputFactory.getProperty(SUPPORT_DTD);

    assertFalse(Boolean.parseBoolean(supportDtd.toString()));
  }

  @Test
  public void shouldNotSupportExternalEntities() {
    XMLInputFactory xmlInputFactory = SafeXmlInputFactory.xmlInputFactory();

    Object supportExternalEntities = xmlInputFactory.getProperty(IS_SUPPORTING_EXTERNAL_ENTITIES);

    assertFalse(Boolean.parseBoolean(supportExternalEntities.toString()));
  }
}
