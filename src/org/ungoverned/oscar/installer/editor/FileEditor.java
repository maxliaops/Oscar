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
package org.ungoverned.oscar.installer.editor;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.*;
import java.io.File;

import javax.swing.*;

import org.ungoverned.oscar.installer.Property;
import org.ungoverned.oscar.installer.StringProperty;

public class FileEditor extends JPanel
{
    private StringProperty m_prop = null;
    private JTextField m_textField = null;
    private JButton m_browseButton = null;
    private boolean m_isDirectory = false;

    public FileEditor(StringProperty prop, boolean isDirectory)
    {
        super();
        m_prop = prop;
        m_isDirectory = isDirectory;
        init();
    }

    public Property getProperty()
    {
        return m_prop;
    }

    public void setEnabled(boolean b)
    {
        m_textField.setEnabled(b);
        m_browseButton.setEnabled(b);
    }

    protected void init()
    {
        // Set layout.
        GridBagLayout grid = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 2, 0, 2);
        setLayout(grid);

        // Add field.
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 1;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        m_textField = new JTextField(30);
        m_textField.setText(m_prop.getStringValue());
        grid.setConstraints(m_textField, gbc);
        add(m_textField);

        // Add button.
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridheight = 1;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        m_browseButton = new JButton("Browse...");
        m_browseButton.setMargin(new Insets(1, 1, 1, 1));
        grid.setConstraints(m_browseButton, gbc);
        add(m_browseButton);

        // Add focus listener.
        m_textField.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent event)
            {
            }
            public void focusLost(FocusEvent event)
            {
                if (!event.isTemporary())
                {
                    // Set the new value.
                    m_prop.setStringValue(normalizeValue(m_textField.getText()));

                }
            }
        });

        // Add action listener.
        m_browseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event)
            {
                JFileChooser fileDlg = new JFileChooser();
                if (m_isDirectory)
                {
                    fileDlg.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    fileDlg.setDialogTitle("Please select a directory...");
                }
                else
                {
                    fileDlg.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    fileDlg.setDialogTitle("Please select a file...");
                }
                fileDlg.setApproveButtonText("Select");
                if (fileDlg.showOpenDialog(FileEditor.this) ==
                    JFileChooser.APPROVE_OPTION)
                {
                    m_textField.setText(fileDlg.getSelectedFile().getAbsolutePath());
                    m_prop.setStringValue(normalizeValue(m_textField.getText()));
                }
            }
        });
    }

    private String normalizeValue(String value)
    {
        // Make sure that directories never end with a slash,
        // for consistency.
        if (m_isDirectory)
        {
            if (value.endsWith(File.separator))
            {
                value = value.substring(0, value.length() - 1);
            }
        }
        return value;
    }
}