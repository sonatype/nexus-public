package com.softwarementors.extjs.djn.jscodegen;

/**
 * This is an override of original Minifier code, that will not do any minification, or order to get rid of
 * yui compressor / rhino.
 */
public class Minifier
{

  public static String getMinifiedFileName(String file) {
    return file.replace(".js", "-min.js");
  }

  public static final String minify( String input, String inputFilename, int debugCodeLength ) {
    throw new IllegalStateException("Minification is not supposed to be used");
  }

}
