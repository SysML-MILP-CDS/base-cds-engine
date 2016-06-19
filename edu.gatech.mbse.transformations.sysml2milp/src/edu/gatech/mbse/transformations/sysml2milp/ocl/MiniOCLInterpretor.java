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
package edu.gatech.mbse.transformations.sysml2milp.ocl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Namespace;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.TypedElement;

import edu.gatech.mbse.transformations.sysml2milp.InternalCorrespondences;
import edu.gatech.mbse.transformations.sysml2milp.TransformationCache;
import edu.gatech.mbse.transformations.sysml2milp.TransformationState;
import edu.gatech.mbse.transformations.sysml2milp.utils.DSEMLUtils;
import edu.gatech.mbse.transformations.sysml2milp.utils.UMLModelUtils;

/**
 * Interpretor and transformer for a subset of OCL. Expressions in standard OCL format are
 * interpreted and transformed to an appropriate MILP form.
 * <P>
 * This class should be split up into a generic OCL parsing part and a part that is specific
 * to the translations to YALMIP.
 * 
 * @author Sebastian
 * @version 0.1.0
 */
public class MiniOCLInterpretor {

	/** Log4J object. */
	private static final Logger logger = LogManager.getLogger(MiniOCLInterpretor.class.getName());
	
	/** Remove in future, if possible. */
	public static Element rootModelElement = null;
	
	/**
	 * Interpret a full expression.
	 * <P>
	 * This function will interpret a full expression by first decomposing it into
	 * logical parts. Decomposition occurs according to groups, mathematical operators
	 * and properties.
	 * 
	 * @param e The element associated with the expression (e.g., a Property or Constraint).
	 * @param expression The expression to be interpreted in String form.
	 * @param context The context in which the element lies (usually the parent of e).
	 * @return An interpreted expression.
	 */
	public static String interpretExpression(Element e, String expression, Element context) {
		if(expression == null)
			return "";

		String milpCode = "";
		
		expression = treatSpecialExpressions(e, expression);
		
		// Had [^\\>] after \\-
		//Pattern pattern = Pattern.compile("((\\()|(\\))|(\\d*\\.\\d+)|(\\d+)|(\\+)|(\\>(\\=)*)|(\\<(\\=)*)|(\\-)|(\\*)|(\\/)|(((\\.*|(\\-\\>))[a-zA-Z_:0-9]+)+\\(*\\)*))");
		//Pattern pattern = Pattern.compile("((\\()|(\\))|(\\[a-zA-Z_:0-9]*\\.\\[a-zA-Z_:0-9]+)|(\\[a-zA-Z_:0-9]+)|(\\+)|(\\>(\\=)*)|(\\<(\\=)*)|(\\=\\=)|(\\-)|(\\*)|(\\/)|(((\\.*|(\\-\\>))[a-zA-Z_:0-9\\(\\)]+)+\\(*\\)*))");
		//Pattern pattern = Pattern.compile("((\\()|(\\))|([a-zA-Z_:0-9]*\\.[a-zA-Z_:0-9.]+)|([a-zA-Z_:0-9]+)|(\\+)|(\\>(\\=)*)|(\\<(\\=)*)|(\\=\\=)|(\\-)|(\\*)|(\\/)|(((\\.*|(\\-\\>))[a-zA-Z_:0-9\\(\\)]+)+\\(\\))|[a-zA-Z_:0-9]+)");

		// This regex pattern will take apart an equation into elementary pieces for interpretation - these elementary pieces are defined by:
		// logical or mathematical constants, grouping elements, and paths / operations on elements. For instance, consider the following
		// expression: Some::Qualified::Name.allInstances()->size() + (1 / (someProperty.someOtherProperty->sum() + someThirdProperty))
		// This will be split up into:
		// 1) Some::Qualified::Name.allInstances()->size()
		// 2) +
		// 3) (
		// 4) 1
		// 5) /
		// 6) (
		// 7) someProperty.someOtherProperty->sum()
		// 8) +
		// 9) someThirdProperty
		// 10) )
		// 11) )
		// Each of these sub-expressions is then processed in the interpret(...) function
		Pattern pattern = Pattern.compile("((\\+)|((\\>(\\=)*))|(\\<(\\=)*)|(\\=\\=)|(\\-((^\\\\>)))|(\\*)|(\\/)|(\\))|(\\()|(([a-zA-Z_:0-9.]+|\\-\\>|\\(\\))+)|\\-)");
		Matcher m = pattern.matcher(expression.replace(" ", ""));
		while (m.find()) {
			milpCode += interpret(e, m.group(), context);
		}
		
		return milpCode;
	}
	
	/** @see #interpretExpression(Element, String, Element) */
	public static String interpretExpression(Element e, String expression) {
		return interpretExpression(e, expression, null);
	}
	
	/**
	 * Treat special expressions.
	 * <P>
	 * This was introduced particularly for "throughput" - while it is possible to have
	 * a more flexible formulation, the computation time is very large. Hence, restrict
	 * constraints on throughput, and reformulate to avoid non-linearities.
	 * 
	 * @param expression The expression to treat.
	 * @return Treated expression with certain sub-expressions potentially rewritten.
	 */
	public static String treatSpecialExpressions(Element e, String expression) {
		if (e instanceof Constraint
				&& !DSEMLUtils.isObjective((Constraint) e)) {
			// Check whether throughput referenced
			if (expression.replace(" ", "").startsWith("throughput<")
					|| expression.replace(" ", "").startsWith("throughput>")
					|| expression.replace(" ", "").startsWith("throughput=")) {
				expression = expression.replaceFirst("throughput", "MaxBusyTime");
				
				// Switch around any ">..." or "<..." conditions
				if (expression.contains(">"))
					expression = expression.replaceFirst(">", "<");
				else if (expression.contains("<"))	// If no if-then statement, then replaced back!
					expression = expression.replaceFirst("<", ">");
				
				// Find right hand side of expression
				int term = expression.lastIndexOf("=");
				
				if (term == -1)
					term = expression.lastIndexOf("<");
				
				if (term == -1)
					term = expression.lastIndexOf(">");
				
				// Reformulate throughput >= 0.03 to MaxBusyTime <= 1 / 0.03
				expression = expression.substring(0, term + 1)
						+ " 1 / ("
						+ expression.substring(term + 2)
						+ ")";
			}
			
			// Also replace any single "=" with "==" so that YALMIP / MILP will understand them as constraints
			if (expression.contains("=")
					&& !expression.contains("==")
					&& !expression.contains("<=")
					&& !expression.contains(">="))
				expression = expression.replaceFirst("=", "==");		// Replace only first to allow for expressions like A == (B = C)
		}
		// If we are dealing with a constraint that represents an objective, we may be able to rewrite it easily
		else if (e instanceof Constraint
				&& DSEMLUtils.isObjective((Constraint) e)) {
			// If throughput is somewhere in middle or end
			if (expression.replace(" ", "").contains("+throughput")
					|| expression.replace(" ", "").contains("-throughput")) {
				// Might be dangerous if there is some decision variable that starts with "throughput"
				expression = expression.replace(" ", "").replace("+throughput", "-MaxBusyTime");
				
				if (expression.indexOf("-throughtput") > 0)
					expression = expression.replace(" ", "").replace("-throughput", "+MaxBusyTime");
				else
					expression = expression.replace(" ", "").replace("-throughput", "MaxBusyTime");
			}
			// If at beginning
			else if (expression.replace(" ", "").startsWith("throughput+")
					|| expression.replace(" ", "").startsWith("throughput-")
					|| expression.replace(" ", "").startsWith("throughput/")
					|| expression.replace(" ", "").startsWith("throughput*")) {
				expression = expression.replaceFirst("throughput", "-MaxBusyTime");
			}
		}
		
		return expression;
	}
	
	/**
	 * Interpretation of a term.
	 * <P>
	 * A term can be complex in that it can contain mathematical operators or groups.
	 * This function also navigates paths in expressions such as prop1.prop2.prop3... .
	 * 
	 * @param e The element the expression originates from (typically a constraint
	 * 		or property object).
	 * @param expression The term to interpret and transform.
	 * @return An interpreted (and transformed) expression.
	 */
	public static String interpret(Element e, String expression, Element context) {
		boolean setSelected = false;
		boolean classifierSelected = false;
		
		if(expression == null)
			return "";
		
		logger.trace("interpret() called with: " + expression);
		// Leave operators and numeric constants the same for matlab
		if (isMathematicalOperator(expression)
				|| isNumeric(expression)
				|| isGroupSymbol(expression)) {
			return expression;
		}
		else {		// Anything else must be interpreted further
			// Anything that is bla. is some path, bla-> is a set access, and bla() is an operator
			Pattern pattern = Pattern.compile("(([a-zA-Z0-9_:]+\\-\\>)|([a-zA-Z0-9_:]+[\\(]+\\.*[\\)]+)|([a-zA-Z0-9_:]+\\.*))");
			Matcher m = pattern.matcher(expression.replace(" ", ""));
			
			Element parent = e.getOwner();
			if (context != null)
				parent = context;
			
			ArrayList<Element> res = null;
			String quantifiedElement = "";
			
			Element currentElementInNavChain = e.getOwner();
			if (context != null)
				currentElementInNavChain = context;
			
			while (m.find()) {
				logger.trace("Sub-expression: " + m.group());
				
				String token = m.group();
				
				// wp = Working principle: if e is an activity, then generate string that selects active WP
				// resources = Resources: if e is working principle, then interpreted as set of selected resources
				// wp.resources.* = * is a specific property potentially present in the set of resources
				if (token.equals("{wp}.")) {
					// FIXME Set appropriate context here
				}
				else if (token.equals("{resources}.") && DSEMLUtils.isWorkingPrinciple(parent)) {
					res = DSEMLUtils.getAssociatedConcreteResources((Activity) parent, TransformationCache.getResources());
				}
				else if (token.endsWith(".")) {		// A property common to all previous reference elements (or current) (path), or a Class
					logger.trace("Just called " + token);
					
					// Access the subelement, if not self
					if (!token.replace(".", "").equals("self")) {
						TypedElement p = UMLModelUtils.getProperty(currentElementInNavChain, token.replace(".", ""));
						
						// Maybe it's a parameter...
						if (p == null && currentElementInNavChain instanceof Activity)
							p = UMLModelUtils.getParameter((Activity) currentElementInNavChain, token.replace(".", ""));
						
						if (p != null) {
							currentElementInNavChain = p.getType();
							
							classifierSelected = false;
							
							logger.trace("Navigated to " + ((NamedElement) currentElementInNavChain).getName());
						}
						else {
							Element prevElement = currentElementInNavChain;
							currentElementInNavChain = UMLModelUtils.findClassifierByQualifiedName(token.replace(".", ""), e.getModel());
							
							if (currentElementInNavChain == null) {
								// Note: this happened when a parameter name and pin name were out of sync
								logger.trace("Problem: property or class " + token.replace(".", "") + " (if property, owned (or inherited) by type " + ((NamedElement) prevElement).getName() + ") does not exist");
							}
							else
								classifierSelected = true;
						}
					}
				}
				else if (token.endsWith("->")) {	// A property common to all previous reference elements (or current) (quantification)
					quantifiedElement = token.replace("->", "");
					
					setSelected = true;
				}
				else if (token.endsWith(")")) {		// A function applied to the previous arguments
					// FIXME Currently assumes that "res" is set, and that the quantified element is a property
					if (token.equals("selected()")) {
						String toRet = "";

						// FIXME Put this into a separate function
						for (Element resource : res) {
							String defaultValue = interpret(resource, UMLModelUtils.getDefaultValue(UMLModelUtils.getProperty(resource, quantifiedElement)));
							
							if (defaultValue != null && !defaultValue.equals("") && !defaultValue.equals("null")) {
								if (!toRet.equals(""))
									toRet += " + ";
								
								ArrayList<Integer> instances = InternalCorrespondences.getResourceTypeInstanceIDMapping().get(resource);
								if (instances != null && instances.size() != 0) {
									for (int i=0; i<instances.size(); i++) {
										if (i > 0)
											toRet += " + ";
										
										// Only for current activity considered
										toRet += defaultValue + "*A(" + InternalCorrespondences.getAllocationMatrixResourceInstanceIndex(resource, i+1) + "," + TransformationState.CURRENT_ACTION_WP_COUNT + ")"; 
									}
								}
								
								logger.trace("Default value is: " + defaultValue);
							}
						}
						
						if (!toRet.equals(""))
							return "(" + toRet + ")";
					}
					else if (token.equals("sum()") && setSelected) {
						// Build sum
						logger.trace("Sum over " + quantifiedElement);
						
						// FIXME This is a bad hack -> currently assume that all composite resources have aggregate figures for mfg cell computations.
						//		 Rationale for this is that we cannot clearly identify which resources are direct aggregates of the manufacturing cell...
						boolean ignoreComposites = false;
						
						if (DSEMLUtils.isSystemUnderDesign(parent))
							ignoreComposites = true;
						
						Element navigatedOwner = currentElementInNavChain;
						currentElementInNavChain = UMLModelUtils.getProperty(currentElementInNavChain, quantifiedElement);
						
						String expr = oclBuildSetExpression(currentElementInNavChain, quantifiedElement, navigatedOwner, parent, ignoreComposites);
						
						if (!expr.equals(""))
							return expr;
					}
					else if (token.equals("size()") && setSelected) {				// Size of a set
						// Depends on what this is - allocated to working principle, resource or system under design
						// --> Use allocation matrix to determine
						// FIXME Limited support
						if (!quantifiedElement.equals(""))	// Classifier may be selected
							currentElementInNavChain = UMLModelUtils.getProperty(currentElementInNavChain, quantifiedElement);
						
						if (currentElementInNavChain instanceof Property
									&& !((Property) currentElementInNavChain).getName().toLowerCase().equals("resources"))
							logger.log(Level.WARN, "Unsupported use of size() in model for property '" + ((Property) currentElementInNavChain).getName() + "' owned by '" + ((NamedElement) parent).getName() + "'");
						else if(!(currentElementInNavChain instanceof Property)
								&& DSEMLUtils.isResource(currentElementInNavChain)) {
							// Set of instances of a class, most likely
							
							HashSet<Element> resourcesToConsider = new HashSet<Element>();
							String toRet = "";
							
							// Get any related concrete types (if constraint is on an abstract type)
							for (Element resource : TransformationCache.getResources()) {
								if (DSEMLUtils.isTypeOfResource((Classifier) currentElementInNavChain, (Classifier) resource))
									resourcesToConsider.add(resource);
							}
							
							// Externalize this part: same thing is used below
							for (Element resource : resourcesToConsider) {
								int machineID = InternalCorrespondences.getResourceTypeMachineIDMapping().get(resource);
								
								if (!toRet.equals(""))
									toRet += " + sum(";
								else
									toRet += "sum(";
								
								toRet += "IM" + machineID;
								toRet += ")";
							}
							
							return toRet;
						}
						else {
							String toRet = "";
							
							// TODO If all resource instances are referenced, then this is easy to translate: sum(any(A'))
							
							// Here we need the resources DIRECTLY associated with whatever element we are considering: if this is
							// the system under design we need those associated with the system under design: e.g., resources, this will
							// be the resources DIRECTLY associated with the working principles (e.g., some composites, but not some parts)
							// --> Or? Or should this be the elementary parts?
							
							// Cases:
							//	a) Property of SUD: references elements allocated to it (resources) from working principles
							//	b) Property of a WP: references elements allocateable to working principle (one or more columns in A) (extension)
							//	c) Property of activity: unused (extension)
							//	d) Property of composite resource: constraint on sub resources (extension)

							// Consider only immediately related resources (i.e., no parts of composites)
							HashSet<Element> resourcesToConsider = new HashSet<Element>();
							
							// Abstract resource types to consider
							for (Element resourceType : DSEMLUtils.getImmediateResourceTypes(
									TransformationCache.getAllWorkingPrinciples())) {
								resourcesToConsider.addAll(DSEMLUtils.getConcreteResources(resourceType, TransformationCache.getResources()));		// Concrete types
								
								// Also add composites
								if (DSEMLUtils.isCompositeResource(resourceType))
									resourcesToConsider.add(resourceType);
							}
							
							for (Element resource : resourcesToConsider) {
								int machineID = InternalCorrespondences.getResourceTypeMachineIDMapping().get(resource);
								
								if (!toRet.equals(""))
									toRet += " + sum(";
								else
									toRet += "sum(";
								
								toRet += "IM" + machineID;
								toRet += ")";
							}
							
							return toRet;
						}
					}
					else if(token.equals("allInstances()") && classifierSelected) {
						// Check precondition: must have selected a class / instantiable element
						
						// Set as context (so that can be accessed in size() etc.)
						
						setSelected = true;
					}
				}
				else if (m.hitEnd()) {								// Attempt to navigate to element, return object (value)
					if (currentElementInNavChain != null) {
						Element navigatedOwner = currentElementInNavChain;
						currentElementInNavChain = UMLModelUtils.getProperty(currentElementInNavChain, token);
						
						if (currentElementInNavChain != null)
							logger.trace("Extracted property " + ((Property) currentElementInNavChain).getName());
						
						////
						// FIXME For composite resources, we need to somehow add allocations from its parts
						//		 to the whole - otherwise cost calculations become nearly impossible!
						//		 E.g., I2M4_alloc_I1M13 (for each composite child, need n variables for n
						//       instances)
						// FIXME e.g., desiredforce is a "property" (parameter) of activity / callbehavioraction
						//		 -> need to generalize this that variables created on milp side and then used
						////
						
						if (navigatedOwner != null) {
							if (DSEMLUtils.isResource(navigatedOwner)
									&& UMLModelUtils.isAbstract((Classifier) navigatedOwner))
								quantifiedElement = "";
							
							if (e instanceof Constraint) {
								// If this is a special property, handle it accordingly
								if (DSEMLUtils.isThroughputProperty((Property) currentElementInNavChain)) {
									if(DSEMLUtils.isObjective(((Constraint) e)))
										TransformationState.MUST_OPTIMIZE_THROUGHPUT = true;
								}
							}
							
							// If the resource is abstract, collect concrete ones
							if (DSEMLUtils.isResource(navigatedOwner)) {
								ArrayList<Element> concreteResources = new ArrayList<Element>();
								
								if (UMLModelUtils.isAbstract((Classifier) navigatedOwner))
									concreteResources = DSEMLUtils.getConcreteResourcesForAbstractResourceType(TransformationCache.getResources(), navigatedOwner);
								else
									concreteResources.add(navigatedOwner);
								
								// Each of these should have the same property (whether redefined or inherited)
								String toRet = "";
								for (Element conc : concreteResources) {
									if (conc != null && InternalCorrespondences.getResourceTypeMachineIDMapping().get(conc) != null) {
										int machineID = InternalCorrespondences.getResourceTypeMachineIDMapping().get(conc);
										
										ArrayList<Integer> instances = InternalCorrespondences.getResourceTypeInstanceIDMapping().get(conc);
										
										if (instances != null) {
											for (int instance : instances) {
												if (!toRet.equals(""))
													toRet += " + ";
												
												// FIXME Weird hack...
												String instanceID = "I" + instance + "M" + machineID;
												String compositeAllocID = "";
												
												if (DSEMLUtils.isCompositeResource(parent)) {
													// compositeMachineInstances is incremented with every instance of a composite resource - since the
													// lists are ordered, this should not pose a problem...
													int compInstID = TransformationState.CURRENT_COMPOSITE_RESOURCE_INSTANCE_ID;
													
													compositeAllocID = "C(" + InternalCorrespondences.getAllocationMatrixResourceInstanceIndex(conc, instance) + "," + compInstID + ")*";
												}
												
												String allocID = "A(" + InternalCorrespondences.getAllocationMatrixResourceInstanceIndex(conc, instance) + "," + TransformationState.CURRENT_ACTION_WP_COUNT + ")";
										
												// Need to now consider allocations
												// FIXME Also this is somewhat of a weird hack... assumes that there is only 1 comp. resource per WP
												//		in future, we may want to move towards a separate allocation matrix, or third dimension for allocating
												//		machine instances to composite resource instances
												/*if (actWPCount == 0
														&& TransformationHelper.isCompositeResource(conc)) {
													// Allocated resources
													allocID = "min(1, sum(A(" + this.getAllocationMatrixResourceInstanceIndex(conc, instance) + ",:)))";
												}
												else */if (TransformationState.CURRENT_ACTION_WP_COUNT == 0)		// Not yet translating process...
													allocID = instanceID;
												
												toRet += compositeAllocID + allocID + "*" + instanceID + "_" + token;
											}
										}
									}
									else {
										logger.trace("Weirdly, " + ((NamedElement) conc).getName() + " (navi item: " + ((NamedElement) currentElementInNavChain).getName() + ") had no machine ID");
									}
								}
								
								return toRet;
							}
								
							if (DSEMLUtils.isValueProperty(currentElementInNavChain)) {
								String value = UMLModelUtils.getDefaultValue((Property) currentElementInNavChain);
								
								// FIXME Complex expressions
								if (value != null && !value.equals("") && !value.equals("null")) {
									if (isNumeric(value))
										return value;
								}
							}
							
							// Parameters of activities
							if (DSEMLUtils.isWorkingPrincipleAtSomeLevel(navigatedOwner, TransformationCache.getAllWorkingPrinciples())) {
								//Parameter param = extractParameter((Activity) navigatedOwner, token);
								
								// In this case, we need to refer to activity "instances"
								// TODO Analyze whether this is sensible - we're making a few assumptions here, I think...
								String value = DSEMLUtils.getInterpretedParameterValue(
										(CallBehaviorAction) TransformationState.CURRENT_ACTION,
										token,
										TransformationCache.getObjectFlowList());
								
								if (value != null
										&& !value.equals("")
										&& !value.equals("null"))
									return value;
								
								// Otherwise return the declared variable
								// FIXME For any level of activity connected to a WP
								return "IA" + TransformationState.CURRENT_ACTION_COUNT + "W" + TransformationState.CURRENT_WORKING_PRINCIPLE_COUNT + "_" + token;
							}
							
							// If this failed, check to see whether this is an abstract type
							// FIXME This is a hack: currently, only known abstract types are handled (e.g., Workpiece)
							if (navigatedOwner instanceof Classifier
									&& UMLModelUtils.isAbstract((Classifier) navigatedOwner)
									&& DSEMLUtils.isWorkpiece(navigatedOwner)) {
								// Seems like this is an abstract workpiece, find concrete one in project, if any
								
								// FIXME This leads to some problems if there are multiple work pieces:
								//		 to be more specific, it is not clear which concrete workpiece is
								//		 considered.
								//		 What needs to be done:
								//			1) Using the current context (from TransformationState), determine
								//			   which CBA we are currently considering
								//			2) Using the objectflow list from TransformationCache, then
								//			   determine which exact type is flowing? Or maybe by analyzing
								//			   the pin?
								//		 A way around it: secondary work pieces should NOT inherit from GenericWorkPiece
								//		 --> Maybe rename GenericWorkPiece to GenericProduct or something?
								
								Classifier workpiece = DSEMLUtils.getFirstWorkpiece(rootModelElement);
								
								if (workpiece != null) {
									// Try to extract property from non-abstract workpiece
									currentElementInNavChain = UMLModelUtils.getProperty(workpiece, token);
									
									if (currentElementInNavChain != null) {
										// Try again
										String value = UMLModelUtils.getDefaultValue((Property) currentElementInNavChain);
										
										// FIXME Complex expressions
										if (value != null && !value.equals("") && !value.equals("null")) {
											if (isNumeric(value))
												return value;
										}
									}
								}
							}
							
							// Default to OwnerName_ParameterName, unless special property referenced
							if (navigatedOwner instanceof Classifier
									&& !isSpecialProperty(token)) {
								return ((Classifier) navigatedOwner).getName() + "_" + token;
							}
							
							// If it is a special property, just return the internal name
							if (isSpecialProperty(token))
								return token;
						}
					}
				}
			}
		}

		logger.log(Level.ERROR, "Error interpreting OCL expression: could not interpret OCL expression \"" + expression + "\" - malformed?");
		
		return null;
	}
	
	/** {@see #interpretExpression(Element, String, Element)} */
	public static String interpret(Element e, String expression) {
		return interpret(e, expression, null);
	}
	
	/**
	 * Check whether the string is a mathematical operator
	 * 
	 * @param toCheck The String to check.
	 * @return true if the given string represents a mathematical operator, false
	 * 		otherwise.
	 */
	private static boolean isMathematicalOperator(String toCheck) {
		if(toCheck == null)
			return false;
		
		if (toCheck.equals("+")
				|| toCheck.equals("-")
				|| toCheck.equals("*")
				|| toCheck.equals("/")
				|| toCheck.equals(">")
				|| toCheck.equals("<")
				|| toCheck.equals(">=")
				|| toCheck.equals("<=")
				|| toCheck.equals("=="))
			return true;
		
		return false;
	}
	
	/**
	 * Check whether the string is a grouping symbol.
	 * 
	 * @param toCheck The string to check.
	 * @return True if toCheck is either ")" or "(", false otherwise.
	 */
	private static boolean isGroupSymbol(String toCheck) {
		if(toCheck == null)
			return false;
		
		if (toCheck.equals("(")
				|| toCheck.equals(")"))
			return true;
		
		return false;
	}
	
	/**
	 * Check whether the string is of a numeric type.
	 * <P>
	 * The check is done based on {@link Double#parseDouble(String)}.
	 * 
	 * @param toCheck The String to check.
	 * @return true if the string can be interpreted as a floating point number,
	 * 		false otherwise.
	 */
	private static boolean isNumeric(String toCheck) {
		if (toCheck == null)
			return false;
		
		try {
			Double d = Double.parseDouble(toCheck);
		} catch (NumberFormatException e) {
			return false;
		}
		
		return true;
	}
	
	/**
	 * Checks whether the given property name is a known special property.
	 * Currently, only MaxBusyTime is checked for.
	 * 
	 * @param propertyName The name of the property.
	 * @return true if the property name refers to a special property, false
	 * 		otherwise.
	 */
	private static boolean isSpecialProperty(String propertyName) {
		String[] specialPropertyNames = {
			"MaxBusyTime"
		};
		
		for (String name : specialPropertyNames)
			if (propertyName.equals(name))
				return true;
		
		return false;
	}
	
	/**
	 * Build a set expression to preamble functions like "sum()".
	 * 
	 * @param currentElementInNavChain Current element from the path.
	 * @param quantifiedElement The name of the property being summed over.
	 * @param navigatedOwner The owner of the navigated element.
	 * @param parent The parent element of the property that the expression belongs to.
	 * @param ignoreComposites Whether or not composite resources should be ignored.
	 * @return A string representation of the right hand side of a sum() set expression.
	 */
	private static String oclBuildSetExpression(Element currentElementInNavChain,
			String quantifiedElement,
			Element navigatedOwner,
			Element parent,
			boolean ignoreComposites) {
		if (currentElementInNavChain != null)
			logger.trace("Extracted property " + ((Property) currentElementInNavChain).getName());
		
		// If the resource is abstract, collect concrete ones
		if (DSEMLUtils.isResource(navigatedOwner)) {
			ArrayList<Element> concreteResources = new ArrayList<Element>();
			
			if (UMLModelUtils.isAbstract((Classifier) navigatedOwner))
				concreteResources = DSEMLUtils.getConcreteResourcesForAbstractResourceType(TransformationCache.getResources(), navigatedOwner);
			else
				concreteResources.add(navigatedOwner);
			
			// Each of these should have the same property (whether redefined or inherited)
			String toRet = "";
			for (Element conc : concreteResources) {
				// Check whether to skip composites...
				if (ignoreComposites && DSEMLUtils.isCompositeResource(conc))
					continue;
				
				if (conc != null && InternalCorrespondences.getResourceTypeMachineIDMapping().get(conc) != null) {
					int machineID = InternalCorrespondences.getResourceTypeMachineIDMapping().get(conc);
					
					ArrayList<Integer> instances = InternalCorrespondences.getResourceTypeInstanceIDMapping().get(conc);
					
					if (instances != null) {
						for (int instance : instances) {
							if (!toRet.equals(""))
								toRet += " + ";
							
							// FIXME Weird hack...
							String instanceID = "I" + instance + "M" + machineID;
							String compositeAllocID = "";
							
							if (DSEMLUtils.isCompositeResource(parent)) {
								// compositeMachineInstances is incremented with every instance of a composite resource - since the
								// lists are ordered, this should not pose a problem...
								int compInstID = TransformationState.CURRENT_COMPOSITE_RESOURCE_INSTANCE_ID;
								
								compositeAllocID = "C(" + InternalCorrespondences.getAllocationMatrixResourceInstanceIndex(conc, instance) + "," + compInstID + ")*";
							}
							
							String allocID = "A(" + InternalCorrespondences.getAllocationMatrixResourceInstanceIndex(conc, instance) + "," + TransformationState.CURRENT_ACTION_WP_COUNT + ")";
					
							// Need to now consider allocations
							// FIXME Also this is somewhat of a weird hack... assumes that there is only 1 comp. resource per WP
							//		in future, we may want to move towards a separate allocation matrix, or third dimension for allocating
							//		machine instances to composite resource instances
							/*if (actWPCount == 0
									&& TransformationHelper.isCompositeResource(conc)) {
								// Allocated resources
								allocID = "min(1, sum(A(" + this.getAllocationMatrixResourceInstanceIndex(conc, instance) + ",:)))";
							}
							else */if (TransformationState.CURRENT_ACTION_WP_COUNT == 0)		// Not yet translating process...
								allocID = instanceID;
							
							if (!quantifiedElement.equals(""))
								toRet += compositeAllocID + allocID + "*" + instanceID + "_" + quantifiedElement;
							else
								toRet += compositeAllocID + allocID + "*" + instanceID + "_" + quantifiedElement;
						}
					}
				}
				else {
					logger.trace("Weirdly, " + ((NamedElement) conc).getName() + " (navi item: " + ((NamedElement) currentElementInNavChain).getName() + ") had no machine ID");
				}
			}
			
			if (!toRet.equals(""))
				toRet = "(" + toRet + ")";
			
			return toRet;
		}
		
		return "";
	}

}
