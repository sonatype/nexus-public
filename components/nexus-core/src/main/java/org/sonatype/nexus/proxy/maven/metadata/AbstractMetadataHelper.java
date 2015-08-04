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
package org.sonatype.nexus.proxy.maven.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.sonatype.nexus.proxy.maven.gav.Gav;
import org.sonatype.nexus.proxy.maven.gav.GavCalculator;
import org.sonatype.nexus.proxy.maven.gav.M2ArtifactRecognizer;

import org.apache.maven.artifact.repository.metadata.Plugin;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.slf4j.Logger;

/**
 * a Maven metadata helper containing all the logic for creating maven-metadata.xml <br/>
 * and logic for creating md5 and sh1 checksum files
 *
 * @author Juven Xu
 */
abstract public class AbstractMetadataHelper
{
  static final String MD5_SUFFIX = ".md5";

  static final String SHA1_SUFFIX = ".sha1";

  static final String METADATA_SUFFIX = "/maven-metadata.xml";

  static final String APPROPRIATE_GAV_PATTERN = "^[\\d\\w\\.-]*$";

  protected Logger logger;

  // key is the path where need to create g md, value is a collection of plugins
  Map<String, Collection<Plugin>> gData = new HashMap<String, Collection<Plugin>>();

  // key is the path where need to create ga md, value is a collection of versions
  Map<String, Collection<String>> gaData = new HashMap<String, Collection<String>>();

  // key is the path where need to create gav md, value is a collection of path names
  Map<String, Collection<String>> gavData = new HashMap<String, Collection<String>>();

  private Collection<AbstractMetadataProcessor> metadataProcessors;

  public AbstractMetadataHelper(Logger logger) {
    this.logger = logger;

    // here the order matters
    metadataProcessors = new ArrayList<AbstractMetadataProcessor>(4);

    metadataProcessors.add(new VersionDirMetadataProcessor(this));

    metadataProcessors.add(new ArtifactDirMetadataProcessor(this));

    metadataProcessors.add(new GroupDirMetadataProcessor(this));

    metadataProcessors.add(new ObsoleteMetadataProcessor(this));
  }

  public void onDirEnter(String path)
      throws IOException
  {
    // do nothing
  }

  public void onDirExit(String path)
      throws IOException
  {

    for (AbstractMetadataProcessor metadataProcessor : metadataProcessors) {
      if (metadataProcessor.process(path)) {
        break;
      }
    }

  }

  public void processFile(String path)
      throws IOException
  {
    // remove checksums that does not have corresponding "main" file (those without .sha1 ext)
    if (isObsoleteChecksum(path)) {
      remove(path);

      return;
    }

    rebuildChecksum(path);

    if (!M2ArtifactRecognizer.isMetadata(path)) {
      updateMavenInfo(path);
    }
  }

  private boolean isObsoleteChecksum(String path)
      throws IOException
  {
    if (!isChecksumFile(path)) {
      return false;
    }

    String originalPath = path.substring(0, path.lastIndexOf('.'));

    if (exists(originalPath)) {
      return false;
    }

    return true;
  }

  private String getName(String path) {
    int pos = path.lastIndexOf('/');

    if (pos == -1) {
      return path;
    }

    return path.substring(pos + 1);
  }

  protected void updateMavenInfo(String path)
      throws IOException
  {
    // groupId, artifactId, version, artifactName
    String g = null;
    String a = null;
    String v = null;
    String n = null;
    n = path.substring(path.lastIndexOf('/') + 1);

    final Gav gav = getGavCalculator().pathToGav(path);

    if (gav != null) {
      g = gav.getGroupId();
      a = gav.getArtifactId();
      v = gav.getBaseVersion();
    }
    else {
      // mute it, we still don't know is this a M2 artifact or not
      if (logger.isDebugEnabled()) {
        logger.debug("Unable to parse good GAV values. Path: '" + path + "'. GAV: '" + g + ":" + a + ":" + v
            + "'");
      }
    }

    // try to see is this a POM
    // TODO: even if it does not fit M2 layout?
    Model model = null;
    if (path.endsWith("pom")) {
      Reader reader = ReaderFactory.newXmlReader(retrieveContent(path));

      MavenXpp3Reader xpp3 = new MavenXpp3Reader();

      try {
        model = xpp3.read(reader);

        String mg = model.getGroupId() == null ? model.getParent().getGroupId() : model.getGroupId();
        String ma = model.getArtifactId();
        String mv = model.getVersion() == null ? model.getParent().getVersion() : model.getVersion();

        // if the pom could provide good values
        if (!isInpropriateValue(mg)) {
          g = mg;
        }
        if (!isInpropriateValue(ma)) {
          a = ma;
        }
        if (!isInpropriateValue(mv)) {
          v = mv;
        }
      }
      catch (Exception e) {
        // quietly ignore, we still can process this using gav from path
        logger.debug("Unable to parse POM model from '" + path + "'.", e);
        // throw new Exception( "Unable to parse POM model from '" + path + "'.", e );
      }
      finally {
        reader.close();

        reader = null;
      }

    }

    if (g == null || a == null || v == null) {
      // we were called with a path not pointing to a file that obeys M2 layout, just silently return
      if (logger.isDebugEnabled()) {
        logger.debug("Unable to resolve gav for '" + path + "'. g:" + g + " a:" + a + " v:" + v);
      }

      return;
    }

    if (path.endsWith("pom")) {
      // G
      if (model != null && model.getPackaging().equals("maven-plugin")) {
        Plugin plugin = new Plugin();

        plugin.setArtifactId(a);

        plugin.setPrefix(getPluginPrefix(a, path));

        if (!StringUtils.isEmpty(model.getName())) {
          plugin.setName(model.getName());
        }

        String gPath = "/" + g.replace('.', '/');

        if (gData.get(gPath) == null) {
          gData.put(gPath, new ArrayList<Plugin>());
        }

        gData.get(gPath).add(plugin);
      }

      // GA
      String gaPath = "/" + g.replace('.', '/') + "/" + a;

      if (gaData.get(gaPath) == null) {
        gaData.put(gaPath, new ArrayList<String>());
      }

      gaData.get(gaPath).add(v);

    }

    // GAV
    if (v.endsWith("SNAPSHOT")) {
      String gavPath = "/" + g.replace('.', '/') + "/" + a + "/" + v;

      if (gavData.get(gavPath) == null) {
        gavData.put(gavPath, new ArrayList<String>());
      }

      gavData.get(gavPath).add(n);
    }
  }

  private boolean isInpropriateValue(String value) {
    if (StringUtils.isEmpty(value)) {
      return true;
    }
    if (!value.matches(APPROPRIATE_GAV_PATTERN)) {
      return true;
    }

    return false;
  }

  private String getPluginPrefix(String artifactId, String path) {
    String jarPath = path.replace(".pom", ".jar");
    String prefix = null;
    try {
      if (exists(jarPath)) {
        try (ZipInputStream zip = new ZipInputStream(retrieveContent(jarPath))) {
          ZipEntry entry;
          while ((entry = zip.getNextEntry()) != null) {
            if (!entry.isDirectory() && entry.getName().equals("META-INF/maven/plugin.xml")) {
              PlexusConfiguration plexusConfig =
                  new XmlPlexusConfiguration(Xpp3DomBuilder.build(new InputStreamReader(zip)));

              prefix = plexusConfig.getChild("goalPrefix").getValue();
              zip.closeEntry();
              break;
            }
            zip.closeEntry();
          }
        }
      }
    }
    catch (Exception e) {
      // can't read plugin.xml
      logger.debug("Unable to read plugin.xml", e);
    }

    if (prefix != null) {
      return prefix;
    }

    if ("maven-plugin-plugin".equals(artifactId)) {
      return "plugin";
    }
    else {
      return artifactId.replaceAll("-?maven-?", "").replaceAll("-?plugin-?", "");
    }
  }

  void rebuildChecksum(String path)
      throws IOException
  {
    if (!exists(path)) {
      if (exists(path + MD5_SUFFIX)) {
        remove(path + MD5_SUFFIX);
      }
      if (exists(path + SHA1_SUFFIX)) {
        remove(path + SHA1_SUFFIX);
      }

      return;
    }

    if (!shouldBuildChecksum(path)) {
      return;
    }

    store(buildMd5(path), path + MD5_SUFFIX);

    store(buildSh1(path), path + SHA1_SUFFIX);
  }

  protected boolean shouldBuildChecksum(String path) {
    if (isChecksumFile(path)) {
      return false;
    }

    return true;
  }

  protected boolean isChecksumFile(String path) {
    if (getName(path).endsWith(MD5_SUFFIX) || getName(path).endsWith(SHA1_SUFFIX)) {
      return true;
    }
    return false;
  }

  abstract public String buildMd5(String path)
      throws IOException;

  abstract public String buildSh1(String path)
      throws IOException;

  /**
   * Store the content to the file of the path
   */
  abstract public void store(String content, String path)
      throws IOException;

  /**
   * Remove the file of the path
   */
  abstract public void remove(String path)
      throws IOException;

  /**
   * Retrieve the content according to the path
   */
  abstract public InputStream retrieveContent(String path)
      throws IOException;

  /**
   * Check if the file or item of this path exists
   */
  abstract public boolean exists(String path)
      throws IOException;

  abstract protected GavCalculator getGavCalculator();

}
