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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.ActivityParameterNode;
import org.eclipse.uml2.uml.DecisionNode;
import org.eclipse.uml2.uml.ForkNode;
import org.eclipse.uml2.uml.InputPin;
import org.eclipse.uml2.uml.JoinNode;
import org.eclipse.uml2.uml.MergeNode;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.ObjectFlow;
import org.eclipse.uml2.uml.OutputPin;
import org.eclipse.uml2.uml.ParameterDirectionKind;
import org.eclipse.uml2.uml.Pin;

import edu.gatech.mbse.transformations.sysml2milp.TransformationCache;

/**
 * Generates a {@link Process} from an Eclipse UML2 Activity.
 * <P>
 * Note that the input process should be: <BR />
 * 1) Acyclic <BR />
 * 2) Be governed by object flows (current limitation)
 * 
 * @author Sebastian
 * @version 0.1.0
 */
public class ProcessFlattener {
	
	private static final Logger logger = LogManager.getLogger(ProcessFlattener.class.getName());

	/**
	 * Generates a process form an Eclipse UML2 Activity as the starting
	 * point.
	 * 
	 * @param startingPoint The starting activity.
	 * @return An internal process representation.
	 */
	public static Process toProcess(Activity startingPoint) {
		// FIXME Ensure virtual process step for each parallel branch
		
		Process topLevelProcess = new Process();
		topLevelProcess.globalID = 1;
		
		ArrayList<ActivityNode> nextNodes = new ArrayList<ActivityNode>();
		
		// Find starting point - this can be one of two things: an IN parameter, or a
		// start node
		for (ActivityNode n : startingPoint.getOwnedNodes()) {
			// FIXME Currently assumes that a workpiece flows through activity
			//		 This can be fixed by defining some top level "flow element"?
			//		 But what if this is purely informational? May want to somehow
			//		 pass to function what kind of element we are looking for.
			// TODO Even necessary to search for workpiece? Not sufficient to search for
			//      any input? (done for now)
			// TODO Also search for standard starting points and follow control flow.
			// TODO Blocked activities!!!!
			if (n instanceof ActivityParameterNode) {
				ActivityParameterNode p = (ActivityParameterNode) n;
				
				if (p.getParameter().getDirection() == ParameterDirectionKind.IN_LITERAL
						|| p.getParameter().getDirection() == ParameterDirectionKind.INOUT_LITERAL
						/*&& TransformationHelper.isWorkpiece(p.getType())*/) {
					nextNodes = findNextActions(p);
					
					// Starting point(s)
					for (ActivityNode node : nextNodes) {
						// FIXME Should probably surround by a virtual process if multiple starting points
						
						// Create corresponding process
						Process subProcess = new Process();
						subProcess.setCorrespondingAction(node);
						
						// Add as sub process
						topLevelProcess.addSubProcess(subProcess);
						subProcess.setParentProcess(topLevelProcess);
						
						ActivityNode nextNode = handleProcessBranch(subProcess);
						
						while (nextNode != null) {
							nextNode = handleProcessBranch(subProcess);
						}
					}
				}
			}
		}
		
		return topLevelProcess;
	}
	
	/**
	 * Add a process step in sequence, or parallel "sub"-processes. Returns an object
	 * representing a point of joining / merging if it has been reached.
	 * 
	 * @param processStep The particular process step to handle.
	 * @return The next action or a join or fork node.
	 */
	private static ActivityNode handleProcessBranch(Process processStep) {
		ActivityNode toReturn = null;
		
		// Recursively add subprocesses, and add successors
		ArrayList<ActivityNode> nextN = findNextActions(processStep.getCorrespondingAction());
		
		// If there is only one next node - no parallelization
		if (nextN.size() == 1) {
			// FIXME Since used in a couple of places, change to isMergePoint(ActivityNode)
			if (!(nextN.get(0) instanceof JoinNode)) {
				// Create a new process step as the successor
				Process successor = new Process();
				successor.setCorrespondingAction(nextN.get(0));
				
				// Add this new process step as successor
				processStep.setSuccessor(successor);
				successor.setPredecessor(processStep);
				successor.setParentProcess(processStep.getParentProcess());
				
				handleProcessBranch(successor);
			}
			else
				return nextN.get(0);
		}
		else if (nextN.size() > 1) {
			// Create a virtual process step, which contains multiple parallel tracks
			Process virtual = new Process();
			processStep.setSuccessor(virtual);
			virtual.setPredecessor(processStep);
			virtual.setParentProcess(processStep.getParentProcess());
			
			ArrayList<ActivityNode> endPoints = new ArrayList<ActivityNode>();
			
			// Parallel processes: those process steps following the current one
			for (ActivityNode n : nextN) {
				// For each parallel process, create a chain of processes that are sub processes
				// of the virtual one
				Process subProcess = new Process();
				subProcess.setCorrespondingAction(n);
				
				// Add the chain of processes as a virtual sub-process
				virtual.addSubProcess(subProcess);
				subProcess.setParentProcess(virtual);
				
				// Detail the sub-process
				ActivityNode endPoint = handleProcessBranch(subProcess);
				
				if (endPoint != null
						&& !endPoints.contains(endPoint))
					endPoints.add(endPoint);
			}
			
			for (ActivityNode endPoint : endPoints) {
				virtual.setCorrespondingAction(endPoint);
				toReturn = handleProcessBranch(virtual);
				while (toReturn != null) {
					toReturn = handleProcessBranch(virtual);
				}
				virtual.setCorrespondingAction(null);
			}
		}
		
		return null;
	}
	
	/**
	 * Find the next activity / activities to be executed based on a starting
	 * activity node.
	 * 
	 * @param sourceNode The source node to start from.
	 * @return The next action, determined by following object flows.
	 */
	private static ArrayList<ActivityNode> findNextActions(NamedElement sourceNode) {
		ArrayList<ActivityNode> targets = new ArrayList<ActivityNode>();
		
		//if (sourceNode instanceof JoinNode) {
			// Can have only one outgoing node
		//	return findNextActions(((JoinNode) sourceNode).getOutgoings().get(0).getTarget());
		//}
		
		// TODO If the sourceNode is an Action then parse the output / inout pins
		// FIXME Only takes into account output pins
		if (sourceNode instanceof Action) {
			// Extract pins with output
			for (OutputPin pin : ((Action) sourceNode).getOutputs()) {
				// Call findNextActions and add to targets array
				targets.addAll(findNextActions((NamedElement) pin));
			}
		}
		else {
			// Collect object flows that have sourceNode as source and return all targets
			// FIXME Assumes object flow only
			for (ObjectFlow flow : TransformationCache.getObjectFlowList()) {
				if (flow.getSource() == sourceNode && flow.getTarget() != sourceNode && flow.getTarget() instanceof ActivityNode) {
					// FIXME decision nodes not supported
					// Skip any merge, fork, decision or joins
					if (flow.getTarget() instanceof ForkNode
							/*|| hasMultipleOutflows(flow.getTarget())*/) {
						targets.addAll(findNextActions(flow.getTarget()));
					}
					else if (flow.getTarget() instanceof JoinNode) {
						targets.add(flow.getTarget());
					}
					else if (flow.getTarget() instanceof MergeNode
							|| flow.getTarget() instanceof DecisionNode) {
						logger.warn("Found a merge or decision node - this is potentially problematic and may lead to undesired reuslts.");
					}
					else {
						// In all likelihood, these will be pins
						if (flow.getTarget() instanceof Pin) {
							// Call behavior action owns pin
							targets.add((ActivityNode) flow.getTarget().getOwner());
						}
						else if (flow.getTarget() instanceof Action) {
							targets.add(flow.getTarget());
						}
					}
				}
			}
		}
		
		return targets;
	}
	
	/**
	 * Searches through the given process hierarchy to find a particular
	 * process step that is associated with a particular activity node.
	 * <P>
	 * The function is used to search for elements that have already been
	 * visited in generating the flat hierarchy from the Activity diagram
	 * so that infinite loops when parsing can be avoided.
	 * 
	 * @param actNode The activity node that the needle in the process haystack is
	 * 		associated with
	 * @param process The top level process to search from
	 * @return null if no process found, the process that is associated with the given
	 * 		activity node otherwise
	 */
	public static Process findSubProcessWithCorrespondingAction(ActivityNode actNode,
			Process process) {
		Process currentProcess = process;
		
		if (actNode instanceof Pin)
			actNode = (ActivityNode) actNode.getOwner();
		
		while (currentProcess != null) {
			// Check if virtual, and if so, dig deeper down
			if (currentProcess.getCorrespondingAction() == null && currentProcess.getSubProcesses() != null) {
				for (Process subProcess : currentProcess.getSubProcesses()) {
					while (currentProcess != null) {
						Process toFind = findSubProcessWithCorrespondingAction(actNode, subProcess);
						
						if (toFind != null)
							return toFind;
						
						currentProcess = currentProcess.getSuccessor();
					}
				}
			}
			else {
				if (currentProcess.getCorrespondingAction() == actNode)
					return currentProcess;
			}
			
			currentProcess = process.getSuccessor();
		}
		
		return null;
	}
	
	/**
	 * Returns true if the given node has multiple input parameters. This is used
	 * in determining merge points.
	 * 
	 * @param node The node to check.
	 * @return true if the ActivityNode has multiple inflows.
	 */
	public static boolean hasMultipleObjectInflows(ActivityNode node) {
		if (!(node instanceof Action))
			return false;
		
		if (((Action) node).getInputs() == null)
			return false;
		
		ArrayList<ActivityNode> sources = new ArrayList<ActivityNode>();
		
		int numUniqueInflows = 0;
		
		for (InputPin in : ((Action) node).getInputs()) {
			// Check source - if not same, then has multiple (from different actions)
			for (ObjectFlow flow : TransformationCache.getObjectFlowList()) {
				if (flow.getTarget() == in && flow.getSource() != in) {
					if (!sources.contains(flow.getSource()))
						numUniqueInflows++;
					
					if (numUniqueInflows > 1)
						return true;
				}
			}
		}
		
		if (numUniqueInflows > 1)
			return true;
		
		return false;
	}
	
	/**
	 * Returns true if the given node has multiple output parameters.
	 * 
	 * @param node The ActivityNode to check.
	 * @return true if the specified node has multiple outflows.
	 */
	public static boolean hasMultipleObjectOutflows(ActivityNode node) {
		if (!(node instanceof Action))
			return false;
		
		if (((Action) node).getOutputs() == null)
			return false;
		
		ArrayList<ActivityNode> targets = new ArrayList<ActivityNode>();
		
		int numUniqueOutflows = 0;
		
		for (OutputPin out : ((Action) node).getOutputs()) {
			// Check source - if not same, then has multiple (from different actions)
			for (ObjectFlow flow : TransformationCache.getObjectFlowList()) {
				if (flow.getSource() == out && flow.getTarget() != out) {
					if (!targets.contains(flow.getTarget()))
						numUniqueOutflows++;
					
					if (numUniqueOutflows > 1)
						return true;
				}
			}
		}
		
		if (numUniqueOutflows > 1)
			return true;
		
		return false;
	}
	
}
