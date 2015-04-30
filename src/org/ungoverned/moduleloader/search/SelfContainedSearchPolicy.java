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
package org.ungoverned.moduleloader.search;

import java.net.URL;

import org.ungoverned.moduleloader.Module;
import org.ungoverned.moduleloader.ModuleManager;
import org.ungoverned.moduleloader.SearchPolicy;

/**
 * <p>
 * This class implements a <tt>ModuleLoader</tt> search policy that
 * assumes that all modules are self-contained. In other words, when
 * loading a class or resource for a particular module, only that
 * particular module's resource sources are search. No classes or
 * resources are shared among modules.
 * </p>
 * @see org.ungoverned.moduleloader.SearchPolicy
 * @see org.ungoverned.moduleloader.Module
 * @see org.ungoverned.moduleloader.ModuleClassLoader
 * @see org.ungoverned.moduleloader.ModuleManager
**/
public class SelfContainedSearchPolicy implements SearchPolicy
{
    private ModuleManager m_mgr = null;

    /**
     * This method is part of the <tt>SearchPolicy</tt> interface.
     * This method is called by the <tt>ModuleManager</tt> once to
     * give the search policy instance a reference to its associated
     * module manager. This method should be implemented such that
     * it cannot be called twice; calling this method a second time
     * should produce an illegal state exception.
     * @param mgr the module manager associated with this search policy.
     * @throws java.lang.IllegalStateException if the method is called
     *         more than once.
    **/
    public void setModuleManager(ModuleManager mgr)
        throws IllegalStateException
    {
        if (m_mgr == null)
        {
            m_mgr = mgr;
        }
        else
        {
            throw new IllegalStateException("Module manager is already initialized");
        }
    }

    /**
     * Simply returns <tt>null</tt> which forces the module class
     * loader to only search the target module's resource sources
     * for the specified class.
     * @param module the target module that is loading the class.
     * @param name the name of the class being loaded.
     * @return <tt>null</tt>.
    **/
    public Class findClass(Module module, String name)
    {
        return null;
    }

    /**
     * Simply returns <tt>null</tt> which forces the module class
     * loader to only search the target module's resource sources
     * for the specified resource.
     * @param module the target module that is loading the class.
     * @param name the name of the resource being loaded.
     * @return <tt>null</tt>.
    **/
    public URL findResource(Module module, String name)
    {
        return null;
    }
}