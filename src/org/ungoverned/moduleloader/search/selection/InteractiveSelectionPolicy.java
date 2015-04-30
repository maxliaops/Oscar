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
package org.ungoverned.moduleloader.search.selection;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.ungoverned.moduleloader.Module;
import org.ungoverned.moduleloader.search.SelectionPolicy;
import org.ungoverned.moduleloader.search.CompatibilityPolicy;

/**
 * This class implements an interactive selection policy for the
 * <tt>ImportSearchPolicy</tt>. This policy simply uses standard
 * output to present the list of candidate modules and uses standard
 * input to allow the user to select a specific module from the
 * candidates. This selection policy is generally only useful for
 * debugging purposes.
 * @see org.ungoverned.moduleloader.search.SelectionPolicy
 * @see org.ungoverned.moduleloader.search.ImportSearchPolicy
**/
public class InteractiveSelectionPolicy implements SelectionPolicy
{
    /**
     * Returns a single package from an array of packages.
     * @param sources array of packages from which to choose.
     * @return the selected package or <tt>null</tt> if no package
     *         can be selected.
    **/
    public Module select(Module module, Object target,
        Object version, Module[] candidates, CompatibilityPolicy compatPolicy)
    {
        try {
            if (candidates.length == 1)
            {
                return candidates[0];
            }
            // Now start an interactive prompt.
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            do
            {
                System.out.println("\nImporting '" + target
                    + "(" + version + ")" + "' for '" + module + "'.");
                System.out.println("");
                for (int i = 0; i < candidates.length; i++)
                {
                    System.out.println((i + 1) + ". " + candidates[i]);
                }
                System.out.print("Select: ");
                String s = br.readLine();

                int choice = -1;
                try {
                    choice = Integer.parseInt(s);
                } catch (Exception ex) {
                }

                if (choice == 0)
                {
                    break;
                }
                else if ((choice > 0) && (choice <= candidates.length))
                {
                    return candidates[choice - 1];
                }
            }
            while (true);
        } catch (Exception ex) {
        }

        return null;
    }
}