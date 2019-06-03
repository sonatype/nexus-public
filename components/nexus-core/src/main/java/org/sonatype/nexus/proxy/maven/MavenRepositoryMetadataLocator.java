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
package org.sonatype.nexus.proxy.maven;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.maven.MavenXpp3Reader;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.RequestContext;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.item.StringContentLocator;
import org.sonatype.nexus.proxy.maven.gav.Gav;
import org.sonatype.nexus.proxy.maven.gav.GavCalculator;
import org.sonatype.nexus.proxy.maven.metadata.operations.MetadataBuilder;
import org.sonatype.nexus.proxy.maven.metadata.operations.MetadataException;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Plugin;
import org.apache.maven.model.Model;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * A MetadataLocator powered by Nexus' MavenRepository.
 *
 * @author cstamas
 */
@Singleton
@Named
public class MavenRepositoryMetadataLocator
    implements MetadataLocator
{
  @Override
  public Gav getGavForRequest(ArtifactStoreRequest request) {
    return request.getGav();
  }

  @Override
  public Plugin extractPluginElementFromPom(ArtifactStoreRequest request)
      throws IOException
  {
    Model pom = retrievePom(request);

    if (!"maven-plugin".equals(pom.getPackaging())) {
      return null;
    }

    Plugin plugin = new Plugin();

    plugin.setArtifactId(pom.getArtifactId());

    plugin.setName(pom.getName());

    if ("maven-plugin-plugin".equals(pom.getArtifactId())) {
      plugin.setPrefix("plugin");
    }
    else {
      plugin.setPrefix(pom.getArtifactId().replaceAll("-?maven-?", "").replaceAll("-?plugin-?", ""));
    }

    return plugin;
  }

  protected Gav getPomGav(ArtifactStoreRequest request) {
    Gav pomGav = new Gav(request.getGav().getGroupId(), // groupId
        request.getGav().getArtifactId(), // artifactId
        request.getGav().getVersion(), // version
        null, // classifier
        "pom", // extension
        request.getGav().getSnapshotBuildNumber(), // snapshotBuildNumber
        request.getGav().getSnapshotTimeStamp(), // snapshotTimeStamp
        null, // name
        false, // hash
        null, // hashType
        false, // signature
        null // signatureType
    );

    return pomGav;
  }

  @Override
  public String retrievePackagingFromPom(ArtifactStoreRequest request)
      throws IOException
  {
    String packaging;

    GavCalculator gavCalculator = request.getMavenRepository().getGavCalculator();

    try {
      Gav pomGav = getPomGav(request);

      if (pomGav == null) {
        return null;
      }

      String pomPath = gavCalculator.gavToPath(pomGav);

      request.setRequestPath(pomPath);

      StorageFileItem pomFile = (StorageFileItem) request.getMavenRepository().retrieveItem(false, request);

      try (final Reader reader = ReaderFactory.newXmlReader(pomFile.getInputStream())) {
        packaging = getPackaging(reader);
      }
    }
    catch (ItemNotFoundException e) {
      return null;
    }
    catch (Exception e) {
      throw createIOExceptionWithCause(e.getMessage(), e);
    }
    return packaging;
  }

  private String getPackaging(Reader reader)
      throws XmlPullParserException, IOException
  {
    String packaging = "jar";

    XmlPullParser parser = new MXParser();

    parser.setInput(reader);

    boolean foundRoot = false;

    int eventType = parser.getEventType();

    while (eventType != XmlPullParser.END_DOCUMENT) {
      if (eventType == XmlPullParser.START_TAG) {
        if (parser.getName().equals("project")) {
          foundRoot = true;
        }
        else if (parser.getName().equals("packaging")) {
          // 1st: if found project/packaging -> overwrite
          if (parser.getDepth() == 2) {
            packaging = StringUtils.trim(parser.nextText());
            break;
          }
        }
        else if (!foundRoot) {
          throw new XmlPullParserException("Unrecognised tag: '" + parser.getName() + "'", parser, null);
        }
      }

      eventType = parser.next();
    }

    return packaging;
  }

  @Override
  public Model retrievePom(ArtifactStoreRequest request)
      throws IOException
  {
    try {
      Gav gav = getPomGav(request);

      if (gav == null) {
        return null;
      }

      String pomPath = request.getMavenRepository().getGavCalculator().gavToPath(gav);

      request.setRequestPath(pomPath);

      StorageFileItem pomFile = (StorageFileItem) request.getMavenRepository().retrieveItem(false, request);

      try (final InputStream is = pomFile.getInputStream()) {
        MavenXpp3Reader rd = new MavenXpp3Reader();
        return rd.read(is);
      }
      catch (XmlPullParserException e) {
        throw createIOExceptionWithCause(e.getMessage(), e);
      }
    }
    catch (Exception e) {
      throw createIOExceptionWithCause(e.getMessage(), e);
    }
  }

  @Override
  public Metadata retrieveGAVMetadata(ArtifactStoreRequest request)
      throws IOException
  {
    try {
      Gav gav = getGavForRequest(request);

      return readOrCreateGAVMetadata(request, gav);
    }
    catch (Exception e) {
      throw createIOExceptionWithCause(e.getMessage(), e);
    }
  }

  @Override
  public Metadata retrieveGAMetadata(ArtifactStoreRequest request)
      throws IOException
  {
    try {
      Gav gav = getGavForRequest(request);

      return readOrCreateGAMetadata(request, gav);
    }
    catch (Exception e) {
      throw createIOExceptionWithCause(e.getMessage(), e);
    }
  }

  @Override
  public Metadata retrieveGMetadata(ArtifactStoreRequest request)
      throws IOException
  {
    try {
      Gav gav = getGavForRequest(request);

      return readOrCreateGMetadata(request, gav);
    }
    catch (Exception e) {
      throw createIOExceptionWithCause(e.getMessage(), e);
    }
  }

  @Override
  public void storeGAVMetadata(ArtifactStoreRequest request, Metadata metadata)
      throws IOException
  {
    try {
      Gav gav = getGavForRequest(request);

      writeGAVMetadata(request, gav, metadata);
    }
    catch (Exception e) {
      throw createIOExceptionWithCause(e.getMessage(), e);
    }
  }

  @Override
  public void storeGAMetadata(ArtifactStoreRequest request, Metadata metadata)
      throws IOException
  {
    try {
      Gav gav = getGavForRequest(request);

      writeGAMetadata(request, gav, metadata);
    }
    catch (Exception e) {
      throw createIOExceptionWithCause(e.getMessage(), e);
    }
  }

  @Override
  public void storeGMetadata(ArtifactStoreRequest request, Metadata metadata)
      throws IOException
  {
    try {
      Gav gav = getGavForRequest(request);

      writeGMetadata(request, gav, metadata);
    }
    catch (Exception e) {
      throw createIOExceptionWithCause(e.getMessage(), e);
    }
  }

  // ==================================================
  // -- internal stuff below
  // ==================================================

  private IOException createIOExceptionWithCause(String message, Throwable cause) {
    IOException result = new IOException(message);

    result.initCause(cause);

    return result;
  }

  protected Metadata readOrCreateMetadata(RepositoryItemUid uid, ArtifactStoreRequest request)
      throws IllegalOperationException, IOException, MetadataException
  {
    Metadata result = null;

    String storedPath = request.getRequestPath();

    try {
      request.setRequestPath(uid.getPath());

      StorageItem item = uid.getRepository().retrieveItem(false, request);

      if (StorageFileItem.class.isAssignableFrom(item.getClass())) {
        StorageFileItem fileItem = (StorageFileItem) item;

        try (final InputStream is = fileItem.getInputStream()) {
          result = MetadataBuilder.read(is);
        }
      }
      else {
        throw new IllegalArgumentException("The UID " + uid.toString() + " is not a file!");
      }
    }
    catch (ItemNotFoundException e) {
      result = new Metadata();
    }
    finally {
      request.setRequestPath(storedPath);
    }

    return result;
  }

  protected void writeMetadata(RepositoryItemUid uid, RequestContext ctx, Metadata md)
      throws IllegalOperationException, UnsupportedStorageOperationException, MetadataException, IOException
  {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    MetadataBuilder.write(md, outputStream);

    String mdString = outputStream.toString("UTF-8");

    outputStream.close();

    DefaultStorageFileItem file =
        new DefaultStorageFileItem(uid.getRepository(), new ResourceStoreRequest(uid.getPath()), true, true,
            new StringContentLocator(mdString));

    ((MavenRepository) uid.getRepository()).storeItemWithChecksums(false, file);
  }

  protected Metadata readOrCreateGAVMetadata(ArtifactStoreRequest request, Gav gav)
      throws IllegalOperationException, IOException, MetadataException
  {
    String mdPath = request.getRequestPath();

    // GAV
    mdPath = mdPath.substring(0, mdPath.lastIndexOf(RepositoryItemUid.PATH_SEPARATOR)) + "/maven-metadata.xml";

    RepositoryItemUid uid = request.getMavenRepository().createUid(mdPath);

    Metadata result = readOrCreateMetadata(uid, request);

    result.setGroupId(gav.getGroupId());

    result.setArtifactId(gav.getArtifactId());

    result.setVersion(gav.getBaseVersion());

    return result;
  }

  protected Metadata readOrCreateGAMetadata(ArtifactStoreRequest request, Gav gav)
      throws IllegalOperationException, IOException, MetadataException
  {
    String mdPath = request.getRequestPath();

    // GAV
    mdPath = mdPath.substring(0, mdPath.lastIndexOf(RepositoryItemUid.PATH_SEPARATOR));

    // GA
    mdPath = mdPath.substring(0, mdPath.lastIndexOf(RepositoryItemUid.PATH_SEPARATOR)) + "/maven-metadata.xml";

    RepositoryItemUid uid = request.getMavenRepository().createUid(mdPath);

    Metadata result = readOrCreateMetadata(uid, request);

    result.setGroupId(gav.getGroupId());

    result.setArtifactId(gav.getArtifactId());

    result.setVersion(null);

    return result;
  }

  protected Metadata readOrCreateGMetadata(ArtifactStoreRequest request, Gav gav)
      throws IllegalOperationException, IOException, MetadataException
  {
    String mdPath = request.getRequestPath();

    // GAV
    mdPath = mdPath.substring(0, mdPath.lastIndexOf(RepositoryItemUid.PATH_SEPARATOR));

    // GA
    mdPath = mdPath.substring(0, mdPath.lastIndexOf(RepositoryItemUid.PATH_SEPARATOR));

    // G
    mdPath = mdPath.substring(0, mdPath.lastIndexOf(RepositoryItemUid.PATH_SEPARATOR)) + "/maven-metadata.xml";

    RepositoryItemUid uid = request.getMavenRepository().createUid(mdPath);

    Metadata result = readOrCreateMetadata(uid, request);

    result.setGroupId(null);

    result.setArtifactId(null);

    result.setVersion(null);

    return result;
  }

  protected void writeGAVMetadata(ArtifactStoreRequest request, Gav gav, Metadata md)
      throws UnsupportedStorageOperationException, IllegalOperationException, MetadataException, IOException
  {
    String mdPath = request.getRequestPath();

    // GAV
    mdPath = mdPath.substring(0, mdPath.lastIndexOf(RepositoryItemUid.PATH_SEPARATOR)) + "/maven-metadata.xml";

    RepositoryItemUid uid = request.getMavenRepository().createUid(mdPath);

    writeMetadata(uid, request.getRequestContext(), md);
  }

  protected void writeGAMetadata(ArtifactStoreRequest request, Gav gav, Metadata md)
      throws UnsupportedStorageOperationException, IllegalOperationException, MetadataException, IOException
  {
    String mdPath = request.getRequestPath();

    // GAV
    mdPath = mdPath.substring(0, mdPath.lastIndexOf(RepositoryItemUid.PATH_SEPARATOR));

    // GA
    mdPath = mdPath.substring(0, mdPath.lastIndexOf(RepositoryItemUid.PATH_SEPARATOR)) + "/maven-metadata.xml";

    RepositoryItemUid uid = request.getMavenRepository().createUid(mdPath);

    writeMetadata(uid, request.getRequestContext(), md);
  }

  protected void writeGMetadata(ArtifactStoreRequest request, Gav gav, Metadata md)
      throws UnsupportedStorageOperationException, IllegalOperationException, MetadataException, IOException
  {
    String mdPath = request.getRequestPath();

    // GAV
    mdPath = mdPath.substring(0, mdPath.lastIndexOf(RepositoryItemUid.PATH_SEPARATOR));

    // GA
    mdPath = mdPath.substring(0, mdPath.lastIndexOf(RepositoryItemUid.PATH_SEPARATOR));

    // G
    mdPath = mdPath.substring(0, mdPath.lastIndexOf(RepositoryItemUid.PATH_SEPARATOR)) + "/maven-metadata.xml";

    RepositoryItemUid uid = request.getMavenRepository().createUid(mdPath);

    writeMetadata(uid, request.getRequestContext(), md);
  }

}
