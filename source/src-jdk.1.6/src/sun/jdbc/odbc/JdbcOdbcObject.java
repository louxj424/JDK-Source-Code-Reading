/*
 * @(#)JdbcOdbcObject.java	1.35 01/12/03
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

//----------------------------------------------------------------------------
//
// Module:      JdbcOdbcObject.java
//
// Description: Base class for all JdbcOdbc Classes.  This class implements
//              the tracing facility.
//
// Product:     JDBCODBC (Java DataBase Connectivity using
//              Open DataBase Connectivity)
//
// Author:      Karl Moss
//
// Date:        March, 1996
//
//----------------------------------------------------------------------------

package sun.jdbc.odbc;

import java.sql.*;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;

public class JdbcOdbcObject extends Object {

	//--------------------------------------------------------------------
	// Constructor
	// Perform any necessary initialization.
	//--------------------------------------------------------------------

	public JdbcOdbcObject ()
	{
	}
	//--------------------------------------------------------------------
	// dumpByte
	// Dumps the given byte array to the tracing output stream.  Both
	// hex and ascii are traced
	//--------------------------------------------------------------------

	protected static void dumpByte (
		byte b[],
		int len)
	{
		int line;
		int i;
		String s;
		int offset;
		String asciiLine;

		//trace ("Dump (" + len + " bytes):");

       	// Loop for each required line

		for (line = 0; (line * 16) < len; line++) {

			// Dump buffer offset
			s = toHex (line * 16);

			// Trim hex string (take off 0x) and pad to 8
			// characters

			//trace (" " + hexPad (s, 8) + "  ", false);
			asciiLine = "";

			// Create hex portion
			for (i=0; i < 16; i++) {
				offset = (line * 16) + i;

				// Past the end of the buffer, output spaces
				if (offset >= len) {
					s = "  ";
					asciiLine += " ";
				}
				else {
					s = toHex (b[offset]);
					s = hexPad (s, 2);
					if ((b[offset] < 32) || (b[offset] > 128)) {
						asciiLine += ".";
					}
					else {
						asciiLine += new String (b, offset, 1);
					}
				}
				//trace (s + " ", false);
			}

			//trace ("   " + asciiLine);
		}
	}

	//--------------------------------------------------------------------
	// hexPad
	// Trim hex string (take off 0x) and pad (left justifying) to the
	// given number of characters
	//--------------------------------------------------------------------
	public static String hexPad (
		String inString,
		int toLen)
	{
		// If the input string is not hex, just return it

		if (!inString.startsWith ("0x")) {
			return inString;
		}

		// Remove first 2 characters (0x)

		String s = inString.substring (2);
		int l = s.length ();

		// If we have more string than we want, truncate it
		if (l > toLen) {
			s = s.substring (l - toLen);
		}
		else if (l < toLen) {
			// We need to pad
			String z = "0000000000000000";
			String work = z.substring(0,toLen - l) + s;
			s = work;
		}
		s = s.toUpperCase ();
		return s;
	}

	//--------------------------------------------------------------------
	// toHex
	// Convert the given int to a hex string
	//--------------------------------------------------------------------
	public static String toHex (
		int n)
	{
		char c[] = new char[8];
		String digits = "0123456789ABCDEF";
		byte oneByte;

		// Loop for each byte

		for (int i = 0; i < 4; i++) {
			oneByte = (byte) (n & 0xFF);
			c[6 - (i * 2)] = digits.charAt ((oneByte >> 4) & 0x0F);
			c[7 - (i * 2)] = digits.charAt (oneByte & 0x0F);

			// Shift over

			n >>= 8;
		}

		return "0x" + new String (c);
	}

	//--------------------------------------------------------------------
	// hexStringToByteArray
	// Converts a hex string into a byte array.  It is assumed that
	// 2 hex characters make up 1 byte.
	//--------------------------------------------------------------------
	public static byte[] hexStringToByteArray (
		String inString)
		throws java.lang.NumberFormatException
	{
		byte b[];
		int fromLen = inString.length ();
		int toLen = (fromLen + 1) / 2;

		// Allocate the byte array
		b = new byte[toLen];

		// Loop through the string and convert each character
		// pair into a single byte value

		for (int i = 0; i < toLen; i++) {
			b[i] = (byte) hexPairToInt (
				inString.substring (i * 2, (i + 1) * 2));
		}
		return b;
	}

	//--------------------------------------------------------------------
	// hexPairToInt
	// Converts a 2 character hexadecimal pair into an integer (the
	// first 2 characters of the string are used)
	//--------------------------------------------------------------------

	public static int hexPairToInt (
		String inString)
		throws java.lang.NumberFormatException
	{
		String digits = "0123456789ABCDEF";
		String s = inString.toUpperCase ();
		int n = 0;
		int thisDigit = 0;
		int sLen = s.length ();

		if (sLen > 2) {
			sLen = 2;
		}

		// Loop through both digits

		for (int i = 0; i < sLen; i++) {
			thisDigit = digits.indexOf (s.substring (i, i + 1));

			// Invalid hex character
			if (thisDigit < 0) {
				throw new java.lang.NumberFormatException ();
			}

			if (i == 0) {
				thisDigit *= 0x10;
			}
			n += thisDigit;
		}
		return n;
	}

	//--------------------------------------------------------------------
	// BytesToChars
        // Converts an array of bytes into the user input char array.
	//--------------------------------------------------------------------
	public String BytesToChars (String charSet, byte[] inBytes)
		throws java.io.UnsupportedEncodingException
	{
            String retString = new String(); 
            try {
		retString = Charset.forName(charSet).newDecoder()
		    .onMalformedInput(CodingErrorAction.REPLACE)
		    .onUnmappableCharacter(CodingErrorAction.REPLACE)
		    .replaceWith("?")
		    .decode (ByteBuffer.wrap(inBytes))
		    .toString();
	    } 
	    catch (IllegalCharsetNameException x) {
		throw new UnsupportedEncodingException (charSet);
	    } 
	    catch (IllegalStateException x) { } 
	    catch (CharacterCodingException x) { }
           // return new String(new char[0]);
  
        //The following trims off extra space on the end from the conversion above
        char[] bTmp = retString.toCharArray();
        int ix;
        byte tempo;
        //boolean NotNull=true;
        //find the first null character
        for (ix=0; ix < bTmp.length; ix++)
        {
                //tempo=(byte)bTmp[ix];
                //if (tempo==0) NotNull=false;
                if (bTmp[ix] == Character.MIN_VALUE)
                {
                        break;
                }
        }
        //create a new string of the propper length
        char[] bTmp2 = new char[ix];
        System.arraycopy(bTmp, 0, bTmp2, 0, ix);
        retString=new String(bTmp2);
        return retString;
        }
    
	//--------------------------------------------------------------------
	// CharsToBytes
	// Converts an array of chars into the user input type of byte array.
	//--------------------------------------------------------------------

	public byte[] CharsToBytes (String charSet, char[] inChars)
		throws java.io.UnsupportedEncodingException
	{

            try {
                char[] cc = new char[inChars.length + 1];
                System.arraycopy(inChars, 0, cc, 0, inChars.length);
                ByteBuffer bb = Charset.forName(charSet).newEncoder()
		.onMalformedInput(CodingErrorAction.REPLACE)
		.onUnmappableCharacter(CodingErrorAction.REPLACE)
		.replaceWith(new byte[]{(byte)0x3f})
		.encode (CharBuffer.wrap(cc));
                byte[] ba = new byte[bb.limit()];
                System.arraycopy(bb.array(), 0, ba, 0, bb.limit());
		return ba;
	    } 
	    catch (IllegalCharsetNameException x) {
		throw new UnsupportedEncodingException (charSet);
	    } 
	    catch (IllegalStateException x) { x.printStackTrace(); } 
	    catch (CharacterCodingException x) { x.printStackTrace(); } 
            return new byte[0];
	}
}

