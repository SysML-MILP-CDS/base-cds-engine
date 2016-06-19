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
import java.util.HashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.ObjectFlow;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.PackageImport;
import org.eclipse.uml2.uml.Profile;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.ReadStructuralFeatureAction;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.ValueSpecificationAction;

import edu.gatech.mbse.transformations.sysml2milp.ocl.MiniOCLInterpretor;

/**
 * Collection of functions specific to the design space exploration modeling
 * language (DSEML).
 * <P>
 * Interpretation and querying functions to retrieve or compute or infer elements
 * or sets of elements from the problem definition in SysML / UML.
 * 
 * @author Sebastian
 * @version 0.1
 */
public class DSEMLUtils extends UMLModelUtils {

	/** Log4J object. */
	private static final Logger logger = LogManager.getLogger(DSEMLUtils.class.getName());
	
	/**
	 * Create a resource, owned by the specified element.
	 * 
	 * @param name Name of resource.
	 * @param owner Owner of resource.
	 * @return Newly created UML Classifier representing resource.
	 */
	public static Classifier createResource(String name, Package owner) {
		// Make sure we can access the necessary stereotype
		ArrayList<Stereotype> allStereotypes = new ArrayList<Stereotype>();
		
		for (Profile p : owner.getModel().getAllAppliedProfiles()) {
			allStereotypes.addAll(p.getOwnedStereotypes());
		}
		
		Stereotype resourceStereotype = findStereotypeByName("Resource", allStereotypes);
		
		if (resourceStereotype == null)
			return null;
		
		// Create class
		Classifier newResource = createClass(name, owner);
		
		// Now add stereotypes
		newResource.applyStereotype(resourceStereotype);
		
		return newResource;
	}
	
	/**
	 * Create a specialized resource, owned by the specified element.
	 * <P>
	 * This function is primarily used to create "singleton instances" - i.e., classifiers that
	 * should only be instantiated once, and represent the equivalent of an instance.
	 * 
	 * @param name Name of the resource.
	 * @param generalResource The general classifier
	 * @param owner The owner of the new classifier.
	 * @return The new classifier with an owned Generalization.
	 */
	public static Classifier createSpecializedResource(String name,
			Classifier generalResource,
			Package owner) {
		// Create the basic resource
		Classifier newResource = createResource(name, owner);
		
		// Add generalization
		createGeneralization(newResource, generalResource);
		
		// Inherit & redefine properties
		redefineInheritedProperties(newResource);
		
		return newResource;
	}
	
	/**
	 * Creates a specialied system under design by inheriting from the system under
	 * design found in the model.
	 * 
	 * @param name Name of the specialized system under design.
	 * @param generalResource The general system under design.
	 * @param owner Owning package of the new system under design.
	 * @return A new UML Classifier object representing the new system under design.
	 */
	public static Classifier createSpecializedSystemUnderDesign(String name,
			Classifier systemUnderDesign,
			Package owner) {
		// Create the basic resource
		Classifier newSUD = createClass(name, owner);
		
		// Add generalization
		createGeneralization(newSUD, systemUnderDesign);
		
		// Inherit & redefine properties
		redefineInheritedProperties(newSUD);
		
		return newSUD;
	}
	
	/**
	 * Retrieve nested resource types.
	 * <p>
	 * Performs a depth first search to identify resources at any level.
	 * 
	 * @param resource The resource to start from
	 * @return A set of resources (abstract and non-abstract) contained in
	 * 		the specified aggregate resource.
	 */
	public static HashSet<Element> getAllNestedResourceTypes(Element resource) {
		HashSet<Element> parts = new HashSet<Element>();
		
		if (!isCompositeResource(resource))
			return parts;
		
		for (Element part : getResourceParts(resource)) {
			parts.add(part);
			parts.addAll(getAllNestedResourceTypes(part));
		}
		
		return parts;
	}
	
	/**
	 * Returns a list of all types of resources (abstract and non-abstract).
	 * 
	 * @param startingElement The element to start from.
	 * @param topLevelActivity The functional specification.
	 * @param rootElement The root model element.
	 * @return A list of all types of resources found.
	 */
	public static ArrayList<Element> getAllResources(Element startingElement, 
			Activity topLevelActivity,
			Element rootElement) {
		ArrayList<Element> resources = new ArrayList<Element>();
		
		// Iterate through owned elements to find resources
		for (Element e : startingElement.getOwnedElements()) {
			// Depth first search
			if (e.getOwnedElements() != null)
				resources.addAll(getAllResources(e, topLevelActivity, rootElement));
			
			// If model library...
			if (isPackageImport(e))
				resources.addAll(getAllResources(((PackageImport) e).getImportedPackage(), topLevelActivity, rootElement));
			
			// FIXME Fairly slow right now due to a repeated search through the model - pre-compute the relevant resources
			if (e instanceof Classifier
					//&& oneBaseClassifierIsResource(e)
					&& isResource(e)
					&& isResourceRelevant(e, topLevelActivity, rootElement)) {
				logger.trace("Looks like a resource: " + ((Classifier) e).getName());
				
				resources.add(e);
			}
		}
		
		return resources;
	}
	
	/** @see TransformationHelper#getAllResources(Element, Activity, Element) */
	public static ArrayList<Element> getAllResources(Element rootElement, Activity topLevelActivity) {
		return getAllResources(rootElement, topLevelActivity, rootElement);
	}
	
	/**
	 * Collect concrete resources associated with this working principle.
	 * 
	 * @param workingPrinciple The working principle.
	 * @param concreteResources A list of all concrete (and relevant) resources.
	 * @return A list of concrete resources for all directly associated resources (i.e.,
	 * 		for example not the parts of aggregates).
	 */
	public static ArrayList<Element> getAssociatedConcreteResources(Activity workingPrinciple,
			ArrayList<Element> concreteResources) {
		ArrayList<Element> res = new ArrayList<Element>();
		
		// If this is not a working principle, return null
		if(!isWorkingPrinciple(workingPrinciple))
			return null;
		
		ArrayList<Element> associatedResources = getAssociatedResourceTypes(workingPrinciple);
		
		// Retrieve concrete resources for any abstract types
		for (Element resType : associatedResources)
			res.addAll(getConcreteResourcesForAbstractResourceType(concreteResources, resType));
		
		return res;
	}
	
	/**
	 * Get a list of resource types associated with a working principle. Note
	 * that these are only the <emph>directly</emph> related resources.
	 * 
	 * @param workingPrinciple The working principle to collect resources from.
	 * @return An ordered list of directly associated resource types.
	 */
	public static ArrayList<Element> getAssociatedResourceTypes(Activity workingPrinciple) {
		ArrayList<Element> res = new ArrayList<Element>();
		
		// If this is not a working principle, return null
		if (!isWorkingPrinciple(workingPrinciple))
			return null;
		
		// Return empty list if no associated resources
		if (workingPrinciple.getAllAttributes() == null)
			return res;
		
		// Look through the attributes of the working principle
		for (Property p : workingPrinciple.getAllAttributes()) {
			if (isResource(p.getType())
					&& !res.contains(p.getType()))
				res.add(p.getType());
		}
		
		return res;
	}
	
	/**
	 * Get a list of resource types associated with a working principle.
	 * <P>
	 * Note that this function is currently imposing some constraints on the
	 * well-formedness of the model: for each working principle, there may be
	 * at most 1 of each type of resource associated with it. Redefining
	 * properties should therefore always have a type that is a specialization
	 * of the type of the inherited redefined property.
	 * 
	 * @param workingPrinciple The working principle to collect resources from
	 * @return A list of all resource types associated with this working principle
	 * 		(at any level).
	 */
	public static ArrayList<Element> getAllAssociatedResourceTypes(Classifier workingPrinciple) {
		ArrayList<Element> res = new ArrayList<Element>();
		
		// Return empty list if no associated resources
		if (workingPrinciple.getAllAttributes() == null)
			return res;
		
		// Look through the attributes of the working principle
		for (Property p : workingPrinciple.getAllAttributes()) {
			if (isResource(p.getType())
					&& !res.contains(p.getType()))
				res.add(p.getType());
			
			if (isCompositeResource(p.getType()))
				res.addAll(getAllAssociatedResourceTypes((Classifier) p.getType()));
		}
		
		return res;
	}
	
	/**
	 * Returns a list of elements that constitute composites that own
	 * the given element as a part.
	 * 
	 * @param e The resource to look for.
	 * @param allResources A list of all relevant abstract and non-abstract resources.
	 * @param concreteResources A list of non-abstract (concrete) relevant resources.
	 * @return A list of aggregate resources that own / are associated with the
	 * 		given part.
	 */
	public static ArrayList<Element> getCompositeParents(Element e,
			ArrayList<Element> allResources,
			ArrayList<Element> concreteResources) {
		ArrayList<Element> composites = new ArrayList<Element>();
		
		// TODO This will only return the immediate owners, but not any specializations - must go through
		// specializations as well -> e.g., RobotWithEndEffector => RobotWithGripper, etc.
		for (Element res : allResources) {
			Classifier resC = (Classifier) res;
			
			if (resC.getAllAttributes() != null) {
				for (Property p : resC.getAllAttributes()) {
					if (e == p.getType()) {
						// Have a potential association
						if (p.isComposite()
								&& isResource(p.getOwner())) {
							// If owner is abstract, then add specialized...
							ArrayList<Element> owners = new ArrayList<Element>();
							
							if (isAbstract((Classifier) p.getOwner())) {
								owners = getConcreteResourcesForAbstractResourceType(concreteResources, p.getOwner());
							}
							else {
								owners.add(p.getOwner());
							}
							
							// In case of composites, the composite itself is part of the set (bug or feature?)
							owners.remove(e);
							
							//System.out.println(e.toString() + "'s composite parent is: " + owners.get(0).toString());
							for (Element owner : owners)
								if (!composites.contains(owner))
									composites.add(owner);
						}
					}
				}
			}
		}
		
		return composites;
	}
	
	/**
	 * Returns a list of composite resources in the provided list of resources.
	 * 
	 * @param resources A list of resources
	 * @return A sub-list of the input list with only aggregate resources.
	 */
	public static ArrayList<Element> getCompositeResources(ArrayList<Element> resources) {
		ArrayList<Element> composites = new ArrayList<Element>();
		
		// Go through list of resources (concrete) and add if composite
		for (Element e : resources) {
			if (isCompositeResource(e))
				composites.add(e);
		}
		
		return composites;
	}
	
	/**
	 * Returns the non-abstract machining resources.
	 * 
	 * @param startingElement The element to start the search from.
	 * @param topLevelActivity The functional specification.
	 * @param rootElement The root model element.
	 * @return A list of non-abstract and relevant resources.
	 */
	public static ArrayList<Element> getConcreteResources(Element startingElement, 
			Activity topLevelActivity,
			Element rootElement) {
		ArrayList<Element> resources = new ArrayList<Element>();
		
		// Iterate through owned elements to find resources
		for(Element e : startingElement.getOwnedElements()) {
			// Depth first search
			if(e.getOwnedElements() != null)
				resources.addAll(getConcreteResources(e, topLevelActivity, rootElement));
			
			// If model library...
			if (isPackageImport(e))			// Then add referenced elements
				resources.addAll(getConcreteResources(((PackageImport) e).getImportedPackage(), topLevelActivity, rootElement));
			
			if(e instanceof Classifier
					//&& oneBaseClassifierIsResource(e)
					&& isResource(e)
					&& !isAbstract((Classifier) e)
					&& isResourceRelevant(e, topLevelActivity, rootElement)) {
				logger.trace("Looks like a concrete resource: " + ((Classifier) e).getName());
				
				resources.add(e);
			}
		}
		
		return resources;
	}
	
	/** @see TransformationHelper#getConcreteResources(Element, Activity, Element) */
	public static ArrayList<Element> getConcreteResources(Element rootElement, Activity topLevelActivity) {
		return getConcreteResources(rootElement, topLevelActivity, rootElement);
	}
	
	/**
	 * Get concrete resources for an abstract resource.
	 * 
	 * @param abstractResource The abstract resource.
	 * @param concreteResources A list of concrete resources relevant to the given
	 * 		design problem.
	 * @return A list of concrete resources that specialize the given abstract resource
	 * 		type.
	 */
	public static ArrayList<Element> getConcreteResources(
			Element abstractResource,
			ArrayList<Element> concreteResources) {
		// FIXME See comment in next function - this function is only called by interpret()
		//		 in the OCL interpreter.
		
		ArrayList<Element> res = new ArrayList<Element>();
		
		for (Element r : concreteResources)
			if (oneBaseClassifierIsSpecificClassifier((Classifier) r, (Classifier) abstractResource)
					&& !res.contains(r))
				res.add(r);
		
		return res;
	}
	
	/**
	 * Returns the non-abstract machining resources.
	 * 
	 * @param concreteResources A list of concrete resource types.
	 * @param abstractResource An abstract resource type.
	 * @return A list of concrete (non-abstract) resource types that specialize
	 * 		the given abstract resource.
	 */
	public static ArrayList<Element> getConcreteResourcesForAbstractResourceType(
			ArrayList<Element> concreteResources,
			Element abstractResource) {
		// FIXME Seems to be a duplicate with possibly slightly different behavior?
		
		ArrayList<Element> resources = new ArrayList<Element>();
		
		if (abstractResource instanceof Classifier
				&& !isAbstract((Classifier) abstractResource))
			resources.add(abstractResource);
		
		// Iterate through owned elements to find resources
		for (Element e : concreteResources) {
			// Depth first search
			if (!e.equals(abstractResource)
					&& oneBaseClassifierIsSpecificClassifier((Classifier) e, (Classifier) abstractResource)) {
				resources.add(e);
			}
		}
		
		return resources;
	}
	
	/**
	 * Returns first work piece found in project.
	 * <P>
	 * The work piece is identified by inheriting from the generic work piece, which
	 * is identified purely by name. Note that while this function is referenced in
	 * interpret() in the OCL interpreter, I don't think it is actually being called
	 * anymore.
	 * 
	 * @param rootElement The root model element.
	 * @return The first work piece found.
	 */
	public static Classifier getFirstWorkpiece(Element rootElement) {
		Classifier workpiece = null;
		
		if (isWorkpiece(rootElement) && !isAbstract((Classifier) rootElement))
			return (Classifier) rootElement;
		
		if (rootElement.getOwnedElements() != null) {
			for (Element e : rootElement.getOwnedElements()) {
				if (isWorkpiece(e) && !isAbstract((Classifier) e))
					return (Classifier) e;
				
				workpiece = getFirstWorkpiece(e);
				
				// Return first one found
				if (workpiece != null)
					return workpiece;
			}
		}
		
		return workpiece;
	}
	
	/**
	 * Returns a list of resources directly associated with a particular set of working principles
	 * that are associated with a given activity. That is, this function is similar to
	 * {@link #getResourceTypes(ArrayList)}, with the difference being that nested resource types
	 * (from composites) are skipped.
	 * 
	 * @param workingPrinciples The working principles as concrete implementations of the activity
	 * @return A list of resources that are specified as part of the working principles
	 */
	public static HashSet<Element> getImmediateResourceTypes(ArrayList<Activity> workingPrinciples) {
		HashSet<Element> res = new HashSet<Element>();
		
		for (Activity wp : workingPrinciples) {
			if (wp.getOwnedAttributes() != null) {
				for (Property p : wp.getOwnedAttributes()) {
					Type t = p.getType();
					
					if (isResource(t))
						res.add(t);
				}
			}
		}
		
		return res;
	}
	
	/**
	 * Returns the objective.
	 * 
	 * @param systemUnderDesign The system under design
	 * @return The objective as a UML Constraint object or null if the system under
	 * 		design is null or the objective cannot be found.
	 */
	public static Constraint getObjective(Classifier systemUnderDesign) {
		Constraint c = null;
		
		if (systemUnderDesign == null)
			return null;
		
		// Objective is always part of system under design
		for (NamedElement n : systemUnderDesign.getMembers())
			if (n instanceof Constraint
					&& isObjective((Constraint) n))
				return (Constraint) n;
		
		// Note that old revisions of the transformation contained a search for an
		// element that has, as an owned element, the system under design. This
		// was done to separate the system under design from the design problem
		// definition. See old revisions for this code.
		
		return c;
	}
	
	
	/**
	 * Extract the value passed to a parameter of a specific call behavior action.
	 * <P>
	 * Extracted is the value of a ValueSpecificationAction or a
	 * ReadStructuralFeatureAction.
	 * 
	 * @param action The action to handle.
	 * @param parameterName The name of the parameter to extract the value for.
	 * @param objectFlows A list of object flows in the model.
	 * @return A string representation of the value associated with the parameter.
	 */
	public static String getInterpretedParameterValue(CallBehaviorAction action,
			String parameterName,
			ArrayList<ObjectFlow> objectFlows) {
		String value = "";
		
		for (ObjectFlow o : objectFlows) {
			// FIXME If object flow has no source or target, this will NOT be caught here! (weirdly)
			if (o == null
					|| o.getSource() == null
					|| o.getTarget() == null)
				continue;
			
			ActivityNode source = o.getSource();
			ActivityNode target = o.getTarget();
			
			// FIXME A bit of a hack, since it assumes that they have the same name... might want to
			// 		 document that
			if (target != null && target.getName() != null && target.getOwner() != null
					&& target.getName().equals(parameterName)
					&& target.getOwner().equals(action)) {
				// Then, extract value from source (if appropriate)
				Element valueInput = source.getOwner();
				
				if (valueInput instanceof ValueSpecificationAction) {				// Input from a value specification action
					//OpaqueExpression val = (OpaqueExpression) ((ValueSpecificationAction) valueInput).getValue().toString();
					//LiteralString lit = (LiteralString) val.getExpression();
					//OpaqueExpression expr = (OpaqueExpression) ((ValueSpecificationAction) valueInput).getValue();
					String expression = getStringValue(((ValueSpecificationAction) valueInput).getValue());	//"";
					// FIXME What happens if this is multi-line? Would break matlab script anyway...
					//for (String body : expr.getBodies())
					//	expression += body;
					
					if (!expression.equals(""))
						return expression;
					
					// Common mistake: value = name
					return ((ValueSpecificationAction) valueInput).getName();
				}
				else if (valueInput instanceof ReadStructuralFeatureAction) {		// Input from reading a structural feature
					// Then need to navigate to object
					if (((ReadStructuralFeatureAction) valueInput).getStructuralFeature() instanceof Property) {
						Property structFeature = (Property) ((ReadStructuralFeatureAction) valueInput).getStructuralFeature();
						
						// Possibly more than one object...
						// FIXME Is this already the correct property? Why is the read self necessary then?
						
						String expression = getDefaultValue(structFeature);
						
						if(!expression.equals(""))
							expression = MiniOCLInterpretor.interpretExpression(structFeature, expression);
						
						return expression;
					}
					// Otherwise something is wrong...
				}
			}
		}
		
		return value;
	}
	
	/**
	 * Returns a list of non-shareable resources.
	 * 
	 * @param workingPrinciples A list of working principles.
	 * @param compositeResources A list of aggregate resources.
	 * @param allRelevantResources A list of all relevant resources.
	 * @return A list of non-shareable resource types.
	 */
	public static ArrayList<Element> getNonShareableResources(ArrayList<Activity> workingPrinciples,
			ArrayList<Element> compositeResources,
			ArrayList<Element> allRelevantResources) {
		ArrayList<Element> res = new ArrayList<Element>();
		
		// Go through working principles and composite resources
		for (Activity act : workingPrinciples) {
			if (act.getAllAttributes() != null) {
				for (Property p : act.getAllAttributes()) {
					if (p.isComposite()
							&& isResource(p.getType())
							&& allRelevantResources.contains(p.getType())
							&& (isWorkingPrinciple(p.getOwner())
									|| isResource(p.getOwner()))) {
						logger.trace("Looks like " + p.getName() + " is non-shared (type " + p.getType().getName() + ")");
						
						res.add(p.getType());
					}
				}
			}
		}
		
		for (Element compositeRes : compositeResources) {
			Classifier compositeResource = (Classifier) compositeRes;
			
			if (compositeResource.getAllAttributes() != null) {
				for (Property p : compositeResource.getAllAttributes()) {
					if (p.isComposite()
							&& isResource(p.getType())
							&& allRelevantResources.contains(p.getType())
							&& (isWorkingPrinciple(p.getOwner())
									|| isResource(p.getOwner()))) {
						logger.trace("Looks like " + p.getName() + " is non-shared (type " + p.getType().getName() + ")");
						
						res.add(p.getType());
					}
				}
			}
		}
		
		return res;
	}
	
	/**
	 * Returns the composite / aggregate parts of a resource, if any.
	 * <P>
	 * This function returns all of the composite parts of a composite resource.
	 * If the resource is not a composite resource, an empty arraylist is returned.
	 * 
	 * @param e The resource to analyzed.
	 * @return A potentially empty list of resources that are a part of the
	 * 		input resource (in which case the input is an aggregate resource).
	 */
	public static ArrayList<Element> getResourceParts(Element e) {
		ArrayList<Element> composites = new ArrayList<Element>();
		
		// FIXME The way redefined properties are treated still seems a little shaky
		ArrayList<Element> ownedAndInheritedProperties = new ArrayList<Element>();
		ownedAndInheritedProperties.addAll(e.getOwnedElements());
		ownedAndInheritedProperties.addAll(((Classifier) e).getInheritedMembers());
		
		// Skip redefined properties in inherited
		ArrayList<Property> skip = new ArrayList<Property>();
		
		if (e.getOwnedElements() != null) {
			for (Element o : ownedAndInheritedProperties) {
				if (o instanceof Property
						&& isResource(((Property) o).getType())
						&& !skip.contains(o)) {		// Skip properties that have been redefined
					composites.add(((Property) o).getType());
					
					if (((Property) o).getRedefinedProperties() != null
							&& !((Property) o).getRedefinedProperties().isEmpty()) {
						for (Property toSkip : ((Property) o).getRedefinedProperties()) {
							skip.add(toSkip);
						}
					}
				}
			}
		}
		
		return composites;
	}
	
	/**
	 * Returns a list of resources associated with a particular set of working principles
	 * that are associated with a given activity.
	 * <p>
	 * Note that the returned list contains <emph>all</emph> related resources, at any level of
	 * composition and whether abstract or not.
	 * 
	 * @param workingPrinciples The working principles as concrete implementations of the activity
	 * @return A list of resources that are specified as part of the working principles
	 */
	public static HashSet<Element> getResourceTypes(ArrayList<Activity> workingPrinciples) {
		HashSet<Element> res = new HashSet<Element>();
		
		for (Activity wp : workingPrinciples) {
			if (wp.getOwnedAttributes() != null) {
				for (Property p : wp.getOwnedAttributes()) {
					Type t = p.getType();
					
					if (isResource(t))
						res.add(t);
					
					// Get nested resource types, if any
					res.addAll(getAllNestedResourceTypes(t));
				}
			}
		}
		
		return res;
	}
	
	/**
	 * Returns a list of resources (potentially with duplicates) that are associated
	 * with a particular working principle.
	 * 
	 * @param workingPrinciple The working principle to check.
	 * @return A list of resources (potentially with duplicates) that are associated with
	 * 		a particular working principle.
	 */
	public static ArrayList<Element> getResourceTypesWithDuplicates(Activity workingPrinciple) {
		// TODO Shouldn't we rather collect the properties, then algorithmically do other stuff
		//		such as composites / shareability
		ArrayList<Element> res = new ArrayList<Element>();
		
		if (workingPrinciple.getOwnedAttributes() != null) {
			for (Property p : workingPrinciple.getOwnedAttributes()) {
				Type t = p.getType();
				
				if (isResource(t))
					res.add(t);
				
				// Get nested resource types, if any
				res.addAll(getAllNestedResourceTypes(t));
			}
		}
		
		return res;
	}
	
	/**
	 * Returns a list of shareable resources.
	 * 
	 * @param workingPrinciples A list of working principles.
	 * @param compositeResources A list of composite / aggregate resources)
	 * @param allRelevantResources A list of all relevant resources.
	 * @return A list of shareable resources.
	 */
	public static ArrayList<Element> getShareableResources(ArrayList<Activity> workingPrinciples,
			ArrayList<Element> compositeResources,
			ArrayList<Element> allRelevantResources) {
		ArrayList<Element> res = new ArrayList<Element>();
		
		// Go through working principles and composite resources
		for (Activity act : workingPrinciples) {
			if (act.getAllAttributes() != null) {
				for (Property p : act.getAllAttributes()) {
					if (!p.isComposite()
							&& isResource(p.getType())
							&& allRelevantResources.contains(p.getType())
							&& (isWorkingPrinciple(p.getOwner())
									|| isResource(p.getOwner()))) {
						logger.trace("Looks like " + p.getName() + " is shared (type " + p.getType().getName() + ")");
						
						res.add(p.getType());
					}
				}
			}
		}
		
		for (Element compositeRes : compositeResources) {
			Classifier compositeResource = (Classifier) compositeRes;
			
			if (compositeResource.getAllAttributes() != null) {
				for (Property p : compositeResource.getAllAttributes()) {
					if (!p.isComposite()
							&& isResource(p.getType())
							&& allRelevantResources.contains(p.getType())
							&& (isWorkingPrinciple(p.getOwner())
									|| isResource(p.getOwner()))) {
						logger.trace("Looks like " + p.getName() + " is shared (type " + p.getType().getName() + ")");
						
						res.add(p.getType());
					}
				}
			}
		}
		
		return res;
	}
	
	
	/**
	 * Returns the system under design.
	 * 
	 * @param rootElement The root element of the SysML model (e.g., "Data" package)
	 * @return The system under design (as type Classifier) or null if no appropriate system
	 * 		under design was found
	 */
	public static Classifier getSystemUnderDesign(Element rootElement) {
		Classifier cell = null;
		
		if (isSystemUnderDesign(rootElement)) {
			return (Classifier) rootElement;
		}
		
		// NOTE: This will ONLY search the current project - NOT any imported model libraries!!!
		if (rootElement.getOwnedElements() != null) {
			for (Element e : rootElement.getOwnedElements()) {
				if (isSystemUnderDesign(e)) {
					return (Classifier) e;
				}
				else {
					cell = getSystemUnderDesign(e);
					
					if (cell != null)
						break;
				}
			}
		}
		
		return cell;
	}
	
	/**
	 * Collect all working principles in the project.
	 * 
	 * @param rootElement The root model element.
	 * @return A list of working principles contained in the model or an imported
	 * 		model library.
	 */
	public static ArrayList<Activity> getWorkingPrinciples(Element rootElement) {
		ArrayList<Activity> wps = new ArrayList<Activity>();
		
		if(rootElement.getOwnedElements() != null) {
			for(Element e : rootElement.getOwnedElements()) {
				if(isWorkingPrinciple(e))
					wps.add((Activity) e);
				
				wps.addAll(getWorkingPrinciples(e));
				
				// If model library...
				if (isPackageImport(e))
					wps.addAll(getWorkingPrinciples(((PackageImport) e).getImportedPackage()));
			}
		}
		
		return wps;
	}
	
	/**
	 * Returns a list of all working principles associated with a particular
	 * activity.
	 * <P>
	 * The function returns a set with the activity if the specified activity is a working
	 * principle itself. An empty set is returned if no working principles can be
	 * found that are associated with the particular activity specified.
	 * 
	 * @param act The (abstract) activity (function definition).
	 * @param workingPrinciples A list of all working principles.
	 * @return The list of working principles that specialize the given activity act.
	 */
	public static ArrayList<Activity> getWorkingPrinciples(Activity act,
			ArrayList<Activity> workingPrinciples) {
		ArrayList<Activity> wps = new ArrayList<Activity>();
		
		if(isWorkingPrinciple(act))
			wps.add(act);
		
		for(Activity wp : workingPrinciples) {
			// Go through inheritance tree to try and see whether one of the parents is "act"
			if (isInInheritanceHierarchy(act, wp))
				wps.add(wp);
		}
		
		return wps;
	}
	
	/**
	 * Checks whether a particular resource is a composite resource.
	 * <P>
	 * This function returns true or false depending on whether the specified
	 * resource is a composite resource (e.g., a robot with a gripper).
	 * 
	 * @param e The resource to check.
	 * @return true if the given resource is a composite resource, false otherwise.
	 */
	public static boolean isCompositeResource(Element e) {
		if (!isResource(e))
			return false;
		
		if (!getResourceParts(e).isEmpty())
			return true;
		
		return false;
	}
	
	/**
	 * Determine whether a given resource is directly associated with a working principle.
	 * <P>
	 * Note that this function will also search through the inheritance tree, meaning that
	 * if a more general resource is associated with a working principle, this will count
	 * as well.
	 * 
	 * @param resource The resource to check.
	 * @param workingPrinciples A list of all working principles.
	 * @return true if the specified resource is directly associated with any working
	 * 		principle, false otherwise.
	 */
	public static boolean isDirectlyAssociatedWithAnyWorkingPrinciple(Classifier resource,
			ArrayList<Activity> workingPrinciples) {
		
		for (Activity workingPrinciple : workingPrinciples) {
			ArrayList<Element> res = getAssociatedResourceTypes(workingPrinciple);
			
			for (Element assocResource : res)
				if (DSEMLUtils.isTypeOfResource((Classifier) assocResource, resource))
					return true;
		}
		
		return false;
	}
	
	/**
	 * Check whether a particular property is a duration property.
	 * 
	 * @param p The property to check.
	 * @return true if p is a duration property, false otherwise.
	 */
	public static boolean isDurationProperty(Property p) {
		if(p == null)
			return false;
		
		// If any of the stereotypes applied to this element is of type modelcenter data model, return true
		return isStereotypeApplied(p, "processDuration");
	}
	
	/**
	 * Check whether a Constraint object is an objective
	 * 
	 * @param c The constraint to check.
	 * @return true if the given constraint is an objective, false otherwise.
	 */
	public static boolean isObjective(Constraint c) {
		if(c == null)
			return false;
		
		// If any of the stereotypes applied to this element is of type modelcenter data model, return true
		return isStereotypeApplied(c, "Objective");
	}
	
	/**
	 * Checks whether a given resource is part of any composite (at any type hierarchy
	 * level).
	 * 
	 * @param resource The resource to check.
	 * @param compositeResources A list of all aggregate resources.
	 * @return true if the specified resource is part of any of the composites, false
	 * 		otherwise.
	 */
	public static boolean isPartOfAnyComposite(Classifier resource,
			ArrayList<Element> compositeResources) {
		// Go through all composite resources
		for (Element composite : compositeResources) {
			// Retrieve the parts of the composite
			ArrayList<Element> parts = getResourceParts(composite);
			
			// For each part, check whether it is the element in question, or a more general element
			for (Element part : parts)
				if (oneBaseClassifierIsSpecificClassifier(resource, (Classifier) part))
					return true;
		}
		
		return false;
	}
	
	/**
	 * Check whether an Element is a machining resource.
	 * <p>
	 * Check whether an Element is a machining resource by going through the list of
	 * applied stereotypes, and checking for the "Resource" stereotype.
	 * 
	 * @param element The element to check.
	 * @return true if the Element is a machining resource, false otherwise.
	 */
	public static boolean isResource(Element e) {
		if (e == null)
			return false;
		
		// "Machine" still checked for for backwards compatibility
		return (isStereotypeApplied(e, "Resource") || isStereotypeApplied(e, "Machine"));
	}
	
	/**
	 * Checks whether a particular resource is relevant within the context of a specific
	 * process / functional specification.
	 * 
	 * @param resource The resource to query for
	 * @param topLevelActivity The top level activity
	 * @param rootElement The root element of the model tree
	 * @return true if the resource is relevant, false otherwise
	 */
	public static boolean isResourceRelevant(Element resource,
			Activity topLevelActivity,
			Element rootElement) {
		boolean found = false;
		
		ArrayList<Activity> allWPs = getWorkingPrinciples(rootElement);
		
		// TODO We can optimize this by caching the relevant resources and then checking whether the
		//      resource queried for is a part of that set.
		// TODO A little slow currently... goes through activities twice or more potentially
		
		// Collect activities - order is not important
		ArrayList<Activity> activities = collectSubActivities(topLevelActivity);
		
		for (Activity act : activities) {
		//for (Element cba : topLevelActivity.getOwnedElement()) {
			//if (cba instanceof CallBehaviorAction) {
				// TODO Nested activities
				//Activity act = (Activity) ((CallBehaviorAction) cba).getBehavior();
				
			// Find working principles associated with it
			ArrayList<Activity> workingPrinciples = getWorkingPrinciples(act, allWPs);
			
			// Find resources associated with it
			HashSet<Element> resources = getResourceTypes(workingPrinciples);
			
			// Check whether it directly contains the resource
			if (resources.contains(resource))
				return true;
			
			// Otherwise do a check with the oneBaseClassifier function
			for (Element r : resources) {
				if (resource instanceof Classifier && r instanceof Classifier) {
					if (oneBaseClassifierIsSpecificClassifier((Classifier) resource, (Classifier) r)
							|| oneBaseClassifierIsSpecificClassifier((Classifier) r, (Classifier) resource))
						return true;
				}
			}
			//}
		//}
		}
		
		return found;
	}

	/**
	 * Check whether an Element is a system under design.
	 * 
	 * @param element The element to check.
	 * @return true if the Element is a system under design, false otherwise.
	 */
	public static boolean isSystemUnderDesign(Element e) {
		if (e == null)
			return false;
		
		if (!(e instanceof Classifier))
			return false;
		
		// CHANGE 2015-07-01: Changed from stereotype to model library approach
		//return isStereotypeApplied(e, "SystemUnderDesign");
		
		boolean isSystemUnderDesign = false;
		
		// Library element
		isSystemUnderDesign |= ((Classifier) e).isAbstract() && ((Classifier) e).getName().equals("GenericSystemUnderDesign");
		
		// Go through super types
		if (!isSystemUnderDesign && ((Classifier) e).getGenerals() != null) {
			for (Classifier c : ((Classifier) e).getGenerals()) {
				isSystemUnderDesign |= isSystemUnderDesign(c);
				
				if (isSystemUnderDesign)
					break;
			}
		}
		
		return isSystemUnderDesign;
	}
	
	/**
	 * Check whether a particular property is a property whose value will
	 * be the estimated throughput.
	 * 
	 * @param p The property to check.
	 * @return true if the property is a throughput property, false otherwise.
	 */
	public static boolean isThroughputProperty(Property p) {
		if(p == null)
			return false;
		
		// If any of the stereotypes applied to this element is of type modelcenter data model, return true
		return isStereotypeApplied(p, "throughput");
	}
	
	/**
	 * Checks whether two resources are equal or at least somehow related.
	 * 
	 * @param abstr The abstract resource (relative to the other).
	 * @param concrete The concrete resource (relative to the other).
	 * @return true if the assumed abstract / concrete relation holds, false
	 * 		otherwise.
	 */
	public static boolean isTypeOfResource(Classifier abstr, Classifier concrete) {
		return oneBaseClassifierIsSpecificClassifier(concrete, abstr);
	}
	
	/**
	 * Checks whether the given property is a SysML value property.
	 * 
	 * @param p The property to check.
	 * @return true if the specified property is a SysML value property, false
	 * 		otherwise.
	 */
	public static boolean isValueProperty(Element p) {
		if (p == null)
			return false;
		
		//if (!(p instanceof Property)) //isStereotypeApplied(p, "ValueProperty");
		//	return false;
		
		if (!(p instanceof Property))
			return false;
		
		// FIXME Need something better for this
		if (((Property) p).getType() != null
				&& !isResource(((Property) p).getType())
				&& !isWorkingPrinciple(((Property) p).getType())
				&& !isWorkpiece(((Property) p).getType()))
			return true;
		
		return false;
	}

	/**
	 * Checks whether the given element is stereotyped with
	 * "WorkingPrinciple".
	 * 
	 * @param e The element to check.
	 * @return true if the given element is a working principle, false otherwise.
	 */
	public static boolean isWorkingPrinciple(Element e) {
		if (e == null)
			return false;
		
		// Updated profile: working principles have to be activities (by definition)
		if (!(e instanceof Activity))
			return false;
		
		return isStereotypeApplied(e, "WorkingPrinciple");
	}
	
	/**
	 * Checks whether the given element is stereotyped with
	 * "WorkingPrinciple" at some level of inheritance.
	 * <P>
	 * Note: this function searches from general to specific
	 * 
	 * @param e The element to check.
	 * @param workingPrinciples A list of working principles.
	 * @return true if the element is a working principle at
	 * 		some level of inheritance, false otherwise.
	 */
	public static boolean isWorkingPrincipleAtSomeLevel(Element e,
			ArrayList<Activity> workingPrinciples) {
		boolean isWorkingPrinciple = false;
		
		if (e == null)
			return false;
		
		// Cannot be a working principle if not an activity
		if (!(e instanceof Activity))
			return false;
		
		if (isWorkingPrinciple(e))
			return true;
		
		// Check inheritance hierarchy
		if (getWorkingPrinciples((Activity) e, workingPrinciples).size() > 0)
			return true;
		
		return isWorkingPrinciple;
	}
	
	/**
	 * Checks whether the given element is a workpiece.
	 * 
	 * @param e The element to check.
	 * @return true if the specified element is a workpiece,
	 * 		false otherwise.
	 */
	public static boolean isWorkpiece(Element e) {
		if (e == null)
			return false;
		
		// Cannot be a workpiece if not a classifier
		if (!(e instanceof Classifier))
			return false;
		
		// CHANGE 2015-07-01: Changed from stereotype to model library approach
		//if (isStereotypeApplied(e, "Workpiece"))
			//return true;
		
		boolean isWorkpiece = false;
		
		// Library element
		isWorkpiece |= ((Classifier) e).isAbstract() && ((Classifier) e).getName().equals("GenericWorkpiece");
		
		// Go through super types
		if (!isWorkpiece && ((Classifier) e).getGenerals() != null) {
			for (Classifier c : ((Classifier) e).getGenerals()) {
				isWorkpiece |= isWorkpiece(c);
				
				if (isWorkpiece)
					break;
			}
		}
		
		return isWorkpiece;
	}
	
	/**
	 * Check whether an Element is a machining resource.
	 * <p>
	 * Check whether an Element is a machining resource by going through the list of
	 * applied stereotypes, and checking for the "Resource" stereotype.
	 * 
	 * @param e The element to check
	 * @return true if e is a machining resource at some level of inheritance, false
	 * 		otherwise.
	 */
	public static boolean oneBaseClassifierIsResource(Element e) {
		boolean isResource = false;
		
		if(e == null)
			return false;
		
		if(e instanceof Classifier && isAbstract((Classifier) e) && isResource(e))
			return true;
		
		if(((Classifier)e).getGenerals() == null)
			return false;
		
		// If any of the stereotypes applied to this element is "Resource", then 
		for(Classifier c : ((Classifier)e).getGenerals()) {
			isResource |= oneBaseClassifierIsResource(c);
			
			// Optimization: just return true instead of searching through rest
			if(isResource)
				return true;
		}
		
		return isResource;
	}
	
}
