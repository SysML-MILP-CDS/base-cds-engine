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

import org.eclipse.uml2.uml.Action;

/**
 * State variables for transformation.
 * 
 * @author Sebastian
 * @version 0.1
 */
public class TransformationState {

	/** Current index for activity + working principle (in allocation matrix). */
	public static int CURRENT_RESOURCE_INSTANCE_ID = 1;
	
	/** Current index for activity + working principle (in allocation matrix). */
	public static int CURRENT_COMPOSITE_RESOURCE_INSTANCE_ID = 1;
	
	/** Current index for activity + working principle (in allocation matrix). */
	public static int CURRENT_ACTION_WP_COUNT = 0;			// 0 indicates not yet translating process!
	
	/** Holds current call behavior action being processed. */
	public static Action CURRENT_ACTION = null;
	
	/** Current action ID being worked on. */
	public static int CURRENT_ACTION_COUNT = 1;
	
	/** Current working principle ID being worked on. */
	public static int CURRENT_WORKING_PRINCIPLE_COUNT = 1;

	/** Is true, if 'throughput' is identified as necessarily having to be a design variable. */
	public static boolean MUST_OPTIMIZE_THROUGHPUT = false;
	
	/**
	 * Reset state of state variables
	 */
	public static void resetState() {
		CURRENT_RESOURCE_INSTANCE_ID = 1;
		CURRENT_COMPOSITE_RESOURCE_INSTANCE_ID = 1;
		CURRENT_ACTION_WP_COUNT = 0;
		CURRENT_ACTION = null;
		CURRENT_WORKING_PRINCIPLE_COUNT = 1;
		MUST_OPTIMIZE_THROUGHPUT = false;
	}
	
}
