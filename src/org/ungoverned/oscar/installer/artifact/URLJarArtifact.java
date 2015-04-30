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
import java.net.URL;
import java.net.URLConnection;

import org.ungoverned.oscar.installer.Status;
import org.ungoverned.oscar.installer.StringProperty;

public class URLJarArtifact extends AbstractJarArtifact
{
    public URLJarArtifact(StringProperty sourceName)
    {
        this(sourceName, null);
    }

    public URLJarArtifact(StringProperty sourceName, StringProperty destDir)
    {
        this(sourceName, destDir, false);
    }

    public URLJarArtifact(
        StringProperty sourceName, StringProperty destDir, boolean localize)
    {
        super(sourceName, destDir, localize);
    }

    public InputStream getInputStream(Status status)
        throws IOException
    {
        String fileName = getSourceName().getStringValue();
        fileName = (fileName.lastIndexOf('/') > 0)
            ? fileName.substring(fileName.lastIndexOf('/') + 1)
            : fileName;
        
        status.setText("Connecting...");

        File file = File.createTempFile("oscar-install.tmp", null);
        file.deleteOnExit();

        OutputStream os = new FileOutputStream(file);
        URLConnection conn = new URL(getSourceName().getStringValue()).openConnection();
        int total = conn.getContentLength();
        InputStream is = conn.getInputStream();

        int count = 0;
        for (int len = is.read(s_buffer); len > 0; len = is.read(s_buffer))
        {
            count += len;
            os.write(s_buffer, 0, len);
            if (total > 0)
            {
                status.setText("Downloading " + fileName
                    + " ( " + count + " bytes of " + total + " ).");
            }
            else
            {
                status.setText("Downloading " + fileName + " ( " + count + " bytes ).");
            }
        }

        os.close();
        is.close();

        return new FileInputStream(file);
    }

    public String toString()
    {
        return "URL JAR: " + getSourceName().getStringValue();
    }
}