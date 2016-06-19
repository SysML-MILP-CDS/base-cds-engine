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
import java.util.LinkedHashMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.Element;

import edu.gatech.mbse.transformations.sysml2milp.utils.DSEMLUtils;

/**
 * Internal correspondences between, e.g., resources and resource IDs, and
 * instances of resources with their respective IDs, as well as indices in the
 * allocation matrix.
 * <P>
 * The contained correspondences are computed prior to the actual transformation
 * and are cached for easier access. Some of these functions are computationally
 * complex and require deep model traversal.
 * 
 * @author Sebastian
 * @version 0.1
 */
public class InternalCorrespondences {

	/** Resource type -> machine ID mapping. */
	private static HashMap<Element, Integer> resourceTypeMachineIDMapping = new HashMap<Element,Integer>();
	
	/** Resource type -> instances mapping. */
	private static LinkedHashMap<Element, ArrayList<Integer>> resourceTypeInstanceIDMapping = new LinkedHashMap<Element, ArrayList<Integer>>();
	
	/** Mapping from activity - working principle pairs to IDs in matrix A. */
	private static HashMap<Entry<Activity, Element>, Integer> activityWPIndex = new HashMap<Entry<Activity, Element>, Integer>();
	
	/** Mapping from activity node - working principle pairs to IDs in matrix A. */
	private static HashMap<Entry<ActivityNode, Element>, Integer> activityNodeWPIndex = new HashMap<Entry<ActivityNode, Element>, Integer>();
	
	/** Mapping from activity node - working principle IDs to pairs. */
	private static HashMap<String, Entry<ActivityNode, Element>> activityNodeWPIDToElementsMapping = new HashMap<String, Entry<ActivityNode, Element>>();
	
	/** Mapping from activity instance - working principle identifier to activity and working principle. */
	private static HashMap<String, Entry<Activity, Element>> activityInstanceWPIDIndex = new HashMap<String, Entry<Activity, Element>>();

	/** Activity node (e.g., CBA) to instance ID (as used for x in IAxWy) mapping. */
	private static HashMap<ActivityNode,Integer> actInstanceIDMapping = new HashMap<ActivityNode,Integer>();
	
	/** Mapping from a resource to one or more action+wp combination IDs. */
	private static HashMap<Element,HashSet<String>> resourceActivityIDsMapping = new HashMap<Element,HashSet<String>>();
	
	/**
	 * Resets all internal correspondences.
	 */
	public static void resetInternalCorrespondences() {
		resourceTypeMachineIDMapping = new HashMap<Element,Integer>();
		resourceTypeInstanceIDMapping = new LinkedHashMap<Element, ArrayList<Integer>>();
		activityWPIndex = new HashMap<Entry<Activity, Element>, Integer>();
		activityNodeWPIndex = new HashMap<Entry<ActivityNode, Element>, Integer>();
		activityNodeWPIDToElementsMapping = new HashMap<String, Entry<ActivityNode, Element>>();
		activityInstanceWPIDIndex = new HashMap<String, Entry<Activity, Element>>();
		actInstanceIDMapping = new HashMap<ActivityNode,Integer>();
		resourceActivityIDsMapping = new HashMap<Element,HashSet<String>>();
	}
	
	/**
	 * Returns the row index for both the "A" and "C" matrix for individual resource
	 * instances.
	 * 
	 * @param e The resource type to be retrieved.
	 * @param instance The (possible) instance (starting from 1) of the resource type
	 * 		queried for.
	 * @return Index in allocation matrix (> 0) or -1 if Element queried cannot be found
	 * 		in mapping.
	 */
	public static int getAllocationMatrixResourceInstanceIndex(Element e, int instance) {
		int index = 0;
		
		// Linked hash maps are ordered, so calculate index based on that
		for (Entry<Element, ArrayList<Integer>> entry : resourceTypeInstanceIDMapping.entrySet()) {
			index++;
			
			if (entry.getKey() == e)
				return index + (instance-1);
			else
				index += entry.getValue().size() - 1;
		}
		
		return -1;
	}
	
	/**
	 * Returns the column index for the "C" matrix for individual composite resource
	 * instances.
	 * 
	 * @param e The aggregate resource type the column index is requested for.
	 * @param instance The (possible) instance number of the aggregate resource.
	 * @return The column index (> 0) or -1 if e is not in the map.
	 */
	public static int getCompositeMatrixCompositeResourceInstanceColumnIndex(Element e, int instance) {
		int index = 0;
		int firstCompositeIndex = -1;
		
		// Linked hash maps are ordered, so calculate index based on that
		for (Entry<Element, ArrayList<Integer>> entry : resourceTypeInstanceIDMapping.entrySet()) {
			index++;
			
			if (firstCompositeIndex == -1
					&& DSEMLUtils.isCompositeResource(entry.getKey()))
				firstCompositeIndex = index - 1;		// Subtract 1, since "index" is matlab index
			
			if (entry.getKey() == e) {
				if (firstCompositeIndex == -1)
					firstCompositeIndex = 0;
				
				return index + (instance-1) - firstCompositeIndex;
			}
			else
				index += entry.getValue().size() - 1;
		}
		
		return -1;
	}
	
	/**
	 * Returns the column index for the "C" matrix, which stores allocations of
	 * resources to composites.
	 * 
	 * @param e
	 * @param instance
	 * @return
	 * @deprecated
	 */
	public static int getCompositeAllocationMatrixCompositeInstanceIndex(Element composite, int instance) {
		int index = 0;
		
		// Linked hash maps are ordered, so calculate index based on that
		for (Entry<Element, ArrayList<Integer>> entry : resourceTypeInstanceIDMapping.entrySet()) {
			if (DSEMLUtils.isCompositeResource(entry.getKey())) {
				index++;
				
				if (entry.getKey() == composite)
					return index + (instance-1);
				else
					index += entry.getValue().size() - 1;
			}
		}
		
		return -1;
	}
	
	/**
	 * @return the resourceTypeMachineIDMapping
	 */
	public static HashMap<Element, Integer> getResourceTypeMachineIDMapping() {
		return resourceTypeMachineIDMapping;
	}

	/**
	 * @param resourceTypeMachineIDMapping the resourceTypeMachineIDMapping to set
	 */
	public static void setResourceTypeMachineIDMapping(HashMap<Element, Integer> resourceTypeMachineIDMapping) {
		InternalCorrespondences.resourceTypeMachineIDMapping = resourceTypeMachineIDMapping;
	}

	/**
	 * @return the resourceTypeInstanceIDMapping
	 */
	public static LinkedHashMap<Element, ArrayList<Integer>> getResourceTypeInstanceIDMapping() {
		return resourceTypeInstanceIDMapping;
	}

	/**
	 * @param resourceTypeInstanceIDMapping the resourceTypeInstanceIDMapping to set
	 */
	public static void setResourceTypeInstanceIDMapping(
			LinkedHashMap<Element, ArrayList<Integer>> resourceTypeInstanceIDMapping) {
		InternalCorrespondences.resourceTypeInstanceIDMapping = resourceTypeInstanceIDMapping;
	}

	/**
	 * @return the activityWPIndex
	 */
	public static HashMap<Entry<Activity, Element>, Integer> getActivityWPIndex() {
		return activityWPIndex;
	}

	/**
	 * @param activityWPIndex the activityWPIndex to set
	 */
	public static void setActivityWPIndex(HashMap<Entry<Activity, Element>, Integer> activityWPIndex) {
		InternalCorrespondences.activityWPIndex = activityWPIndex;
	}

	/**
	 * @return the activityNodeWPIndex
	 */
	public static HashMap<Entry<ActivityNode, Element>, Integer> getActivityNodeWPIndex() {
		return activityNodeWPIndex;
	}

	/**
	 * @param activityNodeWPIndex the activityNodeWPIndex to set
	 */
	public static void setActivityNodeWPIndex(HashMap<Entry<ActivityNode, Element>, Integer> activityNodeWPIndex) {
		InternalCorrespondences.activityNodeWPIndex = activityNodeWPIndex;
	}

	/**
	 * @return the activityNodeWPIDToElementsMapping
	 */
	public static HashMap<String, Entry<ActivityNode, Element>> getActivityNodeWPIDToElementsMapping() {
		return activityNodeWPIDToElementsMapping;
	}

	/**
	 * @param activityNodeWPIDToElementsMapping the activityNodeWPIDToElementsMapping to set
	 */
	public static void setActivityNodeWPIDToElementsMapping(
			HashMap<String, Entry<ActivityNode, Element>> activityNodeWPIDToElementsMapping) {
		InternalCorrespondences.activityNodeWPIDToElementsMapping = activityNodeWPIDToElementsMapping;
	}

	/**
	 * @return the activityInstanceWPIDIndex
	 */
	public static HashMap<String, Entry<Activity, Element>> getActivityInstanceWPIDIndex() {
		return activityInstanceWPIDIndex;
	}

	/**
	 * @param activityInstanceWPIDIndex the activityInstanceWPIDIndex to set
	 */
	public static void setActivityInstanceWPIDIndex(HashMap<String, Entry<Activity, Element>> activityInstanceWPIDIndex) {
		InternalCorrespondences.activityInstanceWPIDIndex = activityInstanceWPIDIndex;
	}

	/**
	 * @return the actInstanceIDMapping
	 */
	public static HashMap<ActivityNode, Integer> getActInstanceIDMapping() {
		return actInstanceIDMapping;
	}

	/**
	 * @param actInstanceIDMapping the actInstanceIDMapping to set
	 */
	public static void setActInstanceIDMapping(HashMap<ActivityNode, Integer> actInstanceIDMapping) {
		InternalCorrespondences.actInstanceIDMapping = actInstanceIDMapping;
	}

	/**
	 * @return the resourceActivityIDsMapping
	 */
	public static HashMap<Element, HashSet<String>> getResourceActivityIDsMapping() {
		return resourceActivityIDsMapping;
	}

	/**
	 * @param resourceActivityIDsMapping the resourceActivityIDsMapping to set
	 */
	public static void setResourceActivityIDsMapping(HashMap<Element, HashSet<String>> resourceActivityIDsMapping) {
		InternalCorrespondences.resourceActivityIDsMapping = resourceActivityIDsMapping;
	}
	
	/**
	 * Pre-computes internal identifiers and mappings from UML elements to these.
	 * 
	 * @param functionalSpecification The functional specification.
	 */
	public static void preComputeFunctionStructureCorrespondences(Activity functionalSpecification) {
		// Reset global mappings
		InternalCorrespondences.setResourceActivityIDsMapping(new HashMap<Element,HashSet<String>>());
		InternalCorrespondences.setActivityWPIndex(new HashMap<Entry<Activity, Element>, Integer>());
		InternalCorrespondences.setActivityNodeWPIndex(new HashMap<Entry<ActivityNode, Element>, Integer>());
		InternalCorrespondences.setActivityNodeWPIDToElementsMapping(new HashMap<String, Entry<ActivityNode,Element>>());
		InternalCorrespondences.setActivityInstanceWPIDIndex(new HashMap<String, Entry<Activity, Element>>());
		
		TransformationState.CURRENT_ACTION_COUNT = 1;
		TransformationState.CURRENT_ACTION_WP_COUNT = 1;
		
		for (Element e : functionalSpecification.getOwnedElements()) {
			if (e instanceof CallBehaviorAction) {
				CallBehaviorAction cba = (CallBehaviorAction) e;
				
				// Set state
				TransformationState.CURRENT_WORKING_PRINCIPLE_COUNT = 1;
				
				String topLevelID = "IA" + TransformationState.CURRENT_ACTION_COUNT + "W";
				
				// Store the mapping from action -> action ID (should be part of an Action or
				// Function object in the future)
				InternalCorrespondences.getActInstanceIDMapping().put(cba, TransformationState.CURRENT_ACTION_COUNT);

				// Get corresponding activity / behavior
				// FIXME Can also be opaque behavior
				if (!(cba.getBehavior() instanceof Activity))
					continue;
				
				Activity act = (Activity) cba.getBehavior();

				// Get associated working principles
				ArrayList<Activity> workingPrinciples = DSEMLUtils.getWorkingPrinciples(act,
						TransformationCache.getAllWorkingPrinciples());
				
				for(Activity end : workingPrinciples) {
					String identifier = topLevelID + TransformationState.CURRENT_WORKING_PRINCIPLE_COUNT;
					
					InternalCorrespondences.getActivityNodeWPIndex().put(new SimpleEntry<ActivityNode, Element>(cba, end), TransformationState.CURRENT_ACTION_WP_COUNT);
					InternalCorrespondences.getActivityNodeWPIDToElementsMapping().put(identifier, new SimpleEntry<ActivityNode, Element>(cba, end));
					InternalCorrespondences.getActivityWPIndex().put(new SimpleEntry<Activity, Element>(act, end), TransformationState.CURRENT_ACTION_WP_COUNT);
					InternalCorrespondences.getActivityInstanceWPIDIndex().put(identifier, new SimpleEntry<Activity, Element>(act, end));
					
					TransformationState.CURRENT_ACTION_WP_COUNT++;
					
					ArrayList<Element> resourceTypes = DSEMLUtils.getAllAssociatedResourceTypes(end); //collectAssociatedConcreteResources(end);
					if (resourceTypes != null) {
						// At least 1 concrete type ("instance") of each resource type must be present
						for (Element resource : resourceTypes) {
							// Add abstract type to mapping
							if(InternalCorrespondences.getResourceActivityIDsMapping().get(resource) == null)
								InternalCorrespondences.getResourceActivityIDsMapping().put(resource, new HashSet<String>());
							InternalCorrespondences.getResourceActivityIDsMapping().get(resource).add(identifier);
							
							ArrayList<Element> concreteResources = DSEMLUtils.getConcreteResourcesForAbstractResourceType(TransformationCache.getResources(), resource);
							for (Element concRes : concreteResources) {
								if(InternalCorrespondences.getResourceActivityIDsMapping().get(concRes) == null)
									InternalCorrespondences.getResourceActivityIDsMapping().put(concRes, new HashSet<String>());
								InternalCorrespondences.getResourceActivityIDsMapping().get(concRes).add(identifier);
							}
						}
					}
					
					TransformationState.CURRENT_WORKING_PRINCIPLE_COUNT++;
				}
				
				TransformationState.CURRENT_ACTION_COUNT++;
			}
		}
		
		TransformationState.CURRENT_ACTION_WP_COUNT = 0;
	}
	
}
