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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.ungoverned.oscar.installer.BooleanProperty;
import org.ungoverned.oscar.installer.Property;

public class BooleanEditor extends JPanel
{
    private BooleanProperty m_prop = null;
    private JRadioButton m_trueButton = null;
    private JRadioButton m_falseButton = null;
    private String m_trueString = null;
    private String m_falseString = null;

    public BooleanEditor(BooleanProperty prop)
    {
        this(prop, "true", "false");
    }

    public BooleanEditor(BooleanProperty prop, String trueString, String falseString)
    {
        m_prop = prop;
        m_trueString = trueString;
        m_falseString = falseString;
        init();
    }

    public Property getProperty()
    {
        return m_prop;
    }

    public void setEnabled(boolean b)
    {
        m_trueButton.setEnabled(b);
        m_falseButton.setEnabled(b);
    }

    protected void init()
    {
        add(m_trueButton = new JRadioButton(m_trueString));
        add(m_falseButton = new JRadioButton(m_falseString));
        ButtonGroup group = new ButtonGroup();
        group.add(m_trueButton);
        group.add(m_falseButton);
        if (m_prop.getBooleanValue())
        {
            m_trueButton.setSelected(true);
        }
        else
        {
            m_falseButton.setSelected(true);
        }

        // Add action listeners.
        m_trueButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event)
            {
                m_prop.setBooleanValue(true);
            }
        });
        m_falseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event)
            {
                m_prop.setBooleanValue(false);
            }
        });
    }
}