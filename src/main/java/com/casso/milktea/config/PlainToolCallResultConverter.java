package com.casso.milktea.config;

import org.springframework.ai.tool.execution.ToolCallResultConverter;
import org.springframework.stereotype.Component;

/**
 * Custom ToolCallResultConverter that strips newlines from function results.
 *
 * Problem: Spring AI's default converter wraps the result in a JSON string
 * and sends it to Groq as a tool message. Groq sees the data as "already
 * shown" and outputs only a short acknowledgment instead of displaying it.
 *
 * Fix: replace newlines with spaces so Groq no longer recognizes the data as
 * formatted (menu list / cart). It then includes the raw data in its response.
 */
@Component
public class PlainToolCallResultConverter implements ToolCallResultConverter {

    @Override
    public String convert(Object result, java.lang.reflect.Type paramType) {
        if (result == null) {
            return "Done";
        }
        // Strip newlines so Groq cannot treat this as a pre-formatted list.
        // Groq will include the raw text in its response.
        return result.toString().replace("\n", " ").replace("\r", " ");
    }
}
