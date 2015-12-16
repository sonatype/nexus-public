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
package org.sonatype.nexus.ruby.layout;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.zip.GZIPInputStream;
import javax.xml.bind.DatatypeConverter;

import org.sonatype.nexus.ruby.DependencyFile;
import org.sonatype.nexus.ruby.Directory;
import org.sonatype.nexus.ruby.RubygemsFile;
import org.sonatype.nexus.ruby.SpecsIndexFile;
import org.sonatype.nexus.ruby.SpecsIndexZippedFile;

/**
 * simple storage implementation using the system's filesystem.
 * it uses <code>InputStream</code>s as payload.
 *
 * @author christian
 */
public class SimpleStorage
    implements Storage
{

  static interface StreamLocation {
    InputStream openStream() throws IOException;
  }

  static class URLStreamLocation implements StreamLocation {
    private URL url;

    URLStreamLocation(URL url) {
      this.url = url;
    }

    public URLConnection openConnection() throws IOException {
        URLConnection con = url.openConnection();
        String userinfo = this.url.getUserInfo();
        if(userinfo != null) {
            String basicAuth = "Basic " + DatatypeConverter.printBase64Binary(URLDecoder.decode(userinfo, "UTF-8").getBytes(StandardCharsets.UTF_8));
            con.setRequestProperty ("Authorization", basicAuth);
        }
        return con;
    }

    @Override
    public InputStream openStream() throws IOException {
      return openConnection().getInputStream();
    }
  }

  static class BytesStreamLocation implements StreamLocation {
    private ByteArrayInputStream stream;

    BytesStreamLocation(ByteArrayInputStream stream) {
      this.stream = stream;
    }

    @Override
    public InputStream openStream() throws IOException {
      return stream;
    }
  }

  static class URLGzipStreamLocation implements StreamLocation {
    private StreamLocation stream;

    URLGzipStreamLocation(StreamLocation stream) {
      this.stream = stream;
    }

    @Override
    public InputStream openStream() throws IOException {
      return new GZIPInputStream(stream.openStream());
    }
  }
  
  
  private final SecureRandom random = new SecureRandom();

  private final File basedir;

  /**
   * create the storage with given base-directory.
   */
  public SimpleStorage(File basedir) {
    this.basedir = basedir;
    this.random.setSeed(System.currentTimeMillis());
  }

  @Override
  public InputStream getInputStream(RubygemsFile file) throws IOException {
    if (file.hasException()) {
      throw new IOException(file.getException());
    }
    InputStream is;
    if (file.get() == null) {
      is = Files.newInputStream(toPath(file));
    }
    else {
      is = ((StreamLocation) file.get()).openStream();
    }
    // reset state since we have a payload and no exceptions
    file.resetState();
    return is;
  }

  /**
   * convert <code>RubygemsFile</code> into a <code>Path</code>.
   */
  protected Path toPath(RubygemsFile file) {
    return new File(basedir, file.storagePath()).toPath();
  }

  @Override
  public long getModified(RubygemsFile file) {
    return toPath(file).toFile().lastModified();
  }

  @Override
  public void retrieve(RubygemsFile file) {
    file.resetState();

    Path path = toPath(file);
    if (Files.notExists(path)) {
      file.markAsNotExists();
    }
    else {
      try {
        set(file, path);
      }
      catch (IOException e) {
        file.setException(e);
      }
    }
  }

  @Override
  public void retrieve(DependencyFile file) {
    retrieve((RubygemsFile) file);
  }

  @Override
  public void retrieve(SpecsIndexZippedFile file) {
    retrieve((RubygemsFile) file);
  }

  @Override
  public void retrieve(SpecsIndexFile file) {
    SpecsIndexZippedFile zipped = file.zippedSpecsIndexFile();
    retrieve(zipped);
    if (zipped.notExists()) {
      file.markAsNotExists();
    }
    if (zipped.hasException()) {
      file.setException(zipped.getException());
    }
    file.set(new URLGzipStreamLocation((StreamLocation) zipped.get()));
  }

  @Override
  public void create(InputStream is, RubygemsFile file) {
    Path target = toPath(file);
    Path mutex = target.resolveSibling(target.getFileName() + ".lock");
    Path source = target.resolveSibling("tmp." + Math.abs(random.nextLong()));
    try {
      createDirectory(source.getParent());
      Files.createFile(mutex);
      Files.copy(is, source);
      Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
      set(file, target);
    }
    catch (FileAlreadyExistsException e) {
      mutex = null;
      file.markAsTempUnavailable();
    }
    catch (IOException e) {
      file.setException(e);
    }
    finally {
      if (mutex != null) {
        mutex.toFile().delete();
      }
      source.toFile().delete();
    }
  }

  /**
   * set the payload
   * @param file which gets the payload
   * @param path the path to the payload
   * @throws MalformedURLException
   */
  private void set(RubygemsFile file, Path path) throws MalformedURLException{
    file.set(new URLStreamLocation(path.toUri().toURL()));
  }

  @Override
  public void update(InputStream is, RubygemsFile file) {
    Path target = toPath(file);
    Path source = target.resolveSibling("tmp." + Math.abs(random.nextLong()));
    try {
      createDirectory(source.getParent());
      Files.copy(is, source);
      Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
      set(file, target);
    }
    catch (IOException e) {
      file.setException(e);
    }
    finally {
      source.toFile().delete();
    }
  }

  /**
   * create a directory if it is not existing
   */
  protected void createDirectory(Path parent) throws IOException {
    if (!Files.exists(parent)) {
      Files.createDirectories(parent);
    }
  }

  @Override
  public void delete(RubygemsFile file) {
    try {
      Files.deleteIfExists(toPath(file));
    }
    catch (IOException e) {
      file.setException(e);
    }
  }

  @Override
  public void memory(ByteArrayInputStream data, RubygemsFile file) {
    file.set(new BytesStreamLocation(data));
  }

  @Override
  public void memory(String data, RubygemsFile file) {
    memory(new ByteArrayInputStream(data.getBytes()), file);
  }

  @Override
  public String[] listDirectory(Directory dir) {
    String[] list = toPath(dir).toFile().list();
    if (list == null) {
        list = new String[0];
    }
    return list;
  }
}
