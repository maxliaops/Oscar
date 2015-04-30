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

/**
 * <p>
 * This interface represents the naming and version numbering policy of
 * import and export identifiers for the <tt>ImportSearchPolicy</tt>. A concrete
 * implementation of this interface is required to create an instance
 * of <tt>ImportSearchPolicy</tt>. The sole purpose of this interface
 * is to allow the <tt>ImportSearchPolicy</tt> to determine if one
 * import/export identifier and version is compatible with another.
 * </p>
 * @see org.ungoverned.moduleloader.search.ImportSearchPolicy
**/
public interface CompatibilityPolicy
{
    /**
     * Compares two import/export identifiers.
     * @param leftId the identifier to test for compatibility.
     * @param leftVersion the version number to test for compatibility.
     * @param rightId the identifier used as the compatibility base line.
     * @param rightVersion the version used as the compatibility base line.
     * @return <tt>0</tt> if the identifiers are equal, <tt>-1</tt> if the
     *         left identifier is less then the right identifier, and <tt>1</tt>
     *         if the left identifier is greater than the right identifier.
     * @throws java.lang.IllegalArgumentException if the two identifiers
     *         are not comparable, i.e., they refer to intrinsically different
     *         entities.
    **/
    public int compare(
        Object leftId, Object leftVersion,
        Object rightId, Object rightVersion);

    /**
     * Returns whether the first import/export identifer is compatible
     * with the second; this method should not throw any exceptions.
     * @param leftId the identifier to test for compatibility.
     * @param leftVersion the version number to test for compatibility.
     * @param rightId the identifier used as the compatibility base line.
     * @param rightVersion the version used as the compatibility base line.
     * @return <tt>true</tt> if the left version number object is compatible
     *         with the right version number object, otherwise <tt>false</tt>.
    **/
    public boolean isCompatible(
        Object leftId, Object leftVersion,
        Object rightId, Object rightVersion);
}