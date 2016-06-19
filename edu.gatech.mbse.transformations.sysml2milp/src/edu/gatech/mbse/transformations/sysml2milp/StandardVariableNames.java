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
package edu.gatech.mbse.transformations.sysml2milp;

/**
 * This class contains a number of "standard" variables that the transformation
 * knows of and specifically generates code for. Examples are the total duration
 * of the activities / functions to be performed, a variable for the throughput,
 * and the objective.
 * <P>
 * Note that these variables may very well be solver / target environment specific.
 * If this is the case, and future versions take more than one solver into account,
 * then this class should become specific to, e.g., YALMIP, or a group of
 * environments.
 * 
 * @author Sebastian
 * @version 0.1
 */
public class StandardVariableNames {

	/** Variable used for defining the objective function. */
	public static final String OBJECTIVE = "Objective";
	
	/** Throughput - e.g., how many items are produced per time unit? */
	public static final String THROUGHPUT = "TH";
	
	/** Total duration - e.g., the total time spent working on a single product. */
	public static final String TOTAL_DURATION = "Duration";
	
	/** Constraints vector. */
	public static final String CONSTRAINTS = "Constraints";
	
	/** Vector of all machine instances (or, rather, indicator variables for their existence). */
	public static final String INSTANCE_VECTOR = "IM";
	
	/** List of qualified names of types of the resource instance - used for back transformation. */
	public static final String TYPE_LIST = "ResourceTypeList";
	
	/** List of qualified names of types of the resource instance - used for back transformation. */
	public static final String COMPOSITE_TYPE_LIST = "CompositeResourceTypeList";
	
	/** List of qualified names of types of the instances of working principles - used for back transformation. */
	public static final String WP_TYPE_LIST = "WPTypeList";
	
	/** List of names of functions. */
	public static final String FUNCTION_NAMES_LIST = "FunctionNameList";

}
