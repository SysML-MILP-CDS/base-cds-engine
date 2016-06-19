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
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.Association;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.ObjectFlow;
import org.eclipse.uml2.uml.PackageImport;

import edu.gatech.mbse.transformations.sysml2milp.utils.DSEMLUtils;
import edu.gatech.mbse.transformations.sysml2milp.utils.UMLModelUtils;

/**
 * Class containing several static fields and getters and setters for lists of
 * elements that are often required in the transformation, but require a complete
 * traversal of the model. The functions in this class are meant to pre-compute and
 * cache these lists (e.g., relevant concrete resources, relevant working
 * principles, and all object flows).
 * 
 * @author Sebastian
 * @version 0.1
 */
public class TransformationCache {

	/** Log4J object. */
	private static final Logger logger = LogManager.getLogger(TransformationCache.class.getName());
	
	/** Pre-computed and cached list of object flows. */
	private static ArrayList<ObjectFlow> objectFlowList = null;
	
	/** List of working principles. */
	private static ArrayList<Activity> allWorkingPrinciples = null;
	
	/** List of concrete resources. */
	private static ArrayList<Element> resources;
	
	/** List of all resources. */
	private static ArrayList<Element> allResources;
	
	/** List of composite resources. */
	private static ArrayList<Element> compositeResources;
	
	/**
	 * Clear all cached mappings and collections / sets.
	 */
	public static void clearCache() {
		objectFlowList = new ArrayList<ObjectFlow>();
		allWorkingPrinciples = new ArrayList<Activity>();
		resources = new ArrayList<Element>();
		allResources = new ArrayList<Element>();
		compositeResources = new ArrayList<Element>();
	}
	
	/**
	 * Rebuild all cached mappings and collections / sets.
	 * <P>
	 * This function will rebuild the various lists. Rebuilding is performed by parsing
	 * the model, typically starting from the root model element.
	 * 
	 * @param rootElement The root model element.
	 * @param functionalSpecification The functional specification (used in determining
	 * 		relevant items)
	 */
	public static void rebuildCache(Element rootElement,
			Activity functionalSpecification) {
		objectFlowList = rebuildObjectFlowList(rootElement);
		allWorkingPrinciples = rebuildListOfAllWorkingPrinciples(rootElement);
		resources = rebuildListOfResources(rootElement, functionalSpecification);
		allResources = rebuildListOfAllResources(rootElement, functionalSpecification);
		compositeResources = rebuildListOfCompositeResources(rootElement);
	}
	
	/**
	 * @return the objectFlowList
	 */
	public static ArrayList<ObjectFlow> getObjectFlowList() {
		return objectFlowList;
	}
	
	/**
	 * @return the allWorkingPrinciples
	 */
	public static ArrayList<Activity> getAllWorkingPrinciples() {
		return allWorkingPrinciples;
	}

	/**
	 * @return the resources
	 */
	public static ArrayList<Element> getResources() {
		return resources;
	}

	/**
	 * @return the allResources
	 */
	public static ArrayList<Element> getAllResources() {
		return allResources;
	}

	/**
	 * @return the compositeResources
	 */
	public static ArrayList<Element> getCompositeResources() {
		return compositeResources;
	}

	/**
	 * Rebuild the cache for the list of associations.
	 * 
	 * @param rootElement The root model element.
	 * @return A list of association relationships contained in the model.
	 * @deprecated Have gotten rid of dependence on this.
	 */
	public static ArrayList<Association> rebuildAssociationList(Element rootElement) {
		ArrayList<Association> associations = new ArrayList<Association>();
		
		// Iterate through owned elements to find associations
		for(Element e : rootElement.getOwnedElements()) {
			// Depth first search
			if(e.getOwnedElements() != null)
				associations.addAll(rebuildAssociationList(e));
			
			// If model library...
			if (UMLModelUtils.isPackageImport(e))
				associations.addAll(rebuildAssociationList(((PackageImport) e).getImportedPackage()));
			
			if(e instanceof Association) {
				associations.add((Association) e);
			}
		}
		
		return associations;
	}
	
	/**
	 * Rebuild the list of object flows.
	 * 
	 * @param rootElement The root model element.
	 * @return An ordered list of UML ObjectFlow objects.
	 */
	public static ArrayList<ObjectFlow> rebuildObjectFlowList(Element rootElement) {
		ArrayList<ObjectFlow> objectFlows = new ArrayList<ObjectFlow>();
		
		// Iterate through owned elements to find associations
		for(Element e : rootElement.getOwnedElements()) {
			// Depth first search
			if(e.getOwnedElements() != null)
				objectFlows.addAll(rebuildObjectFlowList(e));
			
			// If model library...
			if (UMLModelUtils.isPackageImport(e))
				objectFlows.addAll(rebuildObjectFlowList(((PackageImport) e).getImportedPackage()));
			
			if(e instanceof ObjectFlow) {
				objectFlows.add((ObjectFlow) e);
			}
		}
		
		objectFlowList = objectFlows;
		
		return objectFlows;
	}
	
	/**
	 * Rebuilds list of all working principles contained in the model and library.
	 * <P>
	 * Simply calls {@link DSEMLUtils#getWorkingPrinciples(Element)}.
	 * 
	 * @param rootElement The root model element.
	 * @return An ordered list of working principles.
	 */
	public static ArrayList<Activity> rebuildListOfAllWorkingPrinciples(Element rootElement) {
		return DSEMLUtils.getWorkingPrinciples(rootElement);
	}
	
	/**
	 * Rebuilds the list of concrete resources (including composites) that are relevant
	 * within the context of a particular functional specification.
	 * <P>
	 * Simply calls {@link DSEMLUtils#getConcreteResources(Element, Activity)}.
	 * 
	 * @param rootElement The root model element.
	 * @param functionalSpecification The functional specification.
	 * @return An ordered list of UML elements representing concrete (and relevant)
	 * 		resources.
	 */
	public static ArrayList<Element> rebuildListOfResources(Element rootElement, Activity functionalSpecification) {
		return DSEMLUtils.getConcreteResources(rootElement, functionalSpecification);
	}
	
	/**
	 * Rebuilds a list of ALL resources (including abstract types) that are relevant within
	 * the context of a particular functional specification.
	 * <P>
	 * Simply calls {@link DSEMLUtils#getAllResources(Element, Activity)}.
	 * 
	 * @param rootElement The root model element.
	 * @param functionalSpecification The functional specification.
	 * @return An ordered list of abstract and non-abstract resources relevant to the functional
	 * 		specification.
	 */
	public static ArrayList<Element> rebuildListOfAllResources(Element rootElement, Activity functionalSpecification) {
		return DSEMLUtils.getAllResources(rootElement, functionalSpecification);
	}

	/**
	 * Rebuild list of composite resources.
	 * <P>
	 * A precondition for this function is that the list of concrete resources has
	 * already been rebuilt.
	 * <P>
	 * Simply calls {@link DSEMLUtils#getCompositeResources(ArrayList)} with the
	 * list of concrete, relevant resources as argument.
	 * 
	 * @param rootElement The root model element.
	 * @return A list of relevant, concrete aggregate resources.
	 */
	public static ArrayList<Element> rebuildListOfCompositeResources(Element rootElement) {
		return DSEMLUtils.getCompositeResources(getResources());
	}

}
