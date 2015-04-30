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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * <p>
 * This class implements a <tt>ResourceSource</tt> for retrieving resources
 * from the system class loader. Effectively, the methods of this resource
 * source defer their functionality to
 * <tt>ClassLoder.getSystemClassLoader().getResource()</tt>.
 * </p>
 * @see org.ungoverned.moduleloader.ResourceSource
**/
public class SystemResourceSource implements ResourceSource
{
    private static final int BUFSIZE = 4096;
    private boolean m_opened = false;

    public void open()
    {
        m_opened = true;
    }

    public void close()
    {
        m_opened = false;
    }

    public boolean hasResource(String name) throws IllegalStateException
    {
        if (!m_opened)
        {
            throw new IllegalStateException("SystemResourceSource is not open");
        }
        return (ClassLoader.getSystemClassLoader().getResource(name) != null);
    }

    public byte[] getBytes(String name) throws IllegalStateException
    {
        if (!m_opened)
        {
            throw new IllegalStateException("SystemResourceSource is not open");
        }
        InputStream is = null;
        ByteArrayOutputStream baos = null;

        try {
            ClassLoader loader = ClassLoader.getSystemClassLoader();
            is = loader.getResourceAsStream(name);
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
}