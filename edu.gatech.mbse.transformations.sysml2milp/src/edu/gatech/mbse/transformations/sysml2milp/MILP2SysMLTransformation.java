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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.eclipse.emf.transaction.RecordingCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.emf.transaction.util.TransactionUtil;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.Association;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Namespace;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.Property;

import edu.gatech.mbse.transformations.sysml2milp.utils.DSEMLUtils;
import edu.gatech.mbse.transformations.sysml2milp.utils.UMLModelUtils;

/**
 * This class is a rudimentary version of the back-transformation from one or more MILP
 * solutions to a SysML model.
 * 
 * @author Sebastian
 * @version 0.0.1
 */
public class MILP2SysMLTransformation {

	private Element rootModelElement = null;
	
	/**
	 * Constructor.
	 *
	 */
	public MILP2SysMLTransformation() {
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Transforms the MILP solution back to the SysML model.
	 * 
	 * @param functionalSpecification
	 */
	public void transform(Element rootElement, final Activity functionalSpecification) {
		// Create new solution package //
		
		rootModelElement = rootElement;
		
		// Need a transaction domain for the element being modified
		final TransactionalEditingDomain ted = TransactionUtil.getEditingDomain(functionalSpecification.getNearestPackage()); // (TransactionalEditingDomain) AdapterFactoryEditingDomain.getEditingDomainFor(e);
		
		ted.getCommandStack().execute(new RecordingCommand(ted, "Do Back Transformation") {

			@Override
			protected void doExecute() {
				Package solutionOwner = functionalSpecification.getNearestPackage();
				
				// Clean up old solutions
				cleanUpSolutions(solutionOwner);
				
				// Read back results from transformation
				int solution = 0;
				File file = new File(System.getProperty("user.home") + "/milp_solution_" + solution + "_instanceData.smt");
				
				while (file.exists()) {
					Package solutionPackage = createSolutionPackage(solution, solutionOwner);
					
					// Now create the instances
					createInstances(solution, solutionPackage);
					
					solution++;
					file = new File(System.getProperty("user.home") + "/milp_solution_" + solution + "_instanceData.smt");
				}
			}
			
		});
	}

	/**
	 * Remove previously existing solutions.
	 * 
	 * @param topLevelPackage
	 */
	private void cleanUpSolutions(Package topLevelPackage) {
		if (topLevelPackage.getNestedPackages() == null)
			return;
		
		for (Package nestedPackage : topLevelPackage.getNestedPackages())
			if (isSolutionPackage(nestedPackage))
				UMLModelUtils.deleteElementsRecursively(nestedPackage);
	}
	
	/**
	 * 
	 * @return
	 */
	private Package createSolutionPackage(int solution, Package topLevelPackage) {
		return UMLModelUtils.createPackage("Solution_" + solution, topLevelPackage);
	}
	
	/**
	 * 
	 * @param solution
	 * @param solutionPackage
	 */
	private void createCompositeResourceAssociations(int solution, ArrayList<Classifier> solutionElements, Package solutionPackage) {
		String resultsFileExcel = System.getProperty("user.home") + "/milp_solution_" + solution + ".xlsx";
		
		try {
			FileInputStream fis = new FileInputStream(new File(resultsFileExcel));
			
			XSSFWorkbook workBook = new XSSFWorkbook(fis);
			
			// Get second sheet
			XSSFSheet sheet = workBook.getSheetAt(1);
			
			Iterator<Row> rowIterator = sheet.iterator();
			
			// First row contains all composite resources - retrieve these first
			Row firstRow = rowIterator.next();
			
			Iterator<Cell> cellIterator = firstRow.cellIterator();
			ArrayList<String> compositeResourceNames = new ArrayList<String>();
			
			while (cellIterator.hasNext()) {
				Cell nextCell = cellIterator.next();
				
				if (!nextCell.getStringCellValue().equals("")) {
					compositeResourceNames.add(nextCell.getStringCellValue());
				}
			}
			
			String previousResourceName = "";
			int currentInstance = 1;
			String resourceName = "";
			
			// Now extract relations, and create in model as found
			while (rowIterator.hasNext()) {
				Row nextRow = rowIterator.next();
				cellIterator = nextRow.cellIterator();
				int column = 0;
				
				while (cellIterator.hasNext()) {
					Cell nextCell = cellIterator.next();
					
					if (nextCell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
						// Either the MILP or Excel produced a 0.99999999999999999 here, which failed the test below... use round
						double val = Math.round(nextCell.getNumericCellValue());
						
						// Relation exists
						if (val == 1 && !resourceName.equals(compositeResourceNames.get(column-1))) {
							String compositeParent = compositeResourceNames.get(column-1);
							String compositeChild = resourceName;
							
							Classifier parent = null;
							Classifier child = null;
							
							// Compute parent instance
							int parentInstance = 0;
							for (int i=0; i < column; i++) {
								if (compositeResourceNames.get(i).equals(compositeParent))
									parentInstance++;
							}
							
							// Find elements in solution elements
							int countdown = currentInstance;
							int countdownParent = parentInstance;
							for (Classifier sol : solutionElements) {
								if (sol.getName().contains("_" + compositeChild)) {
									if ((countdown--) == 1)
										child = sol;
								}
								
								if (sol.getName().contains("_" + compositeParent)) {
									if ((countdownParent--) == 1)
										parent = sol;
								}
								
								if (parent != null && child != null)
									break;
							}
							
							if (parent != null && child != null)
								DSEMLUtils.createDirectedAssociation(parent, child);
							else
								System.out.println("Something went horribly wrong...");
						}
					}
					else if(nextCell.getCellType() == Cell.CELL_TYPE_STRING) {
						resourceName = nextCell.getStringCellValue();
						
						if (previousResourceName.equals(""))
							previousResourceName = resourceName;
						else if (previousResourceName.equals(resourceName))
							currentInstance++;
						else {
							currentInstance = 1;
							previousResourceName = resourceName;
						}
					}
					
					column++;
				}
			}
			
			fis.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @param solution
	 * @param solutionPackage
	 */
	private ArrayList<Classifier> createInstances(int solution, Package solutionPackage) {
		ArrayList<Classifier> resourceInstances = new ArrayList<Classifier>();
		
		BufferedReader br = null;
		
		try {
			// Open instance data solution file
			br = new BufferedReader(new FileReader(System.getProperty("user.home") + "/milp_solution_" + solution + "_instanceData.smt"));
		    String line = "";
		    
		    Classifier currentType = null;
		    HashMap<String,String> propertyValues = new HashMap<String,String>();
		    
		    while ((line = br.readLine()) != null) {
		       if (line.startsWith("#")) {		// Comment
		    	   // Ignore
		       }
		       else if (line.equals("")) {
		    	   if (DSEMLUtils.isResource(currentType)) {
			    	   // Add instance
			    	   Classifier newRes = DSEMLUtils.createSpecializedResource(
			    			   propertyValues.get("instanceName") + "_" + currentType.getName(),
			    			   currentType,
			    			   solutionPackage);
			    	   
			    	   // Set default values
			    	   if (newRes.getAttributes() != null)
			    		   for (Property p : newRes.getAttributes())
			    			   if (propertyValues.containsKey(p.getName()))
			    				   DSEMLUtils.setDefaultValue(p, propertyValues.get(p.getName()));
			    	   
			    	   resourceInstances.add(newRes);
		    	   }
		    	   else if (DSEMLUtils.isSystemUnderDesign(currentType)) {
		    		   Classifier newSystemUnderDesign = DSEMLUtils.createSpecializedSystemUnderDesign(
		    				    currentType.getName() + "_" + solutionPackage.getName(), 
		    				    currentType, 
		    					solutionPackage);
		    			
		    			// Retrieve the "resources" property
		    			Property resourcesRef = UMLModelUtils.getProperty(newSystemUnderDesign, "resources");
		    			
		    			for (Classifier resource : resourceInstances) {
		    				if (DSEMLUtils.isDirectlyAssociatedWithAnyWorkingPrinciple(
		    						resource,
		    						TransformationCache.getAllWorkingPrinciples())) {
		    					Association assoc = UMLModelUtils.createDirectedAssociation(newSystemUnderDesign, resource);
		    					
		    					// Set subsetted property for resources
		    					for (Property p : assoc.getMemberEnds()) {
		    						if (DSEMLUtils.isResource(p.getType())) {
		    							p.getSubsettedProperties().add(resourcesRef);
		    						}
		    					}
		    				}
		    			}
		    			
		    			// Set default values
			    	   if (newSystemUnderDesign.getAttributes() != null)
			    		   for (Property p : newSystemUnderDesign.getAttributes())
			    			   if (propertyValues.containsKey(p.getName()))
			    				   DSEMLUtils.setDefaultValue(p, propertyValues.get(p.getName()));
		    	   }
		    	   
		    	   // Reset, next type
		    	   currentType = null;
		    	   propertyValues = new HashMap<String,String>();
		       }
		       else if(currentType == null) {
		    	   currentType = UMLModelUtils.findClassifierByQualifiedName(line, (Namespace) rootModelElement);
		       }
		       else {
		    	   String[] keyVal = line.split(" = ");
		    	   propertyValues.put(keyVal[0].substring(keyVal[0].lastIndexOf(":") + 1), keyVal[1]);
		       }
		    }
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		// Create composite resource associations
		createCompositeResourceAssociations(solution, resourceInstances, solutionPackage);
		
		return resourceInstances;
	}
	
	/**
	 * 
	 * @param p
	 * @return
	 */
	private boolean isSolutionPackage(Package p) {
		if (p.getName().startsWith("Solution_"))
			return true;
		
		return false;
	}

}
