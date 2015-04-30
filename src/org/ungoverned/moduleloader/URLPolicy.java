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
 * This interface represents the <tt>ModuleLoader</tt>'s policy for creating
 * <tt>URL</tt> for resource loading and security purposes. Java requires the
 * use of <tt>URL</tt>s for resource loading and security. For resource loading,
 * <tt>URL</tt>s are returned for requested resources. Subsequently, the resource
 * <tt>URL</tt> is used to create an input stream for reading the resources
 * bytes. With respect to security, <tt>URL</tt>s are used when defining a
 * class in order to determine where the code came from, this concept is called
 * a <tt>CodeSource</tt>. This approach enables Java to assign permissions to
 * code that originates from particular locations.
 * </p>
 * <p>
 * The <tt>ModuleManager</tt> requires a concrete implementation of this
 * interface in order to function. Whenever the <tt>ModuleManager</tt> requires
 * a <tt>URL</tt> for either resource loading or security, it delegates to
 * the policy implementation. A default implementation is provided,
 * called <a href="DefaultURLPolicy.html"><tt>DefaultURLPolicy</tt></a>, but
 * it only supports resource loading, not security.
 * </p>
 * @see org.ungoverned.moduleloader.ModuleManager
 * @see org.ungoverned.moduleloader.DefaultURLPolicy
**/
public interface URLPolicy
{
    /**
     * <p>
     * This method should return a <tt>URL</tt> that represents the
     * location from which the module originated. This <tt>URL</tt>
     * can be used when assigning permissions to the module, such as
     * is done in the Java permissions policy file.
     * </p>
     * @param mgr the <tt>ModuleManager</tt> of the module.
     * @param module the module for which the <tt>URL</tt> is to be created.
     * @return an <tt>URL</tt> to associate with the module.
    **/
    public URL createCodeSourceURL(ModuleManager mgr, Module module);

    /**
     * <p>
     * This method should return a <tt>URL</tt> that is suitable
     * for accessing the bytes of the specified resource. It must be possible
     * open a connection to this <tt>URL</tt>, which may require that
     * the implementer of this method also introduce a custom
     * <tt>java.net.URLStreamHander</tt> when creating the <tt>URL</tt>.
     * </p>
     * @param mgr the <tt>ModuleManager</tt> of the module.
     * @param module the module for which the resource is being loaded.
     * @param rsIdx the index of the <tt>ResourceSource</tt> containing the resource.
     * @param name the name of the resource being loaded.
     * @return an <tt>URL</tt> for retrieving the resource.
    **/
    public URL createResourceURL(ModuleManager mgr, Module module, int rsIdx, String name);
}