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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.AggregationKind;
import org.eclipse.uml2.uml.Association;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Generalization;
import org.eclipse.uml2.uml.InstanceSpecification;
import org.eclipse.uml2.uml.InstanceValue;
import org.eclipse.uml2.uml.LiteralBoolean;
import org.eclipse.uml2.uml.LiteralInteger;
import org.eclipse.uml2.uml.LiteralReal;
import org.eclipse.uml2.uml.LiteralString;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Namespace;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.OpaqueExpression;
import org.eclipse.uml2.uml.PackageImport;
import org.eclipse.uml2.uml.Parameter;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.StructuredClassifier;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.ValueSpecification;
import org.eclipse.uml2.uml.internal.impl.LiteralSpecificationImpl;
import org.eclipse.uml2.uml.internal.impl.LiteralStringImpl;
import org.eclipse.uml2.uml.internal.impl.UMLFactoryImpl;

/**
 * Collection of functions to ease querying and computing sets of elements from a
 * UML model (e.g., retrieval of all constraints that are owned and inherited).
 * 
 * @author Sebastian
 * @version 0.1
 */
public class UMLModelUtils {

	/** Log4J object. */
	private static final Logger logger = LogManager.getLogger(UMLModelUtils.class.getName());
	
	/**
	 * Returns the constraints associated with a particular activity / working principle.
	 * <P>
	 * This will return all constraints associated with a working principle at any level of
	 * inheritance.
	 * 
	 * @param workingPrinciple The working principle to collect constraints from.
	 * @return A set of inherited and owned constraints of an activity.
	 */
	public static ArrayList<Constraint> collectConstraints(Activity workingPrinciple) {
		ArrayList<Constraint> constraints = new ArrayList<Constraint>();
		
		// Owned constraints
		for (NamedElement n : workingPrinciple.getOwnedMembers()) {
			if (n instanceof Constraint) {
				constraints.add((Constraint) n);
			}
		}
		
		// Inherited constraints
		for (NamedElement n : workingPrinciple.getInheritedMembers()) {
			if (n instanceof Constraint) {
				constraints.add((Constraint) n);
			}
		}
		
		return constraints;
	}
	
	/**
	 * Parses an activity and, for each call behavior action, extracts the respective behavior
	 * iteratively.
	 * 
	 * @param activity The activity to start from.
	 * @return An ordered list of UML Activity instances.
	 */
	public static ArrayList<Activity> collectSubActivities(Activity activity) {
		ArrayList<Activity> activities = new ArrayList<Activity>();
		
		if (activity.getOwnedElements() != null) {
			for (Element cba : activity.getOwnedElements()) {
				if (cba instanceof CallBehaviorAction) {
					// FIXME The behavior could also be an opaque behavior
					if (((CallBehaviorAction) cba).getBehavior() instanceof Activity) {
						Activity subActivity = (Activity) ((CallBehaviorAction) cba).getBehavior();
						activities.add(subActivity);
						activities.addAll(collectSubActivities(subActivity));
					}
				}
			}
		}
		
		return activities;
	}
	
	/**
	 * Create a directed association.
	 * <P>
	 * Note that the association is automatically added to the model at the relevant
	 * location.
	 * 
	 * @param parent The parent of the association (source).
	 * @param child The child of the association (target).
	 * @return The newly created association.
	 */
	public static Association createDirectedAssociation(Classifier parent, Classifier child) {
		String endName = child.getName();
		endName = endName.substring(0, 1).toLowerCase() + endName.substring(1);
		
		// Also sets owner as nearest package of 'parent'
		return parent.createAssociation(true,
				AggregationKind.COMPOSITE_LITERAL,
				endName,		// camelCase version of ClassName
				1,		// Lower
				1,		// Upper
				(Type) child,
				false,
				AggregationKind.NONE_LITERAL,
				"", 
				1, 		// Lower
				1);		// Upper
	}
	
	/**
	 * Create a class, owned by the specified package.
	 * 
	 * @param name The name of the new class.
	 * @param owner The owning package.
	 * @return The newly created class.
	 */
	public static Class createClass(String name, Package owner) {
		return owner.createOwnedClass(name, false);
	}
	
	/**
	 * Create a generalization relationship between two classifiers.
	 * <P>
	 * Note that the created generalization is automatically added to the model. Papyrus
	 * will make the specific classifier the owner of the generalization relationship.
	 * 
	 * @param specific The specific element.
	 * @param general The general element.
	 * @return The newly created generalization.
	 */
	public static Generalization createGeneralization(Classifier specific, Classifier general) {
		// TODO Test whether this is added to the model?
		return specific.createGeneralization(general);
	}
	
	/**
	 * Create an instance of a Classifier.
	 * <P>
	 * Note that this function is intended for future use - currently, instances are not created
	 * properly using this function, and the function is not called anywhere.
	 * 
	 * @param name The name of the instance.
	 * @param toInstantiate The classifier to instantiate.
	 * @param owner The owning package of the instance.
	 * @return The newly created InstanceSpecification object.
	 */
	public static InstanceSpecification createInstance(
			String name, 
			Classifier toInstantiate, 
			Package owner) {
		// Create instance
		InstanceSpecification instance = UMLFactory.eINSTANCE.createInstanceSpecification();
		instance.getClassifiers().add(toInstantiate);		// Sets classifier
		
		// Add slot values
		// TODO Implement
		
		return instance;
	}
	
	/**
	 * Create a package, owned by the specified package.
	 * 
	 * @param name The name of the new package.
	 * @param owner The owning package.
	 * @return The newly created package.
	 */
	public static Package createPackage(String name, Package owner) {
		return owner.createNestedPackage(name);
	}
	
	/**
	 * Create a new property.
	 * 
	 * @param name Name of the property.
	 * @param type Type associated with the property.
	 * @param owner Owning element.
	 * @return The newly created property.
	 */
	public static Property createProperty(String name, 
			Type type, 
			StructuredClassifier owner) {
		return owner.createOwnedAttribute(name, type);
	}
	
	/**
	 * Performs a depth first search to try and find an element with the given qualified name.
	 * 
	 * @param qualifiedName The qualified name of the element to search for.
	 * @param startingPoint The starting point (usually the root model element).
	 * @return The element if found, null otherwise.
	 */
	public static Element findElementByQualifiedName(
			String qualifiedName, 
			Namespace startingPoint) {
		if (startingPoint == null)
			return null;
		
		if (startingPoint.getQualifiedName() == null)
			return null;
		
		if (startingPoint.getQualifiedName().equals(qualifiedName))
			return startingPoint;
		
		if (startingPoint.getMembers() != null) {
			for (NamedElement ne : startingPoint.getMembers()) {
				if (ne instanceof Namespace) {
					Element el = findElementByQualifiedName(qualifiedName, (Namespace) ne);
					
					if (el != null)
						return el;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Performs a depth first search to try and find a classifier with the given
	 * qualified name.
	 * <P>
	 * This function is similar to {@link #findElementByQualifiedName(String, Namespace)}
	 * but specifically searches for an instance of a UML classifier rather than an
	 * element.
	 * 
	 * @param qualifiedName The qualified name of the element.
	 * @param startingPoint The starting point from which to start the search on.
	 * @return The classifier if found, null otherwise.
	 */
	public static Classifier findClassifierByQualifiedName(
			String qualifiedName, 
			Namespace startingPoint) {
		if (startingPoint == null)
			return null;
		
		if (startingPoint.getQualifiedName() == null)
			return null;
		
		if (startingPoint instanceof Classifier
				&& startingPoint.getQualifiedName().equals(qualifiedName))
			return (Classifier) startingPoint;
		
		if (startingPoint.getMembers() != null) {
			for (NamedElement ne : startingPoint.getMembers()) {
				if (ne instanceof Namespace) {
					Element el = findClassifierByQualifiedName(qualifiedName, (Namespace) ne);
					
					if (el != null
							&& el instanceof Classifier)
						return (Classifier) el;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Remove an element and its owned members.
	 * 
	 * @param element The element to delete.
	 */
	public static void deleteElementsRecursively(Element element) {
		element.destroy();
	}
	
	/**
	 * Performs a depth first search to try and find an element with the given qualified name.
	 * 
	 * @param qualifiedName The qualified name of the element to search for.
	 * @param startingPoint Starting point to start the search from.
	 * @return The classifier if found, otherwise null.
	 */
	public static Classifier findFirstClassifierByName(
			String name, 
			Namespace startingPoint) {
		if (startingPoint instanceof Classifier
				&& startingPoint.getName().equals(name))
			return (Classifier) startingPoint;
		
		if (startingPoint.getMembers() != null) {
			for (NamedElement ne : startingPoint.getMembers()) {
				if (ne instanceof Namespace) {
					Element el = findFirstClassifierByName(name, (Namespace) ne);
					
					if (el != null
							&& el instanceof Classifier)
						return (Classifier) el;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Finds a stereotype by name from a list of stereotypes.
	 * 
	 * @param name The name of the stereotype to search for.
	 * @param stereotypes A list of stereotypes.
	 * @return The stereotype if found, otherwise null.
	 */
	public static Stereotype findStereotypeByName(String name, List<Stereotype> stereotypes) {
		for (Stereotype stereotype : stereotypes)
			if (stereotype.getName().equals(name))
				return stereotype;
		
		return null;
	}
	
	/**
	 * Retrieve the default value associated with a property.
	 * 
	 * @param p The property to retrieve the default value from.
	 * @return The default value as a string if it exists, otherwise
	 * 		null.
	 */
	public static String getDefaultValue(Property p) {
		if(p == null 
				|| (p != null && p.getDefaultValue() == null))
			return null;
		
		return getStringValue(p.getDefaultValue());
	}
	
	/**
	 * Attempts to find a string representation of a value specification.
	 * <P>
	 * This function is currently aware of LiteralString, LiteralInteger,
	 * LiteralReal, LiteralBoolean and OpaqueExpression. For any other type
	 * it will return the empty string.
	 * 
	 * @param vs The value specification.
	 * @return A string representation of the value specification or the
	 * 		empty string.
	 */
	public static String getStringValue(ValueSpecification vs) {
		if (vs instanceof LiteralString) {
			LiteralString v = (LiteralString) vs;
			logger.trace("Default value: " + v.getValue());
			return v.getValue();
		}
		else if (vs instanceof LiteralInteger) {
			LiteralInteger v = (LiteralInteger) vs;
			logger.trace("Default value: " + v.getValue());
			return v.getValue() + "";
		}
		else if (vs instanceof LiteralReal) {
			LiteralReal v = (LiteralReal) vs;
			logger.trace("Default value: " + v.getValue());
			return v.getValue() + "";
		}
		else if (vs instanceof LiteralBoolean) {
			LiteralBoolean v = (LiteralBoolean) vs;
			logger.trace("Default value: " + v.isValue());
			return v.isValue() + "";
		}
		else if (vs instanceof OpaqueExpression) {
			String body = "";
			for (String b : ((OpaqueExpression) vs).getBodies())
				body += b;
			return body;
		}
		
		return "";
	}
	
	/**
	 * Extracts a property of a classifier.
	 * <P>
	 * This routine goes through the set of owned properties and returns the one with
	 * the specified name. If the property was not found in the set of owned properties,
	 * the search is repeated by going through the set of inherited properties as well.
	 * 
	 * @param propertyName Name of the property.
	 * @return The UML Property object if found, null otherwise.
	 */
	public static Property getProperty(Element parent, String propertyName) {
		// FIXME Should probably go through allAttributes() instead
		
		if (parent == null)
			return null;
		
		if (parent.getOwnedElements() == null)
			return null;
		
		for (Element e : parent.getOwnedElements()) {
			if (e instanceof Property
					&& ((Property) e).getName().toLowerCase().equals(propertyName.toLowerCase())) {
				return (Property) e;
			}
		}
		
		// If the property is not owned, check whether it was inherited
		// FIXME This may return duplicates - check this
		if (parent instanceof Classifier) {
			for (Element e : ((Classifier) parent).getInheritedMembers()) {
				if (e instanceof Property
						&& ((Property) e).getName().toLowerCase().equals(propertyName.toLowerCase())) {
					logger.trace("Returning property " + ((Property) e).getName());
					return (Property) e;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Extracts a parameter from an activity.
	 * <P>
	 * Note that this function will also search through the inherited members. This is
	 * not compliant with the UML spec, but is useful at least within our context.
	 * 
	 * @param parent The parent activity.
	 * @param parameterName The name of the parameter.
	 * @return The parameter object or null if not found.
	 */
	public static Parameter getParameter(Activity parent, String parameterName) {
		for (Parameter e : parent.getOwnedParameters()) {
			if (e.getName().toLowerCase().equals(parameterName.toLowerCase())) {
				logger.trace("Returning parameter " + e.getName());
				return e;
			}
		}
		
		// If not owned, then maybe inherited
		for (NamedElement e : parent.getInheritedMembers()) {
			if (e instanceof Parameter
					&& e.getName().toLowerCase().equals(parameterName.toLowerCase())) {
				logger.trace("Returning inherited parameter " + e.getName());
				return (Parameter) e;
			}
		}
		
		// Otherwise, if all fails, do a breadth first search for inherited parameters
		Collection<Classifier> nextItems = parent.getGenerals();
		return searchParametersBreadthFirst(nextItems, parameterName);
	}
	
	/**
	 * Do a breadth first search for an "inherited" activity parameter.
	 * 
	 * @param nextItems Next items to search through.
	 * @param parameterName The name of the parameter to search for.
	 * @return The parameter if found or null otherwise.
	 */
	private static Parameter searchParametersBreadthFirst(
			Collection<Classifier> nextItems,
			String parameterName) {
		HashSet<Classifier> nextNextItems = new HashSet<Classifier>();
		
		if (nextItems != null) {
			for (Classifier a : nextItems) {
				if (a instanceof Activity) {
					for (Parameter e : ((Activity) a).getOwnedParameters()) {
						if (e.getName().toLowerCase().equals(parameterName.toLowerCase())) {
							logger.trace("Returning parameter " + e.getName());
							return e;
						}
					}
					
					if (a.getGenerals() != null) {
						nextNextItems.addAll(a.getGenerals());
					}
				}
			}
		}

		if (nextNextItems != null && nextNextItems.size() > 0)
			return searchParametersBreadthFirst(nextNextItems, parameterName);

		return null;
	}
	
	/** Wrapper function for {@link Classifier#isAbstract()}. */
	public static boolean isAbstract(Classifier c) {
		return c.isAbstract();
	}
	
	
	/**
	 * Search the inheritance tree to find out whether a particular working principle
	 * is a concrete implementation of a particular activity.
	 * 
	 * @param act The activity.
	 * @param wp The working principle.
	 * @return true if the working principle is an implementation fo the activity, false
	 * 		if it is not.
	 */
	public static boolean isInInheritanceHierarchy(
			Activity act,
			Activity wp) {
		// FIXME Probably should generalize to Classifier
		
		return oneBaseClassifierIsSpecificClassifier(wp, act);
	}
	
	/**
	 * Checks whether the given element is a package import.
	 * 
	 * @param e The element to check.
	 * @return true if e is an instance of UML PackageImport, false otherwise.
	 */
	public static boolean isPackageImport(Element e) {
		return (e instanceof PackageImport);
	}
	
	/**
	 * Checks whether a stereotype of a given name is applied to an element.
	 * 
	 * @param e The element to check.
	 * @param stereotypeName The name of the stereotype to check for.
	 * @return true if a stereotype with the given name is applied to the element,
	 * 		false otherwise.
	 */
	public static boolean isStereotypeApplied(Element e, String stereotypeName) {
		if (e == null)
			return false;
		
		for (Stereotype s : e.getAppliedStereotypes())
			if (s.getName().equals(stereotypeName))
				return true;
		
		return false;
	}

	/**
	 * Performs a depth first search to identify whether a particular classifier 'specific' inherits
	 * from a classifier 'general' at some level.
	 * 
	 * @param specific The specific classifier.
	 * @param general The general classifier.
	 * @return true if the specific classifier inherits from the specified general classifier at
	 * 		some level.
	 */
	public static boolean oneBaseClassifierIsSpecificClassifier(Classifier specific, Classifier general) {
		boolean isResource = false;
		
		if (specific == null)
			return false;
		
		if (specific == general)
			return true;
		
		if (((Classifier) specific).getGenerals() == null)
			return false;
		
		// If any of the stereotypes applied to this element is "Resource", then 
		for (Classifier c : ((Classifier) specific).getGenerals()) {
			isResource |= oneBaseClassifierIsSpecificClassifier(c, general);
			
			// Optimization: just return true instead of searching through rest
			if(isResource)
				return true;
		}
		
		return isResource;
	}
	
	/**
	 * Adds and redefines value properties.
	 * <P>
	 * Walks through the inheritance tree and adds and redefines all owned and inherited
	 * value properties.
	 * 
	 * @param c The classifier to redefine properties in.
	 */
	public static void redefineInheritedProperties(Classifier c) {
		// Collect all inherited properties, redefine
		for(NamedElement e : c.getInheritedMembers()) {
			logger.trace("Inherited member is: " + e.getName());
			
			// Check whether element is a value property
			if(e instanceof Property
					&& isNotOwnedMember(e, c)) {
				// Get elements factory
				//ElementsFactory elementsFactory = Application.getInstance().getProject().getElementsFactory();
				
				// Property instance
				//Property newProperty = elementsFactory.createPropertyInstance();
				Property newProperty = ((StructuredClassifier) c).createOwnedAttribute(e.getName(), ((Property) e).getType());
				
				for (Stereotype s : ((Property) e).getAppliedStereotypes())
					newProperty.applyStereotype(s);
				//StereotypesHelper.addStereotype(newProperty, MDSysMLModelHandler.getStereotypeSysMLValueProperty());
				
				// Name & type
				newProperty.setName(e.getName());
				newProperty.setType(((Property) e).getType());
				
				// TODO Multiplicity -> not sure why this doesn't work
				newProperty.setUpper(((Property) e).getUpper());
				newProperty.setLower(((Property) e).getLower());
				
				// Add to parent
				//c.getAttributes().add(newProperty);
				
				// Set redefinition context
				//newProperty.getRedefinitionContexts().add(c);
				newProperty.getRedefinedProperties().add((Property) e);
				//if(!newProperty.getRedefinedElements().contains(e))
				//	newProperty.getRedefinedElements().add((RedefinableElement) e);
				
				// Visibility: private, public or protected
				newProperty.setVisibility(((Property) e).getVisibility());
				
				// Set aggregation kind (leads to Papyrus validation error otherwise!)
				newProperty.setAggregation(((Property) e).getAggregation());
				
				// Keep derived, if no default value
				if (newProperty.getDefault() == null
						|| (newProperty.getDefault() != null && (newProperty.getDefault().equals("") || newProperty.getDefault().equals("null")))) {
					newProperty.setIsDerived(((Property) e).isDerived());
					newProperty.setIsDerivedUnion(((Property) e).isDerivedUnion());
				}
			}
		}
	}

	/**
	 * Checks whether a given property (NamedElement p) is not owned by c. This is done by
	 * checking whether there is an owned property that redefines the given property.
	 * 
	 * @param e The property.
	 * @param c A classifier.
	 * @return true if e is not owned by c, false otherwise.
	 */
	public static boolean isNotOwnedMember(NamedElement p, Classifier c) {
		for(NamedElement e : c.getOwnedMembers()) {
			if (e instanceof Property
					&& ((Property) e).getRedefinitionContexts().contains(c)
					&& ((Property) e).getRedefinedProperties().contains(p))
				return false;
		}
		
		return true;
	}
	
	/**
	 * Sets the default value of a {@link Property}.
	 * 
	 * @param property The property to set the default value for.
	 * @param defaultValue The desired default value represented as a string.
	 */
	public static void setDefaultValue(Property property, String defaultValue) {
		property.setDefault(defaultValue);
	}

}
