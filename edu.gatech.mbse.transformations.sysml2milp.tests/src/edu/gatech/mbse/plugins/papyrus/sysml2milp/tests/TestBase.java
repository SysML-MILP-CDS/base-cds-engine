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
package edu.gatech.mbse.plugins.papyrus.sysml2milp.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.papyrus.sysml.SysmlPackage;
import org.eclipse.papyrus.sysml.activities.ActivitiesPackage;
import org.eclipse.papyrus.sysml.allocations.AllocationsPackage;
import org.eclipse.papyrus.sysml.blocks.BlocksPackage;
import org.eclipse.papyrus.sysml.constraints.ConstraintsPackage;
import org.eclipse.papyrus.sysml.interactions.InteractionsPackage;
import org.eclipse.papyrus.sysml.modelelements.ModelelementsPackage;
import org.eclipse.papyrus.sysml.portandflows.PortandflowsPackage;
import org.eclipse.papyrus.sysml.requirements.RequirementsPackage;
import org.eclipse.papyrus.sysml.statemachines.StatemachinesPackage;
import org.eclipse.papyrus.sysml.usecases.UsecasesPackage;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.resources.util.UMLResourcesUtil;

import edu.gatech.mbse.transformations.sysml2milp.SolverType;
import edu.gatech.mbse.transformations.sysml2milp.SysML2MILPTransformation;
import edu.gatech.mbse.transformations.sysml2milp.TransformationConfig;
import matlabcontrol.MatlabConnectionException;
import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxyFactory;
import matlabcontrol.MatlabProxyFactoryOptions;
import matlabcontrol.MatlabProxyFactoryOptions.Builder;

/**
 * Base class for tests of SysML2MILP transformation. This is a collection of procedures
 * for loading UML models and registering profiles outside of Papyrus and in a standalone
 * Java application.
 * 
 * @author Sebastian J. I. Herzig
 * @version 0.1.0
 */
public class TestBase {
	
	/** File reference to SysML profile. */
	private URI sysmlProfile = URI.createFileURI(new File("sysml/SysML.profile.uml").getAbsolutePath());
	
	/** Pointer to root package. */
	private Package rootPackage = null;
	
	/** Pointer to a resource object. */
	private Resource resource = null;
	
	/** Pointer to a resource set. */
	private ResourceSet resourceSet = null;
	
	/** Flag that checks whether YALMIP and OPTI have already been set up (hope this works). */
	private static boolean setupScriptsRan = false;

	/**
	 * Loads a UML model and registers any specified profiles. Note that the SysML
	 * profile is loaded automatically.
	 * 
	 * @param model An {@link URI} object pointing to the location of the UML model.
	 * @param profilesToRegister A mapping from namespace URIs (as strings) to location
	 * 			of a profile definition (as URI object).
	 */
	public void loadProject(URI model, Map<String,URI> profilesToRegister) {
		// NO LONGER NEEDED SINCE LUNA
		// Load project and fetch root package
		
		//resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
        //	.put("uml", .Factory.INSTANCE);
		
		// Load project - the "Luna-way". First, initialize the resource set object properly
		setResourceSet(new ResourceSetImpl());
		setResourceSet(UMLResourcesUtil.init(getResourceSet()));
		
		// Load SysML profile
		loadSysMLProfile();
		
		// Register profiles
		for (Entry<String,URI> entry : profilesToRegister.entrySet()) {
			getResourceSet().getPackageRegistry().put(entry.getKey(), entry.getValue());
			getResourceSet().getURIConverter().getURIMap().put(URI.createURI(entry.getKey()), entry.getValue());
		}
		
		// Now load UML model, and set it as the current resource
		getResourceSet().createResource(model);
		setResource(getResourceSet().getResource(model, true));
		
		// Fetch root package, and set pointer appropriately
		setRootPackage((Package) EcoreUtil
				.getObjectByType(resource.getContents(),
				UMLPackage.Literals.PACKAGE));
	}
	
	/**
	 * Register a profile.
	 * 
	 * @param namespace Namespace URI as string.
	 * @param profile An {@link URI} object referring to profile location.
	 */
	protected void registerProfile(String namespaceURI, URI profile) {
		getResourceSet().getPackageRegistry().put(namespaceURI, profile);
		getResourceSet().getURIConverter().getURIMap().put(URI.createURI(namespaceURI), profile);
	}
	
	/**
	 * Load SysML profile.
	 */
	private void loadSysMLProfile() {
		// Register top level profile, and add profile URI to URI map
		getResourceSet().getPackageRegistry().put("pathmap://SysML_PROFILES/SysML.profile.uml", sysmlProfile);
		getResourceSet().getURIConverter().getURIMap().put(URI.createURI("pathmap://SysML_PROFILES/SysML.profile.uml"), sysmlProfile);
		
		// Load SysML Profile - interestingly this also allows for the DSE profile to be loaded correctly...
		getResourceSet().getPackageRegistry().put(SysmlPackage.eNS_URI, SysmlPackage.eINSTANCE);
		getResourceSet().getPackageRegistry().put(BlocksPackage.eNS_URI, BlocksPackage.eINSTANCE);
		getResourceSet().getPackageRegistry().put(RequirementsPackage.eNS_URI, RequirementsPackage.eINSTANCE);
		getResourceSet().getPackageRegistry().put(ActivitiesPackage.eNS_URI, ActivitiesPackage.eINSTANCE);
		getResourceSet().getPackageRegistry().put(AllocationsPackage.eNS_URI, AllocationsPackage.eINSTANCE);
		getResourceSet().getPackageRegistry().put(ConstraintsPackage.eNS_URI, ConstraintsPackage.eINSTANCE);
		getResourceSet().getPackageRegistry().put(InteractionsPackage.eNS_URI, InteractionsPackage.eINSTANCE);
		getResourceSet().getPackageRegistry().put(ModelelementsPackage.eNS_URI, ModelelementsPackage.eINSTANCE);
		getResourceSet().getPackageRegistry().put(PortandflowsPackage.eNS_URI, PortandflowsPackage.eINSTANCE);
		getResourceSet().getPackageRegistry().put(StatemachinesPackage.eNS_URI, StatemachinesPackage.eINSTANCE);
		getResourceSet().getPackageRegistry().put(UsecasesPackage.eNS_URI, UsecasesPackage.eINSTANCE);
	}
	
	/**
	 * Executes the transformation.
	 * <P>
	 * Note that the solver can be set via JVM options: just define the property
	 * milp.solver when launching the tests (e.g., via maven command line or jenkins).
	 * 
	 * @param process Top level activity.
	 * @param numSolutionsToGenerate The number of solutions to generate.
	 * @return Generated MILP in YALMIP format.
	 * @throws Exception 
	 */
	protected String executeTransformation(Activity process, int numSolutionsToGenerate) throws Exception {
		// Set solver if a different solver is requested
		if (System.getProperty("tycho.milp.solver") != null 
				&& !System.getProperty("tycho.milp.solver").equals("")
				/*&& SolverType.valueOf(System.getProperty("tycho.milp.solver").toLowerCase()) != null*/) {		// Would throw exception
			try {
				SolverType t = SolverType.valueOf(System.getProperty("tycho.milp.solver").toLowerCase());
				TransformationConfig.CONFIG_SOLVER = t;
			} catch (IllegalArgumentException i) {
				// Happily ignore
			} catch (NullPointerException n) {
				// Happily ignore
			}
		}
		
		// Return result of executing transformation
		return (new SysML2MILPTransformation()).transform(process, getRootPackage(), 1, true);
	}
	
	/**
	 * Execute a Matlab script.
	 * <P>
	 * This function now sets up OPTI and YALMIP automatically as well.
	 * 
	 * @param code A string representation of the matlab code.
	 */
	protected void executeMatlabScript(String code) {
		Builder optionsBuilder = new Builder();
		
		// Default options
		optionsBuilder = optionsBuilder.setHidden(true);
		optionsBuilder = optionsBuilder.setUsePreviouslyControlledSession(true);
		
		// If there are multiple matlab installations, the path to the specific matlab installation can be
		// set with the system property matlab.loc - note that this has to be the absolute path to the
		// executable!
		if (System.getProperty("tycho.matlab.loc") != null 
				&& !System.getProperty("tycho.matlab.loc").equals("")
				&& !System.getProperty("tycho.matlab.loc").equals("null")) {
			optionsBuilder = optionsBuilder.setMatlabLocation(System.getProperty("tycho.matlab.loc"));
		}
				
		MatlabProxyFactoryOptions options = optionsBuilder.build();
		MatlabProxyFactory factory = new MatlabProxyFactory(options);
		MatlabProxy proxy = null;
		
		try {
			proxy = factory.getProxy();
		} catch (MatlabConnectionException e) {
			e.printStackTrace();
			
			fail();
		}
		
		try {
			if (!setupScriptsRan) {
				// Only do this if associated defines are set - opti will not be configured otherwise
				if (System.getProperty("tycho.opti.root") != null
						&& !System.getProperty("tycho.opti.root").equals("")
						&& !System.getProperty("tycho.opti.root").equals("null")) {
					//if (TransformationConfig.CONFIG_SOLVER == SolverType.CBC) {
					// First, setup the solvers & OPTI
					String localPath = (new File("opti")).getAbsolutePath().replace("\\", "/");
					proxy.eval("addpath('" + localPath + "');");
					
					String path = (new File(System.getProperty("tycho.opti.root"))).getAbsolutePath().replace("\\", "/");
					proxy.eval("cd '" + path + "';");
					proxy.eval("opti_Install_jenkins");
					//}
				}
				
				// Now set up YALMIP, if requested
				if (System.getProperty("tycho.yalmip.root") != null
						&& !System.getProperty("tycho.yalmip.root").equals("")
						&& !System.getProperty("tycho.yalmip.root").equals("null")) {
					String yalmipRoot = (new File(System.getProperty("tycho.yalmip.root"))).getAbsolutePath().replace("\\", "/");
					proxy.eval("addpath(genpath('" + yalmipRoot + "'));");
				}
				
				// Avoid running this stuff twice (since the session is kept)
				//setupScriptsRan = true;
			}
			
			// Now execute MILP code
			proxy.eval(code);
		} catch (MatlabInvocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
			fail();
		}
	}
	
	/**
	 * Compares the actual output to expected mappings. Note that, currently, only properties of the
	 * manufacturing cell (at the top level) are output into the temporary file (but this can be changed
	 * fairly easily in the transformation).
	 * 
	 * @param expectedMappings A hash map object of expected mappings.
	 */
	protected void compareActualToExpectedOutput(Map<String,String> expectedMappings) {
		// Matlab script writes to an output file, which we then parse
		File tempFile = new File(System.getProperty("java.io.tmpdir") + "/tmp_milp_results.txt");
		
		// Parse the result to see whether (a) a feasible solution was found and (b) whether the output is as expected
		try {
			BufferedReader br = new BufferedReader(new FileReader(tempFile));
			
			// Read the first line
			String line = br.readLine();
			String extractedMappings = "";
			
			int successfulComparisons = 0;
			
			// Continue until end of file reached
			while (line != null) {
				// Extract the mapping by splitting the prop=value at the "=" into an array of [0]=prop, [1]=value
				String[] mapping = line.replace(" = ", "=").split("=");
				
				extractedMappings += Arrays.toString(mapping) + "\r\n";
				
				if (expectedMappings.get(mapping[0]) != null) {
					String expectedValue = expectedMappings.get(mapping[0]);
					
					// Compare expected to actual value
					try {
						// Try and parse it as a number - if that fails, just do an "equals", which will also compare strings
						double parsedExpValue = Double.parseDouble(expectedValue);
						double parsedActValue = Double.parseDouble(mapping[1]);
						
						assertEquals("Expected " + mapping[0] + " to be " + parsedExpValue + " but it was " + parsedActValue,
								parsedExpValue, parsedActValue, 0.03);
							//fail("Expected " + mapping[0] + " to be " + parsedExpValue + " but it was " + parsedActValue);
					} catch (NumberFormatException e) {
						// Probably not a number, use string value instead
						if (!expectedValue.equals(mapping[1]))
							fail("Expected " + mapping[0] + " to be " + expectedValue + " but it was " + mapping[1]);
					}
					
					successfulComparisons++;
				}
				
				line = br.readLine();
			}
			
			if (extractedMappings.equals(""))
				extractedMappings = "(none)";
			
			if (successfulComparisons != expectedMappings.size())
				fail("Expected the following mappings:\r\n\r\n" + expectedMappings.toString() + "\r\n\r\nHowever, only the following mappings were available:\r\n\r\n" + extractedMappings);
			
			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
			fail("Could not find output file with produced results!\n\n" + e.getLocalizedMessage());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
			fail("Could not find output file with produced results!" + e.getLocalizedMessage());
		} finally {
			// Finally, delete the file
			if (tempFile.exists())
				tempFile.delete();
		}
	}
	
	/**
	 * Finds and returns an element in the UML model by name.
	 * 
	 * @param name The name of the element.
	 * @return The element found, or null if the elemnt could not be found.
	 */
	protected NamedElement findElementByName(String name) {
		// Now fetch relevant process
		for (TreeIterator<EObject> i = getResource().getAllContents(); i.hasNext(); ) {
			EObject cur = i.next();
			
			if (cur instanceof NamedElement) {
				if (((NamedElement) cur).getName().equals(name)) {
					return (NamedElement) cur;
				}
			}
		}
		
		return null;
	}

	/**
	 * @return the rootPackage
	 */
	public Package getRootPackage() {
		return rootPackage;
	}

	/**
	 * @param rootPackage the rootPackage to set
	 */
	private void setRootPackage(Package rootPackage) {
		this.rootPackage = rootPackage;
	}

	/**
	 * @return the resource
	 */
	public Resource getResource() {
		return resource;
	}

	/**
	 * @param resource the resource to set
	 */
	private void setResource(Resource resource) {
		this.resource = resource;
	}

	/**
	 * @return the resourceSet
	 */
	public ResourceSet getResourceSet() {
		return resourceSet;
	}

	/**
	 * @param resourceSet the resourceSet to set
	 */
	private void setResourceSet(ResourceSet resourceSet) {
		this.resourceSet = resourceSet;
	}
	

}
