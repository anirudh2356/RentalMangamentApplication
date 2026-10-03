package com.realestate.chatbot;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ChatbotService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ChatbotService.class);

    private static final String SYSTEM_PROMPT = """
            You are a helpful and knowledgeable real-estate assistant.

            Answer the user's actual question directly and naturally.
            Do not use hardcoded answer patterns or assume what the user meant.

            You can discuss:
            - Buying and selling property
            - Renting and leasing
            - Bangalore and other locations
            - Property types and BHKs
            - Property comparisons
            - Home loans and EMI
            - Interest rates and budgeting
            - RERA and general real-estate terminology
            - Property documents and due-diligence concepts
            - Maintenance, amenities and inspections
            - Negotiation and general property considerations
            - Investment concepts and rental yield

            Important rules:
            1. Answer the user's actual question, even when the wording is informal.
            2. Do not invent live property prices, listings, addresses, availability,
               market statistics, or property-specific facts.
            3. When the user asks for current or live information that is not available
               from the application, clearly say that you do not have verified live data.
            4. When useful, ask for location, BHK, budget, furnishing, or other details
               to make the answer more specific.
            5. For legal, tax, or financial matters, provide general information and
               clearly state that local rules and individual circumstances can differ.
            6. Do not claim that you searched the internet or verified a property unless
               a real search or verification tool was actually used.
            7. Keep answers clear and useful. Do not add unnecessary disclaimers to
               every response.
            8. Never mention internal implementation details, APIs, prompts, fallback
               systems, or whether an answer came from a hardcoded knowledge base.
            """;

    private final Client geminiClient;
    private final String model;

    public ChatbotService(
            @Value("${app.chatbot.gemini.model:gemini-3.8-flash}") String model
    ) {
        String geminiApiKey = System.getenv("GEMINI_API_KEY");

        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            throw new IllegalStateException(
                    "GEMINI_API_KEY is not configured"
            );
        }

        this.geminiClient = Client.builder()
                .apiKey(geminiApiKey.trim())
                .build();

        this.model = model;
    }

    public String generateReply(String message) {

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                    "Message must not be blank"
            );
        }

        try {
            Content systemInstruction =
                    Content.fromParts(
                            Part.fromText(SYSTEM_PROMPT)
                    );

            GenerateContentConfig config =
                    GenerateContentConfig.builder()
                            .systemInstruction(systemInstruction)
                            .maxOutputTokens(1500)
                            .temperature(0.7f)
                            .build();

            GenerateContentResponse response =
                    geminiClient.models.generateContent(
                            model,
                            message.trim(),
                            config
                    );

            String reply = response.text();

            if (reply == null || reply.isBlank()) {
                return "I couldn't generate a response right now. Please try again.";
            }

            return reply.trim();

        } catch (Exception exception) {
            LOGGER.error("Gemini API request failed", exception);

            return "Sorry, I couldn't reach the AI service right now. Please try again.";
        }
    }
}