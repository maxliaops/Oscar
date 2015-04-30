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
 * This interface represents the policy for selecting a specific export
 * target from multiple <i>compatible</i> candidate export targets when
 * the <tt>ImportSearchPolicy</tt> is trying to resolve an import target
 * for a given module. A concrete implementation of this interface is
 * required to create an instance of <tt>ImportSearchPolicy</tt>.
 * </p>
 * @see org.ungoverned.moduleloader.search.ImportSearchPolicy
**/
public interface SelectionPolicy
{
    /**
     * Selects a single module to resolve the specified import
     * from the array of compatible candidate modules.
     * @param module the module that is importing the target.
     * @param identifier the identifier of the import target.
     * @param version the version number of the import target.
     * @param candidates array of compatible candidate modules from which to choose.
     * @param compatPolicy the compatibility policy that is being used.
     * @return the selected module or <tt>null</tt> if no module
     *         can be selected.
    **/
    public Module select(
        Module module, Object identifier, Object version, Module[] candidates,
        CompatibilityPolicy compatPolicy);
}