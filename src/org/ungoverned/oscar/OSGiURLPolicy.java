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

import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

import org.ungoverned.moduleloader.Module;
import org.ungoverned.moduleloader.ModuleManager;
import org.ungoverned.moduleloader.URLPolicy;
import org.ungoverned.oscar.util.OscarConstants;

public class OSGiURLPolicy implements URLPolicy
{
    private Oscar m_oscar = null;
    private BundleURLStreamHandler m_handler = null;
    private FakeURLStreamHandler m_fakeHandler = null;

    public OSGiURLPolicy(Oscar oscar)
    {
        m_oscar = oscar;
    }

    public URL createCodeSourceURL(ModuleManager mgr, Module module)
    {
        URL url = null;
        BundleImpl bundle = null;
        try
        {
            bundle = (BundleImpl)
                m_oscar.getBundle(BundleInfo.getBundleIdFromModuleId(module.getId()));
            if (bundle != null)
            {
                url = new URL(bundle.getInfo().getLocation());
            }
        }
        catch (NumberFormatException ex)
        {
            url = null;
        }
        catch (MalformedURLException ex)
        {
            if (m_fakeHandler == null)
            {
                m_fakeHandler = new FakeURLStreamHandler();
            }
            try
            {
                url = new URL(null,
                    OscarConstants.FAKE_URL_PROTOCOL_VALUE
                    + "//" + bundle.getLocation(), m_fakeHandler);
            }
            catch (Exception ex2)
            {
                url = null;
            }
        }
        return url;
    }

    public URL createResourceURL(ModuleManager mgr, Module module, int rsIdx, String name)
    {
        if (m_handler == null)
        {
            m_handler = new BundleURLStreamHandler(mgr);
        }

        // Add a slash if there is one already, otherwise
        // the is no slash separating the host from the file
        // in the resulting URL.
        if (!name.startsWith("/"))
        {
            name = "/" + name;
        }

        try
        {
            if (System.getSecurityManager() != null)
            {
                return (URL) AccessController.doPrivileged(
                    new CreateURLPrivileged(module.getId(), rsIdx, name));
            }
            else
            {
                return new URL(OscarConstants.BUNDLE_URL_PROTOCOL,
                    module.getId(), -1, "/" + rsIdx + name, m_handler);
            }
        }
        catch (Exception ex)
        {
            System.err.println("OSGiURLPolicy: " + ex);
            return null;
        }
    }

    /**
     * This simple class is used to perform the privileged action of
     * creating a URL using the "bundle:" protocol stream handler.
    **/
    private class CreateURLPrivileged implements PrivilegedExceptionAction
    {
        private String m_id = null;
        private int m_rsIdx = 0;
        private String m_name = null;

        public CreateURLPrivileged(String id, int rsIdx, String name)
        {
            m_id = id;
            m_rsIdx = rsIdx;
            m_name = name;
        }

        public Object run() throws Exception
        {
            return new URL("bundle", m_id, -1, "/" + m_rsIdx + m_name, m_handler);
        }
    }
}