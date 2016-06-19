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
package edu.gatech.mbse.transformations.sysml2milp.model;

import java.util.ArrayList;
import java.util.Map.Entry;

import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.Element;

import edu.gatech.mbse.transformations.sysml2milp.InternalCorrespondences;
import edu.gatech.mbse.transformations.sysml2milp.MILPVariableType;
import edu.gatech.mbse.transformations.sysml2milp.utils.MILPModel2TextUtils;

/**
 * Class containing functions that parse the internal process model and generate constraints
 * and variables to calculate the throughput, and to ensure adherence to precedence constraints
 * and keep track of timing (e.g., also to calculate productionTime).
 * 
 * @author Sebastian
 * @version 0.1
 */
public class ProcessConstraintGenerator {
	
	/**
	 * Constructor.
	 *
	 */
	public ProcessConstraintGenerator() {
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Generates all timing variables relevant for a particular process.
	 * <P>
	 * Genreates variable declarations for the relative start and end time, as well as
	 * the duration.
	 * 
	 * @param process The process to generate timing variables for.
	 * @param milpGen The MILP code generation object.
	 * @return A list of variable declarations in the target MILP syntax.
	 */
	public ArrayList<String> generateVariables(Process process,
			MILPModel2TextUtils milpGen) {
		ArrayList<String> variables = new ArrayList<String>();
		
		// TODO Need access to a map from call behavior actions to working principles (with their IDs) to
		// generate implies constraints (implies(IA1W1, .._duration >= optime))
		
		// Should be consistent with transformation
		String processStepName = (process.getCorrespondingAction() == null ? "V" + process.getID() : "IA" + InternalCorrespondences.getActInstanceIDMapping().get(process.getCorrespondingAction()) + "W");
		
		// Generate a start and end variable, and a duration variable for each process, the subprocesses, and successors
		variables.add(milpGen.generateVariableDeclaration("start" + processStepName, MILPVariableType.SDPVAR));
		variables.add(milpGen.generateVariableDeclaration("end" + processStepName, MILPVariableType.SDPVAR));
		variables.add(milpGen.generateVariableDeclaration(processStepName + "_Duration", MILPVariableType.SDPVAR));
		
		// Subprocesses
		if (process.getSubProcesses() != null)
			for (Process p : process.getSubProcesses())
				variables.addAll(generateVariables(p, milpGen));
		
		// Successor
		if (process.getSuccessor() != null)
			variables.addAll(generateVariables(process.getSuccessor(), milpGen));
		
		return variables;
	}
	
	/**
	 * Computes and returns a set of timing-related constraints.
	 * <P>
	 * These constraints ensure precedence: e.g., end >= start + duration.
	 * Also generated are constraints relevant to parallel process branches
	 * where the execution time of n parallel branches is determined by the
	 * maximum time any of the n branches takes.
	 * 
	 * @param process The process to generate these constraints for
	 * @return An ordered list of constraints.
	 */
	public ArrayList<String> generateTimingConstraints(Process process) {
		ArrayList<String> constraints = new ArrayList<String>();
		
		// TODO Need access to a map from call behavior actions to working principles (with their IDs) to
		// generate implies constraints (implies(IA1W1, .._duration >= optime))
		
		// Should be consistent with transformation
		String processStepName = (process.getCorrespondingAction() == null ? "V" + process.getID() : "IA" + InternalCorrespondences.getActInstanceIDMapping().get(process.getCorrespondingAction()) + "W");
		
		// Generate a start and end variable, and a duration variable for each process, the subprocesses, and successors
		String startVariable = "start" + processStepName;
		String endVariable = "end" + processStepName;
		String durationVariable = processStepName + "_Duration";
		
		// Check if top level - if so, generate starting and ending constraint
		if (process.getParentProcess() == null) {
			// FIXME With this way of storing a process, there may be an opportunity for modeling this more nicely
			constraints.add(startVariable + " >= 0");
			constraints.add("Duration >= " + endVariable);
		}
		
		// Generally applicable for all process steps
		constraints.add(endVariable + " >= " + startVariable + " + " + durationVariable);		// Here, duration is duration of overall process!
		
		// Subprocesses
		if (process.getSubProcesses() != null) {
			// Need to build a string that represents total duration of this process
			String maxDurationConstraint = "";
			
			for (Process p : process.getSubProcesses()) {
				if (!maxDurationConstraint.equals(""))
					maxDurationConstraint += ", ";

				Process nextInChain = p;
				int count = 0;
				
				// Sum of duration of process in parallel branch
				while (nextInChain != null) {
					String subProcessStepName = (nextInChain.getCorrespondingAction() == null ? "V" + nextInChain.getID() : "IA" + InternalCorrespondences.getActInstanceIDMapping().get(nextInChain.getCorrespondingAction()) + "W");
					String subDurationVariable = subProcessStepName + "_Duration";
					
					if (count > 0)
						maxDurationConstraint += " + ";
					
					maxDurationConstraint += subDurationVariable;
					
					nextInChain = nextInChain.getSuccessor();
					count++;
				}
				
				constraints.addAll(generateTimingConstraints(p));
			}
			
			// "Outer" virtual process must last at least as long as longest chain of "inner" processes / parallel branch
			maxDurationConstraint = durationVariable + " >= max([" + maxDurationConstraint + "])";
			constraints.add(maxDurationConstraint);
		} else {	// Atomic step
			// Duration depends on working principle
			int wpIndex = 1;
			String currentActWPID = processStepName + wpIndex++;
			
			// Loop until no more working principles registered / known
			while (InternalCorrespondences.getActivityInstanceWPIDIndex().get(currentActWPID) != null) {
				// Instead of "implies", use equality?
				// Maybe: OR(NOT(IAxWy), IAxWy && Constraint)
				constraints.add("implies(" + currentActWPID + ", " + durationVariable + " >= " + currentActWPID + "_operationTime)");
				
				currentActWPID = processStepName + wpIndex++;
			}
		}
		
		// Successor
		if (process.getSuccessor() != null) {
			String successorProcessStepName = (process.getSuccessor().getCorrespondingAction() == null ? "V" + process.getSuccessor().getID() : "IA" + InternalCorrespondences.getActInstanceIDMapping().get(process.getSuccessor().getCorrespondingAction()) + "W");
			String successorStartVariable = "start" + successorProcessStepName;
			
			// Precedence constraint
			constraints.add(successorStartVariable + " >= " + endVariable);
			
			constraints.addAll(generateTimingConstraints(process.getSuccessor()));
		}
		
		return constraints;
	}
	
	/**
	 * Generates constraints that disallow the sharing of resources across parallel activities.
	 * 
	 * @param process The process to generate these constraints for
	 * @return A list of constraints.
	 */
	public ArrayList<String> generateResourceSharingConstraints(Process process) {
		ArrayList<String> constraints = new ArrayList<String>();
		
		// Access parallel branches
		if (process.getSubProcesses() != null) {
			ArrayList<ArrayList<Process>> processesInParallel = new ArrayList<ArrayList<Process>>();
			
			// For atomic steps, add constraint
			for (Process sp : process.getSubProcesses()) {
				// Collect the processes for each branch
				processesInParallel.add(sp.collectAllNonVirtualSubProcesses());
			}
			
			// Only makes sense if number of parallel branches > 1
			if (processesInParallel.size() > 1) {
				// Now, for each process in each branch, there cannot be overlap in terms of resources used
				for (int i = 0; i < processesInParallel.size(); i++) {
					ArrayList<Process> branch1 = processesInParallel.get(i);
					
					for (int j = i + 1; j < processesInParallel.size(); j++) {
						ArrayList<Process> branch2 = processesInParallel.get(j);
						
						for (int x = 0; x < branch1.size(); x++) {
							Process step1 = branch1.get(x);
							String step1ConstraintPart = "";
							
							for (Entry<Entry<ActivityNode,Element>,Integer> e : InternalCorrespondences.getActivityNodeWPIndex().entrySet()) {
								Entry<ActivityNode,Element> key = e.getKey();
								int index = e.getValue();
								
								if (key.getKey() == step1.getCorrespondingAction()) {
									if (!step1ConstraintPart.equals(""))
										step1ConstraintPart += " + ";
									
									// Note that this column will be filled with zeros if the activity is not realized by the
									// particular working principle in the solution
									step1ConstraintPart += "A(:," + index + ")";
								}
							}
							
							for (int y = 0; y < branch2.size(); y++) {
								// Generate constraint here
								Process step2 = branch2.get(y);
								String step2ConstraintPart = "";
								
								for (Entry<Entry<ActivityNode,Element>,Integer> e : InternalCorrespondences.getActivityNodeWPIndex().entrySet()) {
									Entry<ActivityNode,Element> key = e.getKey();
									int index = e.getValue();
									
									if (key.getKey() == step2.getCorrespondingAction()) {
										if (!step2ConstraintPart.equals(""))
											step2ConstraintPart += " + ";
										
										// Note that this column will be filled with zeros if the activity is not realized by the
										// particular working principle in the solution
										step2ConstraintPart += "A(:," + index + ")";
									}
								}
								
								// Have all the parts we need - add it. Verbally, this constraint ensures that no resource from one branch
								// is allocated to an activity of another branch that is executed in parallel
								if (step1ConstraintPart.equals(step2ConstraintPart))	// Avoid duplicates - this can happen if we have multiple entry points
									constraints.add(step1ConstraintPart + " <= 1");		// into an activity
								else
									constraints.add(step1ConstraintPart + " + " + step2ConstraintPart + " <= 1");
							}
						}
					}
				}
			}
			
			// Dig deeper into branches - this will prevent sharing at lower levels
			for (Process sp : process.getSubProcesses()) {
				// Collect the processes for each branch
				Process current = sp;
				
				while (current != null) {
					if (current.isVirtual())
						constraints.addAll(generateResourceSharingConstraints(current));
						
					current = current.getSuccessor();
				}
			}
		}
		
		return constraints;
	}
	
	/**
	 * Generate variables for utilization constraints.
	 * <P>
	 * For each resource instance, these variables are of the following form:<BR />
	 * ResourceInstanceID_busyTime = A(resInstRow,wp1)*wp1_opTime + ... for all
	 * working principles.<BR />
	 * <BR /
	 * Note that this function also adds variable definitions for MaxBusyTime (i.e.,
	 * the maximum busy time of any resource instance) and a definition for the
	 * internal throughput variable TH.
	 * 
	 * @param resources A list of concrete resources
	 * @param milpGen The MILP code generator.
	 * @return A list of variable definitions holding the busy time of each 
	 */
	public ArrayList<String> generateUtilizationVariables(ArrayList<Element> resources,
			MILPModel2TextUtils milpGen) {
		ArrayList<String> variables = new ArrayList<String>();
		int machineIndex = 0;
		String maxExpr = "";
		
		// Create variables
		for (Element resource : resources) {
			int resID = InternalCorrespondences.getResourceTypeMachineIDMapping().get(resource);
			String varName = "M" + resID;
			
			for (int instanceID : InternalCorrespondences.getResourceTypeInstanceIDMapping().get(resource)) {
				if (!maxExpr.equals(""))
					maxExpr += ", ";
				
				String value = "";
				
				machineIndex++;
				
				// Build expression for value
				for (Entry<String,Entry<ActivityNode, Element>> actWPID : InternalCorrespondences.getActivityNodeWPIDToElementsMapping().entrySet()) {
					if (!value.equals(""))
						value += " + ";
					
					int actWPIndex = InternalCorrespondences.getActivityNodeWPIndex().get(actWPID.getValue());
					
					value += "A(" + machineIndex + "," + actWPIndex + ")*" + actWPID.getKey() + "_operationTime";
				}
				
				maxExpr += "I" + instanceID + varName + "_busyTime";
				
				variables.add(milpGen.generateVariableDeclaration("I" + instanceID + varName + "_busyTime", value, "Busy time of resource"));
			}
		}
		
		if (!maxExpr.equals("")) {
			variables.add(milpGen.generateVariableDeclaration("MaxBusyTime", "max([" + maxExpr + "])", "Maximum busy time for a resource (determines throughput)"));
			variables.add(milpGen.generateVariableDeclaration("TH", "1 / MaxBusyTime", "Maximum sustainable throughput - determined by most busy resource"));
		}
		
		return variables;
	}
	
	/**
	 * Generate variables for utilization constraints.
	 * 
	 * @param milpGen The MILP code generator object.
	 * @return Declaration of throughput variable TH.
	 * @deprecated Should not be used since result is computationally inefficient.
	 */
	public ArrayList<String> generateUtilizationVariablesWithThroughputAsDesignVariable(
			MILPModel2TextUtils milpGen) {
		ArrayList<String> variables = new ArrayList<String>();
		
		variables.add(milpGen.generateVariableDeclaration("TH", MILPVariableType.SDPVAR, "Throughput"));
		
		return variables;
	}
	
	/**
	 * Generates an ordered list of constraints for calculating the throughput (note: in
	 * developer documentation this is referred to as "Option 2").
	 * 
	 * @param resources A set of relevant concrete resources.
	 * @return A list of constraints.
	 * @deprecated Should not be used since result is computationally inefficient.
	 */
	public ArrayList<String> generateUtilizationConstraintsWithThroughputAsDesignVariable(
			ArrayList<Element> resources) {
		int machineIndex = 0;
		short[] startingPoint = new short[InternalCorrespondences.getActivityInstanceWPIDIndex().size()];
		ArrayList<String> constraints = new ArrayList<String>((int) Math.pow(2, startingPoint.length)*resources.size());
		
		// Pre-compute possible ActWP combinations (represented as binary numbers)
		ArrayList<short[]> allCombinations = computeAllCombinations(startingPoint);
		ArrayList<String> actWPIDs = new ArrayList<String>(InternalCorrespondences.getActivityInstanceWPIDIndex().keySet());

		// Bounds on throughput
		constraints.add("0 <= TH <= 1");
		
		// Generate all possible combinations of working principles
		for (Element resource : resources) {
			for (int instanceID : InternalCorrespondences.getResourceTypeInstanceIDMapping().get(resource)) {
				machineIndex++;
				
				for (short[] currentCombination : allCombinations) {
					String currentString = "";
					String impliedConstraint = "";
					int counter = 0;
					
					for (String currentActWPID : actWPIDs) {
						int actWPIndex = InternalCorrespondences.getActivityNodeWPIndex().get(InternalCorrespondences.getActivityNodeWPIDToElementsMapping().get(currentActWPID));
						
						if (!currentString.equals(""))
							currentString += " & ";
						
						if (currentCombination[counter] == 0)
							currentString += "~";
						else {
							if (!impliedConstraint.equals(""))
								impliedConstraint += " + ";
								
							impliedConstraint += "TH*" + currentActWPID + "_operationTime";
						}
						
						currentString += "A(" + machineIndex + "," + actWPIndex + ")";	//currentActWPID;
							
						counter++;
					}
					
					if (!impliedConstraint.equals("")) {
						constraints.add("implies(" + currentString + ", 1 >= " + impliedConstraint + ")");
					}
				}
			}
		}
		
		return constraints;
	}
	
	/**
	 * Computes the permutation of all binary strings computable by adding one
	 * from the given starting point.
	 * 
	 * @param startingPoint Some binary number represented by an array acting as the
	 * 		starting point.
	 * @return A list of binary numbers represented by arrays of shorts that represent all
	 * 		possible combinations of 1s and 0s, starting from the starting point,
	 * 		incrementally adding "1" in each iteration.
	 */
	private ArrayList<short[]> computeAllCombinations(short[] startingPoint) {
		ArrayList<short[]> allCombinations = new ArrayList<short[]>((int) Math.pow(2, startingPoint.length));
		short[] nextEntry = startingPoint.clone();
		
		allCombinations.add(nextEntry);
		
		while ((nextEntry = addOne(nextEntry)) != null)
			allCombinations.add(nextEntry);
		
		return allCombinations;
	}
	
	/**
	 * Adds one to an array representing a binary number.
	 * 
	 * @param startingPoint The binary number represented by an array of shorts to
	 * 		add "1" to.
	 * @return null if next entry is 0 (i.e., reached end / overflow), otherwise the
	 * 		next number in the same format as the input.
	 */
	private short[] addOne(short[] startingPoint) {
		short[] nextEntry = startingPoint.clone();
		
		// Add one to the binary array
		int currentPos = nextEntry.length - 1;
		
		while (currentPos > -1) {
			nextEntry[currentPos]++;
			
			// Carry over to next "bit"?
			if (nextEntry[currentPos] > 1) {
				nextEntry[currentPos] = 0;
				currentPos--;
			}
			else {
				break;
			}
		}
		
		if (sum(nextEntry) == 0)
			return null;
		
		return nextEntry;
	}
	
	/**
	 * Calculates sum of an array of shorts.
	 * 
	 * @param numbers The numbers to sum up
	 * @return The sum of the numbers in the input array.
	 */
	private int sum(short[] numbers) {
		int sum = 0;
		
		for (short n : numbers)
			sum += n;
		
		return sum;
	}

}
