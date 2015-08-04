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
package org.sonatype.nexus.proxy.maven.metadata.operations;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * utility class to help with de/serializing metadata from/to XML
 *
 * @author Oleg Gusakov
 * @version $Id: MetadataBuilder.java 740889 2009-02-04 21:13:29Z ogusakov $
 */
public class MetadataBuilder
{
  /**
   * instantiate Metadata from a stream
   */
  public static Metadata read(InputStream in)
      throws IOException
  {
    try {
      return new MetadataXpp3Reader().read(in);
    }
    catch (NullPointerException e) {
      // XPP3 parser throws NPE on some malformed XMLs
      throw new IOException("Malformed XML!", e);
    }
    catch (XmlPullParserException e) {
      throw new IOException(e);
    }
  }

  /**
   * serialize metadata into xml
   *
   * @param metadata to serialize
   * @param out      output to this stream
   * @return same metadata as was passed in
   * @throws MetadataException if any problems occurred
   */
  public static Metadata write(Metadata metadata, OutputStream out)
      throws IOException
  {
    if (metadata == null) {
      return metadata;
    }

    new MetadataXpp3Writer().write(WriterFactory.newXmlWriter(out), metadata);

    return metadata;
  }

  /**
   * Apply a list of operators to the specified metadata.
   *
   * @param metadata   - to be changed
   * @param operations -
   * @return changed metadata
   */
  public static void changeMetadata(Metadata metadata, List<MetadataOperation> operations)
      throws MetadataException
  {
    // Uncomment these once the fixes are in place
    // Version mdModelVersion = ModelVersionUtility.getModelVersion( metadata );

    if (metadata != null && operations != null && operations.size() > 0) {
      final Metadata clone = metadata.clone();
      for (MetadataOperation op : operations) {
        op.perform(clone);

        // if (currentChanged) {
        // mdModelVersion = max of mdModelVersion and op.getModelVersion;
        // }
      }
      replace(metadata, clone);
    }

    // ModelVersionUtility.setModelVersion( metadata, mdModelVersion );
  }

  /**
   * Apply a list of operators to the specified metadata ignoring failing operations (failing operations will not
   * affect original metadata).
   *
   * @param metadata   - to be changed
   * @param operations - operations to be applied to metadata
   * @return collection of failing operations exceptions
   */
  public static Collection<MetadataException> changeMetadataIgnoringFailures(
      final Metadata metadata,
      final List<MetadataOperation> operations)
  {
    final Collection<MetadataException> failures = new ArrayList<MetadataException>();

    if (metadata != null && operations != null && operations.size() > 0) {
      Metadata savePoint = metadata;
      for (MetadataOperation op : operations) {
        try {
          final Metadata clone = savePoint.clone();
          op.perform(clone);
          savePoint = clone;
        }
        catch (MetadataException e) {
          failures.add(e);
        }
      }
      replace(metadata, savePoint);
    }

    return failures;
  }

  private static void replace(final Metadata metadata, final Metadata newMetadata) {
    if (metadata == null || newMetadata == null) {
      return;
    }
    metadata.setArtifactId(newMetadata.getArtifactId());
    metadata.setGroupId(newMetadata.getGroupId());
    metadata.setModelEncoding(newMetadata.getModelEncoding());
    metadata.setModelVersion(newMetadata.getModelVersion());
    metadata.setPlugins(newMetadata.getPlugins());
    metadata.setVersion(newMetadata.getVersion());
    metadata.setVersioning(newMetadata.getVersioning());
  }

  public static void changeMetadata(Metadata metadata, MetadataOperation op)
      throws MetadataException
  {
    changeMetadata(metadata, Collections.singletonList(op));
  }

  public static void changeMetadata(Metadata metadata, MetadataOperation... ops)
      throws MetadataException
  {
    changeMetadata(metadata, Arrays.asList(ops));
  }

  /**
   * update snapshot timestamp to now
   */
  public static void updateTimestamp(Snapshot target) {
    target.setTimestamp(TimeUtil.getUTCTimestamp());
  }

  /**
   * update versioning's lastUpdated timestamp to now
   */
  public static void updateTimestamp(Versioning target) {
    target.setLastUpdated(TimeUtil.getUTCTimestamp());
  }

  public static Snapshot createSnapshot(String version) {
    Snapshot sn = new Snapshot();

    if (version == null || version.length() < 3) {
      return sn;
    }

    String utc = TimeUtil.getUTCTimestamp();
    sn.setTimestamp(utc);

    if (version.endsWith("-SNAPSHOT")) {
      return sn;
    }

    int pos = version.lastIndexOf('-');

    if (pos == -1) {
      throw new IllegalArgumentException();
    }

    String sbn = version.substring(pos + 1);

    int bn = Integer.parseInt(sbn);
    sn.setBuildNumber(bn);

    String sts = version.substring(0, pos);
    pos = sts.lastIndexOf('-');

    if (pos == -1) {
      throw new IllegalArgumentException();
    }

    sn.setTimestamp(sts.substring(pos + 1));

    return sn;
  }

}
