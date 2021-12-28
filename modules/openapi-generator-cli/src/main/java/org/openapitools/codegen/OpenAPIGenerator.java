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

package org.openapitools.codegen;

import io.airlift.airline.Cli;
import io.airlift.airline.ParseArgumentsUnexpectedException;
import io.airlift.airline.ParseOptionMissingException;
import io.airlift.airline.ParseOptionMissingValueException;
import org.openapitools.codegen.cmd.*;

import java.io.*;
import java.util.*;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import java.util.Locale;

import static org.openapitools.codegen.Constants.CLI_NAME;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.*;
import org.openapitools.codegen.*;
/**
 * User: lanwen Date: 24.03.15 Time: 17:56
 * <p>
 * Command line interface for OpenAPI Generator use `openapi-generator-cli.jar help` for more info
 */
public class OpenAPIGenerator {

    private final static Lock _mutex = new ReentrantLock(true);

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAPIGenerator.class);
    public static void main(String[] args) throws Exception{
        BuildInfo buildInfo = new BuildInfo();
        Cli.CliBuilder<OpenApiGeneratorCommand> builder =
                Cli.<OpenApiGeneratorCommand>builder(CLI_NAME)
                        .withDescription(
                                String.format(
                                        Locale.ROOT,
                                        "OpenAPI Generator CLI %s (%s).",
                                        buildInfo.getVersion(),
                                        buildInfo.getSha()))
                        .withDefaultCommand(HelpCommand.class)
                        .withCommands(
                                ListGenerators.class,
                                Generate.class,
                                Meta.class,
                                HelpCommand.class,
                                ConfigHelp.class,
                                Validate.class,
                                Version.class,
                                CompletionCommand.class,
                                GenerateBatch.class
                        );

        builder.withGroup("author")
                .withDescription("Utilities for authoring generators or customizing templates.")
                .withDefaultCommand(HelpCommand.class)
                .withCommands(AuthorTemplate.class);

        try {

        CommandLineParser parser = new BasicParser();
         try{
            new File(".temp").mkdirs();
            FileWriter  writer = new FileWriter(".temp/__env_api_paths__.txt", false);
                        writer = new FileWriter(".temp/__env_api_methods__.txt", false);
                        writer = new FileWriter(".temp/__env_api_tags__.txt", false);
                        writer = new FileWriter(".temp/__env_api_names__.txt", false);
                        writer = new FileWriter(".temp/__env_models_names__.txt", false);
                        writer = new FileWriter(".temp/__env_models_filenames__.txt", false);
                        writer = new FileWriter(".temp/__env_api__.txt", false);
        
        int options_i_index = -1;
        int options_o_index = -1;

        for(int i = 0;i<args.length;i++)
                if(args[i].equals("-i")) options_i_index = i+1;
                else if(args[i].equals("-o")) options_o_index = i+1;

        String[] outputs = args[options_o_index].split(":::");
        String[] inputs = args[options_i_index].split(":::");


        if(outputs.length != inputs.length)
            {
                LOGGER.error("Inpust amount != outputs amount");
                return;
            }


        for(String arg:args)
            LOGGER.info("-arg "+arg);
        for(String output:outputs)
            LOGGER.info("-o "+output);
        for(String input:inputs)
            LOGGER.info("-i "+input);
            
            int i_index = options_i_index-1;
            int o_index = options_o_index-1;
            

            for(int i=0;i<outputs.length;i++){
                        _mutex.lock();
                        if(i == outputs.length-1){
                            String text = "__last__";
                            LOGGER.info("Last apis generating!");
                            writer.write(text);
                            writer.flush();
                            Pipeline.isLast = true;
                        }
                        _mutex.unlock();

                        if(i_index > 0)
                        args[i_index+1] = inputs[i];
                        if(o_index > 0)
                        args[o_index+1] = outputs[i];
                        LOGGER.info("--i "+inputs[i]);
                        LOGGER.info("--o "+outputs[i]);

                        
                        builder.build().parse(args).run();
            }
         }catch (Exception e) {
            LOGGER.error("First parsing faild: "+e.getMessage(), e);
            System.exit(1);
        }finally{
            File file = new File(".temp/__env_api_paths__.txt");file.delete();
            file = new File(".temp/__env_api_methods__.txt");file.delete();
            file = new File(".temp/__env_api_tags__.txt");file.delete();
            file = new File(".temp/__env_api_names__.txt");file.delete();
            // file = new File(".temp/__env_models_names__.txt");file.delete();
            file = new File(".temp/__env_models_filenames__.txt");file.delete();
            file = new File(".temp/__env_api__.txt");file.delete();
        }
            // builder.build().parse(args).run();

            // If CLI runs without a command, consider this an error. This exists after initial parse/run
            // so we can present the configured "default command".
            // We can check against empty args because unrecognized arguments/commands result in an exception.
            // This is useful to exit with status 1, for example, so that misconfigured scripts fail fast.
            // We don't want the default command to exit internally with status 1 because when the default command is something like "list",
            // it would prevent scripting using the command directly. Example:
            //     java -jar cli.jar list --short | tr ',' '\n' | xargs -I{} echo "Doing something with {}"
            if (args.length == 0) {
                System.exit(1);
            }
        } catch (ParseArgumentsUnexpectedException e) {
            System.err.printf(Locale.ROOT, "[error] %s%n%nSee '%s help' for usage.%n", e.getMessage(), CLI_NAME);
            System.exit(1);
        } catch (ParseOptionMissingException | ParseOptionMissingValueException e) {
            System.err.printf(Locale.ROOT, "[error] %s%n", e.getMessage());
            System.exit(1);
        }
    }
}
