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
package org.sonatype.nexus.plugins.p2.repository.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import de.pdark.decentxml.XMLIOSource;
import de.pdark.decentxml.XMLParser;
import org.eclipse.tycho.model.Feature;

/**
 * Helper class to analyze (possible) P2 artifacts.
 *
 * @author msoftch
 */
public class P2ArtifactAnalyzer
{

  /**
   * Manifest entry for bundle symbolic name.
   */
  private static final String BUNDLE_SYMBOLIC_NAME = "Bundle-SymbolicName";

  /**
   * File name of a feature description.
   */
  private static final String FEATURE_XML = "feature.xml";

  /**
   * Return the p2 artifact type of the given file.
   *
   * @param file A jar file
   * @return The p2 artifact type or null if not a known p2 artifact
   */
  public static P2ArtifactType getP2Type(final File file) {
    if (file == null) {
      return null;
    }
    JarFile jarFile = null;
    try {
      jarFile = new JarFile(file);
      final ZipEntry feature = jarFile.getEntry(FEATURE_XML);
      // TODO more checks required?
      if (feature != null) {
        return P2ArtifactType.FEATURE;
      }
      if (getMainAttribute(jarFile.getManifest(), BUNDLE_SYMBOLIC_NAME) != null) {
        return P2ArtifactType.BUNDLE;
      }
      return null;
    }
    catch (final Exception e) {
      return null;
    }
    finally {
      if (jarFile != null) {
        try {
          jarFile.close();
        }
        catch (final Exception ignored) {
          // safe to ignore...
        }
      }
    }
  }

  /**
   * Returns the specified main attribute of the given manifest.
   *
   * @param manifest The manifest or null
   * @param attr     The main attribute name
   * @return The attribute name or null if a null manifest was passed or if no such attribute exists
   */
  private static String getMainAttribute(Manifest manifest, String attr) {
    if (manifest == null) {
      return null;
    }
    return manifest.getMainAttributes().getValue(attr);
  }

  /**
   * Extracts the generic information out of the given P2 artifact.
   *
   * @param file The artifact file
   * @return The generic artifact information or null
   * @throws IOException If an parsing error occurred
   */
  public static GenericP2Artifact parseP2Artifact(File file)
      throws IOException
  {
    JarFile jarFile = null;
    try {
      jarFile = new JarFile(file);
      final ZipEntry featureEntry = jarFile.getEntry(FEATURE_XML);
      if (featureEntry != null) {
        return parseFeature(jarFile.getInputStream(featureEntry));
      }
      final Manifest manifest = jarFile.getManifest();
      return manifest != null ? parseBundle(manifest) : null;
    }
    finally {
      if (jarFile != null) {
        try {
          jarFile.close();
        }
        catch (final Exception ignored) {
          // safe to ignore...
        }
      }
    }
  }

  /**
   * Parses the generic artifact information of a feature.
   *
   * @param in The input stream of the feature.xml
   * @return The generic description
   * @throws IOException If a parser error occurred
   */
  private static GenericP2Artifact parseFeature(InputStream in)
      throws IOException
  {
    // no closing of input stream required - our caller will close the jar
    // at the end
    final Feature f = new Feature(new XMLParser().parse(new XMLIOSource(in)));
    return new GenericP2Artifact(f.getId(), f.getVersion(), P2ArtifactType.FEATURE);
  }

  /**
   * Parses the generic artifact information of a bundle.
   *
   * @param manifest The manifest of the bundle
   * @return The generic description or null if not a bundle
   */
  private static GenericP2Artifact parseBundle(final Manifest manifest) {
    final Attributes mainAttributes = manifest.getMainAttributes();

    // get part before first semicolon
    // bug fix NEXUS-4552 & NEXUS-4567
    final String bsn = mainAttributes.getValue(BUNDLE_SYMBOLIC_NAME).split(";")[0].trim();
    if (bsn == null) {
      return null;
    }
    return new GenericP2Artifact(bsn, mainAttributes.getValue("Bundle-Version"), P2ArtifactType.BUNDLE);
  }
}
