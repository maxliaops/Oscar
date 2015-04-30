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

import org.ungoverned.moduleloader.search.CompatibilityPolicy;

public class OSGiCompatibilityPolicy implements CompatibilityPolicy
{
    private Oscar m_oscar = null;

    public OSGiCompatibilityPolicy(Oscar oscar)
    {
        m_oscar = oscar;
    }

    /**
     * Compares two versioned identifiers.
     * @param leftId the identifier to test for compatibility.
     * @param leftVersion the version number to test for compatibility.
     * @param rightId the identifier used as the compatibility base line.
     * @param rightVersion the version used as the compatibility base line.
     * @return <tt>0</tt> if the identifiers are equal, <tt>-1</tt> if the
     *         left identifier is less then the right identifier, and <tt>1</tt>
     *         if the left identifier is greater than the right identifier.
     * @throws java.lang.IllegalArgumentException if the two identifiers
     *         are not comparable, i.e., they refer to completely different
     *         entities.
    **/
    public int compare(
        Object leftId, Object leftVersion,
        Object rightId, Object rightVersion)
    {
        if (isComparable(leftId, rightId))
        {
            return compareVersion((int[]) leftVersion, (int[]) rightVersion);
        }
        else
        {
            throw new IllegalArgumentException("Identifiers are not comparable.");
        }
    }

    /**
     * Returns whether the first import/export target is compatible
     * with the second.
     * @param leftId the identifier to test for compatibility.
     * @param leftVersion the version number to test for compatibility.
     * @param rightId the identifier used as the compatibility base line.
     * @param rightVersion the version used as the compatibility base line.
     * @return <tt>true</tt> if the left version number object is compatible
     *         with the right version number object, otherwise <tt>false</tt>.
    **/
    public boolean isCompatible(
        Object leftId, Object leftVersion,
        Object rightId, Object rightVersion)
    {
        try {
            // The OSGi spec says that identifiers are always backwards compatible.
            return (compare(leftId, leftVersion, rightId, rightVersion) >= 0);
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Returns whether the two identifiers are comparable. This method was
     * intended to allow different policies of comparing imports to exports,
     * such as allowing "wildcard" exports, but it does not fully work for
     * that purpose and is possibly not necessary.
     * 
     * @param leftId the first identifier to test.
     * @param rightId the second identifier to test.
     * @return <tt>true</tt> if the two identifiers are comparable,
     *         <tt>false</tt> otherwise.
    **/
    private boolean isComparable(Object leftId, Object rightId)
    {
        return leftId.equals(rightId);
    }

    private int compareVersion(int[] left, int[] right)
    {
        if (left[0] > right[0])
            return 1;
        else if (left[0] < right[0])
            return -1;
        else if (left[1] > right[1])
            return 1;
        else if (left[1] < right[1])
            return -1;
        else if (left[2] > right[2])
            return 1;
        else if (left[2] < right[2])
            return -1;
        return 0;
    }
}