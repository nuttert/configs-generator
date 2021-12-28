/*
 * Copyright 2018 OpenAPI-Generator Contributors (https://openapi-generator.tech)
 * Copyright 2018 SmartBear Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openapitools.codegen.templating.mustache;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import org.openapitools.codegen.CodegenConfig;

import java.io.IOException;
import java.io.Writer;


/**
 * Converts text in a fragment to camelCase.
 *
 * Register:
 * <pre>
 * additionalProperties.put("camelcase", new CamelCaseLambda());
 * </pre>
 *
 * Use:
 * <pre>
 * {{#camelcase}}{{name}}{{/camelcase}}
 * </pre>
 */
public class CustomPath implements Mustache.Lambda {
    private CodegenConfig generator = null;
    private Boolean escapeParam = false;
    private String pattern;

    public CustomPath(String pattern) {
      this.pattern = pattern;
    }

    public CustomPath() {}

    public CustomPath generator(final CodegenConfig generator) {
        this.generator = generator;
        return this;
    }

    public CustomPath escapeAsParamName(final Boolean escape) {
        this.escapeParam = escape;
        return this;
    }

    @Override
    public void execute(Template.Fragment fragment, Writer writer) throws IOException {
        String text = fragment.execute().replaceAll("\\{(.*?)}", "");
        
        writer.write(text);
    }
}
