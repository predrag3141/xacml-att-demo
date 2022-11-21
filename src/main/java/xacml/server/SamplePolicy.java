package xacml.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;

import com.att.research.xacml.api.*;
import com.att.research.xacml.api.pdp.PDPEngine;
import com.att.research.xacml.api.pdp.PDPEngineFactory;
import com.att.research.xacml.api.pdp.PDPException;
import com.att.research.xacml.std.*;
import com.att.research.xacml.std.dom.DOMRequest;
import com.att.research.xacml.std.dom.DOMStructureException;
import com.att.research.xacml.util.XACMLProperties;
import com.att.research.xacml.util.FactoryException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathExpression;

public class SamplePolicy {
    private PDPEngine engine;

    public SamplePolicy() {
        try {
            // This is what the xacml-3.0 README recommends
            File xacmlPropertiesFile = new File("target/classes/xacml.properties");
            System.setProperty(XACMLProperties.XACML_PROPERTIES_NAME, xacmlPropertiesFile.getPath());
            Properties properties = new Properties();
            try (InputStream inStream = Files.newInputStream(xacmlPropertiesFile.toPath())) {
                properties.load(inStream);
            }
            catch (IOException e) {
                System.out.println("Caught IOException " + e.getMessage());
            }

            // Instantiate the policy engine
            PDPEngineFactory factory = PDPEngineFactory.newInstance();
            engine = factory.newEngine(properties);
        }
        catch(FactoryException e) {
            System.out.println("Caught FactoryException " + e.getMessage());
        }
    }

    private Request getRequest(String requestFileAsStr, String personRequestingAccess) {
        // Initializations to read the request from requestFileAsStr
        Request requestFromFile = null;
        if (!requestFileAsStr.endsWith(".xml")) {
            System.out.println("Error: request file " + requestFileAsStr + " is not XML");
            return null; // Do not throw exceptions for errors, as this is a prototype
        }
        File requestFile = new File(requestFileAsStr);

        try {
            // Load the request file
            requestFromFile = DOMRequest.load(requestFile);

            // Create modifiedRequest, which is the same as requestFromFile, except
            // that the name of the person requesting access is personRequestingAccess
            //
            StdMutableRequest modifiedRequest = new StdMutableRequest(requestFromFile.getStatus());
            modifiedRequest.setCombinedDecision(requestFromFile.getCombinedDecision());
            modifiedRequest.setRequestDefaults(requestFromFile.getRequestDefaults());
            modifiedRequest.setReturnPolicyIdList(requestFromFile.getReturnPolicyIdList());
            Iterator<RequestAttributes> iterRequestAttributesFromFile =
                requestFromFile.getRequestAttributes().iterator();
            System.out.println("========== Person requesting access: " + personRequestingAccess);
            while (iterRequestAttributesFromFile.hasNext()) {
                RequestAttributes requestAttributesFromFile	= iterRequestAttributesFromFile.next();
                System.out.println("========== Whole thing: " + requestAttributesFromFile);
                System.out.println("           Category:    " + requestAttributesFromFile.getCategory());
                System.out.println("           XmlId:       " + requestAttributesFromFile.getXmlId());
                System.out.println("           Attributes:  " + requestAttributesFromFile.getAttributes());
                if (requestAttributesFromFile.getCategory() == null) {
                    continue;
                }
                if (!Objects.equals(requestAttributesFromFile.getCategory().toString(),
                        "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject"
                )) {
                    modifiedRequest.add(requestAttributesFromFile);
                }
                else {
                    // modifiedAttributes is a copy of requestAttributesFromFile except for the
                    // one attribute that requests access for Julius Hibbert, who is replaced by
                    // personRequestingAccess.
                    Collection<Attribute> modifiedAttributeCollection = new ArrayList<>();
                    for (Attribute attributeFromFile : requestAttributesFromFile.getAttributes()) {
                        System.out.println("          Attribute: " + attributeFromFile);
                        StdMutableAttribute modifiedAttribute = new StdMutableAttribute();
                        modifiedAttribute.setAttributeId(attributeFromFile.getAttributeId());
                        for (AttributeValue<?> valueFromFile : modifiedAttribute.getValues()) {
                            if (valueFromFile.toString().contains("Hibbert")) {
                                AttributeValue<String> modifiedValue = new AttributeValue<String>() {

                                    @Override
                                    public Identifier getDataTypeId() {
                                        return valueFromFile.getDataTypeId();
                                    }

                                    @Override
                                    public String getValue() {
                                        return personRequestingAccess;
                                    }

                                    @Override
                                    public Identifier getXPathCategory() {
                                        return valueFromFile.getXPathCategory();
                                    }
                                };
                                modifiedAttribute.addValue(modifiedValue);
                            }
                        }
                    }

                    // Create a modifiedAttributes that differs from one read in from the file
                    // only in the name of the person requesting access.
                    RequestAttributes modifiedAttributes = new RequestAttributes() {
                        @Override
                        public Node getContentRoot() {
                            return requestAttributesFromFile.getContentRoot();
                        }

                        @Override
                        public Node getContentNodeByXpathExpression(XPathExpression xPathExpression) {
                            return requestAttributesFromFile.getContentNodeByXpathExpression(xPathExpression);
                        }

                        @Override
                        public NodeList getContentNodeListByXpathExpression(XPathExpression xPathExpression) {
                            return requestAttributesFromFile.getContentNodeListByXpathExpression(xPathExpression);
                        }

                        @Override
                        public String getXmlId() {
                            return requestAttributesFromFile.getXmlId();
                        }

                        @Override
                        public Identifier getCategory() {
                            return requestAttributesFromFile.getCategory();
                        }

                        @Override
                        public Collection<Attribute> getAttributes() {
                            return modifiedAttributeCollection;
                        }

                        @Override
                        public Iterator<Attribute> getAttributes(Identifier identifier) {
                            return modifiedAttributeCollection.iterator();
                        }

                        @Override
                        public boolean hasAttributes(Identifier identifier) {
                            return requestAttributesFromFile.hasAttributes(identifier);
                        }
                    }; // End definition of modifiedAttributes

                    // Add the modified attributes
                    modifiedRequest.add(modifiedAttributes);
                } // If access-subject attribute
            }
            return modifiedRequest;
        }
        catch (DOMStructureException e) {
            System.out.println("Caught DOMStructureException " + e.getMessage());
        }
        return null;
    }

    public List<String> processRequest(String requestFileAsStr, String personRequestingAccess) {
        ArrayList<String> rval = new ArrayList<>();
        try {
            // Read the request file into a Request instance
            //
            Request request = getRequest(requestFileAsStr, personRequestingAccess);

            // Get the response from the engine and print it. Note that
            // the engine was provided the path to the policy file via
            // properties in the constructor.
            //
            Response response = engine.decide(request);
            Collection<Result> results = response.getResults();
            for (Result result : results) {
                Decision decision = result.getDecision();
                String decisionAsStr = decision.toString();
                rval.add(decisionAsStr);
                System.out.println("decision: " + decisionAsStr);
            }
        }
        catch(PDPException e) {
            System.out.println("Caught PDPException " + e.getMessage());
        }
        return rval;
    }
}
