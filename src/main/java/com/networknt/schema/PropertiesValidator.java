/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class PropertiesValidator extends BaseJsonValidator implements JsonValidator {
    public static final String PROPERTY = "properties";
    private static final Logger logger = LoggerFactory.getLogger(PropertiesValidator.class);
    private Map<String, JsonSchema> schemas;

    public PropertiesValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidationContext validationContext) {
        super(schemaPath, schemaNode, parentSchema, ValidatorTypeCode.PROPERTIES, validationContext);
        schemas = new HashMap<String, JsonSchema>();
        for (Iterator<String> it = schemaNode.fieldNames(); it.hasNext(); ) {
            String pname = it.next();
            schemas.put(pname, new JsonSchema(validationContext, schemaPath + "/" + pname, parentSchema.getCurrentUri(), schemaNode.get(pname), parentSchema));
        }
    }

    public Set<ValidationMessage> validate(JsonNode node, JsonNode rootNode, String at) {
        debug(logger, node, rootNode, at);

        Set<ValidationMessage> errors = new LinkedHashSet<ValidationMessage>();

        for (Map.Entry<String, JsonSchema> entry : schemas.entrySet()) {
            JsonSchema propertySchema = entry.getValue();
            JsonNode propertyNode = node.get(entry.getKey());

            if (propertyNode != null) {
            	// check whether this is a complex validator. save the state
            	boolean isComplex = config.isComplexValidator();
            	// if this is a complex validator, the node has matched, and all it's child elements, if available, are to be validated
            	if(config.isComplexValidator()) {
            		config.setMatchedNode(true);
            	}
            	// reset the complex validator for child element validation, and reset it after the return from the recursive call
            	config.setComplexValidator(false);
            	//validate the child element(s)
                errors.addAll(propertySchema.validate(propertyNode, rootNode, at + "." + entry.getKey()));
                // reset the complex flag to the original value before the recursive call
                config.setComplexValidator(isComplex);
                // if this was a complex validator, the node has matched and has been validated
            	if(config.isComplexValidator()) {
            		config.setMatchedNode(true);
            	}
            } else {
            	// decide which behavior to eomploy when validator has not matched
            	if(config.isComplexValidator()) {
            		// this was a complex validator (ex oneOf) and the node has not been matched
            		config.setMatchedNode(false);
            		return Collections.unmodifiableSet(new LinkedHashSet<ValidationMessage>());
            	}
            		
            	// check whether the node which has not matched was mandatory or not
        		if(getParentSchema().hasRequiredValidator())
            		errors.addAll(getParentSchema().getRequiredValidator().validate(node,  rootNode, at));     
            }
        }

        return Collections.unmodifiableSet(errors);
    }

}
