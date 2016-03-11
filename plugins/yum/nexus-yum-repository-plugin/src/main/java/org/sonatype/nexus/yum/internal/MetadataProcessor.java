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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.PreparedContentLocator;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.Repository;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.io.CountingInputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import static javax.xml.xpath.XPathConstants.NODE;
import static org.sonatype.nexus.util.DigesterUtils.getDigest;
import static org.sonatype.nexus.yum.Yum.PATH_OF_REPOMD_XML;

/**
 * Utilities for rewriting YUM metadata.
 *
 * @since 2.11
 */
public class MetadataProcessor
{

  private static final Logger log = LoggerFactory.getLogger(MetadataProcessor.class);

  /**
   * Location tag.
   */
  private static final String LOCATION = "location";

  /**
   * Attribute of {@link #LOCATION} tag.
   */
  private static final String LOCATION_XML_BASE = "xml:base";

  /**
   * Attribute of {@link #LOCATION} tag.
   */
  private static final String LOCATION_HREF = "href";

  private MetadataProcessor() {
  }

  /**
   * Processes metadata:
   * - Rewrites locations in primary.xml after a merge. Locations are wrongly written as file urls of merged group
   * repository base dirs, which will be rewritten to be relative to group repository.
   * - Removes sqlite databases from repomd.xml.
   *
   * @param repository                 containing yum repository
   * @param memberRepositoriesBaseDirs list of merged group repository base dirs
   * @return true if primary.xml/repomd.xml was changed
   */
  public static boolean processMergedMetadata(final Repository repository,
                                              final List<File> memberRepositoriesBaseDirs)
  {
    log.debug("Checking if {}:primary.xml locations should be rewritten after merge", repository.getId());
    return processMetadata(
        repository,
        new Processor()
        {
          @Override
          public boolean process(final Element location) {
            String xmlBase = location.getAttribute(LOCATION_XML_BASE);
            if (xmlBase != null) {
              String href = location.getAttribute(LOCATION_HREF);
              if (!xmlBase.endsWith("/")) {
                xmlBase += "/";
              }
              href = xmlBase + href;
              for (File memberReposBaseDir : memberRepositoriesBaseDirs) {
                String memberRepoDirPath = memberReposBaseDir.getPath();
                int pos = href.indexOf(memberRepoDirPath);
                if (pos > -1) {
                  href = href.substring(pos + memberRepoDirPath.length());
                  if (href.startsWith("/")) {
                    href = href.substring(1);
                  }
                  location.setAttribute(LOCATION_HREF, href);
                  location.removeAttribute(LOCATION_XML_BASE);
                  return true;
                }
              }
            }
            return false;
          }
        }
    );
  }

  /**
   * Processes metadata:
   * - Rewrites locations in primary.xml after it had been proxied. All locations that have an xml:base + url matching
   * repository url will be changed to be relative to repository.
   * - Removes sqlite databases from repomd.xml.
   *
   * @param repository containing yum repository
   * @return true if primary.xml/repomd.xml was changed
   */
  public static boolean processProxiedMetadata(final ProxyRepository repository) {
    log.debug("Checking if {}:primary.xml locations should be rewritten after being proxied", repository.getId());
    final String repositoryUrl = repository.getRemoteUrl();
    return processMetadata(
        repository,
        new Processor()
        {
          @Override
          public boolean process(final Element location) {
            String xmlBase = location.getAttribute(LOCATION_XML_BASE);
            if (xmlBase != null) {
              String href = location.getAttribute(LOCATION_HREF);
              if (!xmlBase.endsWith("/")) {
                xmlBase += "/";
              }
              href = xmlBase + href;
              if (href.startsWith(repositoryUrl)) {
                href = href.substring(repositoryUrl.length());
                if (href.startsWith("/")) {
                  href = href.substring(1);
                }
                location.setAttribute(LOCATION_HREF, href);
                location.removeAttribute(LOCATION_XML_BASE);
                return true;
              }
            }
            return false;
          }
        }
    );
  }

  /**
   * Processes metadata:
   * - Use processor to process all locations in primary.xml.
   * - Update primary data entry in repomd.xml if primary.xml changes.
   * - Removes location/@xml:base attributes if any.
   * - Removes sqllite from repomd.xml.
   *
   * @param repository containing primary.xml
   * @param processor  location processor
   * @return true if primary.xml/repomd.xml was changed
   */
  private static boolean processMetadata(final Repository repository, final Processor processor) {
    try {
      Document repoMDDoc = parseRepoMD(repository);
      String primaryHref = processPrimary(repository, processor, repoMDDoc);
      boolean changed = updatePrimaryInRepoMD(repository, repoMDDoc, primaryHref);
      changed = removeLocationXmlBaseInRepoMD(repository, repoMDDoc) || changed;
      changed = removeSqliteFromRepoMD(repository, repoMDDoc) || changed;
      if (changed) {
        storeRepoMD(repository, repoMDDoc);
      }
      return changed;
    }
    catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  /**
   * Read and process all location entries using provided processor. If there are changes to locations will save the
   * new primary.xml.
   *
   * @param repository repository containing primary.xml
   * @param processor  location processor
   * @param repoMDDoc  parsed repomx.xml
   * @return path of primary.xml
   */
  private static String processPrimary(final Repository repository,
                                       final Processor processor,
                                       final Document repoMDDoc)
      throws Exception
  {
    XPath xPath = XPathFactory.newInstance().newXPath();
    String primaryHref = xPath.compile("/repomd/data[@type='primary']/location/@href").evaluate(repoMDDoc);
    String primaryChecksum = xPath.compile("/repomd/data[@type='primary']/checksum").evaluate(repoMDDoc);

    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

    StorageFileItem primaryItem = (StorageFileItem) repository.retrieveItem(
        false, new ResourceStoreRequest("/" + primaryHref)
    );
    Document doc;
    boolean changed = false;
    try (InputStream primaryIn = new GZIPInputStream(new BufferedInputStream(primaryItem.getInputStream()))) {
      doc = documentBuilder.parse(primaryIn);
      NodeList locations = doc.getElementsByTagName(LOCATION);
      if (locations != null) {
        for (int i = 0; i < locations.getLength(); i++) {
          Element location = (Element) locations.item(i);
          if (processor.process(location)) {
            changed = true;
          }
        }
      }
    }
    if (changed) {
      log.debug("Rewriting locations in {}:primary.xml", repository.getId());
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      transformer.transform(new DOMSource(doc), new StreamResult(out));
      byte[] primaryContent = compress(out.toByteArray());
      if (primaryHref.contains(primaryChecksum)) {
        repository.deleteItem(false, new ResourceStoreRequest("/" + primaryHref));
        primaryHref = primaryHref.replace(
            primaryChecksum, getDigest("SHA-256", new ByteArrayInputStream(primaryContent))
        );
      }
      storeItem(repository, primaryHref, primaryContent, "application/x-gzip");
    }
    return primaryHref;
  }

  /**
   * Store primary.xml and update content of repomd.xml accordingly.
   *
   * @param repository  repository containing primary.xml/repomd.xml
   * @param repoMDDoc   parsed repomd.xml
   * @param primaryPath path of primary.xml
   * @return true if repomd.xml changed
   */
  private static boolean updatePrimaryInRepoMD(final Repository repository,
                                               final Document repoMDDoc,
                                               final String primaryPath)
      throws Exception
  {
    XPath xPath = XPathFactory.newInstance().newXPath();
    String primaryHref = xPath.compile("/repomd/data[@type='primary']/location/@href").evaluate(repoMDDoc);

    if (!Objects.equals(primaryPath, primaryHref)) {
      log.debug("Updating 'primary' data entry in {}:repomd.xml", repository.getId());

      Element primaryEl = (Element) xPath.compile("/repomd/data[@type='primary']").evaluate(repoMDDoc, NODE);

      StorageFileItem primaryItem = (StorageFileItem) repository.retrieveItem(
          false, new ResourceStoreRequest("/" + primaryPath)
      );
      try (InputStream in = primaryItem.getInputStream();
           CountingInputStream cis = new CountingInputStream(new GZIPInputStream(new BufferedInputStream(in)))) {
        primaryEl.getElementsByTagName("open-checksum").item(0).setTextContent(String.valueOf(
            getDigest("SHA-256", cis)
        ));
        primaryEl.getElementsByTagName("open-size").item(0).setTextContent(String.valueOf(
            cis.getCount()
        ));
      }

      primaryItem = (StorageFileItem) repository.retrieveItem(
          false, new ResourceStoreRequest("/" + primaryPath)
      );
      try (InputStream in = primaryItem.getInputStream();
           CountingInputStream cis = new CountingInputStream(new BufferedInputStream(in))) {
        primaryEl.getElementsByTagName("checksum").item(0).setTextContent(String.valueOf(
            getDigest("SHA-256", cis)
        ));
        primaryEl.getElementsByTagName("size").item(0).setTextContent(String.valueOf(
            cis.getCount()
        ));
      }

      ((Element) primaryEl.getElementsByTagName(LOCATION).item(0)).setAttribute(LOCATION_HREF, primaryPath);

      return true;
    }
    return false;
  }

  /**
   * Remove location tag's xml:base attribute from repomd.xml
   *
   * @param repository containing repomd.xml
   * @return true if repomd.xml was changed
   */
  private static boolean removeLocationXmlBaseInRepoMD(final Repository repository, final Document repoMDDoc) {
    boolean changed = false;
    NodeList locationNodes = repoMDDoc.getElementsByTagName(LOCATION);
    for (int i = 0; i < locationNodes.getLength(); i++) {
      Element location = (Element) locationNodes.item(i);
      if (location.hasAttribute(LOCATION_XML_BASE)) {
        location.removeAttribute(LOCATION_XML_BASE);
        changed = true;
      }
    }
    if (changed) {
      log.debug("Removed location/@xml:base attributes from {}:repomd.xml", repository.getId());
    }

    return changed;
  }

  /**
   * Remove references to sqlite from repomd.xml
   *
   * @param repository containing repomd.xml
   * @return true if repomd.xml was changed
   */
  private static boolean removeSqliteFromRepoMD(final Repository repository, final Document repoMDDoc)
      throws Exception
  {
    boolean changed = false;
    List<Element> elementsToRemove = Lists.newArrayList();
    NodeList dataNodes = repoMDDoc.getElementsByTagName("data");
    for (int i = 0; i < dataNodes.getLength(); i++) {
      Element data = (Element) dataNodes.item(i);
      if (data.getAttribute("type").endsWith("_db")) {
        elementsToRemove.add(data);
        changed = true;
      }
    }
    if (changed) {
      log.debug("Removing sqllite from {}:repomd.xml", repository.getId());
      for (Element element : elementsToRemove) {
        element.getParentNode().removeChild(element);
      }
    }

    return changed;
  }

  private static void storeRepoMD(final Repository repository, final Document repoMDDoc)
      throws Exception
  {
    Transformer transformer = TransformerFactory.newInstance().newTransformer();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    transformer.transform(new DOMSource(repoMDDoc), new StreamResult(out));
    storeItem(repository, PATH_OF_REPOMD_XML, out.toByteArray(), "application/xml");
  }

  /**
   * Store repository item.
   *
   * @param repository containing item to be stored
   * @param path       of item to be stored
   * @param content    of item to be stored
   * @param mimeType   of item to be stored
   */
  private static void storeItem(final Repository repository,
                                final String path,
                                final byte[] content,
                                final String mimeType)
      throws Exception
  {
    log.debug("Storing {}:{}", repository.getId(), path);
    DefaultStorageFileItem item = new DefaultStorageFileItem(
        repository,
        new ResourceStoreRequest("/" + path),
        true,
        true,
        new PreparedContentLocator(new ByteArrayInputStream(content), mimeType, ContentLocator.UNKNOWN_LENGTH)
    );

    repository.storeItem(false, item);
  }

  /**
   * GZip provided bytes.
   *
   * @param bytes to be compressed
   * @return compressed bytes
   */
  private static byte[] compress(final byte[] bytes)
      throws Exception
  {
    byte[] primaryCompressedBytes;
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
         OutputStream gzos = new GZIPOutputStream(baos)) {
      IOUtils.copy(new ByteArrayInputStream(bytes), gzos);
      gzos.close();
      primaryCompressedBytes = baos.toByteArray();
    }
    return primaryCompressedBytes;
  }

  /**
   * Read content of repomd.xml.
   *
   * @param repository repository containing repomd.xml
   * @return parsed repomd.xml
   */
  private static Document parseRepoMD(final Repository repository)
      throws Exception
  {
    StorageFileItem repoMDItem = (StorageFileItem) repository.retrieveItem(
        false, new ResourceStoreRequest("/" + PATH_OF_REPOMD_XML)
    );
    try (InputStream in = repoMDItem.getInputStream()) {
      DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
      return documentBuilder.parse(in);
    }
  }

  /**
   * Location processor.
   */
  private static interface Processor
  {
    boolean process(Element location);
  }

}
