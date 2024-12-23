package io.quarkiverse.langchain4j.ollama.deployment;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import dev.langchain4j.service.UserName;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.testing.internal.WiremockAware;
import io.quarkus.test.QuarkusUnitTest;

public class OllamaTextOutputTest extends WiremockAware {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .overrideConfigKey("quarkus.langchain4j.ollama.base-url", WiremockAware.wiremockUrlForConfig())
            .overrideConfigKey("quarkus.langchain4j.devservices.enabled", "false");

    @Singleton
    @RegisterAiService
    interface AiService {
        String question(@UserName String text);
    }

    @Inject
    AiService aiService;

    @Test
    void extract() {
        wiremock().register(
                post(urlEqualTo("/api/chat"))
                        .withRequestBody(equalToJson(
                                """
                                        {
                                            "model": "llama3.2",
                                            "messages": [
                                                {
                                                    "role": "user",
                                                    "content": "Tell me something about Alan Wake"
                                                }
                                            ],
                                            "stream": false,
                                            "options": {
                                                "temperature": 0.8,
                                                "top_k": 40,
                                                "top_p": 0.9
                                            }
                                        }"""))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("""
                                        {
                                            "model": "llama3.2",
                                            "created_at": "2024-12-11T15:21:23.422542932Z",
                                            "message": {
                                                "role": "assistant",
                                                "content": "He is a writer!"
                                            },
                                            "done_reason": "stop",
                                            "done": true,
                                            "total_duration": 8125806496,
                                            "load_duration": 4223887064,
                                            "prompt_eval_count": 31,
                                            "prompt_eval_duration": 1331000000,
                                            "eval_count": 18,
                                            "eval_duration": 2569000000
                                        }""")));

        var result = aiService.question("Tell me something about Alan Wake");
        assertEquals("He is a writer!", result);
    }
}
