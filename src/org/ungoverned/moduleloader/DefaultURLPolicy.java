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

import java.net.URL;

/**
 * <p>
 * This class implements a simple <tt>URLPolicy</tt> that the <tt>ModuleManager</tt>
 * uses if the application does not specify one. This implementation always returns
 * <tt>null</tt> for <tt>CodeSource</tt> <tt>URL</tt>s, which means that security
 * is simply ignored. For resource <tt>URL</tt>s, it returns an <tt>URL</tt> in the
 * form of:
 * </p>
 * <pre>
 *     module://&lt;module-id&gt;/&lt;resource-path&gt;
 * </pre>
 * <p>
 * In order to properly handle the "<tt>module:</tt>" protocol, this policy
 * also defines a custom <tt>java.net.URLStreamHandler</tt> that it assigns
 * to each <tt>URL</tt> as it is created. This custom handler is used to
 * return a custom <tt>java.net.URLConnection</tt> that will correctly parse
 * the above <tt>URL</tt> and retrieve the associated resource bytes using
 * methods from <tt>ModuleManager</tt> and <tt>Module</tt>.
 * </p>
 * @see org.ungoverned.moduleloader.ModuleManager
 * @see org.ungoverned.moduleloader.Module
 * @see org.ungoverned.moduleloader.URLPolicy
**/
public class DefaultURLPolicy implements URLPolicy
{
    private ModuleURLStreamHandler m_handler = null;

    /**
     * <p>
     * This method is a stub and always returns <tt>null</tt>.
     * </p>
     * @param mgr the <tt>ModuleManager</tt> of the module.
     * @param module the module for which the <tt>URL</tt> is to be created.
     * @return <tt>null</tt>.
    **/
    public URL createCodeSourceURL(ModuleManager mgr, Module module)
    {
        return null;
    }

    /**
     * <p>
     * This method returns a <tt>URL</tt> that is suitable
     * for accessing the bytes of the specified resource.
     * </p>
     * @param mgr the <tt>ModuleManager</tt> of the module.
     * @param module the module for which the resource is being loaded.
     * @param rsIdx the index of the <tt>ResourceSource</tt> containing the resource.
     * @param name the name of the resource being loaded.
     * @return an <tt>URL</tt> for retrieving the resource.
    **/
    public URL createResourceURL(ModuleManager mgr, Module module, int rsIdx, String name)
    {
        if (m_handler == null)
        {
            m_handler = new ModuleURLStreamHandler(mgr);
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
            return new URL("module", module.getId(), -1, "/" + rsIdx + name, m_handler);
        }
        catch (Exception ex)
        {
            System.err.println("DefaultResourceURLPolicy: " + ex);
            return null;
        }
    }
}