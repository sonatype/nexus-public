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
package org.sonatype.nexus.repository.maven.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import javax.annotation.Nullable;

import com.google.common.base.Charsets;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.archetype.catalog.io.xpp3.ArchetypeCatalogXpp3Reader;
import org.apache.maven.archetype.catalog.io.xpp3.ArchetypeCatalogXpp3Writer;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maven2 models read and write helpers.
 *
 * @since 3.0
 */
public final class MavenModels
{
  private MavenModels() {
    // nope
  }

  private static final Logger log = LoggerFactory.getLogger(MavenModels.class);

  private static final MetadataXpp3Reader METADATA_READER = new MetadataXpp3Reader();

  private static final MetadataXpp3Writer METADATA_WRITER = new MetadataXpp3Writer();

  private static final ArchetypeCatalogXpp3Reader ARCHETYPE_CATALOG_READER = new ArchetypeCatalogXpp3Reader();

  private static final ArchetypeCatalogXpp3Writer ARCHETYPE_CATALOG_WRITER = new ArchetypeCatalogXpp3Writer();

  private static final MavenXpp3Reader MODEL_READER = new MavenXpp3Reader();

  /**
   * Parses input into {@link Xpp3Dom}, returns {@code null} if input not parsable. Passed in {@link InputStream} is
   * closed always on return.
   */
  @Nullable
  public static Xpp3Dom parseDom(final InputStream is) throws IOException {
    try (InputStreamReader reader = new InputStreamReader(is, Charsets.UTF_8)) {
      return Xpp3DomBuilder.build(reader);
    }
    catch (XmlPullParserException e) {
      log.debug("Could not parse XML into Xpp3Dom", e);
      return null;
    }
  }

  /**
   * Parses input into {@link Metadata}, returns {@code null} if input not parsable. Passed in {@link InputStream} is
   * closed always on return.
   */
  @Nullable
  public static Metadata readMetadata(final InputStream inputStream) throws IOException {
    try (InputStream is = inputStream) {
      return METADATA_READER.read(is, false);
    }
    catch (XmlPullParserException e) {
      log.debug("Could not parse XML into Metadata", e);
      return null;
    }
  }

  /**
   * Writes out {@link Metadata} into {@link OutputStream}. The stream is not closed on return.
   */
  public static void writeMetadata(final OutputStream outputStream, final Metadata metadata)
      throws IOException
  {
    METADATA_WRITER.write(outputStream, metadata);
  }

  /**
   * Parses input into {@link ArchetypeCatalog}, returns {@code null} if input not parsable. Passed in {@link
   * InputStream} is closed always on return.
   */
  @Nullable
  public static ArchetypeCatalog readArchetypeCatalog(final InputStream inputStream)
      throws IOException
  {
    try (InputStream is = inputStream) {
      return ARCHETYPE_CATALOG_READER.read(is, false);
    }
    catch (XmlPullParserException e) {
      log.debug("Could not parse XML into ArchetypeCatalog", e);
      return null;
    }
  }

  /**
   * Writes out {@link ArchetypeCatalog} into {@link OutputStream}. The stream is not closed on return.
   */
  public static void writeArchetypeCatalog(final OutputStream outputStream, final ArchetypeCatalog archetypeCatalog)
      throws IOException
  {
    ARCHETYPE_CATALOG_WRITER.write(outputStream, archetypeCatalog);
  }

  /**
   * Parses input into {@link Model}, returns {@code null} if input not parsable. Passed in {@link InputStream} is
   * closed always on return.
   */
  @Nullable
  public static Model readModel(final InputStream inputStream) throws IOException {
    try (InputStream is = inputStream) {
      return MODEL_READER.read(is, false);
    }
    catch (XmlPullParserException e) {
      log.debug("Could not parse XML into Model", e);
      return null;
    }
  }
}
