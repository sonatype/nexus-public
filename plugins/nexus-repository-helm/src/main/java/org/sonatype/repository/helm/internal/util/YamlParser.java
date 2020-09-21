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
package org.sonatype.repository.helm.internal.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.repository.helm.internal.metadata.ChartEntry;
import org.sonatype.repository.helm.internal.metadata.ChartIndex;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Construct;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeId;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.reader.UnicodeReader;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility methods for getting attributes from yaml files, writing to yaml files
 *
 * @since 3.next
 */
@Named
@Singleton
public class YamlParser
    extends ComponentSupport
{
  private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

  public Map<String, Object> load(InputStream is) throws IOException {
    checkNotNull(is);
    String data = IOUtils.toString(new UnicodeReader(is));

    Map<String, Object> map;

    try {
      Yaml yaml = new Yaml(new SafeConstructor());
      map = yaml.load(data);
    }
    catch (YAMLException e) {
      map = (Map<String, Object>) mapper.readValue(data, Map.class);
    }
    return map;
  }

  public String getYamlContent(final ChartIndex index) {
    Yaml yaml = new Yaml(new JodaPropertyConstructor(),
        setupRepresenter(),
        new DumperOptions(),
        new Resolver());
    return yaml.dumpAsMap(index);
  }

  public void write(final OutputStream os, final ChartIndex index) {
    try (OutputStreamWriter writer = new OutputStreamWriter(os)) {
      String result = getYamlContent(index);
      writer.write(result);
    }
    catch (IOException ex) {
      log.error("Unable to write to OutputStream for index.yaml", ex);
    }
  }

  private Representer setupRepresenter() {
    Representer representer = new JodaTimeRepresenter();

    representer.addClassTag(ChartEntry.class, Tag.MAP);

    return representer;
  }

  private class JodaPropertyConstructor
      extends Constructor
  {
    public JodaPropertyConstructor() {
      yamlClassConstructors.put(NodeId.scalar, new TimeStampConstruct());
    }

    class TimeStampConstruct
        extends Constructor.ConstructScalar
    {
      @Override
      public Object construct(Node nnode) {
        if (nnode.getTag().toString().equals("tag:yaml.org,2002:timestamp")) {
          Construct dateConstructor = yamlConstructors.get(Tag.TIMESTAMP);
          Date date = (Date) dateConstructor.construct(nnode);
          return new DateTime(date, DateTimeZone.UTC);
        }
        else {
          return super.construct(nnode);
        }
      }
    }
  }

  /**
   * Necessary to output Joda DateTime correctly with Snakey Yamls
   * See: https://bitbucket.org/asomov/snakeyaml/wiki/Howto#markdown-header-how-to-parse-jodatime
   */
  class JodaTimeRepresenter
      extends Representer
  {
    public JodaTimeRepresenter() {
      multiRepresenters.put(DateTime.class, new RepresentJodaDateTime());
    }

    /**
     * Prevents writing null values out to index.yaml, not a part of JodaTimeRepresenter necessarily
     * but included here as we are setting up a {@link Representer}
     */
    @Override
    protected NodeTuple representJavaBeanProperty(Object javaBean,
                                                  Property property,
                                                  Object propertyValue,
                                                  Tag customTag)
    {
      // if value of property is null, ignore it.
      if (propertyValue == null) {
        return null;
      }
      else {
        return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
      }
    }

    private class RepresentJodaDateTime
        extends RepresentDate
    {
      public Node representData(Object data) {
        DateTime date = (DateTime) data;
        return super.representData(new Date(date.getMillis()));
      }
    }
  }
}
