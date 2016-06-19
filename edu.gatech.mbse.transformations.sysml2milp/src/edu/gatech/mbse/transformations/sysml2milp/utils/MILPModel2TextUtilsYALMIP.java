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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.gatech.mbse.transformations.sysml2milp.MILPVariableType;
import edu.gatech.mbse.transformations.sysml2milp.SolverType;
import edu.gatech.mbse.transformations.sysml2milp.StandardVariableNames;
import edu.gatech.mbse.transformations.sysml2milp.SysML2MILPTransformation;
import edu.gatech.mbse.transformations.sysml2milp.TransformationConfig;

/**
 * Collection of utilities for generating a textual representation of a MILP problem
 * in YALMIP / Matlab syntax.
 * <P>
 * This is the only file that should produce YALMIP-specific output.
 * 
 * @author Sebastian
 * @version 0.1
 */
public class MILPModel2TextUtilsYALMIP implements MILPModel2TextUtils {

	/** Log4J object. */
	private static final Logger logger = LogManager.getLogger(MILPModel2TextUtilsYALMIP.class.getName());
	
	/**
	 * Constructor.
	 *
	 */
	public MILPModel2TextUtilsYALMIP() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see edu.gatech.mbse.transformations.sysml2milp.utils.MILPModel2TextUtils#generateVariableDeclaration(java.lang.String, edu.gatech.mbse.transformations.sysml2milp.MILPVariableType)
	 *
	 * @param name
	 * @param varType
	 * @return
	 */
	@Override
	public String generateVariableDeclaration(String name, MILPVariableType varType) {
		return generateVariableDeclaration(name, varType, "");
	}

	/**
	 * @see edu.gatech.mbse.transformations.sysml2milp.utils.MILPModel2TextUtils#generateVariableDeclaration(java.lang.String, edu.gatech.mbse.transformations.sysml2milp.MILPVariableType, java.lang.String)
	 *
	 * @param name
	 * @param varType
	 * @param comment
	 * @return
	 */
	@Override
	public String generateVariableDeclaration(String name, MILPVariableType varType, String comment) {
		switch (varType) {
			case SDPVAR:
				return generateVariableDeclaration(name, "sdpvar(1)", comment);
			
			case INTVAR:
				return generateVariableDeclaration(name, "intvar(1)", comment);
				
			default:
				// Fall through
			case BINVAR:
				return generateVariableDeclaration(name, "binvar(1)", comment);
		}
	}

	/**
	 * @see edu.gatech.mbse.transformations.sysml2milp.utils.MILPModel2TextUtils#generateVariableDeclaration(java.lang.String, java.lang.String)
	 *
	 * @param name
	 * @param value
	 * @return
	 */
	@Override
	public String generateVariableDeclaration(String name, String value) {
		return generateVariableDeclaration(name, value, "");
	}

	/**
	 * @see edu.gatech.mbse.transformations.sysml2milp.utils.MILPModel2TextUtils#generateVariableDeclaration(java.lang.String, java.lang.String, java.lang.String)
	 * <P>
	 * This is the only function that actually produces concrete textual output for
	 * constraints.
	 * 
	 * @param name
	 * @param value
	 * @param comment
	 * @return
	 */
	@Override
	public String generateVariableDeclaration(String name, String value, String comment) {
		if (value == null || value.equals("") || value.equals("null"))
			return "";
		
		if (comment.equals(""))
			return name + " = " + value + ";\r\n";
		
		return name + " = " + value + ";		% " + comment + "\r\n";
	}

	/**
	 * @see edu.gatech.mbse.transformations.sysml2milp.utils.MILPModel2TextUtils#generateConstraintDeclaration(java.lang.String)
	 *
	 * @param constraint
	 * @return
	 */
	@Override
	public String generateConstraintDeclaration(String constraint) {
		ArrayList<String> constraints = new ArrayList<String>(1);
		constraints.add(constraint);
		
		return generateConstraintDeclaration(constraints);
	}

	/**
	 * @see edu.gatech.mbse.transformations.sysml2milp.utils.MILPModel2TextUtils#generateConstraintDeclaration(java.lang.String, java.lang.String)
	 *
	 * @param constraint
	 * @param comment
	 * @return
	 */
	@Override
	public String generateConstraintDeclaration(String constraint, String comment) {
		ArrayList<String> constraints = new ArrayList<String>(1);
		constraints.add(constraint);
		
		return generateConstraintDeclaration(constraints, comment);
	}

	/**
	 * @see edu.gatech.mbse.transformations.sysml2milp.utils.MILPModel2TextUtils#generateConstraintDeclaration(java.util.ArrayList)
	 *
	 * @param constraints
	 * @return
	 */
	@Override
	public String generateConstraintDeclaration(ArrayList<String> constraints) {
		return generateConstraintDeclaration(constraints, "");
	}

	/**
	 * @see edu.gatech.mbse.transformations.sysml2milp.utils.MILPModel2TextUtils#generateConstraintDeclaration(java.util.ArrayList, java.lang.String)
	 * <P>
	 * This is the only function that actually produces concrete textual output for
	 * constraints.
	 * 
	 * @param constraints
	 * @param comment
	 * @return
	 */
	@Override
	public String generateConstraintDeclaration(ArrayList<String> constraints, String comment) {
		int count = 0;			// MATLAB does not like nesting with depth > 32
		int numCons = 0;
		StringBuilder builder = new StringBuilder();

		if (constraints == null
				|| constraints.size() < 1)
			return "";
		
		// First generate a comment
		if (!comment.equals("") && constraints.size() > 1)
			builder.append(generateComment(comment));
		
		// Then the constraint vector
		builder.append("Constraints = [Constraints, ");
		
		boolean first = true;
		for(String constraint : constraints) {
			if(first)
				first = false;
			else
				builder.append(", ");
			
			builder.append(constraint);
			
			count++;
			numCons++;
			
			if(count == 1 && numCons < constraints.size()) {
				count = 0;
				
				builder.append("];\r\nConstraints = [Constraints");
			}
		}
		
		if (!comment.equals("") && constraints.size() == 1)
			builder.append("];\t\t% " + comment + "\r\n");
		else
			builder.append("];\r\n");
		
		return builder.toString();
	}

	/**
	 * @see edu.gatech.mbse.transformations.sysml2milp.utils.MILPModel2TextUtils#generateComment(java.lang.String)
	 *
	 * @param comment
	 * @return
	 */
	@Override
	public String generateComment(String comment) {
		return "\r\n% " + comment + "\r\n";
	}
	
	/**
	 * Adds a line separator - for readability of output only.
	 * 
	 * @see edu.gatech.mbse.transformations.sysml2milp.utils.MILPModel2TextUtils#generateLineSeparator()
	 *
	 * @return
	 */
	@Override
	public String generateLineSeparator() {
		return "\r\n";
	}

	/**
	 * @see edu.gatech.mbse.transformations.sysml2milp.utils.MILPModel2TextUtils#generateObjectiveDelaration(java.lang.String)
	 *
	 * @param objective
	 * @return
	 */
	@Override
	public String generateObjectiveDelaration(String objective) {
		return generateVariableDeclaration(
				StandardVariableNames.OBJECTIVE,
				objective);
	}
	
	/**
	 * Note that the argument to this function must follow MATLAB sprintf function
	 * syntax - you may have a constant text, which will be encompassed with
	 * "'", or a more complex string, where it is the user's responsibility to
	 * include "'" characters.
	 * 
	 * @see edu.gatech.mbse.transformations.sysml2milp.utils.MILPModel2TextUtils#generateTextVisualization(java.lang.String)
	 *
	 * @param textToVisualize
	 * @return
	 */
	@Override
	public String generateTextVisualization(String textToVisualize) {
		if (!textToVisualize.startsWith("'"))
			textToVisualize = "'" + textToVisualize.replace("'", "\\'") + "'";
		
		return "disp(sprintf(" + textToVisualize + "))\r\n";
	}

	/**
	 * @see edu.gatech.mbse.transformations.sysml2milp.utils.MILPModel2TextUtils#generatePreamble()
	 *
	 * @return
	 */
	@Override
	public String generatePreamble() {
		return "yalmip('clear');\r\nclear all;\r\nclc;\r\n\r\n";
	}

	/**
	 * @see edu.gatech.mbse.transformations.sysml2milp.utils.MILPModel2TextUtils#generatePostamble()
	 *
	 * @param visualizationOuput
	 * @param numSolutions
	 * @param testingMode
	 * @return
	 */
	@Override
	public String generatePostamble(String visualizationOuput,
			int numSolutions,
			boolean testingMode) {
		String milpCode = "";
		
		// Get configured solver
		String solverString = SolverType.solverTypeToYALMIPString(TransformationConfig.CONFIG_SOLVER);
		
		// Write loop that will a) call the MILP solver b) output results c) Exclude solution from next iteration
		milpCode += "\r\ncounter = " + numSolutions + ";\t\t% Number of solutions to generate\r\n";
		milpCode += "\r\nwhile counter > 0\r\n";
		milpCode += "\tcounter = counter - 1;\r\n";
		milpCode += "\tsol = optimize(Constraints, Objective, sdpsettings('solver','" + solverString + "','debug','1'))\r\n";
		milpCode += "\r\n\tif sol.problem ~= 0\r\n\t\tbreak;\r\n\tend\r\n";		// Catch error code from YALMIP
		
		// Visualization (tabulate for cleaner formatting)
		milpCode += "\r\n\t" + visualizationOuput.replace("\r\n", "\r\n\t");
		
		// If desired, also write output to file
		if (testingMode) {
			// Open file
			milpCode += "\r\n";
			milpCode += "\thome = '" + System.getProperty("java.io.tmpdir") + "';";
			milpCode += "\r\n";
			milpCode += "\tfID = fopen(strcat(home, '/tmp_milp_results.txt'), 'w');\r\n";
			milpCode += "\r\n";
			
			// Write results
			// This will convert all disp(sprintf(... statements into fprintf(... statements
			// -> Not the best way, but a fairly generic way
			String[] visLines = visualizationOuput.split("\r\n");
			
			for (String line : visLines) {
				if (line.startsWith("disp(sprintf(")) {
					line = line.replace("disp(sprintf(", "\tfprintf(fID, ");
					line = line.replace(":", " =");
					line = line.replace("',", "\\r\\n',");
					line = line.substring(0, line.length() - 1) + ";";
					milpCode += line + "\r\n";
				}
				
				// Note: correspondence output will simply be skipped here
			}
			
			// Close file
			milpCode += "\r\n";
			milpCode += "\tfclose(fID);\r\n";
		}
		else {
			// Add Excel output as well
			milpCode += generateExcelOutput();
		}
		
		// Exclude solution from next iteration, and loop
		milpCode += "\r\n\tConstraints = [Constraints, exclude(IM, round(value(IM)))];    % Exclude combination of values in association matrix (one unique solution)\r\n";
		milpCode += "end\r\n";
		
		return milpCode;
	}
	
	
	/**
	 * Generates Matlab code that will generate an excel spreadsheet showing the
	 * solution.<BR />
	 * <BR />
	 * Note that this function is NOT generic and very specific to Matlab! This should
	 * be considered a temporary "hack" until a correspondence model can be implemented.
	 * 
	 * @return A set of MATLAB specific commands to generate the required Excel output.
	 */
	private String generateExcelOutput() {
		String matlabCode = "";
		
		// Generate table data, then write to excel file
		matlabCode += "\r\n";
		matlabCode += "\t% Excel output generation\r\n";
		matlabCode += "\tfilename = strcat('" + System.getProperty("user.home") + "/milp_solution_', int2str(counter), '.xlsx');\r\n";
		matlabCode += "\r\n";
		
		// Compute applicable working principle and function names
		matlabCode += "\tWPNames = {''};\r\n";
		matlabCode += "\tfor i=1:length(value(A(1,:)))\r\n";
		matlabCode += "\t\tif sum(round(value(A(:,i)))) >= 1\r\n";
		matlabCode += "\t\t\tindices = strfind(" + StandardVariableNames.WP_TYPE_LIST + "{i}, '::');\r\n";
		matlabCode += "\t\t\tif length(indices) > 0\r\n";
		matlabCode += "\t\t\t\tname = " + StandardVariableNames.WP_TYPE_LIST + "{i}(indices(length(indices))+2:length(" + StandardVariableNames.WP_TYPE_LIST + "{i}));\r\n";
		matlabCode += "\t\t\telse\r\n";
		matlabCode += "\t\t\t\tname = " + StandardVariableNames.WP_TYPE_LIST + "{i};\r\n";
		matlabCode += "\t\t\tend\r\n";
		matlabCode += "\r\n";
		matlabCode += "\t\t\tname = strcat(" + StandardVariableNames.FUNCTION_NAMES_LIST + "{i}, ' (', name, ')');\r\n";
		matlabCode += "\r\n";
		matlabCode += "\t\t\tWPNames = [WPNames, name];\r\n";
		matlabCode += "\t\tend\r\n";
		matlabCode += "\tend\r\n";
		matlabCode += "\t\r\n";
		
		// Compute list of names of resources (with qualified path stripped), where duplicates indicate multiple
		// instances of the same type
		matlabCode += "\tResourceNames = {''};\r\n";
		matlabCode += "\tResourceNamesPartsOfComposite = {''};\r\n";
		matlabCode += "\tfor i=1:length(value(A(:,1)))\r\n";
		matlabCode += "\t\tif sum(round(value(A(i,:)))) >= 1\r\n";
		matlabCode += "\t\t\tindices = strfind(" + StandardVariableNames.TYPE_LIST + "{i}, '::');\r\n";
		matlabCode += "\t\t\tif length(indices) > 0\r\n";
		matlabCode += "\t\t\t\tname = " + StandardVariableNames.TYPE_LIST + "{i}(indices(length(indices))+2:length(" + StandardVariableNames.TYPE_LIST + "{i}));\r\n";
		matlabCode += "\t\t\telse\r\n";
		matlabCode += "\t\t\t\tname = " + StandardVariableNames.TYPE_LIST + "{i};\r\n";
		matlabCode += "\t\t\tend\r\n";
		matlabCode += "\r\n";
		matlabCode += "\t\t\tResourceNames = [ResourceNames, name];\r\n";
		matlabCode += "\r\n";
		matlabCode += "\t\t\tif sum(round(value(C(i,:)))) >= 1\r\n";
		matlabCode += "\t\t\t\tResourceNamesPartsOfComposite = [ResourceNamesPartsOfComposite, name];\r\n";
		matlabCode += "\t\t\tend\r\n";
		matlabCode += "\r\n";
		matlabCode += "\t\tend\r\n";
		matlabCode += "\tend\r\n";
		matlabCode += "\r\n";
		
		// Composite resource names
		matlabCode += "\tCompositeResourceNames = {''};\r\n";
		matlabCode += "\tfor i=1:length(value(C(1,:)))\r\n";
		matlabCode += "\t\tif sum(round(value(C(:,i)))) >= 1\r\n";
		matlabCode += "\t\t\tindices = strfind(" + StandardVariableNames.COMPOSITE_TYPE_LIST + "{i}, '::');\r\n";
		matlabCode += "\t\t\tif length(indices) > 0\r\n";
		matlabCode += "\t\t\t\tname = " + StandardVariableNames.COMPOSITE_TYPE_LIST + "{i}(indices(length(indices))+2:length(" + StandardVariableNames.COMPOSITE_TYPE_LIST + "{i}));\r\n";
		matlabCode += "\t\t\telse\r\n";
		matlabCode += "\t\t\t\tname = " + StandardVariableNames.COMPOSITE_TYPE_LIST + "{i};\r\n";
		matlabCode += "\t\t\tend\r\n";
		matlabCode += "\r\n";
		matlabCode += "\t\t\tCompositeResourceNames = [CompositeResourceNames, name];\r\n";
		matlabCode += "\t\tend\r\n";
		matlabCode += "\tend\r\n";
		matlabCode += "\r\n";
		
		// WP/Function <-> Resource relationships
		matlabCode += "\tdata = value(A);\r\n";
		matlabCode += "\tdata( ~any(data,2), : ) = [];  % Remove rows with 0's\r\n";
		matlabCode += "\tdata( :, ~any(data,1) ) = [];  % Remove columns with 0's\r\n";
		matlabCode += "\r\n";
		
		// Composite allocation matrix (sheet 2)
		matlabCode += "\tdataC = value(C);\r\n";
		matlabCode += "\tdataC( ~any(dataC,2), : ) = [];  % Remove rows with 0's\r\n";
		matlabCode += "\tdataC( :, ~any(dataC,1) ) = [];  % Remove columns with 0's\r\n";
		matlabCode += "\r\n";
		
		// Write data
		matlabCode += "\tif exist(filename, 'file')==2\r\n";
		matlabCode += "\t\tdelete(filename);\r\n";
		matlabCode += "\tend\r\n";
		matlabCode += "\r\n";
		
		// Disable warnings about adding new sheets automatically
		matlabCode += "\twarning off MATLAB:xlswrite:AddSheet;\r\n";
		matlabCode += "\r\n";
		
		// Sheet 1: WP/Function <-> Resource relationships (intended for user)
		matlabCode += "\txlswrite(filename, WPNames);\r\n";
		matlabCode += "\txlswrite(filename, ResourceNames');\r\n";
		matlabCode += "\txlswrite(filename, data, 1, 'B2');\r\n";
		matlabCode += "\r\n";
		
		// Sheet 2: Composite allocation matrix (intended for user)
		matlabCode += "\txlswrite(filename, CompositeResourceNames, 2);\r\n";
		matlabCode += "\txlswrite(filename, ResourceNamesPartsOfComposite', 2);\r\n";
		matlabCode += "\txlswrite(filename, dataC, 2, 'B2');\r\n";
		matlabCode += "\r\n";
		
		return matlabCode;
	}

}
