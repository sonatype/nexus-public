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
package org.sonatype.nexus.yum.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.item.AbstractContentLocator;
import org.sonatype.nexus.proxy.item.ContentGenerator;
import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.rest.RepositoryURLBuilder;

/**
 * @since yum 3.0
 */
@Named(YumConfigContentGenerator.ID)
@Singleton
public class YumConfigContentGenerator
    implements ContentGenerator
{

  public static final String ID = "YumConfigContentGenerator";

  private RepositoryURLBuilder repositoryURLBuilder;

  @Inject
  public YumConfigContentGenerator(final RepositoryURLBuilder repositoryURLBuilder) {
    this.repositoryURLBuilder = repositoryURLBuilder;
  }

  @Override
  public String getGeneratorId() {
    return ID;
  }

  @Override
  public ContentLocator generateContent(final Repository repository,
                                        final String path,
                                        final StorageFileItem item)
  {
    return new AbstractContentLocator("text/plain", true, ContentLocator.UNKNOWN_LENGTH)
    {
      private String content;

      @Override
      public InputStream getContent()
          throws IOException
      {
        if (content == null) {
          final ByteArrayOutputStream baos = new ByteArrayOutputStream();

          try (PrintWriter out = new PrintWriter(baos)) {
            out.println("[" + repository.getId() + "]");
            out.println("name=" + repository.getName());
            out.println("baseurl=" + repositoryURLBuilder.getExposedRepositoryContentUrl(repository, true));
            out.println("enabled=1");
            out.println("protect=0");
            out.println("gpgcheck=0");
            out.println("metadata_expire=30s");
            out.println("autorefresh=1");
            out.println("type=rpm-md");
          }
          content = new String(baos.toByteArray());
        }
        return new ByteArrayInputStream(content.getBytes("UTF-8"));
      }
    };
  }

  public static String configFilePath(final String repositoryId) {
    return ".meta/" + repositoryId + ".repo";
  }

}
