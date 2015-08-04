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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.ZipInputStream;

import org.sonatype.nexus.NexusAppTestSupport;
import org.sonatype.nexus.configuration.application.NexusConfiguration;
import org.sonatype.nexus.configuration.model.CLocalStorage;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.DefaultCRepository;
import org.sonatype.nexus.obr.metadata.ObrMetadataSource;
import org.sonatype.nexus.obr.metadata.ObrSite;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.maven.maven2.M2Repository;
import org.sonatype.nexus.proxy.repository.Repository;

public abstract class AbstractObrMetadataTest
    extends NexusAppTestSupport
{

  protected ObrMetadataSource obrMetadataSource;

  protected M2Repository testRepository;

  protected NexusConfiguration nexusConfig;

  @Override
  protected void setUp()
      throws Exception
  {
    super.setUp();

    nexusConfig = lookup(NexusConfiguration.class);

    obrMetadataSource = lookup(ObrMetadataSource.class, "obr-bindex");

    testRepository = (M2Repository) lookup(Repository.class, "maven2");

    nexusConfig.loadConfiguration();

    final CRepository crepo = new DefaultCRepository();
    crepo.setId("test-repository");
    crepo.setName("test-repository");
    final CLocalStorage clocal = new CLocalStorage();
    clocal.setUrl(getBasedir() + "/target/test-classes");
    clocal.setProvider("file");
    crepo.setLocalStorage(clocal);
    testRepository.configure(crepo);

    // initialize attribute cache, otherwise storeItem recurses
    testRepository.getRepositoryItemUidAttributeManager().reset();
  }

  protected ObrSite openObrSite(final RepositoryItemUid uid)
      throws StorageException, ItemNotFoundException
  {
    return openObrSite(uid.getRepository(), uid.getPath());
  }

  protected ObrSite openObrSite(final Repository repository, final String path)
      throws StorageException, ItemNotFoundException
  {
    final ResourceStoreRequest request = new ResourceStoreRequest(path);

    final URL url = repository.getLocalStorage().getAbsoluteUrlFromBase(repository, request);

    return new ObrSite()
    {
      public String getMetadataPath() {
        return path;
      }

      public URL getMetadataUrl() {
        return url;
      }

      public InputStream openStream()
          throws IOException
      {
        final URLConnection conn = url.openConnection();

        if ("application/zip".equalsIgnoreCase(conn.getContentType())) {
          // assume metadata is the first entry in the zipfile
          final ZipInputStream zis = new ZipInputStream(conn.getInputStream());
          zis.getNextEntry();
          return zis;
        }

        return conn.getInputStream();
      }
    };
  }
}
