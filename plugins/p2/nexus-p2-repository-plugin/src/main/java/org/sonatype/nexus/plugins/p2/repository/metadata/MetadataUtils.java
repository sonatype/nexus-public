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
package org.sonatype.nexus.plugins.p2.repository.metadata;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.item.StorageFileItem;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Utilities to create DOM out of metadata items.
 *
 * @author cstamas
 */
public class MetadataUtils
{
  private MetadataUtils() {
    // static utils here
  }

  /**
   * Method that will create the DOM for given P2 metadata file. It handles files like "artifacts.xml" and
   * "content.xml", but also thier JAR counterparts, like "artifacts.jar" and "content.jar" by cranking them up,
   * getting the entry with same name but with modified extension to ".xml".
   */
  public static Xpp3Dom getMetadataXpp3Dom(final StorageFileItem item)
      throws IOException, XmlPullParserException
  {
    // TODO: if we ever want to have the DOM reused, this method could put the parsed DOM
    // into item context (to not have it reparsed). For now, this call always parses as
    // currently we'd go rather to P2 "eat CPU" then "eat heap", as P2 metadata DOM
    // objects might get very large
    final Xpp3Dom dom;
    if (item.getName().endsWith(".jar")) {
      dom = parseJarItem(item, item.getName().replace(".jar", ".xml"));
    }
    else if (item.getName().endsWith(".xml")) {
      dom = parseXmlItem(item);
    }
    else {
      throw new IOException("Cannot parse the DOM for metadata in item " + item.getRepositoryItemUid());
    }
    return dom;
  }

  // ==

  private static Xpp3Dom parseXmlItem(final StorageFileItem item)
      throws IOException, XmlPullParserException
  {
    try (InputStream is = item.getInputStream()) {
      return Xpp3DomBuilder.build(new XmlStreamReader(is));
    }
  }

  private static Xpp3Dom parseJarItem(final StorageFileItem item, final String jarPath)
      throws IOException, XmlPullParserException
  {
    final File file = File.createTempFile("p2file", "zip");
    try {
      try (InputStream is = item.getInputStream()) {
        FileUtils.copyInputStreamToFile(is, file);
        try (ZipFile z = new ZipFile(file)) {
          final ZipEntry ze = z.getEntry(jarPath);
          if (ze == null) {
            throw new LocalStorageException("Corrupted P2 metadata jar " + jarPath);
          }
          try (InputStream zis = z.getInputStream(ze)) {
            return Xpp3DomBuilder.build(new XmlStreamReader(zis));
          }
        }
      }
    }
    finally {
      file.delete();
    }
  }
}
