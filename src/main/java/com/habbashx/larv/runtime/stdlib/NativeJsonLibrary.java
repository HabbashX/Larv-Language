package com.habbashx.larv.runtime.stdlib;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.habbashx.larv.error.LarvError;
import com.habbashx.larv.runtime.ExecutionContext;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * NativeJsonLibrary
 *
 * JSON standard library for the Larv runtime.
 *
 * Provides parsing, serialization, validation, and file-based JSON I/O.
 * Built on Jackson ObjectMapper for fast and reliable processing.
 *
 * This library converts between JSON and Larv-native data structures
 * (Map, List, String, Double, Boolean, null).
 *
 * Features:
 * - Parse JSON strings into Larv objects
 * - Convert Larv objects into JSON strings
 * - Pretty-print JSON output
 * - Validate JSON syntax
 * - Read JSON files
 * - Write JSON files
 *
 * Type Mapping:
 * JSON object  → Map<String, Object>
 * JSON array   → List<Object>
 * JSON string  → String
 * JSON number  → Double
 * JSON boolean → Boolean
 * JSON null    → null
 *
 * Thread Safety:
 * ObjectMapper is shared and thread-safe.
 * All methods are stateless.
 * File operations are synchronous.
 *
 * Dependency:
 * Jackson Databind (com.fasterxml.jackson.core:jackson-databind)
 *
 * Larv API:
 * jsonParse(text)
 * jsonStringify(value)
 * jsonPretty(value)
 * jsonValid(text)
 * jsonRead(path)
 * jsonWrite(path, value)
 *
 * Errors:
 * All methods throw LarvError (RUNTIME) on failure.
 */
@Native("JSON Library")
@Deprecated(since = "1.1.0") // unused by compiler & interpreter
public class NativeJsonLibrary extends NativeLibrary {

    /**
     * Shared JSON processor used for all serialization and parsing.
     *
     * - Static: shared across all instances
     * - Thread-safe after initialization
     * - Handles conversion between Java objects and JSON
     * - Optimized for repeated runtime usage
     */
    private static final ObjectMapper MAPPER =
            new ObjectMapper();

    /**
     * Creates a new JSON library instance bound to a Larv execution context.
     *
     * @param context The Larv runtime execution context used for registering
     *                native functions.
     *
     * Responsibilities:
     *  - Stores execution context reference
     *  - Prepares library for function registration
     */
    public NativeJsonLibrary(ExecutionContext context) {
        super(context);
    }

    /**
     * Registers all JSON native functions into the Larv runtime.
     *
     * Registered functions:
     *  - jsonParse
     *  - jsonStringify
     *  - jsonPretty
     *  - jsonValid
     *  - jsonRead
     *  - jsonWrite
     *
     * Called automatically during runtime initialization.
     */
    @Override
    public void registerAll() {

        getExecutionContext().registerNative(
                "jsonParse",
                this::jsonParse
        );

        getExecutionContext().registerNative(
                "jsonStringify",
                this::jsonStringify
        );

        getExecutionContext().registerNative(
                "jsonPretty",
                this::jsonPretty
        );

        getExecutionContext().registerNative(
                "jsonValid",
                this::jsonValid
        );

        getExecutionContext().registerNative(
                "jsonRead",
                this::jsonRead
        );

        getExecutionContext().registerNative(
                "jsonWrite",
                this::jsonWrite
        );
    }

    /**
     * Parses a JSON string into Larv native objects.
     *
     * @param args List of arguments:
     *              args[0] = JSON string
     *
     * @return Parsed object:
     *         - Map for JSON objects
     *         - List for JSON arrays
     *         - String/Double/Boolean/null for primitives
     *
     * @throws LarvError if JSON is invalid or malformed
     */
    private @Nullable Object jsonParse(
            @NotNull List<Object> args
    ) {

        String json = strArg(
                args,
                0,
                "jsonParse"
        );

        try {

            return MAPPER.readValue(
                    json,
                    new TypeReference<Object>() {}
            );

        } catch (IOException e) {

            throw new LarvError(
                    "jsonParse(): " + e.getMessage(),
                    -1,
                    LarvError.Kind.RUNTIME
            );
        }
    }

    /**
     * Converts a Larv object into a JSON string.
     *
     * @param args List of arguments:
     *              args[0] = Larv object (Map/List/primitive)
     *
     * @return JSON string representation
     *
     * @throws LarvError if object cannot be serialized
     */
    private @NotNull Object jsonStringify(
            @NotNull List<Object> args
    ) {

        Object value =
                args.isEmpty()
                        ? null
                        : args.get(0);

        try {

            return MAPPER.writeValueAsString(
                    value
            );

        } catch (JsonProcessingException e) {

            throw new LarvError(
                    "jsonStringify(): " + e.getMessage(),
                    -1,
                    LarvError.Kind.RUNTIME
            );
        }
    }

    /**
     * Converts a Larv object into a formatted (pretty-printed) JSON string.
     *
     * @param args List of arguments:
     *              args[0] = Larv object
     *
     * @return Pretty formatted JSON string
     *
     * Example output:
     * {
     *   "name" : "Alice"
     * }
     *
     * @throws LarvError if serialization fails
     */
    private @NotNull @Unmodifiable Object jsonPretty(
            @NotNull List<Object> args
    ) {

        Object value =
                args.isEmpty()
                        ? null
                        : args.get(0);

        try {

            return MAPPER
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(value);

        } catch (JsonProcessingException e) {

            throw new LarvError(
                    "jsonPretty(): " + e.getMessage(),
                    -1,
                    LarvError.Kind.RUNTIME
            );
        }
    }

    /**
     * Validates whether a string is valid JSON.
     *
     * @param args List of arguments:
     *              args[0] = JSON string
     *
     * @return true if valid JSON, false otherwise
     *
     * Note:
     * - Does not throw exceptions for invalid JSON
     * - Uses lightweight parsing (readTree)
     */
    private @NotNull Object jsonValid(List<Object> args) {

        String json = strArg(
                args,
                0,
                "jsonValid"
        );

        try {

            MAPPER.readTree(json);

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    /**
     * Reads a JSON file and parses it into Larv objects.
     *
     * @param args List of arguments:
     *              args[0] = file path
     *
     * @return Parsed JSON object (Map/List)
     *
     * @throws LarvError if:
     *         - File does not exist
     *         - File cannot be read
     *         - JSON is invalid
     */
    private @Nullable Object jsonRead(
            @NotNull List<Object> args
    ) {

        String path = strArg(
                args,
                0,
                "jsonRead"
        );

        try {

            String json = Files.readString(
                    Path.of(path)
            );

            return MAPPER.readValue(
                    json,
                    new TypeReference<Object>() {}
            );

        } catch (IOException e) {

            throw new LarvError(
                    "jsonRead(): " + e.getMessage(),
                    -1,
                    LarvError.Kind.RUNTIME
            );
        }
    }

    /**
     * Writes a Larv object into a JSON file.
     *
     * @param args List of arguments:
     *              args[0] = file path
     *              args[1] = object to serialize
     *
     * @return null
     *
     * Behavior:
     *  - Overwrites file if it exists
     *  - Creates new file if missing
     *  - Uses UTF-8 encoding
     *  - Outputs pretty JSON format
     *
     * @throws LarvError if file cannot be written or object invalid
     */
    private @Nullable Object jsonWrite(
            @NotNull List<Object> args
    ) {

        String path = strArg(
                args,
                0,
                "jsonWrite"
        );

        Object value =
                args.size() > 1
                        ? args.get(1)
                        : null;

        try {

            String json =
                    MAPPER
                            .writerWithDefaultPrettyPrinter()
                            .writeValueAsString(value);

            Files.writeString(
                    Path.of(path),
                    json
            );

            return null;

        } catch (IOException e) {

            throw new LarvError(
                    "jsonWrite(): " + e.getMessage(),
                    -1,
                    LarvError.Kind.RUNTIME
            );
        }
    }
}