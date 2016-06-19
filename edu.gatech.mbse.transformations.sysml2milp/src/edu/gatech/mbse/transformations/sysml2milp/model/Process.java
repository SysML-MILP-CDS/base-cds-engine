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

import org.eclipse.uml2.uml.ActivityNode;

/**
 * Class describing a process.
 * <P>
 * This structure is used for easier parsing of the activities when
 * generating MILP code that takes into account parallel processes.
 * Processes can contain sub-processes, and can be "virtual". E.g.,
 * when an action branches off into parallel processes, a "virtual"
 * process is generated that contains the two parallel branches as
 * separate, concurrently executed processes. Branches are marked by
 * splits and merges, pins going in different directions, etc. Note
 * that the implementation of the Process model is similar to that
 * of multiple nested linked lists.
 * 
 * @author Sebastian
 * @version 0.0.1
 */
public class Process {

	/** First process steps of sub-processes contained in this process. */
	private ArrayList<Process> subProcesses = null;
	
	/** Parent process - null if top level process. */
	private Process parentProcess = null;
	
	/** Successor. */
	private Process successor = null;
	
	/** Predecessor. */
	private Process predecessor = null;
	
	/** Corresponding Eclipse UML2 Action - this will be null for "virtual" process steps. */
	private ActivityNode correspondingAction = null;
	
	/** Global instance count - used for ID generation. */
	public static int globalID = 1;
	
	/** A process ID, unique to each process step within a larger process. */
	private int iD = -1;
	
	/**
	 * Default constructor
	 */
	public Process() {
		iD = globalID++;
	}

	/**
	 * Returns a list of the contained sub processes. Note that these sub processes
	 * are the first elements of a list of processes. That is, the processes in this
	 * list will have predecessor set to null, but may have a successor that is not
	 * null.
	 * 
	 * @return The sub processes
	 */
	public ArrayList<Process> getSubProcesses() {
		return subProcesses;
	}

	/**
	 * @param subProcesses the subProcesses to set
	 */
	public void setSubProcesses(ArrayList<Process> subProcesses) {
		this.subProcesses = subProcesses;
	}
	
	/**
	 * Convenience function for calling {@link #getSubProcesses()} and
	 * add(...).
	 * 
	 * @param subProcess
	 */
	public void addSubProcess(Process subProcess) {
		if (getSubProcesses() == null)
			setSubProcesses(new ArrayList<Process>());
		
		getSubProcesses().add(subProcess);
	}

	/**
	 * @return the parentProcess
	 */
	public Process getParentProcess() {
		return parentProcess;
	}

	/**
	 * @param parentProcess the parentProcess to set
	 */
	public void setParentProcess(Process parentProcess) {
		this.parentProcess = parentProcess;
	}

	/**
	 * @return the successor
	 */
	public Process getSuccessor() {
		return successor;
	}

	/**
	 * @param successor the successor to set
	 */
	public void setSuccessor(Process successor) {
		this.successor = successor;
	}

	/**
	 * @return the predecessor
	 */
	public Process getPredecessor() {
		return predecessor;
	}

	/**
	 * @param predecessor the predecessor to set
	 */
	public void setPredecessor(Process predecessor) {
		this.predecessor = predecessor;
	}

	/**
	 * @return the correspondingAction
	 */
	public ActivityNode getCorrespondingAction() {
		return correspondingAction;
	}

	/**
	 * @param correspondingAction the correspondingAction to set
	 */
	public void setCorrespondingAction(ActivityNode correspondingAction) {
		this.correspondingAction = correspondingAction;
	}
	
	/**
	 * @return the iD
	 */
	public int getID() {
		return iD;
	}

	/**
	 * Returns whether the process is virtual or not.
	 * <P>
	 * This is determined by whether or not the corresponding UML Action is null
	 * or not.
	 * 
	 * @return true if virtual, false otherwise.
	 */
	public boolean isVirtual() {
		return (getCorrespondingAction() == null);
	}
	
	/**
	 * Collects all non-virtual processes and adds them to a list that is then
	 * returned. In the traversal, sub-processes are first added (and the function
	 * is applied recursively to them) after which the successors are handled.
	 * 
	 * @return A list of non-virtual processes contained in this and any sub-processes.
	 */
	public ArrayList<Process> collectAllNonVirtualSubProcesses() {
		ArrayList<Process> processes = new ArrayList<Process>();
		
		if (getSubProcesses() != null)
			for (Process p : getSubProcesses())
				processes.addAll(p.collectAllNonVirtualSubProcesses());
		else
			processes.add(this);
		
		if (getSuccessor() != null)
			processes.addAll(getSuccessor().collectAllNonVirtualSubProcesses());
		
		return processes;
	}
	
	/**
	 * Turns a process into a string representation.
	 * 
	 * @see java.lang.Object#toString()
	 * @return A string representation of this Process.
	 */
	@Override
	public String toString() {
		String str = "";
		Process p = this;
		int oc = 0;
		
		if (p.getCorrespondingAction() != null)
			str = p.getCorrespondingAction().getName();
		else
			str = "V";
		
		while (p != null) {
			if (oc > 0)
				if (p.getCorrespondingAction() != null)
					str += " -> " + p.getCorrespondingAction().getName();
				else
					str += " -> V";
				
			if (p.getSubProcesses() != null
					&& p.getSubProcesses().size() != 0) {
				str += "[";
				
				int c = 0;
				for (Process sp : p.getSubProcesses()) {
					if (c > 0)
						str += ", ";
					
					str += sp.toString();
					
					c++;
				}
				
				str += "]";
			}
			
			p = p.getSuccessor();
			
			oc++;
		}
		
		return str;
	}
	
}
