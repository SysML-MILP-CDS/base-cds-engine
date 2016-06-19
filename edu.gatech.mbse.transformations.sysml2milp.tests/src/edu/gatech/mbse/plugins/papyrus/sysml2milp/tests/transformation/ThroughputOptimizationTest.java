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
package edu.gatech.mbse.plugins.papyrus.sysml2milp.tests.transformation;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.uml2.uml.Activity;
import org.junit.Before;
import org.junit.Test;

import edu.gatech.mbse.plugins.papyrus.sysml2milp.tests.TestBase;

/**
 * Test case - throughput optimization.
 * 
 * @author Sebastian
 */
public class ThroughputOptimizationTest extends TestBase {

	/** File reference to UML model that is part of test. */
	private URI uri = URI.createFileURI(new File("test-resources/ThroughputOptimizationTest/Model/model.uml").getAbsolutePath());

	/** File reference to DSE profile being used. */
	private URI dseProfile = URI.createFileURI(new File("test-resources/ThroughputOptimizationTest/DSEProfile/DSEProfile.profile.uml").getAbsolutePath());
	
	/** Name of process to look for in UML model. */
	private String processName = "ManufacturingProcess";

	@Before
	public void setup() {
		// Set up profiles to register
		Map<String,URI> profiles = new HashMap<String,URI>();
		profiles.put("http:///schemas/DSEProfile/_xhIlkBUWEeWdZqvVzWkEVA/9", dseProfile);

		// Load project
		loadProject(uri, profiles);
	}
	
	@Test
	public void throughputOptimizationTest() {
		// Get process
		Activity process = (Activity) findElementByName(processName);
		
		if (process == null)
			fail();
		
		// Now run transformation
		String milpCode;
		try {
			milpCode = executeTransformation(process, 1);
		
			// Execute the MILP transformation
			executeMatlabScript(milpCode);
			
			// These are the expected mappings:
			HashMap<String,String> expectedMappings = new HashMap<String,String>();
			expectedMappings.put("throughput", "8.230453e-02");
			
			// Compare actual to expected output
			compareActualToExpectedOutput(expectedMappings);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
