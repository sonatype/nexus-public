/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.exam.karaf.container.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;

public class KarafPropertiesFile {

    private final Properties properties;
    private final File propertyFile;

    public KarafPropertiesFile(File karafHome, String location) {
        if (location.startsWith("/")) {
            propertyFile = new File(karafHome + location);
        } 
        else {
            propertyFile = new File(karafHome + "/" + location);
        }
        properties = new Properties();
    }

    public boolean exists() {
        return propertyFile.exists();
    }

    public void load() throws IOException {
        if (!propertyFile.exists()) {
            return;
        }
        properties.load(new FileInputStream(propertyFile));
    }

    public void put(String key, String value) {
        properties.put(key, value);
    }

    public void extend(String key, String value) {
        if (properties.get(key) == null) {
            properties.put(key, value);
            return;
        }
        properties.put(key, JoinUtil.join((String)properties.get(key), value));
    }

    public String get(String key) {
        return properties.getProperty(key);
    }

    public void store() throws IOException {
        properties.store(new FileOutputStream(propertyFile), "Modified by paxexam");
    }

    public void replace(File source) {
        try {
            FileUtils.copyFile(source, propertyFile);
        } 
        catch (IOException e) {
            throw new IllegalStateException("It is required to replace propertyFile", e);
        }
    }

}
