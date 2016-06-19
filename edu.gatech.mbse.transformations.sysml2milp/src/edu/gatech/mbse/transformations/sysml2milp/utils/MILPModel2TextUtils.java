/**
 * Copyright (c) 2015, Model-Based Systems Engineering Center, Georgia Institute of Technology.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 * 
 *    Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *    following disclaimer.
 * 
 *    Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 *    the following disclaimer in the documentation and/or other materials provided with the distribution.
 *   
 *    Neither the name of salesforce.com, inc. nor the names of its contributors may be used to endorse or
 *    promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package edu.gatech.mbse.transformations.sysml2milp.utils;

import java.util.ArrayList;

import edu.gatech.mbse.transformations.sysml2milp.MILPVariableType;

/**
 * Interface for MILP model to text transformation.
 * <P>
 * Note that these functions are still specific to YALMIP in some sense. In future
 * versions, MILPVariableType should be generalized. In future versions, these
 * functions should also take Variable and Constraint objects as input.
 * 
 * @author Sebastian
 * @version 0.1
 */
public interface MILPModel2TextUtils {

	/**
	 * Generate a textual representation of a MILP variable.
	 * <P>
	 * This function will define a variable and its specific type.
	 * 
	 * @param name Name of variable.
	 * @param varType Variable type.
	 * @return MILP code.
	 */
	public String generateVariableDeclaration(String name, MILPVariableType varType);
	
	/**
	 * Similar to {@link #generateVariableDeclaration(String, MILPVariableType)}, with the
	 * difference being that, as an additional argument, a commment may be passed.
	 * 
	 * @param name Name of variable.
	 * @param varType Variable type.
	 * @param comment A comment to add to target MILP code.
	 * @return MILP code.
	 */
	public String generateVariableDeclaration(String name, MILPVariableType varType, String comment);
	
	/**
	 * Generate a textual representation of a MILP variable or constant with a value.
	 * 
	 * @param name Name of variable
	 * @param value Variable type.
	 * @return MILP code.
	 */
	public String generateVariableDeclaration(String name, String value);
	
	/**
	 * Works similarly to {@link #generateVariableDeclaration(String, String)}, with the
	 * additional feature of being able to supply a comment.
	 * 
	 * @param name Name of variable.
	 * @param value Variable type.
	 * @param comment A comment to add to target MILP code.
	 * @return MILP code.
	 */
	public String generateVariableDeclaration(String name, String value, String comment);
	
	/**
	 * Generate a constraint definition for a single constraint.
	 * 
	 * @param constraint The constraint to represent.
	 * @return MILP code.
	 */
	public String generateConstraintDeclaration(String constraint);
	
	/**
	 * Generate a constraint declaration followed by a comment.
	 * 
	 * @param constraint The constraint to represent
	 * @param comment A comment to add to target MILP code.
	 * @return MILP code.
	 */
	public String generateConstraintDeclaration(String constraint, String comment);
	
	/**
	 * Generate a constraint declaration / definition for multiple constraints.
	 * 
	 * @param constraints The constraints to represent.
	 * @return MILP code.
	 */
	public String generateConstraintDeclaration(ArrayList<String> constraints);
	
	/**
	 * Generate a constraint declaration / definition for multiple constraints, preceded
	 * by a comment (to represent a block of constraints).
	 * 
	 * @param constraints The constraints to represent.
	 * @param comment The comment to add to the target MILP code.
	 * @return MILP code.
	 */
	public String generateConstraintDeclaration(ArrayList<String> constraints, String comment);
	
	/**
	 * Generate a free-standing comment in the MILP code.
	 * 
	 * @param comment The comment to add to MILP code.
	 * @return MILP code.
	 */
	public String generateComment(String comment);
	
	/**
	 * Generate a line separator in the target MILP code.
	 * 
	 * @return MILP code.
	 */
	public String generateLineSeparator();
	
	/**
	 * Generate an objective definition.
	 * 
	 * @param objective Objective function as string (interpreted).
	 * @return MILP code.
	 */
	public String generateObjectiveDelaration(String objective);
	
	/**
	 * Generate text visualization procedure in target MILP environment.
	 * 
	 * @param textToVisualize Test to visualize.
	 * @return MILP code.
	 */
	public String generateTextVisualization(String textToVisualize);
	
	/**
	 * Generate required preamble (e.g., resetting of variables in solver environment,
	 * or some other form of cleanup and preparation).
	 * 
	 * @return MILP code.
	 */
	public String generatePreamble();
	
	/**
	 * Generate postamble.
	 * <P>
	 * Typically, this will include some visualization output, call the solver (or
	 * configure it) and generate code that will produce n solutions as specified. The last
	 * argument refers to whether or not the required output (Excel data and instanceData)
	 * is to be produced, or just the output for the testing infrastructure.
	 * 
	 * @param visualizationOuput The visualization commands.
	 * @param numSolutions The number of solutions to produce.
	 * @param writeResults Whether or not to write the required Excel and instanceData.
	 * @return MILP code.
	 */
	public String generatePostamble(String visualizationOuput, int numSolutions, boolean writeResults);

}
