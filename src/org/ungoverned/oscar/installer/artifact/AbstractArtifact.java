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
 *
**/
package org.ungoverned.oscar.installer.artifact;

import java.io.*;
import java.util.Map;

import org.ungoverned.oscar.installer.*;
import org.ungoverned.oscar.installer.property.*;

public abstract class AbstractArtifact implements Artifact
{
    private StringProperty m_sourceName = null;
    private StringProperty m_destDir = null;
    private boolean m_localize = false;

    // This following shared buffer assumes that there is
    // no concurrency when processing resources.
    protected static byte[] s_buffer = new byte[2048];

    public AbstractArtifact(
        StringProperty sourceName, StringProperty destDir, boolean localize)
    {
        if (destDir == null)
        {
            destDir = new StringPropertyImpl("empty", "");
        }
        m_sourceName = sourceName;
        m_destDir = destDir;
        m_localize = localize;
    }

    public StringProperty getSourceName()
    {
        return m_sourceName;
    }

    public StringProperty getDestinationDirectory()
    {
        return m_destDir;
    }

    public boolean localize()
    {
        return m_localize;
    }

    protected static void copy(
        InputStream is, String installDir, String destName, String destDir)
        throws IOException
    {
        if (destDir == null)
        {
            destDir = "";
        }

        // Make sure the target directory exists and
        // that is actually a directory.
        File targetDir = new File(installDir, destDir);
        if (!targetDir.exists())
        {
            if (!targetDir.mkdirs())
            {
                throw new IOException("Unable to create target directory: "
                    + targetDir);
            }
        }
        else if (!targetDir.isDirectory())
        {
            throw new IOException("Target is not a directory: "
                + targetDir);
        }

        BufferedOutputStream bos = new BufferedOutputStream(
            new FileOutputStream(new File(targetDir, destName)));
        int count = 0;
        while ((count = is.read(s_buffer)) > 0)
        {
            bos.write(s_buffer, 0, count);
        }
        bos.close();
    }

    protected static void copyAndLocalize(
        InputStream is, String installDir, String destName,
        String destDir, Map propMap)
        throws IOException
    {
        if (destDir == null)
        {
            destDir = "";
        }

        // Make sure the target directory exists and
        // that is actually a directory.
        File targetDir = new File(installDir, destDir);
        if (!targetDir.exists())
        {
            if (!targetDir.mkdirs())
            {
                throw new IOException("Unable to create target directory: "
                    + targetDir);
            }
        }
        else if (!targetDir.isDirectory())
        {
            throw new IOException("Target is not a directory: "
                + targetDir);
        }

        BufferedOutputStream bos = new BufferedOutputStream(
            new FileOutputStream(new File(targetDir, destName)));
        int i = 0;
        while ((i = is.read()) > 0)
        {
            // Parameters start with "%%", so check to see if
            // we have a parameter.
            if ((char)i == '%')
            {
                // One of three possibilities, we have a parameter,
                // we have an end of file, or we don't have a parameter.
                int i2 = is.read();
                if ((char) i2 == '%')
                {
                    Object obj = readParameter(is);

                    // If the byte sequence was not a parameter afterall,
                    // then a byte array is returned, otherwise a string
                    // containing the parameter m_name is returned.
                    if (obj instanceof byte[])
                    {
                        bos.write(i);
                        bos.write(i2);
                        bos.write((byte[]) obj);
                    }
                    else
                    {
                        Property prop = (Property) propMap.get(obj);
                        String value = (prop == null) ? "" : prop.toString();
                        bos.write(value.getBytes());
                    }
                }
                else if (i2 == -1)
                {
                    bos.write(i);
                }
                else
                {
                    bos.write(i);
                    bos.write(i2);
                }
            }
            else
            {
                bos.write(i);
            }
        }
        bos.close();
    }

    protected static Object readParameter(InputStream is)
        throws IOException
    {
        int count = 0;
        int i = 0;
        while ((count < s_buffer.length) && ((i = is.read()) > 0))
        {
            if ((char) i == '%')
            {
                // One of three possibilities, we have the end of
                // the parameter, we have an end of file, or we
                // don't have the parameter end.
                int i2 = is.read();
                if ((char) i2 == '%')
                {
                    return new String(s_buffer, 0, count);
                }
                else if (i2 == -1)
                {
                    s_buffer[count] = (byte) i;
                    byte[] b = new byte[count];
                    for (int j = 0; j < count; j++)
                        b[j] = s_buffer[j];
                    return b;
                }
                else
                {
                    s_buffer[count++] = (byte) i;
                    s_buffer[count++] = (byte) i2;
                }
            }
            else
            {
                s_buffer[count++] = (byte) i;
            }
        }

        byte[] b = new byte[count - 1];
        for (int j = 0; j < (count - 1); j++)
            b[j] = s_buffer[j];

        return b;
    }

    public static String getPath(String s, char separator)
    {
        return (s.lastIndexOf(separator) < 0)
            ? "" : s.substring(0, s.lastIndexOf(separator));
    }

    public static String getPathHead(String s, char separator)
    {
        return (s.lastIndexOf(separator) < 0)
            ? s : s.substring(s.lastIndexOf(separator) + 1);
    }
}