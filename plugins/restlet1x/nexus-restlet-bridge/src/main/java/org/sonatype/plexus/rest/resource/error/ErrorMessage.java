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
package org.sonatype.plexus.rest.resource.error;

//---------------------------------/
//- Imported classes and packages -/
//---------------------------------/

/**
 * An item describing the error.
 *
 * @version $Revision$ $Date$
 */
public class ErrorMessage
    implements java.io.Serializable
{


  //--------------------------/
  //- Class/Member Variables -/
  //--------------------------/

  /**
   * Field id.
   */
  private String id;

  /**
   * Field msg.
   */
  private String msg;


  //-----------/
  //- Methods -/
  //-----------/

  /**
   * Get the id field.
   *
   * @return String
   */
  public String getId() {
    return this.id;
  } //-- String getId()

  /**
   * Get the msg field.
   *
   * @return String
   */
  public String getMsg() {
    return this.msg;
  } //-- String getMsg()

  /**
   * Set the id field.
   */
  public void setId(String id) {
    this.id = id;
  } //-- void setId(String)

  /**
   * Set the msg field.
   */
  public void setMsg(String msg) {
    this.msg = msg;
  } //-- void setMsg(String)


  //    private String modelEncoding = "UTF-8";
  //
  //    /**
  //     * Set an encoding used for reading/writing the model.
  //     *
  //     * @param modelEncoding the encoding used when reading/writing the model.
  //     */
  //    public void setModelEncoding( String modelEncoding )
  //    {
  //        this.modelEncoding = modelEncoding;
  //    }
  //
  //    /**
  //     * @return the current encoding used when reading/writing this model.
  //     */
  //    public String getModelEncoding()
  //    {
  //        return modelEncoding;
  //    }
}
