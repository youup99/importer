/* Copyright 2017-2018 Norconex Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.norconex.importer.handler.tagger.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.text.RegexKeyValueExtractor;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.util.ExternalApp;

public class ExternalTaggerTest {

    public static final String INPUT = "1 2 3\n4 5 6\n7 8 9";
    public static final String EXPECTED_OUTPUT = "3 2 1\n6 5 4\n9 8 7";

    @Test
    public void testWriteRead() throws IOException {
        ExternalTagger t = new ExternalTagger();
        t.setCommand("my command");
        t.setInputDisabled(true);
        t.setTempDir(Paths.get("/some/path"));

        t.setMetadataInputFormat("json");
        t.setMetadataOutputFormat("xml");

        t.setMetadataExtractionPatterns(
            new RegexKeyValueExtractor("asdf.*", "blah"),
            new RegexKeyValueExtractor("qwer.*", "halb")
        );

        Map<String, String> envs = new HashMap<>();
        envs.put("env1", "value1");
        envs.put("env2", "value2");
        t.setEnvironmentVariables(envs);

        XML.assertWriteRead(t, "handler");
    }

    @Test
    public void testMetaInputMetaOutput()
            throws IOException, ImporterHandlerException {
        testWithExternalApp("-ic ${INPUT} "
                + "-im ${INPUT_META} -om ${OUTPUT_META} "
                + "-ref ${REFERENCE}");
    }

    private void testWithExternalApp(String command)
            throws IOException, ImporterHandlerException {
        InputStream input = inputAsStream();
        ImporterMetadata metadata = new ImporterMetadata();
        metadata.set(
                "metaFileField1", "this is a first test");
        metadata.set("metaFileField2",
                "this is a second test value1",
                "this is a second test value2");

        ExternalTagger t = new ExternalTagger();
        t.setCommand(ExternalApp.newCommandLine(command));
        addPatternsAndEnvs(t);
        t.setMetadataInputFormat("properties");
        t.setMetadataOutputFormat("properties");
        t.tagDocument("N/A", input, metadata, false);

        assertMetadataFiles(metadata);
    }

    private void assertMetadataFiles(ImporterMetadata meta) {
        Assert.assertEquals(
                "test first a is this", meta.getString("metaFileField1"));
        Assert.assertEquals(
                "value1 test second a is this",
                meta.getStrings("metaFileField2").get(0));
        Assert.assertEquals(
                "value2 test second a is this",
                meta.getStrings("metaFileField2").get(1));
    }

    private void addPatternsAndEnvs(ExternalTagger t) {
        Map<String, String> envs = new HashMap<>();
        envs.put(ExternalApp.ENV_STDOUT_BEFORE, "field1:StdoutBefore");
        envs.put(ExternalApp.ENV_STDOUT_AFTER, "<field2>StdoutAfter</field2>");
        envs.put(ExternalApp.ENV_STDERR_BEFORE, "field3 StdErrBefore");
        envs.put(ExternalApp.ENV_STDERR_AFTER, "StdErrAfter:field4");
        t.setEnvironmentVariables(envs);

        t.setMetadataExtractionPatterns(
            new RegexKeyValueExtractor("^(f.*):(.*)", 1, 2),
            new RegexKeyValueExtractor("^<field2>(.*)</field2>", "field2", 1),
            new RegexKeyValueExtractor("^f.*StdErr.*", "field3", 1),
            new RegexKeyValueExtractor("^(S.*?):(.*)", 2, 1)
        );
    }

    private InputStream inputAsStream() throws IOException {
        return new ByteArrayInputStream(INPUT.getBytes());
    }
}
