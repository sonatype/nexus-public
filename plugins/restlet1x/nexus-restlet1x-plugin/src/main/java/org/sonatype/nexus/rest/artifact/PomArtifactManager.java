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
package org.sonatype.nexus.rest.artifact;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Random;

import org.sonatype.nexus.rest.model.ArtifactCoordinate;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * This component simply preserves "state" and gets the needed GAVP values from the POM that it stores temporarily.
 *
 * @author cstamas
 */
public class PomArtifactManager
{
  private File tmpStorage = null;

  private File tmpPomFile = null;

  private int state = 0;

  private ArtifactCoordinate artifactCoordinate;

  private static final int STATE_NONE = 0;

  private static final int STATE_FILE_STORED = 1;

  private static final int STATE_GAV_PARSED = 2;

  private static final Random identifierGenerator = new Random();

  public PomArtifactManager(File tmpStorage) {
    super();

    this.tmpStorage = tmpStorage;
  }

  public void storeTempPomFile(InputStream is)
      throws IOException
  {
    if (STATE_NONE != state) {
      throw new IllegalStateException("There is already a temporary pom file managed by this PomArtifactManager");
    }

    tmpPomFile = new File(tmpStorage, getNextIdentifier() + ".xml");

    tmpPomFile.deleteOnExit();

    try (InputStream in = is;
         FileOutputStream out = new FileOutputStream(tmpPomFile)) {
      IOUtils.copy(is, out);
      state = STATE_FILE_STORED;
    }
  }

  public InputStream getTempPomFileInputStream()
      throws IOException
  {
    if (STATE_FILE_STORED > state) {
      throw new IllegalStateException("The temporary pom file has not yet been stored");
    }

    return new FileInputStream(tmpPomFile);
  }

  public ArtifactCoordinate getArtifactCoordinateFromTempPomFile()
      throws IOException,
             XmlPullParserException
  {
    if (STATE_FILE_STORED > state) {
      throw new IllegalStateException("The temporary POM file has not yet been stored");
    }

    if (STATE_GAV_PARSED == state) {
      return artifactCoordinate;
    }

    try (Reader reader = ReaderFactory.newXmlReader(tmpPomFile)) {
      artifactCoordinate = parsePom(reader);
      state = STATE_GAV_PARSED;
    }

    return artifactCoordinate;
  }

  /**
   * Clean up.
   */
  public void removeTempPomFile() {
    if (STATE_FILE_STORED > state) {
      throw new IllegalStateException("The temporary pom file has not yet been stored");
    }

    tmpPomFile.delete();

    artifactCoordinate = null;

    state = STATE_NONE;
  }

  // ==
  // Private

  private long getNextIdentifier() {
    synchronized (identifierGenerator) {
      return identifierGenerator.nextLong();
    }
  }

  /**
   * Pulls out the GAVP by parsing the temporarily stored POM.
   */
  private ArtifactCoordinate parsePom(Reader reader)
      throws IOException,
             XmlPullParserException
  {
    String groupId = null;

    String artifactId = null;

    String version = null;

    String packaging = "jar";

    XmlPullParser parser = new MXParser();

    parser.setInput(reader);

    boolean foundRoot = false;

    boolean inParent = false;

    int eventType = parser.getEventType();

    // TODO: we should detect when we got all we need and simply stop parsing further
    // since we are neglecting other contents anyway
    while (eventType != XmlPullParser.END_DOCUMENT) {
      if (eventType == XmlPullParser.START_TAG) {
        if (parser.getName().equals("project")) {
          foundRoot = true;
        }
        else if (parser.getName().equals("parent")) {
          inParent = true;
        }
        else if (parser.getName().equals("groupId")) {
          // 1st: if found project/groupId -> overwrite
          // 2nd: if in parent, and groupId is still null, overwrite
          if (parser.getDepth() == 2 || (inParent && groupId == null)) {
            groupId = StringUtils.trim(parser.nextText());
          }
        }
        else if (parser.getName().equals("artifactId")) {
          // 1st: if found project/artifactId -> overwrite
          // 2nd: if in parent, and artifactId is still null, overwrite
          if (parser.getDepth() == 2 || (inParent && artifactId == null)) {
            artifactId = StringUtils.trim(parser.nextText());
          }
        }
        else if (parser.getName().equals("version")) {
          // 1st: if found project/version -> overwrite
          // 2nd: if in parent, and version is still null, overwrite
          if (parser.getDepth() == 2 || (inParent && version == null)) {
            version = StringUtils.trim(parser.nextText());
          }
        }
        else if (parser.getName().equals("packaging")) {
          // 1st: if found project/packaging -> overwrite
          if (parser.getDepth() == 2) {
            packaging = StringUtils.trim(parser.nextText());
          }
        }
        else if (!foundRoot) {
          throw new XmlPullParserException("Unrecognised tag: '" + parser.getName() + "'", parser, null);
        }
      }
      else if (eventType == XmlPullParser.END_TAG) {
        if (parser.getName().equals("parent")) {
          inParent = false;
        }
      }

      eventType = parser.next();
    }

    ArtifactCoordinate artifactCoordinates = new ArtifactCoordinate();

    artifactCoordinates.setGroupId(groupId);

    artifactCoordinates.setArtifactId(artifactId);

    artifactCoordinates.setVersion(version);

    artifactCoordinates.setPackaging(packaging);

    return artifactCoordinates;
  }
}
