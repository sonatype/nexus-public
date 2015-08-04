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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.configuration.ModelUtils.CharacterModelReader;
import org.sonatype.nexus.configuration.ModelUtils.CharacterModelUpgrader;
import org.sonatype.nexus.configuration.ModelUtils.CharacterModelWriter;
import org.sonatype.nexus.configuration.ModelUtils.CorruptModelException;
import org.sonatype.nexus.configuration.ModelUtils.MissingModelVersionException;
import org.sonatype.nexus.configuration.ModelUtils.ModelReader;
import org.sonatype.nexus.configuration.ModelUtils.ModelUpgrader;
import org.sonatype.nexus.configuration.ModelUtils.Versioned;
import org.sonatype.nexus.configuration.model.CProps;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A simple utilities to handle Modello generated models CProps to Map converter, to ease handling of CProps. All
 * these methods are specific to Modello use in Nexus, but still, are generic enough to be used by Nexus Plugins
 * if needed. This class contains methods added in 2.7.0 that are "specialization" of methods from {@link ModelUtils},
 * suited for <a href="https://github.com/sonatype/modello">Modello</a> generated models using Xpp3 IO (XML Pull
 * Parser).
 *
 * @author cstamas
 */
public class ModelloUtils
{
  private ModelloUtils() {
    // no instance
  }

  // ==

  /**
   * {@link List} of {@link CProps} to {@link Map} converter, to ease handling of these thingies.
   */
  public static Map<String, String> getMapFromConfigList(List<CProps> list) {
    final Map<String, String> result = Maps.newHashMapWithExpectedSize(list.size());
    for (CProps props : list) {
      result.put(props.getKey(), props.getValue());
    }
    return result;
  }

  /**
   * {@link Map} to {@link List} of {@link CProps} converter, to ease handling of these thingies.
   */
  public static List<CProps> getConfigListFromMap(final Map<String, String> map) {
    final List<CProps> result = Lists.newArrayListWithExpectedSize(map.size());
    for (Map.Entry<String, String> entry : map.entrySet()) {
      final CProps cprop = new CProps();
      cprop.setKey(entry.getKey());
      cprop.setValue(entry.getValue());
      result.add(cprop);
    }
    return result;
  }

  // == Model IO

  /**
   * Modello models are by default all UTF-8.
   *
   * @since 2.7.0
   */
  public static final Charset DEFAULT_CHARSET = Charsets.UTF_8;

  /**
   * Modello model reader. See {@link CharacterModelReader}. This class adds XmlPullParserException exception
   * conversion.
   *
   * @since 2.7.0
   */
  public static abstract class ModelloModelReader<E>
      extends CharacterModelReader<E>
  {
    protected ModelloModelReader() {
      this(DEFAULT_CHARSET);
    }

    protected ModelloModelReader(final Charset charset) {
      super(charset);
    }

    @Override
    public E read(final Reader reader) throws IOException, CorruptModelException {
      try {
        return doRead(reader);
      }
      catch (XmlPullParserException e) {
        throw new CorruptModelException("Model corrupted" + e.getMessage(), e);
      }
    }

    public abstract E doRead(Reader reader)
        throws IOException, XmlPullParserException;
  }

  /**
   * Versioned Modello helper. Versioning here assumes that Modello uses XML and it contains a node named as
   * {@code fieldName} parameter, that contains some string value. This helper class is usable when extending {@link
   * ModelloModelReader},
   * implement {@link Versioned} and use this class constructed as appropriate.
   *
   * @since 2.7.0
   */
  public static class VersionedInFieldXmlModelloModelHelper
  {
    private final Charset charset;

    private final String fieldName;

    public VersionedInFieldXmlModelloModelHelper(final String fieldName) {
      this(DEFAULT_CHARSET, fieldName);
    }

    public VersionedInFieldXmlModelloModelHelper(final Charset charset, final String fieldName) {
      this.charset = checkNotNull(charset);
      this.fieldName = checkNotNull(fieldName);
    }

    public String readVersion(final InputStream input) throws IOException, CorruptModelException {
      try (final Reader r = new InputStreamReader(input, charset)) {
        try {
          final Xpp3Dom dom = Xpp3DomBuilder.build(r);
          final Xpp3Dom versionNode = dom.getChild(fieldName);
          if (versionNode != null) {
            final String originalFileVersion = versionNode.getValue();
            if (Strings.isNullOrEmpty(originalFileVersion)) {
              throw new MissingModelVersionException("Passed in XML model have empty " + fieldName + " node");
            }
            return originalFileVersion;
          }
          else {
            throw new MissingModelVersionException("Passed in XML model does not have " + fieldName + " node");
          }
        }
        catch (XmlPullParserException e) {
          throw new CorruptModelException("Passed in XML model cannot be parsed", e);
        }
      }
    }
  }


  /**
   * Modello model writer. See {@link CharacterModelWriter}.
   *
   * @since 2.7.0
   */
  public static abstract class ModelloModelWriter<E>
      extends CharacterModelWriter<E>
  {
    protected ModelloModelWriter() {
      this(DEFAULT_CHARSET);
    }

    protected ModelloModelWriter(final Charset charset) {
      super(charset);
    }
  }

  /**
   * Modello model upgrader. See {@link CharacterModelUpgrader}. This class adds XmlPullParserException exception
   * conversion.
   *
   * @since 2.7.0
   */
  public static abstract class ModelloModelUpgrader
      extends CharacterModelUpgrader
  {
    private final Charset charset;

    protected ModelloModelUpgrader(final String fromVersion, final String toVersion) {
      this(fromVersion, toVersion, DEFAULT_CHARSET);
    }

    protected ModelloModelUpgrader(final String fromVersion, final String toVersion, final Charset charset) {
      super(fromVersion, toVersion);
      this.charset = checkNotNull(charset);
    }

    @Override
    public void upgrade(final Reader reader, final Writer writer)
        throws IOException, CorruptModelException
    {
      try {
        doUpgrade(reader, writer);
      }
      catch (XmlPullParserException e) {
        throw new CorruptModelException("Model corrupted" + e.getMessage(), e);
      }
    }

    public abstract void doUpgrade(Reader reader, Writer writer)
        throws IOException, XmlPullParserException;
  }


  /**
   * See {@link ModelUtils#load(String, File, ModelReader, ModelUpgrader...)}.
   *
   * @since 2.7.0
   */
  public static <E> E load(final String currentModelVersion,
                           final File file,
                           final ModelloModelReader<E> reader,
                           final ModelloModelUpgrader... upgraders)
      throws CorruptModelException, IOException
  {
    return ModelUtils.load(currentModelVersion, file, reader, upgraders);
  }

  /**
   * See {@link ModelloUtils#save(Object, File, ModelloModelWriter)}.
   *
   * @since 2.7.0
   */
  public static <E> void save(final E model, final File file, final ModelloModelWriter<E> writer) throws IOException {
    ModelUtils.save(model, file, writer);
  }
}
