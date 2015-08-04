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
package org.sonatype.nexus.obr.metadata;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;

import org.sonatype.nexus.mime.MimeSupport;
import org.sonatype.nexus.obr.util.ObrUtils;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.FileContentLocator;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;

import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.util.StringUtils;
import org.osgi.impl.bundle.obr.resource.ResourceImpl;
import org.osgi.impl.bundle.obr.resource.ResourceImpl.UrlTransformer;
import org.osgi.service.obr.Resource;

/**
 * Default {@link ObrResourceWriter} that writes to an item in a Nexus repository.
 */
public class DefaultObrResourceWriter
    implements ObrResourceWriter
{
  private final File file;

  private final MimeSupport mimeSupport;

  private final PrintWriter pw;

  private final Repository repository;

  private final String path;

  private final UrlTransformer urlTransformer;

  private boolean isComplete = false;

  /**
   * Creates a new {@link ObrResourceWriter} that writes to the item referred to by the given UID.
   *
   * @param uid the target UID
   */
  public DefaultObrResourceWriter(final RepositoryItemUid uid, final File temporaryDirectory,
                                  final MimeSupport mimeSupport)
      throws IOException
  {
    this.mimeSupport = mimeSupport;

    // use a temporary file while we are streaming resources
    file = File.createTempFile("obr", ".xml", temporaryDirectory);
    pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8")));

    repository = uid.getRepository();
    path = uid.getPath();

    // this makes sure we maintain the correct relative paths in the OBR
    urlTransformer = ObrUtils.getUrlChomper(new URL("file:"), path);

    // name will be enclosed by ' so replace any occurrences in the name
    final String name = StringUtils.replace(repository.getName(), '\'', '`');
    final long now = System.currentTimeMillis();

    // standard header, assumes UTF-8 output
    pw.print("<?xml version='1.0' encoding='utf-8'?>\n");
    pw.print("<?xml-stylesheet type='text/xsl' href='http://www2.osgi.org/www/obr2html.xsl'?>\n");
    pw.print("<repository name='" + name + "' lastmodified='" + now + "'>");
  }

  public void append(final Resource resource)
      throws IOException
  {
    if (isComplete) {
      throw new StorageException("OBR metadata is already complete");
    }

    ResourceImpl.toXML(resource, urlTransformer).print(0, pw);
  }

  public Appendable append(final CharSequence csq) {
    // just here to complete the Writer API, it's not actually used
    return pw.append(csq);
  }

  public Appendable append(final char c) {
    // just here to complete the Writer API, it's not actually used
    return pw.append(c);
  }

  public Appendable append(final CharSequence csq, final int start, final int end) {
    // just here to complete the Writer API, it's not actually used
    return pw.append(csq, start, end);
  }

  public void complete() {
    if (!isComplete) {
      pw.print("\n</repository>");
      isComplete = true;
    }
  }

  public void close()
      throws IOException
  {
    IOUtils.closeQuietly(pw);

    if (!isComplete) {
      // we don't want to overwrite the current OBR with bad data
      throw new StorageException("OBR metadata is not complete");
    }

    final ResourceStoreRequest request = new ResourceStoreRequest(path);
    final ContentLocator content = new FileContentLocator(file,
        mimeSupport.guessMimeTypeFromPath(file.getAbsolutePath()));

    try {
      repository.storeItem(false, new DefaultStorageFileItem(repository, request, true, true, content));
    }
    catch (final IllegalOperationException e) {
      throw new StorageException(e);
    }
    catch (final UnsupportedStorageOperationException e) {
      throw new StorageException(e);
    }

    if (!file.delete()) {
      file.deleteOnExit(); // see if we can delete it later...
    }
  }

  public void flush()
      throws IOException
  {
    pw.flush();
  }
}
