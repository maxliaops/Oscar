/*
 * Oscar - An implementation of the OSGi framework.
 * Copyright (c) 2004, Richard S. Hall
 * All rights reserved.
 *  
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *  
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in
 *     the documentation and/or other materials provided with the
 *     distribution.
 *   * Neither the name of the ungoverned.org nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *  
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * Contact: Richard S. Hall (heavy@ungoverned.org)
 * Contributor(s):
 * Dennis Heimbigner
 *
**/
package org.ungoverned.oscar.ldap;

import java.io.IOException;
import java.io.Reader;

public class LdapLexer {

    static final int EOF = -1;
    static final int NOCHAR = 0; // signal no peeked char; different from EOF

    public static final String WHITESPACE = " \t\n\r";

    Reader reader = null;

    int nextChar = NOCHAR; // last peeked character

    public LdapLexer() {}

    public LdapLexer(Reader r)
    {
    setReader(r);
    charno = 1;
    }

    public void setReader(Reader r)
    {
    reader = r;
    }

    /*
    The procedures get(),peek(),skipwhitespace(),getnw(), and peeknw()
    provide the essential LdapLexer interface.
    */

    public int get() throws IOException // any next char
    {
    if(nextChar == NOCHAR) return readChar();
    int c = nextChar;
    nextChar = NOCHAR;
    return c;
    }

    public int peek() throws IOException
    {
    if(nextChar == NOCHAR) {
        nextChar = readChar();
    }
    return nextChar;
    }

    void skipwhitespace() throws IOException
    {
    while(WHITESPACE.indexOf(peek()) >= 0) get();
    }

    public int getnw() throws IOException // next non-whitespace char
    {					   // (note: not essential but useful)
    skipwhitespace();
    return get();
    }

    public int peeknw() throws IOException // next non-whitespace char
    {					   // (note: not essential but useful)
    skipwhitespace();
    return peek();
    }

    // Following is for error reporting

    // Pass all character reads thru this so we can track char count

    int charno; // 1-based

    public int charno() {return charno;}

    int readChar() throws IOException
    {
    charno++;
    return reader.read();
    }

}
