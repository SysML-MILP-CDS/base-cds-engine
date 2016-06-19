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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Property;

import edu.gatech.mbse.transformations.sysml2milp.utils.DSEMLUtils;
import edu.gatech.mbse.transformations.sysml2milp.utils.MILPModel2TextUtils;

/**
 * This class contains a number of helper functions meant to outlie some complex
 * traversal algorithms and keep them separate from the actual mappings performed.
 * These functions include functions that compute and generate certain constraints
 * (e.g., related to the semantics of shared and composite aggregation).
 * 
 * @author Sebastian
 * @version 0.1
 */
public class SysML2MILPMappingsHelper {
	
	/** Log4J object. */
	private static final Logger logger = LogManager.getLogger(SysML2MILPMappingsHelper.class.getName());
	
	/**
	 * Declare standard variables used throughout the MILP formulation.
	 * <P>
	 * Note that a precondition for this function is that the internal correspondences and
	 * transformation cache should already be built up - otherwise this will result in
	 * matrices of size 0.
	 * 
	 * @param milpGen The MILP code generation object.
	 * @return MILP code declaring and / or defining standard variables. These are
	 * 		defined / declared at the very beginning of a target MILP script since they
	 * 		are referenced throughout.
	 */
	public static String declareStandardVariables(MILPModel2TextUtils milpGen) {
		String milpCode = milpGen.generateLineSeparator();
		
		// Allocation matrix - use current counts since should have reached end already
		milpCode += "A = binvar(" + getTotalNumberOfResourceInstances() + ", " + InternalCorrespondences.getActivityNodeWPIndex().size() + ");\r\n";
		
		// Also assign default values
		milpCode += "assign(A, 0);\r\n";
		
		// 'C' matrix (currently unused)
		milpCode += "C = binvar(" + getTotalNumberOfResourceInstances() + ", " + getTotalNumberOfCompositeResourceInstances() + ");\r\n";
		
		// Also assign default values
		milpCode += "assign(C, 0);\r\n";
		
		// Duration
		milpCode += milpGen.generateVariableDeclaration(StandardVariableNames.TOTAL_DURATION, MILPVariableType.SDPVAR, "Duration of complete process (time spent working on input element from start to finish)");
		
		// Constraints
		milpCode += milpGen.generateVariableDeclaration(StandardVariableNames.CONSTRAINTS, "[binary(A), binary(C)]", "Vector of linear constraints");
		
		// Machine instance vector
		milpCode += milpGen.generateVariableDeclaration(StandardVariableNames.INSTANCE_VECTOR, "[]", "Vector of all resource instances (or, rather, binary variables indicating their existence)");
		
		// Type name list
		milpCode += milpGen.generateVariableDeclaration(StandardVariableNames.TYPE_LIST, "{}", "(Ordered) list of qualified names of types of resource instances (used for correspondence and visualization)");
		
		// Type name list (composite resources)
		milpCode += milpGen.generateVariableDeclaration(StandardVariableNames.COMPOSITE_TYPE_LIST, "{}", "(Ordered) list of qualified names of types of composite resource instances (used for correspondence and visualization)");
		
		// Working principle type name list
		milpCode += milpGen.generateVariableDeclaration(StandardVariableNames.WP_TYPE_LIST, "{}", "(Ordered) list of qualified names of types of working principle (used for correspondence and visualization)");
				
		// Function name list
		milpCode += milpGen.generateVariableDeclaration(StandardVariableNames.FUNCTION_NAMES_LIST, "{}", "(Ordered) list of names of functions (used for correspondence and visualization)");
		
		return milpCode;
	}
	
	/**
	 * Returns the number of instances that are created for a particular resource type.
	 * <P>
	 * Note that this function simply accesses one of the maps created when performing
	 * pre-computations in {@link InternalCorrespondences}. Therefore, the computation
	 * of these internal correspondences should be a pre-condition to calling this
	 * method.
	 * 
	 * @param e The resource type.
	 * @return The maximum number of instances that can be created given the functional
	 * 		specification being considered.
	 */
	public static int getNumInstancesForResource(Element e) {
		// Linked hash maps are ordered, so calculate index based on that
		for (Entry<Element, ArrayList<Integer>> entry : InternalCorrespondences.getResourceTypeInstanceIDMapping().entrySet())
			if (entry.getKey() == e)
				return entry.getValue().size();
		
		return -1;
	}
	
	/**
	 * Returns the total number of resource instances created.
	 * 
	 * @return The total number of instances for all resource types.
	 */
	public static int getTotalNumberOfResourceInstances() {
		int totalInstances = 0;
		
		// Linked hash maps are ordered, so calculate index based on that
		for (Entry<Element, ArrayList<Integer>> entry : InternalCorrespondences.getResourceTypeInstanceIDMapping().entrySet())
			totalInstances += entry.getValue().size();
		
		return totalInstances;
	}
	
	/**
	 * Returns the total number of composite resource instances created.
	 * 
	 * @return The total number of instances for all aggregate resource types.
	 */
	public static int getTotalNumberOfCompositeResourceInstances() {
		int totalInstances = 0;
		
		// Linked hash maps are ordered, so calculate index based on that
		for (Entry<Element, ArrayList<Integer>> entry : InternalCorrespondences.getResourceTypeInstanceIDMapping().entrySet())
			if (DSEMLUtils.isCompositeResource(entry.getKey()))
				totalInstances += entry.getValue().size();
		
		return totalInstances;
	}
	
	/**
	 * Compute constraints related to ensuring that a sufficient number of instances
	 * of types of resources that are defined to be parts of aggregate / composite
	 * resource instances are part of the solution.
	 * <P>
	 * The generated constraints take the form of sum(Part1Instances) + sum(Part2Instances)
	 * + ... >= sum(AggregateInstances). Note that this assumes a multiplicity of 1 for all
	 * parts (current limitation). ">=" is used for relaxation and more efficient
	 * computation by the solver. Note that this relaxation <em>may</em> lead to non-sensical
	 * results when multiple solutions are created.
	 * 
	 * @param resource The resource type to create the appropriate constraints for.
	 * @return An ordered list of constraints (as String objects).
	 */
	public static ArrayList<String> addCompositeResourceIntegrityConstraints(Classifier resource) {
		if (!DSEMLUtils.isCompositeResource(resource))
			return new ArrayList<String>();
		
		ArrayList<String> constraints = new ArrayList<String>();
		
		// Add additional constraints for composite resources
		ArrayList<Element> composites = DSEMLUtils.getResourceParts(resource);
		
		for (Element composite : composites) {
			String concrSum = "";
			
			ArrayList<Element> concrCompositeParts = DSEMLUtils.getConcreteResourcesForAbstractResourceType(
					TransformationCache.getResources(),
					composite);
			
			for (Element cc : concrCompositeParts) {
				int rID = InternalCorrespondences.getResourceTypeMachineIDMapping().get(cc);
				
				if (!concrSum.equals(""))
					concrSum += " + ";
				concrSum += "sum(IM" + rID + ")";
			}
			
			if (!concrSum.equals(""))
				constraints.add(concrSum + " >= sum(IM" + InternalCorrespondences.getResourceTypeMachineIDMapping().get(resource) + ")");
		}
		
		return constraints;
	}
	
	/**
	 * Compute a set of constraints that ensure that the correct resource instances
	 * are allocated to the correct aggregate resources.
	 * <P>
	 * This method computes three types of constraints:<BR />
	 * 1) If any 1s appear in a row in matrix A (i.e., the particular resource instance
	 * is allocated to some function), then a 1 must appear in the cell defined by the
	 * row and column of the particular aggregate resource (i.e., the instance is
	 * allocated to itself in matrix C).<BR />
	 * 2) If a resource instance is allocated to itself in C (i.e., constraint 1) holds
	 * then "compatible" parts allocated in A must also be allocated in C.<BR />
	 * 3) The difference between the values in each column in A for the column of the
	 * aggregate resource instance must be greater than 0 (otherwise a resource instance
	 * is allocated in C that is not allocated in A)
	 * 
	 * @param resource The composite / aggregate resource to generate the constraints for.
	 * @param instance The instance number of the aggregate resource.
	 * @return An ordered list of constraints (as Strings).
	 */
	public static ArrayList<String> addCompositeResourceAllocationConstraints(
			Classifier resource,
			int instance) {
		ArrayList<String> constraints = new ArrayList<String>();
		
		// Every column of the C matrix has to have exactly n '1's, where n = number of parts of composites. Also,
		//constraints.add("sum(C(:," + InternalCorrespondences.getCompositeMatrixCompositeResourceInstanceColumnIndex(resource, instance) + ")) == " + DSEMLUtils.getResourceParts(resource).size());
		
		// The resources where there is a '1' in a row, as well as the corresponding composite in the column must all
		// appear in the same row in A
		//for (int wp=1; wp<InternalCorrespondences.getActInstanceIDMapping().size()+1; wp++)
		//	constraints.add("implies(A(" + InternalCorrespondences.getAllocationMatrixResourceInstanceIndex(resource, instance) + "," + wp + "), sum(round(C(:," + InternalCorrespondences.getCompositeMatrixCompositeResourceInstanceColumnIndex(resource, instance) + ")) & round(A(:," + wp + "))) == " + (DSEMLUtils.getResourceParts(resource).size() + 1) + ")");
		int compositeMatrixColumnID = InternalCorrespondences.getCompositeMatrixCompositeResourceInstanceColumnIndex(resource, instance);
		int allocationMatrixRowID = InternalCorrespondences.getAllocationMatrixResourceInstanceIndex(resource, instance);
		String cMatrixEntry = "C(" + allocationMatrixRowID + "," + compositeMatrixColumnID + ")";
		
		// If the instance is allocated to one of the functions / WPs, then it is also allocated in C()
		String rightHandSide = "";
		for (int wp=1; wp<InternalCorrespondences.getActivityInstanceWPIDIndex().size()+1; wp++) {
			if (rightHandSide.equals(""))
				rightHandSide += "(";
			else
				rightHandSide += " | ";
			
			rightHandSide += "A(" + allocationMatrixRowID + ", " + wp + ")";
		}
		
		if(!rightHandSide.equals(""))
			rightHandSide += ")";
		
		constraints.add(cMatrixEntry + " == " + rightHandSide);

		// If the composite resource is "used" (indicated by an allocation to itself), then parts must also be "1"
		for (Element part : DSEMLUtils.getResourceParts(resource)) {
			rightHandSide = "";
			
			for (Element concretePart : TransformationCache.getResources()) {
				if (DSEMLUtils.isTypeOfResource((Classifier) part, (Classifier) concretePart)) {
					if (!rightHandSide.equals(""))
						rightHandSide += " + ";
					
					int startingIndex = InternalCorrespondences.getAllocationMatrixResourceInstanceIndex(concretePart, 1);
					int endingIndex = startingIndex + InternalCorrespondences.getResourceTypeInstanceIDMapping().get(concretePart).size() - 1;
					
					rightHandSide += "sum(C(" + startingIndex + ":" + endingIndex + "," + compositeMatrixColumnID + "))";
				}
			}
			
			constraints.add("implies(" + cMatrixEntry + ", " + rightHandSide + " >= 1)");
			constraints.add("implies(~" + cMatrixEntry + ", " + rightHandSide + " == 0)");
		}
		
		for (int wp=1; wp<InternalCorrespondences.getActivityInstanceWPIDIndex().size()+1; wp++)
			constraints.add("implies(A(" + allocationMatrixRowID + "," + wp + "), A(:," + wp + ") - C(:," + compositeMatrixColumnID + ") >= 0)");
		
		return constraints;
	}
	
	/**
	 * Adds constraints related to the composite allocation matrix to quickly identify
	 * regions that are always 0.
	 * <P>
	 * Note that this function adds a constraint for instances of a specified resource that
	 * are not part of any considered composite resource.
	 * 
	 * @param resource The resource type that is not a composite / aggregate resource or a
	 * 		part of any aggregate resource.
	 * @param instance The instance being transformed.
	 * @return Returns an ordered list of the relevant constraints (in String form).
	 */
	public static ArrayList<String> addCompositeResourceAllocationConstraintsIgnoreNonCompositeParts(
			Classifier resource) {
		ArrayList<String> constraints = new ArrayList<String>();
		
		int startingIndex = InternalCorrespondences.getAllocationMatrixResourceInstanceIndex(resource, 1);
		int endingIndex = startingIndex + InternalCorrespondences.getResourceTypeInstanceIDMapping().get(resource).size() - 1;
		
		// In the composite allocation matrix C (relationship between composite resources and parts), any
		// resources that cannot possibly be part of any of the considered composites will always be zero.
		// This improves computational efficiency by limiting the space available to the solver.
		constraints.add("C(" + startingIndex + ":" + endingIndex + ",:) == 0");
		
		return constraints;
	}
	
	/**
	 * Adds constraints related to the shareability of resources for <em>all</em>
	 * types of resources and their respective instances relevant to the particular
	 * design problem.
	 * 
	 * @param milpGen The MILP code generation object.
	 * @return MILP code containing constraint definitions.
	 */
	public static String addResourceShareabilityConstraints(MILPModel2TextUtils milpGen) {
		// FIXME This should not return MILP code, but rather a list of constraints
		
		ArrayList<String> constraints = new ArrayList<String>();
		
		ArrayList<Element> nonShareableResources = DSEMLUtils.getNonShareableResources(TransformationCache.getAllWorkingPrinciples(),
				TransformationCache.getCompositeResources(),
				TransformationCache.getAllResources());
		
		ArrayList<Element> shareableResources = DSEMLUtils.getShareableResources(TransformationCache.getAllWorkingPrinciples(),
				TransformationCache.getCompositeResources(),
				TransformationCache.getAllResources());
		
		// Aggregate resource shareability
		for (Element r : nonShareableResources) {
			for (Element resource :  DSEMLUtils.getConcreteResourcesForAbstractResourceType(
					TransformationCache.getResources(), r)) {
				// If this element is part of a composite...
				if (DSEMLUtils.isPartOfAnyComposite(
						(Classifier) resource, 
						TransformationCache.getCompositeResources())) {
					// Then add the constraint that it may not be allocated to more than one composite
					for (int i : InternalCorrespondences.getResourceTypeInstanceIDMapping().get(resource)) {
						int matrixRow = InternalCorrespondences.getAllocationMatrixResourceInstanceIndex(resource, i);
						String constr = "sum(C(" + matrixRow + ",:)) <= 1";
						
						if (!constraints.contains(constr))
							constraints.add(constr);
					}
				}
			}
		}
		
		// Shareable and non-shareable resources
		for (Element r : TransformationCache.getAllResources()) {
			ArrayList<String> shCons = SysML2MILPMappingsHelper.addNonShareabilityConstraints(
					r,
					shareableResources,
					nonShareableResources,
					InternalCorrespondences.getResourceActivityIDsMapping(),
					milpGen);
			
			if (shCons != null && shCons.size() > 0)
				constraints.addAll(shCons);
			
			// Used for deeper nested shareable resources (e.g., if a specific machine type is set as "non shareable", but a abstract
			// class of machine types is set to shareable for another working principle
			if (nonShareableResources.contains(r)) {
				// Find topmost shareable in inheritance graph
				Classifier topMostShareable = SysML2MILPMappingsHelper.findTopMostShareable((Classifier) r, shareableResources);
				
				// Add to list of non-shareables
				if (topMostShareable != null) {
					boolean remove = true;
					if (nonShareableResources.contains(topMostShareable))
						remove = false;
					else
						nonShareableResources.add(topMostShareable);
					
					// Generate a constraint for it
					constraints.addAll(SysML2MILPMappingsHelper.addNonShareabilityConstraints(
							topMostShareable,
							shareableResources,
							nonShareableResources,
							InternalCorrespondences.getResourceActivityIDsMapping(),
							milpGen));
					
					// Remove from list of shareables
					if (remove)
						nonShareableResources.remove(topMostShareable);
				}
			}
		}
		
		return milpGen.generateConstraintDeclaration(constraints, "Resource shareability constraints");
	}
	
	/**
	 * Adds non-shareability constraints for a particular resource.
	 * 
	 * @param r The resource type being considered.
	 * @param shareableResources A list of shareable resources.
	 * @param nonShareableResources A list of non-shareable resources.
	 * @param resourceActivityIDsMapping A mapping from function definitions
	 * 		(activities) to a list of internal identifiers representing working
	 * 		principle / function instances.
	 * @param milpGen The MILP code generation object.
	 * @return An ordere list of constraints (as string objects).
	 */
	public static ArrayList<String> addNonShareabilityConstraints(Element r, 
			ArrayList<Element> shareableResources,
			ArrayList<Element> nonShareableResources,
			HashMap<Element,HashSet<String>> resourceActivityIDsMapping,
			MILPModel2TextUtils milpGen) {
		ArrayList<String> constraints = new ArrayList<String>();
		String constr = generateNonShareabilityActWPCondition(r, shareableResources, nonShareableResources, resourceActivityIDsMapping);
		
		if (!constr.equals("")) {
			if (constr.endsWith(" + "))
				constr = constr.substring(0, constr.lastIndexOf(" + "));
			
			logger.trace(constr + " <= " + ((NamedElement) r).getName());
			
			ArrayList<Element> alreadyProcessed = new ArrayList<Element>();
			ArrayList<Element> res = DSEMLUtils.getConcreteResourcesForAbstractResourceType(
					TransformationCache.getResources(), r);
			String rightHandSide = "";
			
			for (Element e : res) {
				if (!alreadyProcessed.contains(e)) {
					String instanceVectorID = "IM" + InternalCorrespondences.getResourceTypeMachineIDMapping().get(e);
					
					if (!rightHandSide.equals(""))
						rightHandSide += " + ";
					rightHandSide += "sum(" + instanceVectorID + ")";
					
					alreadyProcessed.add(e);
				}
			}
			
			constraints.add(constr + " <= " + rightHandSide);
		}
		
		return constraints;
	}
	
	/**
	 * Creates the left hand side of the non-shareability constraint.
	 * <P>
	 * The left-hand side determines the minimal number of required resource instances
	 * to fulfill the shareability constraints imposed by shared and composite
	 * aggregations. It is computed through binary logic and arithmetic, ORing (with "|")
	 * those instances where resources can be shared (create 1 instance at least if any
	 * of the binary variables are true) and adding where compositions are used (need at
	 * least n additional ones). Also see developer documentation.
	 * 
	 * @param r The element to create the left-hand side of the constraint for.
	 * @param shareableResources A list of shareable resources.
	 * @param nonShareableResources A list of non-shareable resources.
	 * @param resourceActivityIDsMapping A mapping from function definitions
	 * 		(activities) to a list of internal identifiers representing working
	 * 		principle / function instances.
	 * @return The left hand-side of the relevant constraint.
	 */
	public static String generateNonShareabilityActWPCondition(Element r, 
			ArrayList<Element> shareableResources,
			ArrayList<Element> nonShareableResources,
			HashMap<Element,HashSet<String>> resourceActivityIDsMapping) {
		String constr = "";
		
		if (nonShareableResources.contains(r)) {
			if (shareableResources.contains(r)) {
				ArrayList<Element> subResources = getNonShareableSubResources(r, nonShareableResources);
				
				for (Element subRes : subResources) {
					if (!constr.equals(""))
						constr += " + ";
					
					constr += generateNonShareabilityActWPCondition(subRes, shareableResources, nonShareableResources, resourceActivityIDsMapping);
				}
			}
			
			HashSet<HashSet<String>> processed = new HashSet<HashSet<String>>();
			
			for (Entry<Element,HashSet<String>> e : resourceActivityIDsMapping.entrySet()) {
				Element resource = e.getKey();
				//System.out.println(e.getKey() + "=>" + resource.toString());
				if (/*isTypeOfResource((Classifier) r, (Classifier) resource)
							|| */DSEMLUtils.isTypeOfResource((Classifier) resource, (Classifier) r)
							&& !processed.contains(e.getValue())) {
					HashSet<String> actInstanceIDs = e.getValue();
					processed.add(e.getValue());
					
					logger.log(Level.TRACE, ((NamedElement) resource).getName() + " => " + actInstanceIDs.toString());
					
					// Check whether this resource has any composite parents (i.e., is part of a composite)
					// This is the case if, e.g., a type of "Robot" is part of the aggregate "RobotWithGripper"
					// and "RobotWithClinchingHead"
					ArrayList<Element> compositeParents = DSEMLUtils.getCompositeParents(
							resource,
							TransformationCache.getAllResources(),
							TransformationCache.getResources());
					
					// OR those activity+wp combinations, which point to a common, shared
					// parent resource
					//// Do this for each distinct type of parent resource, adding them
					HashSet<HashSet<String>> alreadyProcessed = new HashSet<HashSet<String>>();
					for (Element parent : compositeParents) {
						HashSet<String> relIDs = resourceActivityIDsMapping.get(parent);
						
						if (relIDs != null
								&& relIDs.size() > 0
								&& !alreadyProcessed.contains(relIDs)) {
							if (!constr.equals("") && !constr.endsWith(" + "))
								constr += " + ";
							
							boolean first = true;
							
							if (shareableResources.contains(parent)) {
								constr += "(";
								for (String actWP : relIDs) {
									if (!isNonShareableWithinContext(r, actWP)) {
										if (!first)
											constr += " | ";
										constr += actWP;
										
										first = false;
									}
								}
								constr += ")";
							}
							
							for (String actWPID : relIDs) {
								if (isNonShareableWithinContext(r, actWPID)) {
									if (!first)
										constr += " + ";
									
									constr += actWPID;
									
									first = false;
								}
							}
							
							// The following step is necessary, since we only have a resource -> act+wp
							// mapping. This includes any concrete specializations of something more
							// general -> e.g., instance of a robotwithgripper rather than robotwithgripper
							alreadyProcessed.add(relIDs);
						}
					}
					
					// If this is a top level resource...
					if (compositeParents == null || compositeParents.size() == 0) {
						boolean firstEl = true;
						
						if(!constr.equals("") && !constr.endsWith(" + "))
							constr += " + ";
						//constr = "";
						
						// Check whether there is a shareable instance - in that case add 1 later
						if (shareableResources.contains(r)) {
							HashSet<String> relIDs = resourceActivityIDsMapping.get(r);
							
							if (relIDs != null) {
								constr += "(";
								for (String relID : relIDs) {
									if (!isNonShareableWithinContext(r, relID)) {
										if (!firstEl)
											constr += " | ";
										constr += relID;
										
										firstEl = false;
									}
								}
								constr += ")";
							}						
						}
						
						for (String actWPID : actInstanceIDs) {
							if (isNonShareableWithinContext(r, actWPID)) {
								if (!firstEl)
									constr += " + ";
								
								constr += actWPID;
								
								firstEl = false;
							}
						}
					}
				}
			}
		}
		
		return constr;
	}
	
	/**
	 * Finds the non-shareable resources in the inheritance tree below the specified
	 * element r (below in terms of inheritance).
	 * 
	 * @param r The element to start from.
	 * @param nonShareableResources A list of non-shareable resources.
	 * @return A list of elements from nonShareableResources that are specializations
	 * 		of the given resource or the resource itself.
	 */
	public static ArrayList<Element> getNonShareableSubResources(Element r,
			ArrayList<Element> nonShareableResources) {
		ArrayList<Element> ret = new ArrayList<Element>();
		
		if (r instanceof Classifier) {
			Classifier res = (Classifier) r;
			
			for (Element nonShareableRes : nonShareableResources) {
				if (nonShareableRes != res && DSEMLUtils.isTypeOfResource(res, (Classifier) nonShareableRes)) {
					ret.add(nonShareableRes);
				}
			}
		}
		
		return ret;
	}
	
	/**
	 * Finds "top most" shareable resource in inheritance hierarchy using a simple breadth
	 * first search.
	 * <P>
	 * The inheritance tree is searched breadth first (from specific to general) to find
	 * the deepest nested shareable resource (i.e., most general classifier that is in a
	 * shared aggregation somewhere).
	 * 
	 * @param r The resource to perform the search for.
	 * @param shareableResources A list of shareable resources.
	 * @return The top-most shareable resource.
	 */
	public static Classifier findTopMostShareable(Classifier r,
			ArrayList<Element> shareableResources) {
		Classifier toReturn = null;

		Queue<Classifier> queue = new LinkedList<Classifier>();
		
		if (r.getGenerals() != null)
			queue.add(r);
		
		while (!queue.isEmpty()) {
			Classifier current = queue.remove();
			
			if (shareableResources.contains(current))
				toReturn = current;
			
			if (current.getGenerals() != null)
				queue.addAll(current.getGenerals());
		}
		
		return toReturn;
	}
	
	/**
	 * Check whether a particular resource is shareable within a certain context.
	 * 
	 * @param resource The resource type.
	 * @param actWPInstanceID The particular function / working principle internal ID.
	 * @return true if the given resource is non-shareable within the context of the
	 * 		specified function / working principle combination and false otherwise.
	 */
	public static boolean isNonShareableWithinContext(Element resource, String actWPInstanceID) {
		boolean isNonShareable = false;
		Activity workingPrinciple = (Activity) ((SimpleEntry<Activity, Element>) InternalCorrespondences.getActivityInstanceWPIDIndex().get(actWPInstanceID)).getValue();
		
		if (workingPrinciple != null && workingPrinciple.getOwnedAttributes() != null) {
			for (Property resProp : workingPrinciple.getOwnedAttributes()) {
				if (DSEMLUtils.isResource(resProp.getType())) {
					if (resProp.isComposite() && resProp.getType().equals(resource))
						return true;
					else if (!resProp.getType().equals(resource) 
							&& DSEMLUtils.isCompositeResource(resProp.getType()))
						isNonShareable |= isNonShareableWithinContext(resProp.getType(), actWPInstanceID);
				}
			}
		}
		
		return isNonShareable;
	}
	
	/**
	 * Produce some visual output of a UML classifier by adding it to the visualization
	 * buffer.
	 * <P>
	 * This method will simply go through the list of owned and inherited attributes, check
	 * whether a given property has a numeric value and, if it does, create code for the
	 * target MILP environment that will display the property name and the associated value.
	 * Note: this is currently somewhat MATLAB specific syntax.
	 * 
	 * @param classifier The classifier to produce the output for.
	 * @param milpGen The MILP code generation object.
	 */
	public static void displayPropertiesOfUMLClass(Classifier classifier,
			MILPModel2TextUtils milpGen) {
		// Display attributes only
		if (classifier.getAllAttributes() != null)
			for (Property p : classifier.getAllAttributes())
				if (DSEMLUtils.isValueProperty(p))				// Only numeric attributes
					SysML2MILPTransformation.appendResultsVisualizationBuffer(
							milpGen.generateTextVisualization("'" + p.getName() + ": %d', value(" + classifier.getName() + "_" + p.getName() + ")"));
	}

	/**
	 * Stores internal correspondences for a classifier. Works for resource instances, system under
	 * design and working principles.
	 * <P>
	 * Note that the code being generated for the correspondences is MATLAB specific. In future, this
	 * should be replaced by the creation of a separate correspondence model. Here, we "abuse" the
	 * visualization buffer for this purpose.
	 * <P>
	 * General syntax of the output is: IF a particular possible instance is part of the solution,
	 * write sufficient information for the back transformation into the file postfixed with
	 * _instanceData.
	 * 
	 * @param classifier The classifier to store an internal correspondence for.
	 * @param internalIdentifier The internal identifier of the classifier.
	 */
	public static void storeCorrespondences(Classifier classifier, String identifier) {
		// For correspondence: add to visualization buffer that we would like to write the results to a solution file
		String typeName = classifier.getQualifiedName();
		
		String output = "";
		output += "if value(" + identifier + "); ";
		output += "fprintf(resultsFile, '# Instance of " + typeName + " (" + identifier + ")\\r\\n'); ";
		
		// First line is always type
		output += "fprintf(resultsFile, '" + typeName + "\\r\\n'); ";
		
		// Handle static content
		output += "fprintf(resultsFile, '" + typeName + "::instanceName = " + identifier + "\\r\\n'); ";

		// Handle numeric properties
		if (classifier.getAllAttributes() != null) {
			for (Property p : classifier.getAllAttributes()) {
				if (!DSEMLUtils.isResource(p.getType())
						&& !DSEMLUtils.isWorkingPrinciple(p.getType())) {
					String propertyName = identifier + "_" + p.getName();			// Note: for inherited properties the qualified name may be different - here we care about the instantiated class only
					output += "fprintf(resultsFile, '" + typeName + "::" + p.getName() + " = %d\\r\\n', value(" + propertyName + ")); ";
				}
			}
		}
		
		output += "fprintf(resultsFile, '\\r\\n'); ";

		output += "end;\r\n";
		
		// Add to visualization buffer, which is part of the results loop (once result has been computed)
		SysML2MILPTransformation.appendResultsVisualizationBuffer(output);
	}
	
	/**
	 * Generate Matlab code that deletes an old file and opens a fresh writeable copy
	 * of a file used in storing the instance data.
	 * 
	 * @return A string containing MATLAB specific code to delete any existing instance
	 * 		data output file and then open a new copy.
	 */
	public static String generateInstanceDataOutputFilePreamble() {
		String output = "";
		
		// Intentionally on one line
		output += "resultsFileName = strcat('" + System.getProperty("user.home") + "/milp_solution_', int2str(counter), '_instanceData.smt');";
		output += "if exist(resultsFileName, 'file')==2; delete(resultsFileName); end;\r\n";
		output += "resultsFile = fopen(resultsFileName, 'w');\r\n\r\n";
		
		return output;
	}
	
	/**
	 * Postamble for correspondence data.
	 * 
	 * @return MATLAB specific code that closes the instanceData file.
	 */
	public static String generateInstanceDataOutputFilePostamble() {
		return "fclose(resultsFile);\r\n";
	}

}
