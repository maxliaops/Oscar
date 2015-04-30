/*
 * ModuleLoader - A generic, policy-driven class loader.
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
 *
**/
package org.ungoverned.moduleloader;

import java.io.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * <p>
 * This class implements a <tt>ResourceSource</tt> for retrieving resources
 * from a JAR file. The approach used by this implementation is to defer
 * opening the JAR file until a request for a resource is made.
 * </p>
 * @see org.ungoverned.moduleloader.ResourceSource
**/
public class JarResourceSource implements ResourceSource
{
    private static final int BUFSIZE = 4096;

    private File m_file = null;
    private JarFile m_jarFile = null;
    private boolean m_opened = false;

    /**
     * <p>
     * Constructs an instance using the specified file name as the source
     * of the JAR file.
     * </p>
     * @param fileName the name of the JAR file to be used as the source.
    **/
    public JarResourceSource(String fileName)
    {
        m_file = new File(fileName);
    }

    /**
     * <p>
     * Constructs an instance using the specified file as the source
     * of the JAR file.
     * </p>
     * @param file the JAR file to be used as the source.
    **/
    public JarResourceSource(File file)
    {
        m_file = file;
    }

    /**
     * <p>
     * Closes the JAR file if it has not already been closed.
     * <p>
    **/
    protected void finalize()
    {
        if (m_jarFile != null)
        {
            try {
                m_jarFile.close();
            } catch (IOException ex) {
                // Not much we can do, so ignore it.
            }
        }
    }

    /**
     * <p>
     * This method initializes the resource source. Since opening
     * the JAR file is deferred until a request for a resource is
     * actually made, this method really only sets a flag indicating
     * that the resource source has been initialized.
     * <p>
    **/
    public void open()
    {
        m_opened = true;
    }

    /**
     * <p>
     * This method deinitializes the resource source by closing
     * the associated JAR file if it is open.
     * <p>
    **/
    public synchronized void close()
    {
        try {
            if (m_jarFile != null)
            {
                m_jarFile.close();
            }
        } catch (Exception ex) {
            System.err.println("JarResourceSource: " + ex);
        }

        m_jarFile = null;
        m_opened = false;
    }

    // JavaDoc comments are copied from ResourceSource.
    public synchronized boolean hasResource(String name) throws IllegalStateException
    {
        if (!m_opened)
        {
            throw new IllegalStateException("JarResourceSource is not open");
        }

        // Open JAR file if not already opened.
        if (m_jarFile == null)
        {
            try {
                openJarFile();
            } catch (IOException ex) {
                System.err.println("JarResourceSource: " + ex);
                return false;
            }
        }

        try {
            ZipEntry ze = m_jarFile.getEntry(name);
            return ze != null;
        } catch (Exception ex) {
            return false;
        } finally {
        }
    }

    // JavaDoc comments are copied from ResourceSource.
    public synchronized byte[] getBytes(String name) throws IllegalStateException
    {
        if (!m_opened)
        {
            throw new IllegalStateException("JarResourceSource is not open");
        }

        // Open JAR file if not already opened.
        if (m_jarFile == null)
        {
            try {
                openJarFile();
            } catch (IOException ex) {
                System.err.println("JarResourceSource: " + ex);
                return null;
            }
        }

        // Get the embedded resource.
        InputStream is = null;
        ByteArrayOutputStream baos = null;

        try {
            ZipEntry ze = m_jarFile.getEntry(name);
            if (ze == null)
            {
                return null;
            }
            is = m_jarFile.getInputStream(ze);
            if (is == null)
            {
                return null;
            }
            baos = new ByteArrayOutputStream(BUFSIZE);
            byte[] buf = new byte[BUFSIZE];
            int n = 0;
            while ((n = is.read(buf, 0, buf.length)) >= 0)
            {
                baos.write(buf, 0, n);
            }
            return baos.toByteArray();

        } catch (Exception ex) {
            return null;
        } finally {
            try {
                if (baos != null) baos.close();
            } catch (Exception ex) {
            }
            try {
                if (is != null) is.close();
            } catch (Exception ex) {
            }
        }
    }

    private void openJarFile() throws IOException
    {
        if (m_jarFile == null)
        {
            m_jarFile = new JarFile(m_file);
        }
    }

    public String toString()
    {
        return "JAR " + m_file.getPath();
    }
}