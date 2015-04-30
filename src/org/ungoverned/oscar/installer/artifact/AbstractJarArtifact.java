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
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.ungoverned.oscar.installer.Install;
import org.ungoverned.oscar.installer.Status;
import org.ungoverned.oscar.installer.StringProperty;

public abstract class AbstractJarArtifact extends AbstractArtifact
{
    public AbstractJarArtifact(StringProperty sourceName)
    {
        this(sourceName, null);
    }

    public AbstractJarArtifact(StringProperty sourceName, StringProperty destDir)
    {
        this(sourceName, destDir, false);
    }

    public AbstractJarArtifact(
        StringProperty sourceName, StringProperty destDir, boolean localize)
    {
        super(sourceName, destDir, localize);
    }

    public boolean process(Status status, Map propMap)
    {
        try
        {
            InputStream is = getInputStream(status);

            if (is == null)
            {
                return true;
            }

            JarInputStream jis = new JarInputStream(is);
            status.setText("Extracting...");
            unjar(jis, propMap);
            jis.close();
        }
        catch (Exception ex)
        {
            System.err.println(this);
            System.err.println(ex);
            return false;
        }

        return true;
    }

    protected void unjar(JarInputStream jis, Map propMap)
        throws IOException
    {
        String installDir =
            ((StringProperty) propMap.get(Install.INSTALL_DIR)).getStringValue();

        // Loop through JAR entries.
        for (JarEntry je = jis.getNextJarEntry();
             je != null;
             je = jis.getNextJarEntry())
        {
            if (je.getName().startsWith("/"))
            {
                throw new IOException("JAR resource cannot contain absolute paths.");
            }

            File target =
                new File(installDir, getDestinationDirectory().getStringValue());
            target = new File(target, je.getName());

            // Check to see if the JAR entry is a directory.
            if (je.isDirectory())
            {
                if (!target.exists())
                {
                    if (!target.mkdirs())
                    {
                        throw new IOException("Unable to create target directory: "
                            + target);
                    }
                }
                // Just continue since directories do not have content to copy.
                continue;
            }

            int lastIndex = je.getName().lastIndexOf('/');
            String name = (lastIndex >= 0) ?
                je.getName().substring(lastIndex + 1) : je.getName();
            String destination = (lastIndex >= 0) ?
                je.getName().substring(0, lastIndex) : "";

            // JAR files use '/', so convert it to platform separator.
            destination = destination.replace('/', File.separatorChar);

            if (localize())
            {
                copyAndLocalize(jis, installDir, name, destination, propMap);
            }
            else
            {
                copy(jis, installDir, name, destination);
            }
        }
    }
}