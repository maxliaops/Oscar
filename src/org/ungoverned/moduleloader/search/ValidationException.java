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

import org.ungoverned.moduleloader.Module;

/**
 * <p>
 * This exception is thrown if a module cannot be validated. The module
 * that failed to be validated is recorded, along with the failed import target
 * identifier and version number. If the error was a result of a propagation
 * conflict, then the propagation error flag is set.
 * </p>
 * @see org.ungoverned.moduleloader.search.ImportSearchPolicy#validate(org.ungoverned.moduleloader.Module)
**/
public class ValidationException extends Exception
{
    private Module m_module = null;
    private Object m_identifier = null;
    private Object m_version = null;
    private boolean m_isPropagation = false;

    /**
     * Constructs an exception with the specified message, module,
     * import identifier, import version number, and propagation flag.
    **/
    public ValidationException(String msg, Module module,
        Object identifier, Object version, boolean isPropagation)
    {
        super(msg);
        m_module = module;
        m_identifier = identifier;
        m_version = version;
        m_isPropagation = isPropagation;
    }

    /**
     * Returns the module that was being validated.
     * @return the module that was being validated.
    **/
    public Module getModule()
    {
        return m_module;
    }

    /**
     * Returns the identifier of the import target that could not be resolved.
     * @return the identifier of the import target that could not be resolved.
    **/
    public Object getIdentifier()
    {
        return m_identifier;
    }

    /**
     * Returns the version number of the import target that could not be resolved.
     * @return the version number of the import target that could not be resolved.
    **/
    public Object getVersion()
    {
        return m_version;
    }

    /**
     * Returns a flag indicating whether the exception was caused by a
     * a propagation conflict.
     * @return <tt>true</tt> if the exception was thrown due to a propagation
     *         conflict, <tt>false</tt> otherwise.
    **/
    public boolean isPropagationError()
    {
        return m_isPropagation;
    }
}