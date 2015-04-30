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
 * Dennis Heimbigner
 *
**/
package org.ungoverned.oscar.ldap;

import java.util.Stack;
import java.util.Vector;

public class Evaluator {

    Object[] program = null;
    Stack operands = new Stack();
    Mapper mapper = null;

    public Evaluator()
    {
        reset();
    }

    public Evaluator(Object[] prog)
    {
        reset(prog);
    }

    public void reset()
    {
        program = null;
        mapper = null;
        operands.clear();
    }

    public void reset(Object[] prog)
    {
        reset();
        setProgram(prog);
    }

    public void setProgram(Object[] prog)
    {
        program = prog;
    }

    public void setMapper(Mapper mapper)
    {
        this.mapper = mapper;
    }

    public Stack getOperands()
    {
        return operands;
    }

    public boolean evaluate(Mapper mapper) throws EvaluationException
    {
        try
        {
            // The following code is a little complicated because it
            // is trying to deal with evaluating a given filter expression
            // when it contains an attribute that does not exist in the
            // supplied mapper. In such a situation the code below
            // catches the "attribute not found" exception and inserts
            // an instance of Unknown, which is used as a marker for
            // non-existent attributes. The Unknown instance forces the
            // operator to throw an "unsupported type" exception, which
            // the code below converts in a FALSE and this has the effect
            // of evaluating the entire subexpression that contained the
            // non-existent attribute to FALSE. Any other exceptions are
            // rethrown.
            setMapper(mapper);
            for (int i = 0; i < program.length; i++)
            {
                try
                {
                    Operator op = (Operator) program[i];
                    op.execute(operands, mapper);
//                    printAction(op); // for debug output
                }
                catch (AttributeNotFoundException ex)
                {
                    operands.push(new Unknown());
                }
                catch (EvaluationException ex)
                {
                    // If the exception is for an unsupported type of
                    // type Unknown, then just push FALSE onto the
                    // operand stack.
                    if (ex.isUnsupportedType() && (ex.getUnsupportedType() == Unknown.class))
                    {
                        operands.push(Boolean.FALSE);
                    }
                    // Otherwise, rethrow the exception.
                    else
                    {
                        throw ex;
                    }
                }
            }

            if (operands.empty())
            {
                throw new EvaluationException("Evaluation.evalute: final stack is empty");
            }

            Object result = operands.pop();

            if (!operands.empty())
            {
                throw new EvaluationException(
                    "Evaluation.evalute: final stack has more than one result");
            }

            if (!(result instanceof Boolean))
            {
                throw new EvaluationException(
                    "Evaluation.evalute: final result is not Boolean");
            }

            return ((Boolean) result).booleanValue();
        }
        finally
        {
            // Clear the operands just in case an exception was thrown,
            // otherwise stuff will be left in the stack.
            operands.clear();
        }
    }

    // For debugging; Dump the operator and stack
    void printAction(Operator op)
    {
        System.err.println("Operator:"+op.toString());
        System.err.print("Stack After:");
        // recast operands as Vector to make interior access easier
        Vector v = operands;
        int len = v.size();
        for (int i = 0; i < len; i++)
            System.err.print(" " + v.elementAt(i));
        System.err.println();
    }

    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < program.length; i++)
        {
            buf.append((i==0) ? "{" : ";");
                buf.append(((Operator) program[i]).toString());
        }
        buf.append("}");
        return buf.toString();
    }

    public String toStringInfix()
    {
        // First, we "evaluate" the program
        // but for the purpose of re-constructing
        // a parsetree.
        operands.clear();
        for (int i = 0; i < program.length; i++)
        {
            ((Operator) program[i]).buildTree(operands);
        }
        StringBuffer b = new StringBuffer();
        Object result = operands.pop();
        ((Operator)result).toStringInfix(b);
        operands.clear();
        return b.toString();
    }
}
