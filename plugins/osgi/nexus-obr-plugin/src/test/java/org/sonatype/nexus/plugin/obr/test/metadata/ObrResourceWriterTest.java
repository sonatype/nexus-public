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

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.sonatype.nexus.obr.metadata.ObrResourceReader;
import org.sonatype.nexus.obr.metadata.ObrResourceWriter;
import org.sonatype.nexus.obr.metadata.ObrSite;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.osgi.service.obr.Resource;

public class ObrResourceWriterTest
    extends AbstractObrMetadataTest
{

  @Test
  public void testObrSerializing()
      throws Exception
  {
    final ObrSite testSite = openObrSite(testRepository, "/obr/samples/osgi_alliance_obr.zip");

    final RepositoryItemUid uid = testRepository.createUid("/obr/repository.xml");

    ObrResourceReader reader = obrMetadataSource.getReader(testSite);
    final ObrResourceWriter writer = obrMetadataSource.getWriter(uid);

    final Collection<Resource> bundles = new ArrayList<Resource>();

    Resource r;
    while ((r = reader.readResource()) != null) {
      bundles.add(r);
      writer.append(r);
      writer.flush();
    }

    assertEquals(2710, bundles.size());

    writer.complete();
    writer.close();

    reader = obrMetadataSource.getReader(openObrSite(uid));
    while ((r = reader.readResource()) != null) {
      assertTrue(bundles.remove(r));
    }

    assertEquals(Collections.emptyList(), bundles);
  }

  @Test
  public void testRoundTripping()
      throws Exception
  {
    final RepositoryItemUid uid1 = testRepository.createUid("/obr/samples/sample.xml");
    final RepositoryItemUid uid2 = testRepository.createUid("/obr/sample.xml");
    final RepositoryItemUid uid3 = testRepository.createUid("/sample.xml");

    ObrResourceReader reader = obrMetadataSource.getReader(openObrSite(uid1));
    ObrResourceWriter writer = obrMetadataSource.getWriter(uid2);

    final Collection<URL> urls = new ArrayList<URL>();

    Resource r;
    while ((r = reader.readResource()) != null) {
      urls.add(r.getURL());
      writer.append(r);
      writer.flush();
    }

    reader.close();
    writer.complete();
    writer.close();

    reader = obrMetadataSource.getReader(openObrSite(uid2));
    writer = obrMetadataSource.getWriter(uid3);

    while ((r = reader.readResource()) != null) {
      writer.append(r);
    }

    reader.close();
    writer.complete();
    writer.close();

    reader = obrMetadataSource.getReader(openObrSite(uid3));

    while ((r = reader.readResource()) != null) {
      urls.remove(r.getURL());
    }

    reader.close();

    assertEquals(Collections.emptyList(), urls);
  }

  // NXCM-1360
  @Test
  public void testRoundTrippingWithLongPackageNames()
      throws Exception
  {
    RepositoryItemUid uid1 = testRepository.createUid("/obr/samples/long-package-name.xml");
    RepositoryItemUid uid2 = testRepository.createUid("/obr/samples/long-package-name.xml.clone");
    RepositoryItemUid temp;

    for (int i = 0; i < 5; i++) {
      final ObrResourceReader reader = obrMetadataSource.getReader(openObrSite(uid1));
      final ObrResourceWriter writer = obrMetadataSource.getWriter(uid2);

      Resource r;
      while ((r = reader.readResource()) != null) {
        writer.append(r);
        writer.flush();
      }

      reader.close();
      writer.complete();
      writer.close();

      if (i > 0) {
        assertEquals(obrToString(uid1), obrToString(uid2));
      }

      temp = uid1;
      uid1 = uid2;
      uid2 = temp;
    }
  }

  private String obrToString(final RepositoryItemUid uid)
      throws Exception
  {
    try (InputStream is = openObrSite(uid).openStream()) {
      return IOUtils.toString(is).replaceFirst("lastmodified='[0-9]*'", "");
    }
  }
}
