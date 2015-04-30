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

import java.io.*;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

public class Driver {

    public static void main(String[] argv)
    {
    Mapper mapper = new DriverMapper();

    if(argv== null || argv.length == 0) {
        System.err.println("usage: Driver <ldap spec file>");
        return;
    }
    LdapLexer lexer = new LdapLexer();
    FileReader fr = null;
    char[] line = null;
    Evaluator engine = new Evaluator();

    Parser parser = new Parser();
//	parser.setDebug(System.out);

    try {
        File spec = new File(argv[0]);
        fr = new FileReader(spec);

        // The basic operation of the driver is:
        // 1. read a line from the file
        // 2. parse that line
        // 3. print the resulting program
        // 4. repeat 1 until eof

        for(;;) {
        line = getLine(fr);
        if(line == null) break;
        System.out.println("Driver: filter: "+new String(line));
        CharArrayReader car = new CharArrayReader(line);
        lexer.setReader(car);
        parser.reset(lexer);
        boolean status = false;
        try {
            status = parser.start();
            if(!status) {
            System.err.println("parse failed");
            printErrorLocation(line,lexer.charno());
            }
        } catch (ParseException pe) {
            System.err.println(pe.toString());
            printErrorLocation(line,lexer.charno());
        }
        if(status) {
            try {
            engine.reset(parser.getProgram());
//            System.out.println("Driver: program: "+engine.toString());
            System.out.println("Driver: program: "+engine.toStringInfix());
            System.out.println("Eval = " + engine.evaluate(mapper));
            } catch (EvaluationException ee) {
            System.err.print("Driver: ");
            printEvaluationStack(engine.getOperands());
            System.err.println(ee.toString());
            }
        }
        }
    } catch (Exception e) {
        System.err.println(e.toString());
        printErrorLocation(line,lexer.charno());
        e.printStackTrace();
    }
    }

    // Get a line of input at a time and return a char[] buffer
    // containing the line

    static char[] getLine(Reader reader) throws IOException
    {
    StringBuffer buf = new StringBuffer();
    for(;;) {
        int c = reader.read();
        if(c == '\r') continue;
        if(c < 0) {
        if(buf.length() == 0) return null; // no more lines
        break;
        }
        if(c == '\n') break;
        buf.append((char)c);
    }

    char[] cbuf = new char[buf.length()];
    buf.getChars(0,buf.length(),cbuf,0);
    return cbuf;
    }


    static void printErrorLocation(char[] line, int charno)
    {
    System.err.print("|");
    if(line != null) System.err.print(new String(line));
    System.err.println("|");
    for(int i=0;i<charno;i++) System.err.print(" ");
    System.err.println("^");
    }

    // Report the final contents of the evaluation stack
    static void printEvaluationStack(Stack stack)
    {
    System.err.print("Stack:");
    // recast operands as Vector to make interior access easier
    Vector operands = stack;
    int len = operands.size();
    for(int i=0;i<len;i++) System.err.print(" "+operands.elementAt(i));
    System.err.println();
    }

}

class DriverMapper implements Mapper {

    Hashtable hash = new Hashtable();

    public DriverMapper()
    {
        hash.put("cn","Babs Jensen");
        hash.put("objectClass","Person");
        hash.put("sn","Jensen");
        hash.put("o","university of michigan");
        hash.put("foo","bar");
    }

    public Object lookup(String key)
    {
        return hash.get(key);
    }
}
