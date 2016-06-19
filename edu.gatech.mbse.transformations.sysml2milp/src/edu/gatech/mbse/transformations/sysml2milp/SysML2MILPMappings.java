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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.emf.transaction.RecordingCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.emf.transaction.util.TransactionUtil;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.ValueSpecification;

import edu.gatech.mbse.transformations.sysml2milp.model.Process;
import edu.gatech.mbse.transformations.sysml2milp.model.ProcessConstraintGenerator;
import edu.gatech.mbse.transformations.sysml2milp.model.ProcessFlattener;
import edu.gatech.mbse.transformations.sysml2milp.ocl.MiniOCLInterpretor;
import edu.gatech.mbse.transformations.sysml2milp.utils.DSEMLUtils;
import edu.gatech.mbse.transformations.sysml2milp.utils.MILPModel2TextUtils;
import edu.gatech.mbse.transformations.sysml2milp.utils.UMLModelUtils;

/**
 * Explicit mappings from SysML to MILP.
 * <P>
 * Note that this class should be modified if the full route is taken as described
 * in {@link SysML2MILPTransformation}. To do this, the return type would be a 
 * MILP model object, and no MILPModel2TextUtils class would be included in the
 * parameter list. The methods are static to emphasize their declarative nature,
 * and to serve as a basis for future development.
 * 
 * @author Sebastian
 * @version 0.1
 */
public class SysML2MILPMappings {
	
	/** Log4J object. */
	private static final Logger logger = LogManager.getLogger(SysML2MILPMappings.class.getName());
	
	/**
	 * Map a system under design to MILP.
	 * <P>
	 * Note that this is equivalent to the mapping for any UML class, with the difference
	 * being that visual output is produced also.
	 * 
	 * @param systemUnderDesign The system under design object (a UML class / SysML block).
	 * @param milpGen The MILP code generation object.
	 * @return Representation of system under design in MILP code compatible with the
	 * 		specified target MILP environment.
	 */
	public static String mapSystemUnderDesign(Classifier systemUnderDesign,
			MILPModel2TextUtils milpGen) {
		// Generate some visualization output (this is put into the static visualization
		// output buffer of the main transformation class)
		SysML2MILPMappingsHelper.displayPropertiesOfUMLClass(systemUnderDesign, milpGen);
		
		// Other than that, treat like any other UML class (translate properties and constraints)
		String milpCode = "";
		
		milpCode += mapUMLClassifier(systemUnderDesign, systemUnderDesign.getName(), milpGen);
		
		// System under design always exists
		milpCode += milpGen.generateConstraintDeclaration(systemUnderDesign.getName() + " == 1", "System under design is always part of solution");
		
		return milpCode;
	}
	
	/**
	 * Map objective from SysML to MILP.
	 * 
	 * @param objective A UML Constraint object, stereotyped with "Objective".
	 * @param milpGen The MILP code generation object.
	 * @return MILP code compatible with the target environment or an empty string
	 * 		if the specified UML constraint is not an objective.
	 */
	public static String mapObjective(Constraint objective,
			MILPModel2TextUtils milpGen) {
		if (!DSEMLUtils.isObjective(objective))
			return "";
		
		// Evaluate and interpret the objective
		String evaluatedObjective = MiniOCLInterpretor.interpretExpression(
				objective,
				UMLModelUtils.getStringValue(
						objective.getSpecification()));
		
		// Return a declared variable for the objective
		return milpGen.generateObjectiveDelaration(evaluatedObjective);
	}
	
	/**
	 * Map a resource type and generate any required instances, and map these, too.
	 * 
	 * @param resource The resource type to map
	 * @param resourceID The internal (numeric) ID of this resource (note: in the produced
	 * 		MILP code this is the number following the "M").
	 * @param numInstances The number of possible instances to represent in the MILP.
	 * @param milpGen The MILP code generation object.
	 * @return MILP representation of possible instances of the specified type of resource
	 * 		with applicable constraints.
	 */
	public static String mapResource(final Classifier resource,
			int resourceID,
			int numInstances,
			MILPModel2TextUtils milpGen) {
		String milpCode = "";
		
		String instanceCollection = "";
		
		milpCode += milpGen.generateComment("Instances of " + resource.getName());
		
		// Map resource instances, if desired
		for (int i = 0; i < numInstances; i++) {
			final String identifier = "I" + (i+1) + "M" + resourceID;
			
			// Create instance in model
			// TODO Implement
			
			// Map the resource instance
			// TODO This should change in the future to have an InstanceSpecification being handed over
			milpCode += mapResourceInstance(resource, resourceID, i+1, milpGen);
			milpCode += milpGen.generateLineSeparator();
				
			if (!instanceCollection.equals(""))
				instanceCollection += ", ";
			instanceCollection += identifier;
		}
		
		// Array of all instances of this machine (for later processing)
		milpCode += milpGen.generateVariableDeclaration("IM" + resourceID, "[" + instanceCollection + "]", "Vector of all instances (existence variables) for this resource type");
		
		// Add to array of all instances of all resources (for later processing)
		milpCode += milpGen.generateVariableDeclaration("IM", "[IM, IM" + resourceID + "]", "Add these instances to vector of all resource instances");
		
		// Also add integrity constraints for composite resources
		if (DSEMLUtils.isCompositeResource(resource)) {
			// Integrity constraints
			milpCode += milpGen.generateConstraintDeclaration(
					SysML2MILPMappingsHelper.addCompositeResourceIntegrityConstraints(resource), 
					"Integrity constraints / structural well-formedness rules for composite resource instances");
		}
		
		
		// To speed up computation, add additional constraints for resources that are NOT part of ANY composite, and
		// not a composite themselves
		if (!DSEMLUtils.isCompositeResource(resource) 
				&& !DSEMLUtils.isPartOfAnyComposite(resource, TransformationCache.getCompositeResources()))
			milpCode += milpGen.generateConstraintDeclaration(
					SysML2MILPMappingsHelper.addCompositeResourceAllocationConstraintsIgnoreNonCompositeParts(resource),
					"This resource type is not a part of any composite resources - therefore, identify relevant regions in the matrix C as 0");
		
		// Visualization of results
		SysML2MILPTransformation.appendResultsVisualizationBuffer(
				milpGen.generateVariableDeclaration("TotalInstancesM" + resourceID, "sum(value(IM" + resourceID + "))"));
		SysML2MILPTransformation.appendResultsVisualizationBuffer(
				milpGen.generateTextVisualization("'Total number of " + resource.getName() + ": %d', " + "round(TotalInstancesM" + resourceID + ")"));
		
		return milpCode;
	}
	
	/**
	 * Create (implicitly) and map a resource instance to MILP code.
	 * <P>
	 * The mapping is similar to that of a UML class (see
	 * {@link #mapUMLClassifier(Classifier, String, MILPModel2TextUtils)}) with
	 * the difference being that:<BR />
	 * 1) A binary variable is also created to indicate
	 * whether the instance is used in the solution (=1) or not (=0) <BR />
	 * 2) A constraint is added for this binary variable to break symmetry
	 * (i.e., instance 1 must be used before instance 2 is used).
	 * 
	 * @param resource The resource type to base the instance on.
	 * @param resourceID The internally used resource ID.
	 * @param instanceID The instance ID (set externally).
	 * @param milpGen The MILP code generation object.
	 * @return Representation of resource instance in target MILP syntax.
	 */
	public static String mapResourceInstance(Classifier resource,
			int resourceID,
			int instanceID,
			MILPModel2TextUtils milpGen) {
		String milpCode = "";
		
		// Internally used ID based on a resource ID and machine ID.
		// When creating a separate meta-model, this would be part of
		// the Resource and ResourceInstance object.
		String identifier = "I" + instanceID + "M" + resourceID;
		
		// This assumes that instance IDs always start at 1...
		String previousIdentifier = "";
		if (instanceID > 1)
			previousIdentifier = "I" + (instanceID - 1) + "M" + resourceID;
		
		// Map the rest like any other class (properties)
		// NOTE: formally, this should be a UML instance - but we are skipping instance creation right now, so use classifier and default values
		milpCode += mapUMLClassifier(resource, identifier, milpGen);
		
		// Add the type to the list of all types (used for back-transformation / correspondences)
		// Nominally, this should be stored in an external model
		milpCode += milpGen.generateVariableDeclaration(StandardVariableNames.TYPE_LIST, "[" + StandardVariableNames.TYPE_LIST + ", " + identifier + "_type]");
		
		// Composite type name list
		if (DSEMLUtils.isCompositeResource(resource))
			milpCode += milpGen.generateVariableDeclaration(StandardVariableNames.COMPOSITE_TYPE_LIST, "[" + StandardVariableNames.COMPOSITE_TYPE_LIST + ", " + identifier + "_type]");
		
		// Symmetry breaking constraint - always use first available instance; if multiple necessary, then force lowest instance indices
		if (!previousIdentifier.equals(""))
			milpCode += milpGen.generateConstraintDeclaration(
					previousIdentifier + " >= " + identifier,
					"Symmetry breaking constraint");
		
		// Finally, capture constraint that if instance is allocated to any action,
		// it must also exist (i.e., the identifying binary variable must be 1)
		milpCode += milpGen.generateConstraintDeclaration(
				"implies(sum(A(" + TransformationState.CURRENT_RESOURCE_INSTANCE_ID++ + ",:)) >= 1, " + identifier + ")",
				"Consistency constraint for allocation matrix and instance of machine variable");
		
		// Add constraints for composite resources
		if (DSEMLUtils.isCompositeResource(resource)) {
			milpCode += milpGen.generateConstraintDeclaration(
					SysML2MILPMappingsHelper.addCompositeResourceAllocationConstraints(resource, instanceID),
					"Constraints specific to ensuring that the correct aggregate resource instance <-> resource instance relationships are kept");
			
			TransformationState.CURRENT_COMPOSITE_RESOURCE_INSTANCE_ID++;
		}
		
		return milpCode;
	}
	
	/**
	 * Maps a functional specification.
	 * <P>
	 * In this method, Functions and Working Principles are also mapped. In addition,
	 * constraints related to precedence and timing are also generated. Due to their
	 * relevance, constraints and variables related to calculating the possible
	 * throughput are also added.
	 * 
	 * @param functionalSpec The functional specification to map.
	 * @param milpGen The MILP code generation object.
	 * @return A representation of the functional specification as a MILP compatible
	 * 		with the desired target MILP environment.
	 */
	public static String mapFunctionalSpecification(Activity functionalSpec,
			MILPModel2TextUtils milpGen) {
		String milpCode = "";
		
		// Set of variable declarations
		ArrayList<String> vars = null;
		
		// Flatten process - an internal data structure for the functional 
		// specification is used for easier translation to MILP
		Process flattened = ProcessFlattener.toProcess(functionalSpec);
		
		// Create a new constraint generator object
		ProcessConstraintGenerator p = new ProcessConstraintGenerator();
		
		// Set initial state
		TransformationState.CURRENT_ACTION_COUNT = 1;
		TransformationState.CURRENT_ACTION = null;
		TransformationState.CURRENT_ACTION_WP_COUNT = 1;
		
		
		/**** FUNCTIONS ****/
		
		milpCode += milpGen.generateLineSeparator();
		milpCode += milpGen.generateComment("Functional specification");
		
		// Map all contained functions
		for (Element e : functionalSpec.getOwnedElements()) {
			if (e instanceof CallBehaviorAction) {
				milpCode += mapFunction((CallBehaviorAction) e, milpGen);
			}
		}

		// Reset state (necessary for correct OCL interpretation)
		TransformationState.CURRENT_ACTION_WP_COUNT = 0;
		
		
		/**** PRECEDENCE CONSTRAINTS, TIMING & PARALLEL ACTIVITIES ****/
		milpCode += milpGen.generateComment("Variables used in determining start and end times of individual functions");
		
		// Timing-related variables
		vars = p.generateVariables(flattened, milpGen);
		for (String s : vars)
			milpCode += s;
		
		// Timing constraints
		milpCode += milpGen.generateConstraintDeclaration(p.generateTimingConstraints(flattened), "Timing constraints (including virtual activities)");
		
		// Resource sharing constraints for parallel activities
		// FIXME This currently doesn't quite work as expected if there are multiple work
		//		 pieces in the cell at the same time. What will happen is that these
		//		 starting points are treated as parallel entry points into the main activity
		//		 and, hence, none of the tracks can share resources.
		//		 It is suggested to detect this in the process parsing already: what needs to be done is
		//		 identify common "merge points" and leave only everything prior to that.
		//		 E.g., a process Step1 -> Step 2 -> Step 3, where Step2 receives input from Step1 plus
		//		 additional input from outside, would lead to the sequences Step1 -> Step2 -> Step3 AND
		//		 Step2 -> Step3. Then, when parsing identify that "Step2" is a "merge point" and leave
		//		 only its predecessors (none) (hence removing this second track).
		milpCode += milpGen.generateConstraintDeclaration(p.generateResourceSharingConstraints(flattened), "Constraints for restricting shareability among parallel activities");
		
		
		/**** THROUGHPUT ****/
		milpCode += milpGen.generateComment("Throughput calculation");
		
		// Use a different formulation if throughput HAS to be a design variable
		// (e.g., if it is a part of the objective)
		// NOTE: This should never be called (avoided at all cost in MiniOCLInterpretor - but CAN happen)
		if (TransformationState.MUST_OPTIMIZE_THROUGHPUT) {
			// FIXME Should warn user that this will result in very long runtime
			// (find better way)
			milpCode += milpGen.generateTextVisualization("WARNING! You are attempting something that requires the throughput to be a design variable. This is currently computationally VERY inefficient.");
			
			vars = p.generateUtilizationVariablesWithThroughputAsDesignVariable(milpGen);//p.generateUtilizationVariables(resources);
			for (String s : vars)
				milpCode += s;
			
			milpCode += milpGen.generateConstraintDeclaration(p.generateUtilizationConstraintsWithThroughputAsDesignVariable(TransformationCache.getResources()));
		} else {
			vars = p.generateUtilizationVariables(TransformationCache.getResources(), milpGen);
			for (String s : vars)
				milpCode += s;
		}
		
		
		return milpCode;
	}

	/**
	 * Maps a function to MILP code.
	 * 
	 * @param cba The function to map (a UML CallBehaviorAction object).
	 * @param milpGen The MILP code generation object.
	 * @return MILP code representing the particular function or the empty string if
	 * 		the behavior of the CallBehaviorAction is not an instance of a UML 
	 * 		Activity.
	 */
	public static String mapFunction(CallBehaviorAction cba,
			MILPModel2TextUtils milpGen) {
		String milpCode = "";
		
		// Set state
		TransformationState.CURRENT_WORKING_PRINCIPLE_COUNT = 1;
		TransformationState.CURRENT_ACTION = cba;
		
		String topLevelID = "IA" + TransformationState.CURRENT_ACTION_COUNT + "W";
		String wpCollection = "[";

		// Results visualization
		String wpCollectionNames = "{";		// Need a cell array to store an array list of strings
		
		logger.trace("Sub-activity: " + cba.getName());
		
		// Get corresponding activity / behavior
		// FIXME Can also be opaque behavior
		if (!(cba.getBehavior() instanceof Activity))
			return "";
		
		Activity act = (Activity) cba.getBehavior();
		
		milpCode += milpGen.generateComment(cba.getName());
		
		// Get associated working principles
		ArrayList<Activity> workingPrinciples = DSEMLUtils.getWorkingPrinciples(act,
				TransformationCache.getAllWorkingPrinciples());
		
		// Map working principle instances that can implement this (abstract) function
		for(Activity end : workingPrinciples) {
			String identifier = topLevelID + TransformationState.CURRENT_WORKING_PRINCIPLE_COUNT;
			
			// Create vector that includes all of the variables representing WP for an activity
			if (TransformationState.CURRENT_WORKING_PRINCIPLE_COUNT > 1) {
				wpCollection += ", ";
				wpCollectionNames += "; ";
			}
			
			// Generate part of the vector of all WPs associated with this activity
			wpCollection += identifier;
			wpCollectionNames += "'" + end.getName() + "'";
			
			// Map name of function
			milpCode += milpGen.generateVariableDeclaration(
					identifier + "_name", "{'" + cba.getName() + "'}",
					"Name of function");
			
			// Perform the actual mapping (above is mostly used for visualization)
			milpCode += mapWorkingPrincipleInstance(end, act, identifier, milpGen);
			
			// Add function name to name list
			milpCode += milpGen.generateVariableDeclaration(StandardVariableNames.FUNCTION_NAMES_LIST, "[" + StandardVariableNames.FUNCTION_NAMES_LIST + ", " + identifier + "_name]");
			
			TransformationState.CURRENT_ACTION_WP_COUNT++;
			
			// Insert a blank line for readability
			milpCode += milpGen.generateLineSeparator();
		}
		
		wpCollection += "]";
		wpCollectionNames += "}";		// Cell array
		
		milpCode += milpGen.generateVariableDeclaration(topLevelID, wpCollection);
		milpCode += milpGen.generateVariableDeclaration(topLevelID + "Names", wpCollectionNames, "Names of working principles associated with activity");
		
		// Visualization
		SysML2MILPTransformation.setResultsVisualizationBuffer(
				SysML2MILPTransformation.getResultsVisualizationBuffer()
				+ milpGen.generateTextVisualization("'Call Behavior Action " + cba.getName() + " (activity: " + act.getName() + ") -> %s', " + topLevelID + "Names{find(value(" + topLevelID + "), 1)}"));
		
		// Add constraint that at least one is used
		milpCode += milpGen.generateConstraintDeclaration("sum(" + topLevelID + ") == 1",
				"Exactly one working principle must be chosen for this function");		// MUST be 1! Otherwise it might choose 2 WP for one act
		
		// Increase count for activity label
		TransformationState.CURRENT_ACTION_COUNT++;
		
		return milpCode;
	}

	/**
	 * Maps a working principle instance to MILP.
	 * <P>
	 * Note that this function should probably be rewritten to make use of more generic mappings
	 * such as {@link #mapUMLClassifier(Classifier, String, MILPModel2TextUtils)}. However, then,
	 * the fact that pre- and post-conditions are skipped must still be taken into account. Also,
	 * constraints defining semantics of the DSEML side constructs must also be taken into account.
	 * 
	 * @param workingPrinciple The working principle (as UML Activity) to map.
	 * @param implementedBehavior The function to implement. This is the specified behavior of the
	 * 		CallBehaviorAction that defines the function being translated.
	 * @param identifier Internal identifier for this working principle <-> abstract function
	 * 		combination. Note: in the produced MILP code this is an ID of the form IAxWy.
	 * @param milpGen The MILP code generation object.
	 * @return MILP code compatible with the desired target environment describing a particular
	 * 		possible instance of a working principle (for a specific function).
	 */
	public static String mapWorkingPrincipleInstance(Activity workingPrinciple,
			Activity implementedBehavior,
			String identifier,
			MILPModel2TextUtils milpGen) {
		// TODO Much of this is similar to mapUMLInstance(...) -> should be updated at some point to avoid duplicate code
		String milpCode = "";

		// For each, create a variable that indicates its use
		milpCode += milpGen.generateVariableDeclaration(identifier, MILPVariableType.BINVAR, implementedBehavior.getName() + " + " + workingPrinciple.getName());
		
		// Type of working principle
		milpCode += milpGen.generateVariableDeclaration(
				identifier + "_type", "{'" + workingPrinciple.getQualifiedName() + "'}",
				"Qualified name of type of this instance");
		
		// Generate variables for properties of the working principle (time, cost, etc.)
		if (workingPrinciple.getAllAttributes() != null)
			for (Property prop : workingPrinciple.getAllAttributes())
				milpCode += mapUMLProperty(prop, identifier, milpGen);
		
		// TODO Multiplicities
		// For this working principle, also add constraints in terms of what resources are necessary
		ArrayList<Element> resourceTypes = DSEMLUtils.getAllAssociatedResourceTypes(workingPrinciple); //collectAssociatedConcreteResources(end);
		if (resourceTypes != null) {
			int numResourceTypes = 0;
			
			// At least 1 concrete type ("instance") of each resource type must be present
			for (Element resource : resourceTypes) {
				ArrayList<Element> concreteResources = DSEMLUtils.getConcreteResourcesForAbstractResourceType(TransformationCache.getResources(), resource);
				String arg = "";
				
				// FIXME This assumes multiplicity 1
				numResourceTypes++;
				
				for (Element concRes : concreteResources) {
					int startingIndex = InternalCorrespondences.getAllocationMatrixResourceInstanceIndex(concRes, 1);
					int endingIndex = startingIndex + SysML2MILPMappingsHelper.getNumInstancesForResource(concRes) - 1;
					
					if (!arg.equals("")) {
						arg += " + ";
					}
					
					arg += "sum(A(" + startingIndex + ":" + endingIndex + "," + TransformationState.CURRENT_ACTION_WP_COUNT + "))";
					
					// FIXME Assumes multiplicity of 1
					milpCode += milpGen.generateConstraintDeclaration("sum(A(" + startingIndex + ":" + endingIndex + "," + TransformationState.CURRENT_ACTION_WP_COUNT + ")) <= 1");
					
					// Allocations of a particular resource must at least equal number of instances
					milpCode += milpGen.generateConstraintDeclaration("sum(sum(A(" + startingIndex + ":" + endingIndex + ",:))) >= sum(IM" + InternalCorrespondences.getResourceTypeMachineIDMapping().get(concRes) + ")");

					logger.trace("Resource " + ((NamedElement) resource).getName());
				}
				
				milpCode += milpGen.generateConstraintDeclaration("implies(" + identifier + ", " + arg + " >= 1)");			// WAS == 1 (rationale for changing: what if two of same kind needed?) (seems to work, though)
			}
			
			// Sum of all machine instances allocated to this act+wp combination must be either 0 or equal to
			// the number of resources associated with a working principle
			milpCode += milpGen.generateConstraintDeclaration("sum(A(:," + TransformationState.CURRENT_ACTION_WP_COUNT + ")) == " + numResourceTypes + "*" + identifier);
		}
		
		// Extract constraints
		ArrayList<Constraint> constr = UMLModelUtils.collectConstraints(workingPrinciple);
		
		// Note that the mapping for constraints that are owned or inherited by a working
		// principle is a little different: these only have to be true if the working
		// principle is used in the solution
		for (Constraint c : constr) {
			// Added by Kristof's request: skip any pre- and post conditions.
			if (!workingPrinciple.getPreconditions().contains(c)
					&& !workingPrinciple.getPostconditions().contains(c)) {
				ValueSpecification v = c.getSpecification();
				String body = UMLModelUtils.getStringValue(v);
				
				String interpConstr = MiniOCLInterpretor.interpretExpression(c, body, workingPrinciple);
				
				logger.trace("=> Constraint: " + interpConstr);
				//constraints.add("implies(" + identifier + ", " + interpConstr + ")");
				milpCode += milpGen.generateConstraintDeclaration(identifier + " == (" + identifier + " & (" + interpConstr + "))");
			}
		}
		
		// Add the type to the list of all WP types (used for back-transformation / correspondences / visualization)
		// Nominally, this should be stored in an external model
		milpCode += milpGen.generateVariableDeclaration(StandardVariableNames.WP_TYPE_LIST, "[" + StandardVariableNames.WP_TYPE_LIST + ", " + identifier + "_type]");

		TransformationState.CURRENT_WORKING_PRINCIPLE_COUNT++;
		
		return milpCode;
	}
	
	/************ COMMON MAPPINGS. ***************/
	
	/**
	 * Map a UML class.
	 * <P>
	 * Note that, for all intents and purposes, we are really transforming a "possible" instance.
	 * Here, all "slot values" are based on the default values from the underlying type. The solver
	 * determines whether or not the particular instance is a part of the solution.
	 * 
	 * @param classifier The UML Classifier to map (e.g., UML Class or UML Activity).
	 * @param internalIdentifier The internal identifier associated with this UML element.
	 * @param milpGen The MILP code generation object.
	 * @return MILP representation of the UML Classifier and its properties and constraints.
	 */
	public static String mapUMLClassifier(Classifier classifier,
			String internalIdentifier,
			MILPModel2TextUtils milpGen) {
		String milpCode = "";
		String specialProps = "";
		String milpConstraints = "";
		
		// Binary variable indicating whether or not this instance is part of the final solution
		milpCode += milpGen.generateVariableDeclaration(internalIdentifier, MILPVariableType.BINVAR);
		
		if (classifier.getMembers() != null) {
			for (NamedElement o : classifier.getMembers()) {
				if (o instanceof Property) {
					// Heuristic: the "special" properties should be defined first, since some of the
					// other properties may be derived from these
					if (DSEMLUtils.isDurationProperty((Property) o)) {
						specialProps += mapUMLProperty((Property) o, internalIdentifier, milpGen);
					}
					else if (DSEMLUtils.isThroughputProperty((Property) o)) {
						specialProps += mapUMLProperty((Property) o, internalIdentifier, milpGen);
					}
					else {
						milpCode += mapUMLProperty((Property) o, internalIdentifier, milpGen);
					}
				}
				else if (o instanceof Constraint) {
					// Objective treated separately
					if (!DSEMLUtils.isObjective((Constraint) o)) { // Added since the objective can also be a part of the manufacturing cell now
						milpConstraints += mapUMLConstraint((Constraint) o, milpGen);
					}
				}
			}
		}
		
		if (!milpConstraints.equals(""))
			milpConstraints = "\r\n" + milpConstraints;
		
		// For capturing correspondences, also add a type identifier
		// NOTE: formally, this should be done with an external correspondence model
		specialProps = milpGen.generateVariableDeclaration(
				internalIdentifier + "_type", "{'" + classifier.getQualifiedName() + "'}",
				"Qualified name of type of this instance")
				+ specialProps;
		
		// Also store correspondences
		if (!SysML2MILPTransformation.TESTING_MODE)
			SysML2MILPMappingsHelper.storeCorrespondences(classifier,
					internalIdentifier);
		
		return specialProps + milpCode + milpConstraints;
	}

	/**
	 * Maps a UML constraint to MILP code.
	 * <P>
	 * The constraint is mapped after interpreting the expression. This interpretation includes
	 * resolving any transformed variables (see {@link MiniOCLInterpretor}).
	 * 
	 * @param constraint The UML Constraint to map.
	 * @param milpGen The MILP code generation object.
	 * @return A MILP representation of the constraint or the empty string if the specification
	 * 		of the constraint is "null", the empty string or the specification object is null.
	 */
	public static String mapUMLConstraint(Constraint constraint,
			MILPModel2TextUtils milpGen) {
		String milpCode = "";
		String spec = UMLModelUtils.getStringValue(constraint.getSpecification());
		
		// Only generate constraint if specification is not empty
		if (spec != null
				&& !spec.equals("")
				&& !spec.equals("null")) {
			// Interpret expression
			String interpretedConstraint = MiniOCLInterpretor.interpretExpression(constraint, spec);
			
			// Generate constraint in target MILP environment
			milpCode += milpGen.generateConstraintDeclaration(interpretedConstraint);
		}
		
		return milpCode;
	}
	
	/**
	 * Maps a UML property to MILP code.
	 * <P>
	 * Note that special properties (e.g., throughput and productionTime) are treated specially
	 * by defining their values to be equal to standard variables (names as defined in 
	 * {@link StandardVariableNames}, declared in MILP code as in
	 * {@link SysML2MILPMappingsHelper#declareStandardVariables(MILPModel2TextUtils)}.
	 * 
	 * @param property The property to map.
	 * @param ownerIdentifier The internal identifier used in constructing the variable name.
	 * @param milpGen The MILP code generation object.
	 * @return MILP representation of the property or an empty string if the default value is
	 * 		not specified or the property is not recognized as a special (known-to-the-
	 * 		transformation) property.
	 */
	public static String mapUMLProperty(Property property,
			String ownerIdenfier,
			MILPModel2TextUtils milpGen) {
		String milpCode = "";
		String defValue = UMLModelUtils.getDefaultValue(property);
		String milpPropertyName = ownerIdenfier + "_" + property.getName();
		
		if (defValue != null
			&& !defValue.equals("")
			&& !defValue.equals("null")) {
			String interpretedExpression = MiniOCLInterpretor.interpretExpression(property, defValue);
			
			milpCode += milpGen.generateVariableDeclaration(milpPropertyName, interpretedExpression, property.getName() + " of " + ((NamedElement) property.getOwner()).getName());
		} // If not default value, may be a special property
		else if (DSEMLUtils.isDurationProperty(property)) {
			milpCode += milpGen.generateVariableDeclaration(milpPropertyName, StandardVariableNames.TOTAL_DURATION);
		}
		else if (DSEMLUtils.isThroughputProperty(property)) {
			milpCode += milpGen.generateVariableDeclaration(milpPropertyName, StandardVariableNames.THROUGHPUT);
		}
		
		return milpCode;
	}
	
}
