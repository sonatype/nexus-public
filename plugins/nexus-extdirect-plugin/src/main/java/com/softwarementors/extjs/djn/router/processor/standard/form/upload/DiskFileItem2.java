/* Very important!
 * 
 * This is a modification of the DiskFileItem implemented in Apache
 * File Upload library, intended to be used in an
 * scenario where files can't be written to disk -i.e., AppEngine.
 * 
 * The only important modifications are:
 * 
 *    - write(File): throws an UnsupportedOperationException, because
 *           AppEngine does not like it.
 *    - UID: this is now generated via Random, instead of using UID,
 *           a class AppEngine does not allow us to instantiate.
 *    - getTempDir: we removed the call to FileCleaner.track, which
 *           ends up creating a new thread, something AppEngine 
 *           does not like.
 *           
 *  ALL other modifications have been done to please my very demanding
 *  compiler settings, and do not modify the software behavior.
 */

/*
 * Copyright 2001-2005 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.softwarementors.extjs.djn.router.processor.standard.form.upload;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Random;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemHeaders;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.ParameterParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.DeferredFileOutputStream;


/**
 * <p> The default implementation of the
 * {@link org.apache.commons.fileupload.FileItem FileItem} interface.
 *
 * <p> After retrieving an instance of this class from a {@link
 * org.apache.commons.fileupload.DiskFileUpload DiskFileUpload} instance (see
 * {@link org.apache.commons.fileupload.DiskFileUpload
 * #parseRequest(javax.servlet.http.HttpServletRequest)}), you may
 * either request all contents of file at once using {@link #get()} or
 * request an {@link java.io.InputStream InputStream} with
 * {@link #getInputStream()} and process the file without attempting to load
 * it into memory, which may come handy with large files.
 *
 * @author <a href="mailto:Rafal.Krzewski@e-point.pl">Rafal Krzewski</a>
 * @author <a href="mailto:sean@informage.net">Sean Legassick</a>
 * @author <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 * @author <a href="mailto:jmcnally@apache.org">John McNally</a>
 * @author <a href="mailto:martinc@apache.org">Martin Cooper</a>
 * @author Sean C. Sullivan
 *
 * @since FileUpload 1.1
 *
 * @version $Id: DiskFileItem2.java 399546 2006-05-04 04:53:30Z martinc $
 */
public class DiskFileItem2
    implements FileItem {

    // ----------------------------------------------------- Manifest constants


    /**
   * 
   */
  private static final long serialVersionUID = 6555032855727819090L;


    /**
     * Default content charset to be used when no explicit charset
     * parameter is provided by the sender. Media subtypes of the
     * "text" type are defined to have a default charset value of
     * "ISO-8859-1" when received via HTTP.
     */
    public static final String DEFAULT_CHARSET = "ISO-8859-1";


    // ----------------------------------------------------------- Data members


    // PAG: just to please FindBugs!
    private static Random random = new Random(); 
    
    /**
     * UID used in unique file name generation.
     */
    private static final String UID =
            Long.toString(random.nextLong());

    /**
     * Counter used in unique identifier generation.
     */
    private static int counter = 0;


    /**
     * The name of the form field as provided by the browser.
     */
    private String fieldName;


    /**
     * The content type passed by the browser, or <code>null</code> if
     * not defined.
     */
    private String contentType;


    /**
     * Whether or not this item is a simple form field.
     */
    private boolean isFormField;


    /**
     * The original filename in the user's filesystem.
     */
    private String fileName;


    /**
     * The size of the item, in bytes. This is used to cache the size when a
     * file item is moved from its original location.
     */
    private long size = -1;


    /**
     * The threshold above which uploads will be stored on disk.
     */
    private int sizeThreshold;


    /**
     * The directory in which uploaded files will be stored, if stored on disk.
     */
    private File repository;


    /**
     * Cached contents of the file.
     */
    private byte[] cachedContent;


    /**
     * Output stream for this item.
     */
    private transient DeferredFileOutputStream dfos;

    /**
     * File to allow for serialization of the content of this item.
     */
    private File dfosFile;


    // ----------------------------------------------------------- Constructors


    /**
     * Constructs a new <code>DiskFileItem2</code> instance.
     *
     * @param fieldName     The name of the form field.
     * @param contentType   The content type passed by the browser or
     *                      <code>null</code> if not specified.
     * @param isFormField   Whether or not this item is a plain form field, as
     *                      opposed to a file upload.
     * @param fileName      The original filename in the user's filesystem, or
     *                      <code>null</code> if not specified.
     * @param sizeThreshold The threshold, in bytes, below which items will be
     *                      retained in memory and above which they will be
     *                      stored as a file.
     * @param repository    The data repository, which is the directory in
     *                      which files will be created, should the item size
     *                      exceed the threshold.
     */
    public DiskFileItem2(String fieldName, String contentType,
            boolean isFormField, String fileName, int sizeThreshold,
            File repository) {
        this.fieldName = fieldName;
        this.contentType = contentType;
        this.isFormField = isFormField;
        this.fileName = fileName;
        this.sizeThreshold = sizeThreshold;
        this.repository = repository;
    }


    // ------------------------------- Methods from javax.activation.DataSource


    /**
     * Returns an {@link java.io.InputStream InputStream} that can be
     * used to retrieve the contents of the file.
     *
     * @return An {@link java.io.InputStream InputStream} that can be
     *         used to retrieve the contents of the file.
     *
     * @throws IOException if an error occurs.
     */
    public InputStream getInputStream()
        throws IOException {
        if (!isInMemory()) {
            return new FileInputStream(dfos.getFile());
        }

        if (cachedContent == null) {
            cachedContent = dfos.getData();
        }
        return new ByteArrayInputStream(cachedContent);
    }


    /**
     * Returns the content type passed by the agent or <code>null</code> if
     * not defined.
     *
     * @return The content type passed by the agent or <code>null</code> if
     *         not defined.
     */
    public String getContentType() {
        return contentType;
    }


    /**
     * Returns the content charset passed by the agent or <code>null</code> if
     * not defined.
     *
     * @return The content charset passed by the agent or <code>null</code> if
     *         not defined.
     */
    @SuppressWarnings("unchecked")
    public String getCharSet() {
        ParameterParser parser = new ParameterParser();
        parser.setLowerCaseNames(true);
        // Parameter parser can handle null input
        Map params = parser.parse(getContentType(), ';');
        return (String) params.get("charset");
    }


    /**
     * Returns the original filename in the client's filesystem.
     *
     * @return The original filename in the client's filesystem.
     */
    public String getName() {
        return fileName;
    }


    // ------------------------------------------------------- FileItem methods


    /**
     * Provides a hint as to whether or not the file contents will be read
     * from memory.
     *
     * @return <code>true</code> if the file contents will be read
     *         from memory; <code>false</code> otherwise.
     */
    public boolean isInMemory() {
        if (cachedContent != null) {
            return true;
        } 
        //else {
            return dfos.isInMemory();
        //}
    }


    /**
     * Returns the size of the file.
     *
     * @return The size of the file, in bytes.
     */
    public long getSize() {
        if (size >= 0) {
            return size;
        } else if (cachedContent != null) {
            return cachedContent.length;
        } else if (dfos.isInMemory()) {
            return dfos.getData().length;
        } else {
            return dfos.getFile().length();
        }
    }


    /**
     * Returns the contents of the file as an array of bytes.  If the
     * contents of the file were not yet cached in memory, they will be
     * loaded from the disk storage and cached.
     *
     * @return The contents of the file as an array of bytes.
     */
    public byte[] get() {
        if (isInMemory()) {
            if (cachedContent == null) {
                cachedContent = dfos.getData();
            }
            return cachedContent;
        }

        byte[] fileData = new byte[(int) getSize()];
        FileInputStream fis = null;

        try {
            fis = new FileInputStream(dfos.getFile());
            fis.read(fileData);
        } catch (IOException e) {
            fileData = null;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }

        return fileData;
    }


    /**
     * Returns the contents of the file as a String, using the specified
     * encoding.  This method uses {@link #get()} to retrieve the
     * contents of the file.
     *
     * @param charset The charset to use.
     *
     * @return The contents of the file, as a string.
     *
     * @throws UnsupportedEncodingException if the requested character
     *                                      encoding is not available.
     */
    public String getString(final String charset)
        throws UnsupportedEncodingException {
        return new String(get(), charset);
    }


    /**
     * Returns the contents of the file as a String, using the default
     * character encoding.  This method uses {@link #get()} to retrieve the
     * contents of the file.
     *
     * @return The contents of the file, as a string.
     *
     */
    public String getString() {
        byte[] rawdata = get();
        String charset = getCharSet();
        if (charset == null) {
            charset = DEFAULT_CHARSET;
        }
        try {
            return new String(rawdata, charset);
        } catch (UnsupportedEncodingException e) {
            return new String(rawdata);
        }
    }


    /**
     * A convenience method to write an uploaded item to disk. The client code
     * is not concerned with whether or not the item is stored in memory, or on
     * disk in a temporary location. They just want to write the uploaded item
     * to a file.
     * <p>
     * This implementation first attempts to rename the uploaded item to the
     * specified destination file, if the item was originally written to disk.
     * Otherwise, the data will be copied to the specified file.
     * <p>
     * This method is only guaranteed to work <em>once</em>, the first time it
     * is invoked for a particular item. This is because, in the event that the
     * method renames a temporary file, that file will no longer be available
     * to copy or rename again at a later time.
     *
     * @param file The <code>File</code> into which the uploaded item should
     *             be stored.
     *
     * @throws Exception if an error occurs.
     */
    public void write(File file) throws Exception {
      // PAG: we just can't write files
      throw new FileUploadException(
       "Cannot write uploaded file to disk!");
    }


    /**
     * Deletes the underlying storage for a file item, including deleting any
     * associated temporary disk file. Although this storage will be deleted
     * automatically when the <code>FileItem</code> instance is garbage
     * collected, this method can be used to ensure that this is done at an
     * earlier time, thus preserving system resources.
     */
    public void delete() {
        cachedContent = null;
        File outputFile = getStoreLocation();
        if (outputFile != null && outputFile.exists()) {
            outputFile.delete();
        }
    }


    /**
     * Returns the name of the field in the multipart form corresponding to
     * this file item.
     *
     * @return The name of the form field.
     *
     * @see #setFieldName(java.lang.String)
     *
     */
    public String getFieldName() {
        return fieldName;
    }


    /**
     * Sets the field name used to reference this file item.
     *
     * @param fieldName The name of the form field.
     *
     * @see #getFieldName()
     *
     */
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }


    /**
     * Determines whether or not a <code>FileItem</code> instance represents
     * a simple form field.
     *
     * @return <code>true</code> if the instance represents a simple form
     *         field; <code>false</code> if it represents an uploaded file.
     *
     * @see #setFormField(boolean)
     *
     */
    public boolean isFormField() {
        return isFormField;
    }


    /**
     * Specifies whether or not a <code>FileItem</code> instance represents
     * a simple form field.
     *
     * @param state <code>true</code> if the instance represents a simple form
     *              field; <code>false</code> if it represents an uploaded file.
     *
     * @see #isFormField()
     *
     */
    public void setFormField(boolean state) {
        isFormField = state;
    }


    /**
     * Returns an {@link java.io.OutputStream OutputStream} that can
     * be used for storing the contents of the file.
     *
     * @return An {@link java.io.OutputStream OutputStream} that can be used
     *         for storing the contensts of the file.
     *
     * @throws IOException if an error occurs.
     */
    public OutputStream getOutputStream()
        throws IOException {      
        if (dfos == null) {
            File outputFile = getTempFile();
            dfos = new DeferredFileOutputStream(sizeThreshold, outputFile);
        }
        return dfos;
    }


    // --------------------------------------------------------- Public methods


    /**
     * Returns the {@link java.io.File} object for the <code>FileItem</code>'s
     * data's temporary location on the disk. Note that for
     * <code>FileItem</code>s that have their data stored in memory,
     * this method will return <code>null</code>. When handling large
     * files, you can use {@link java.io.File#renameTo(java.io.File)} to
     * move the file to new location without copying the data, if the
     * source and destination locations reside within the same logical
     * volume.
     *
     * @return The data file, or <code>null</code> if the data is stored in
     *         memory.
     */
    public File getStoreLocation() {
        return dfos.getFile();
    }


    // ------------------------------------------------------ Protected methods


    /**
     * Removes the file contents from the temporary storage.
     */
    @Override
    protected void finalize() {
        File outputFile = dfos.getFile();

        if (outputFile != null && outputFile.exists()) {
            outputFile.delete();
        }
    }


    /**
     * Creates and returns a {@link java.io.File File} representing a uniquely
     * named temporary file in the configured repository path. The lifetime of
     * the file is tied to the lifetime of the <code>FileItem</code> instance;
     * the file will be deleted when the instance is garbage collected.
     *
     * @return The {@link java.io.File File} to be used for temporary storage.
     */
    protected File getTempFile() {
        File tempDir = repository;
        if (tempDir == null) {
            tempDir = new File(System.getProperty("java.io.tmpdir"));
        }

        String fileName = "upload_" + UID + "_" + getUniqueId() + ".tmp";

        File f = new File(tempDir, fileName);
        // PAG: we just can't create new threads, which FileCleaner.track does!
        // FileCleaner.track(f, this);
        return f;
    }


    // -------------------------------------------------------- Private methods


    /**
     * Returns an identifier that is unique within the class loader used to
     * load this class, but does not have random-like apearance.
     *
     * @return A String with the non-random looking instance identifier.
     */
    private static String getUniqueId() {
        final int limit = 100000000;
        int current;
        synchronized (DiskFileItem2.class) {
            current = counter++;
        }
        String id = Integer.toString(current);

        // If you manage to get more than 100 million of ids, you'll
        // start getting ids longer than 8 characters.
        if (current < limit) {
            id = ("00000000" + id).substring(id.length());
        }
        return id;
    }




    /**
     * Returns a string representation of this object.
     *
     * @return a string representation of this object.
     */
    @Override
    public String toString() {
        return "name=" + this.getName()
            + ", StoreLocation="
            + String.valueOf(this.getStoreLocation())
            + ", size="
            + this.getSize()
            + "bytes, "
            + "isFormField=" + isFormField()
            + ", FieldName="
            + this.getFieldName();
    }


    // -------------------------------------------------- Serialization methods


    /**
     * Writes the state of this object during serialization.
     *
     * @param out The stream to which the state should be written.
     *
     * @throws IOException if an error occurs.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        // Read the data
        if (dfos.isInMemory()) {
            cachedContent = get();
        } else {
            cachedContent = null;
            dfosFile = dfos.getFile();
        }

        // write out values
        out.defaultWriteObject();
    }

    /**
     * Reads the state of this object during deserialization.
     *
     * @param in The stream from which the state should be read.
     *
     * @throws IOException if an error occurs.
     * @throws ClassNotFoundException if class cannot be found.
     */
    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        // read values
        in.defaultReadObject();

        OutputStream output = getOutputStream();
        if (cachedContent != null) {
            output.write(cachedContent);
        } else {
            FileInputStream input = new FileInputStream(dfosFile);

            IOUtils.copy(input, output);
            dfosFile.delete();
            dfosFile = null;
        }
        output.close();

        cachedContent = null;
    }

  // The code bellow is copied from org.apache.commons.fileupload.disk.DiskFileItem
  // and this are the only changes to original djn DiskFileItem2 (beside removing findbugs annos to easier compile)

  /**
   * The file items headers.
   */
  private FileItemHeaders headers;

  /**
   * Returns the file item headers.
   * @return The file items headers.
   */
  public FileItemHeaders getHeaders() {
    return headers;
  }

  /**
   * Sets the file item headers.
   * @param pHeaders The file items headers.
   */
  public void setHeaders(FileItemHeaders pHeaders) {
    headers = pHeaders;
  }

}
