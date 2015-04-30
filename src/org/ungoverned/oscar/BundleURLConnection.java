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
package org.ungoverned.oscar;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;

import org.ungoverned.moduleloader.Module;
import org.ungoverned.moduleloader.ModuleManager;
import org.ungoverned.moduleloader.ResourceSource;

class BundleURLConnection extends URLConnection
{
    private ModuleManager m_mgr = null;
    private long id;
    private int contentLength;
    private long contentTime;
    private String contentType;
    private InputStream is;

    public BundleURLConnection(ModuleManager mgr, URL url)
    {
        super(url);
        m_mgr = mgr;
    }

    public void connect() throws IOException
    {
        if (!connected)
        {
            // The URL is constructed like this:
            // bundle://<module-id>/<source-idx>/<resource-path>

            Module module = m_mgr.getModule(url.getHost());
            if (module == null)
            {
                throw new IOException("Unable to find bundle's module.");
            }

            String resource = url.getFile();
            if (resource == null)
            {
                throw new IOException("Unable to find resource: " + url.toString());
            }
            if (resource.startsWith("/"))
            {
                resource = resource.substring(1);
            }
            int rsIdx = -1;
            try
            {
                rsIdx = Integer.parseInt(resource.substring(0, resource.indexOf("/")));
            }
            catch (NumberFormatException ex)
            {
                new IOException("Error parsing resource index.");
            }
            resource = resource.substring(resource.indexOf("/") + 1);

            // Get the resource bytes from the resource source.
            byte[] bytes = null;
            ResourceSource[] resSources = module.getResourceSources();
            if ((resSources != null) && (rsIdx < resSources.length))
            {
                if (resSources[rsIdx].hasResource(resource))
                {
                    bytes = resSources[rsIdx].getBytes(resource);
                }
            }

            if (bytes == null)
            {
                throw new IOException("Unable to find resource: " + url.toString());
            }

            is = new ByteArrayInputStream(bytes);
            contentLength = bytes.length;
            contentTime = 0L;  // TODO: Change this.
            contentType = URLConnection.guessContentTypeFromName(resource);
            connected = true;
        }
    }

    public InputStream getInputStream()
        throws IOException
    {
        if (!connected)
        {
            connect();
        }
        return is;
    }

    public int getContentLength()
    {
        if (!connected)
        {
            try {
                connect();
            } catch(IOException ex) {
                return -1;
            }
        }
        return contentLength;
    }

    public long getLastModified()
    {
        if (!connected)
        {
            try {
                connect();
            } catch(IOException ex) {
                return 0;
            }
        }
        if(contentTime != -1L)
            return contentTime;
        else
            return 0L;
    }

    public String getContentType()
    {
        if (!connected)
        {
            try {
                connect();
            } catch(IOException ex) {
                return null;
            }
        }
        return contentType;
    }

    public Permission getPermission()
    {
        // TODO: This should probably return a FilePermission
        // to access the bundle JAR file, but we don't have the
        // necessary information here to construct the absolute
        // path of the JAR file...so it would take some
        // re-arranging to get this to work.
        return null;
    }
}