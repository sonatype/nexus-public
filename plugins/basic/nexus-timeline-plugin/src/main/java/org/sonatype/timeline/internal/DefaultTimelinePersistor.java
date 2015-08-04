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
package org.sonatype.timeline.internal;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sonatype.nexus.util.file.DirSupport;
import org.sonatype.timeline.TimelineCallback;
import org.sonatype.timeline.TimelineConfiguration;
import org.sonatype.timeline.TimelineRecord;
import org.sonatype.timeline.proto.TimeLineRecordProtos;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;

/**
 * The class doing persitence of timeline records using Protobuf.
 *
 * @author juven
 * @author cstamas
 */
public class DefaultTimelinePersistor
{
  // == V3

  private static final String V3_DATA_FILE_NAME_PREFIX = "timeline.";

  private static final String V3_DATA_FILE_NAME_SUFFIX = "-v3.dat";

  private static final String V3_DATA_FILE_NAME_DATE_FORMAT = "yyyy-MM-dd.HH-mm-ssZ";

  private static final Pattern V3_DATA_FILE_NAME_PATTERN = Pattern.compile("^"
      + V3_DATA_FILE_NAME_PREFIX.replace(".", "\\.") + "(\\d{4}-\\d{2}-\\d{2}\\.\\d{2}-\\d{2}-\\d{2}[+-]\\d{4})"
      + V3_DATA_FILE_NAME_SUFFIX.replace(".", "\\.") + "$");

  // ==

  private int rollingIntervalMillis;

  private File persistDirectory;

  private long lastRolledTimestamp = 0L;

  private File lastRolledFile;

  // ==
  // Public API

  /**
   * Protected by DefaultTimeline, as is called from start(), that is exclusive access, so no other call might fall
   * in.
   */
  protected synchronized void setConfiguration(final TimelineConfiguration configuration) {
    if (!configuration.getPersistDirectory().exists()) {
      try {
        DirSupport.mkdir(configuration.getPersistDirectory().toPath());
      }
      catch (IOException e) {
        Throwables.propagate(e);
      }
    }
    persistDirectory = configuration.getPersistDirectory();
    rollingIntervalMillis = configuration.getPersistRollingIntervalMillis();
  }

  /**
   * Persistor writes to file, so we must ensure write request are coming in one by one.
   */
  protected synchronized void persist(final TimelineRecord... records)
      throws IOException
  {
    verify(records);
    try (OutputStream out = new BufferedOutputStream(new FileOutputStream(getDataFile(), true))) {
      for (TimelineRecord record : records) {
        toProto(record).writeDelimitedTo(out);
      }
      out.flush();
    }
  }

  /**
   * Only one method setting AND reading lastRolledTimestamp and lastRolledFile, called only from #persist that is
   * already synced.
   */
  protected File getDataFile()
      throws IOException
  {
    final long now = System.currentTimeMillis();
    if (lastRolledTimestamp == 0L || (now - lastRolledTimestamp) > (rollingIntervalMillis * 1000)) {
      lastRolledTimestamp = now;
      lastRolledFile = new File(persistDirectory, buildTimestampedFileName(now));
      lastRolledFile.createNewFile();
    }
    return lastRolledFile;
  }

  @VisibleForTesting
  protected void readAll(final TimelineCallback callback)
      throws IOException
  {
    readAllSinceDays(Integer.MAX_VALUE, callback);
  }

  /**
   * This method is called only from timeline's repair method, that gains exclusive access to indexer, but also it's
   * own state (wrt start/stop), meaning the configuration, hence the single field will no be modified.
   */
  protected void readAllSinceDays(final int days, final TimelineCallback callback)
      throws IOException
  {
    final List<File> result = collectFiles(days, true);
    int filePtr = 0;
    Iterator<TimelineRecord> currentIterator = null;
    while (true) {
      if (currentIterator != null && currentIterator.hasNext()) {
        if (!callback.processNext(currentIterator.next())) {
          break;
        }
      }
      else if (filePtr >= result.size()) {
        // no more
        break;
      }
      else {
        final File file = result.get(filePtr);
        // jump to next file
        currentIterator = readFile(file);
        filePtr++;
      }
    }
  }

  protected List<File> collectFiles(final int days, final boolean youngerThan) {
    // read data files
    final File[] files = persistDirectory.listFiles(new FilenameFilter()
    {
      public boolean accept(File dir, String fname) {
        return V3_DATA_FILE_NAME_PATTERN.matcher(fname).matches();
      }
    });

    // do we have any?
    if (files == null || files.length == 0) {
      return Collections.emptyList();
    }

    // sort it, youngest goes 1st
    Arrays.sort(files, new Comparator<File>()
    {
      public int compare(File f1, File f2) {
        final long f1ts = getTimestampedFileNameTimestamp(f1);
        final long f2ts = getTimestampedFileNameTimestamp(f2);

        // "reverse" the sort, we need newest-first
        final long result = -(f1ts - f2ts);

        if (result < 0) {
          return -1;
        }
        else if (result > 0) {
          return 1;
        }
        else {
          return 0;
        }
      }
    });

    // get the "last applicable" file time stamp if needed: it is the youngest (1st) minus (going into past) as many
    // days as wanted.
    final long oldestFileTimestampThreshold =
        Integer.MAX_VALUE == days ? 0
            : (getTimestampedFileNameTimestamp(files[0]) - (days * 24L * 60L * 60L * 1000L));

    // "cut"/filter the files
    final ArrayList<File> result = new ArrayList<File>();

    for (File file : files) {
      if (youngerThan) {
        // we collect files between "now" and "now+days"
        if (oldestFileTimestampThreshold <= getTimestampedFileNameTimestamp(file)) {
          result.add(file);
        }
        else {
          // we have sorted array, so we can bail out, we know that older files will come only
          break;
        }
      }
      else {
        // we collect files between "now+days" and "last file"
        if (oldestFileTimestampThreshold > getTimestampedFileNameTimestamp(file)) {
          result.add(file);
        }
      }
    }

    return result;
  }

  /**
   * Reads a whole file into memory, and in case of any problem, it returns an empty collection, making this file to
   * be skipped.
   */
  protected Iterator<TimelineRecord> readFile(File file) {
    final ArrayList<TimelineRecord> result = new ArrayList<TimelineRecord>();
    try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
      // V3 uses delimited format
      TimelineRecord rec = fromProto(TimeLineRecordProtos.TimeLineRecord.parseDelimitedFrom(in));
      while (rec != null) {
        result.add(rec);
        rec = fromProto(TimeLineRecordProtos.TimeLineRecord.parseDelimitedFrom(in));
      }
    }
    catch (Exception e) {
      // just ignore it
    }

    return result.iterator();
  }

  protected TimelineRecord fromProto(TimeLineRecordProtos.TimeLineRecord rec) {
    if (rec == null) {
      return null;
    }
    final Map<String, String> dataMap = new HashMap<String, String>();
    for (TimeLineRecordProtos.TimeLineRecord.Data data : rec.getDataList()) {
      dataMap.put(data.getKey(), data.getValue());
    }
    return new TimelineRecord(rec.getTimestamp(), rec.getType(), rec.getSubType(), dataMap);
  }

  protected int purge(final int days)
      throws IOException
  {
    final List<File> purgedFiles = collectFiles(days, false);
    // start with oldest and go to newest
    Collections.reverse(purgedFiles);
    for (File file : purgedFiles) {
      if (!file.delete()) {
        // FIXME: nag?
      }
    }
    return purgedFiles.size();
  }

  // ==

  /**
   * Builds a file name using provided timestamp.
   *
   * @return the formatted filename that matches pattern of persist files.
   */
  protected String buildTimestampedFileName(final long timestamp) {
    final SimpleDateFormat dateFormat = new SimpleDateFormat(V3_DATA_FILE_NAME_DATE_FORMAT);
    final StringBuilder fileName = new StringBuilder();
    fileName.append(V3_DATA_FILE_NAME_PREFIX).append(dateFormat.format(new Date(timestamp))).append(
        V3_DATA_FILE_NAME_SUFFIX);
    return fileName.toString();
  }

  protected long getTimestampedFileNameTimestamp(final File file) {
    final Matcher v3FnMatcher = V3_DATA_FILE_NAME_PATTERN.matcher(file.getName());
    if (v3FnMatcher.matches()) {
      final String datePattern = v3FnMatcher.group(1);
      try {
        return new SimpleDateFormat(V3_DATA_FILE_NAME_DATE_FORMAT).parse(datePattern).getTime();
      }
      catch (ParseException e) {
        // silently go to next try
      }
    }

    // fallback to lastModified
    return file.lastModified();
  }

  protected TimeLineRecordProtos.TimeLineRecord toProto(final TimelineRecord record) {
    final TimeLineRecordProtos.TimeLineRecord.Builder builder = TimeLineRecordProtos.TimeLineRecord.newBuilder();
    builder.setTimestamp(record.getTimestamp());
    builder.setType(record.getType());
    builder.setSubType(record.getSubType());
    for (Map.Entry<String, String> entry : record.getData().entrySet()) {
      builder.addData(TimeLineRecordProtos.TimeLineRecord.Data.newBuilder().setKey(entry.getKey()).setValue(
          entry.getValue()).build());
    }
    return builder.build();
  }

  protected void verify(final TimelineRecord... records)
      throws IOException
  {
    if (records.length == 0) {
      throw new IllegalArgumentException("Timeline records array is empty");
    }
    for (TimelineRecord record : records) {
      if (record == null) {
        throw new IllegalArgumentException("Timeline record is null");
      }
      final Map<String, String> data = record.getData();
      if (data == null) {
        return;
      }
      for (Map.Entry<String, String> entry : data.entrySet()) {
        if (entry.getKey() == null) {
          throw new IllegalArgumentException("Timeline record contains invalid data: key is null.");
        }
        if (entry.getValue() == null) {
          throw new IllegalArgumentException("Timeline record contains invalid data: value is null.");
        }
      }
    }
  }
}
