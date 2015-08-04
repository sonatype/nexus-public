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
package org.sonatype.nexus.plugin.obr.test.metadata;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.sonatype.nexus.obr.metadata.ObrResourceReader;
import org.sonatype.nexus.obr.metadata.ObrSite;

import org.junit.Test;
import org.osgi.service.obr.Resource;

public class ObrResourceReaderTest
    extends AbstractObrMetadataTest
{

  @Test
  public void testObrParsing()
      throws Exception
  {
    final ObrSite testSite = openObrSite(testRepository, "/obr/samples/osgi_alliance_obr.zip");
    final ObrResourceReader reader = obrMetadataSource.getReader(testSite);

    final BufferedReader br =
        new BufferedReader(new InputStreamReader(getResourceAsStream("/obr/samples/osgi_alliance_obr.lst")));

    int numBundles = 0;

    Resource r;
    while ((r = reader.readResource()) != null) {
      assertEquals(br.readLine(), r.toString());
      numBundles++;
    }

    assertNull(br.readLine());

    assertEquals(2710, numBundles);

    reader.close();
    br.close();
  }

  @Test
  public void testObrReferral()
      throws Exception
  {
    final ObrSite testSite = openObrSite(testRepository, "/obr/samples/referrals.xml");
    final ObrResourceReader reader = obrMetadataSource.getReader(testSite);

    int numBundles = 0;
    int numExceptions = 0;

    while (true) {
      try {
        final Resource r = reader.readResource();
        if (r != null) {
          numBundles++;
        }
        else {
          break;
        }
      }
      catch (final IOException e) {
        if (++numExceptions > 2) {
          throw e;
        }
      }
    }

    assertEquals(2713, numBundles);
    assertEquals(2, numExceptions);

    reader.close();
  }
}
