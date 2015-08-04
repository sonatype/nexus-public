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
package org.sonatype.nexus.configuration;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Objects;

import org.sonatype.nexus.util.file.DirSupport;
import org.sonatype.sisu.goodies.common.Loggers;
import org.sonatype.sisu.goodies.common.io.FileReplacer;
import org.sonatype.sisu.goodies.common.io.FileReplacer.ContentWriter;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Generic "model" utility to handle model IO independent of what technology is used for persisting POJOs to model
 * and other way around, while retaining all the benefits of proper error detection and backup files.
 *
 * @author cstamas
 * @since 2.7.0
 */
public class ModelUtils
{
  private static final Logger log = Loggers.getLogger(ModelUtils.class);

  private ModelUtils() {
    // no instance
  }

  // Stream IO

  /**
   * Model reader.
   */
  public static interface ModelReader<E>
  {
    E read(InputStream input)
        throws IOException, CorruptModelException;
  }

  /**
   * Versioned, a source of the model version that might come from it's content but does not have to.
   */
  public static interface Versioned
  {
    String readVersion(InputStream input)
        throws IOException, CorruptModelException;
  }

  /**
   * Model writer.
   */
  public static interface ModelWriter<E>
  {
    void write(OutputStream output, E model)
        throws IOException;
  }

  /**
   * Model upgrader.
   */
  public static abstract class ModelUpgrader
  {
    private final String fromVersion;

    private final String toVersion;

    public ModelUpgrader(final String fromVersion, final String toVersion) {
      this.fromVersion = checkNotNull(fromVersion);
      this.toVersion = checkNotNull(toVersion);
    }

    public final String fromVersion() {
      return fromVersion;
    }

    public final String toVersion() {
      return toVersion;
    }

    public abstract void upgrade(InputStream input, OutputStream output)
        throws IOException, CorruptModelException;
  }

  // Character Streams

  /**
   * Character models are preferred as UTF-8.
   */
  public static final Charset DEFAULT_CHARSET = Charsets.UTF_8;

  /**
   * Character model reader.
   */
  public static abstract class CharacterModelReader<E>
      implements ModelReader<E>
  {
    protected final Charset charset;

    protected CharacterModelReader() {
      this(DEFAULT_CHARSET);
    }

    protected CharacterModelReader(final Charset charset) {
      this.charset = checkNotNull(charset);
    }

    @Override
    public E read(final InputStream input) throws IOException, CorruptModelException {
      return read(new InputStreamReader(input, charset));
    }

    public abstract E read(Reader reader)
        throws IOException, CorruptModelException;
  }

  /**
   * Character model writer.
   */
  public static abstract class CharacterModelWriter<E>
      implements ModelWriter<E>
  {
    private final Charset charset;

    protected CharacterModelWriter() {
      this(DEFAULT_CHARSET);
    }

    protected CharacterModelWriter(final Charset charset) {
      this.charset = checkNotNull(charset);
    }

    @Override
    public void write(final OutputStream output, final E model) throws IOException {
      write(new OutputStreamWriter(output, charset), model);
    }

    public abstract void write(Writer writer, E model)
        throws IOException;
  }

  /**
   * Character model upgrader.
   */
  public static abstract class CharacterModelUpgrader
      extends ModelUpgrader
  {
    private final Charset charset;

    protected CharacterModelUpgrader(final String fromVersion, final String toVersion) {
      this(fromVersion, toVersion, DEFAULT_CHARSET);
    }

    protected CharacterModelUpgrader(final String fromVersion, final String toVersion, final Charset charset) {
      super(fromVersion, toVersion);
      this.charset = checkNotNull(charset);
    }

    public void upgrade(InputStream input, OutputStream output)
        throws IOException, CorruptModelException
    {
      upgrade(new InputStreamReader(input, charset), new OutputStreamWriter(output, charset));
    }

    public abstract void upgrade(Reader reader, Writer writer)
        throws IOException, CorruptModelException;
  }

  // ==

  /**
   * Exception indicating that model is unreadable by {@link ModelReader}.
   */
  public static class CorruptModelException
      extends RuntimeException
  {
    public CorruptModelException(final String message) {
      super(message);
    }

    public CorruptModelException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }

  /**
   * Exception indicating that model that is {@link Versioned} lacks the version (is non existent or is empty).
   */
  public static class MissingModelVersionException
      extends CorruptModelException
  {
    public MissingModelVersionException(final String message) {
      super(message);
    }
  }

  /**
   * Adapter for {@link ModelUpgrader} to be used with {@link FileReplacer}.
   */
  private static class ModelUpgraderAdapter
      implements ContentWriter
  {
    private final File file;

    private final ModelUpgrader modelUpgrader;

    private ModelUpgraderAdapter(final File file, final ModelUpgrader modelUpgrader) {
      this.file = checkNotNull(file);
      this.modelUpgrader = checkNotNull(modelUpgrader);
    }

    @Override
    public void write(final BufferedOutputStream output) throws IOException {
      try (final InputStream input = new BufferedInputStream(Files.newInputStream(file.toPath()))) {
        modelUpgrader.upgrade(input, output);
      }
    }
  }

  /**
   * Loads a model from a file using given reader. Also, checks the file version and if does not match with given
   * {@code currentModelVersion} will attempt upgrade using passed in upgraders.
   * <p/>
   * Note: this method method assumes few things about model: it is suited for versioned models,
   * They are expected to have a "version". The versions are opaque, they are not sorted and such, they are checked
   * for plain equality. Models <em>without</em> "version" will be rejected with CorruptModelException, kinda
   * considered corrupt.
   * <p/>
   * Also, be aware that this method, even while loading the file and converting it into POJOs, will not
   * perform any semantic validation of it, that's the caller's duty to perform. In case of IO problem, or
   * corruption failures, proper exception is thrown.
   * <p/>
   * Concurrency note: if concurrent invocation of this (thread safe method) is possible at client side,
   * it's is caller role to ensure synchronization in caller code and make sure this method is not called
   * concurrently, as IO side effects will have unexpected results. Invoking it multiple times is fine,
   * but simultaneous invocation from same component (working on same file) should not happen.
   *
   * @throws CorruptModelException if the model to be load is detected as corrupted (while loading or upgrading).
   * @throws IOException           if during load of the model an IO problem happens.
   * @since 2.7.0
   */
  public static <E> E load(final String currentModelVersion,
                           final File file,
                           final ModelReader<E> reader,
                           final ModelUpgrader... upgraders)
      throws CorruptModelException, IOException
  {
    checkNotNull(currentModelVersion, "currentModelVersion");
    checkNotNull(file, "file");
    checkNotNull(reader, "reader");
    checkNotNull(upgraders, "upgraders");
    log.info("Loading model {}", file.getAbsoluteFile(), currentModelVersion);

    try {
      if (reader instanceof Versioned) {
        final String originalFileVersion;
        try (final InputStream input = new BufferedInputStream(Files.newInputStream(file.toPath()))) {
          originalFileVersion = ((Versioned) reader).readVersion(input);
        }

        if (Strings.isNullOrEmpty(originalFileVersion)) {
          throw new MissingModelVersionException("Passed in model has null version");
        }

        if (!Objects.equals(currentModelVersion, originalFileVersion)) {
          log.info("Upgrading model {} from version {} to {}", file.getAbsoluteFile(), originalFileVersion, currentModelVersion);
          String currentFileVersion = originalFileVersion;
          final Map<String, ModelUpgrader> upgradersMap = Maps.newHashMapWithExpectedSize(upgraders.length);
          for (ModelUpgrader upgrader : upgraders) {
            upgradersMap.put(upgrader.fromVersion(), upgrader);
          }
          final FileReplacer fileReplacer = new FileReplacer(file);
          fileReplacer.setDeleteBackupFile(true);
          ModelUpgrader upgrader = upgradersMap.get(currentFileVersion);
          // backup old version
          Files.copy(file.toPath(), new File(file.getParentFile(), file.getName() + ".old").toPath(),
              StandardCopyOption.REPLACE_EXISTING);
          while (upgrader != null && !Objects.equals(currentModelVersion, currentFileVersion)) {
            try {
              fileReplacer.replace(new ModelUpgraderAdapter(file, upgrader));
            }
            catch (CorruptModelException e) {
              final CorruptModelException ex = new CorruptModelException(String
                  .format("Model %s detected as corrupt during upgrade from version %s to version %s",
                      file.getAbsolutePath(), upgrader.fromVersion(),
                      upgrader.toVersion()), e);
              throw ex;
            }
            catch (IOException e) {
              final IOException ex = new IOException(String
                  .format("IO problem during upgrade from version %s to version %s of %s", upgrader.fromVersion(),
                      upgrader.toVersion(), file.getAbsolutePath()), e);
              throw ex;
            }
            currentFileVersion = upgrader.toVersion();
            upgrader = upgradersMap.get(currentFileVersion);
          }

          if (!Objects.equals(currentModelVersion, currentFileVersion)) {
            // upgrade failed
            throw new IOException(String
                .format(
                    "Could not upgrade model %s to version %s, is upgraded to %s, originally was %s, available upgraders exists for versions %s",
                    file.getAbsolutePath(), currentModelVersion, currentFileVersion, originalFileVersion,
                    upgradersMap.keySet()));
          }
        }
      }

      try (final InputStream input = new BufferedInputStream(Files.newInputStream(file.toPath()))) {
        E model = reader.read(input);
        // model.setVersion(currentModelVersion);
        return model;
      }
    }
    catch (NoSuchFileException e) {
      // TODO: "translate" to old FileNotFoundEx as we have existing code relying on this exception
      // Having the new NoSuchFileEx does not buy us much, as two classes are almost identical
      final FileNotFoundException fnf = new FileNotFoundException(e.getMessage());
      fnf.initCause(e);
      throw fnf;
    }
  }

  /**
   * Saves a model to a file using given writer, keeping the a backup of the file.
   * <p/>
   * Concurrency note: if concurrent invocation of this (thread safe method) is possible at client side,
   * it's is caller role to ensure synchronization in caller code and make sure this method is not called
   * concurrently, as IO side effects will have unexpected results. Invoking it multiple times is fine,
   * but simultaneous invocation from same component (working on same file) should not happen.
   *
   * @since 2.7.0
   */
  public static <E> void save(final E model, final File file, final ModelWriter<E> writer) throws IOException {
    checkNotNull(model, "model");
    checkNotNull(file, "File");
    checkNotNull(writer, "ModelWriter");
    log.info("Saving model {}", file.getAbsoluteFile());
    DirSupport.mkdir(file.getParentFile().toPath());
    final File backupFile = new File(file.getParentFile(), file.getName() + ".bak");
    final FileReplacer fileReplacer = new FileReplacer(file);
    fileReplacer.setDeleteBackupFile(false);
    fileReplacer.replace(new ContentWriter()
    {
      @Override
      public void write(final BufferedOutputStream output) throws IOException {
        writer.write(output, model);
        output.flush();
      }
    });
    if (Files.exists(fileReplacer.getBackupFile().toPath())) {
      Files.copy(fileReplacer.getBackupFile().toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
      Files.delete(fileReplacer.getBackupFile().toPath());
    }
  }
}
