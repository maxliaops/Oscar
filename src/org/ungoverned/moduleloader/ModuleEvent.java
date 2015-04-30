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

import java.util.EventObject;

/**
 * <p>
 * This is an event class that is used by the <tt>ModuleManager</tt> to
 * indicate when modules are added, removed, or reset. To receive these
 * events, a <tt>ModuleListener</tt> must be added to the <tt>ModuleManager</tt>
 * instance.
 * </p>
 * @see org.ungoverned.moduleloader.ModuleManager
 * @see org.ungoverned.moduleloader.Module
 * @see org.ungoverned.moduleloader.ModuleListener
**/
public class ModuleEvent extends EventObject
{
    private Module m_module = null;

    /**
     * <p>
     * Constructs a module event with the specified <tt>ModuleManager</tt>
     * as the event source and the specified module as the subject of
     * the event.
     * </p>
     * @param mgr the source of the event.
     * @param module the subject of the event.
    **/
    public ModuleEvent(ModuleManager mgr, Module module)
    {
        super(mgr);
        m_module = module;
    }

    /**
     * <p>
     * Returns the module that is the subject of the event.
     * </p>
     * @return the module that is the subject of the event.
    **/
    public Module getModule()
    {
        return m_module;
    }
}