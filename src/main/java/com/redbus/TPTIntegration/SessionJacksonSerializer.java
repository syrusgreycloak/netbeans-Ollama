/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.redbus.TPTIntegration;

/**
 *
 * @author manoj.kumar
 */
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter; // Using ObjectWriter for efficiency
import com.fasterxml.jackson.databind.ObjectReader; // Using ObjectReader for efficiency
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.adk.sessions.Session; // Import your Session class
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;
 
/**
 * MapDB Serializer for Session using Jackson.
 * Assumes Session, State, and Event classes (and their contents)
 * are properly structured and/or annotated for Jackson serialization/deserialization.
 */
public class SessionJacksonSerializer implements Serializer<Session> {

    // Use a single ObjectMapper instance for efficiency
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Use ObjectWriter/ObjectReader for thread-safe, efficient reuse
    private static final ObjectWriter objectWriter;
    private static final ObjectReader objectReader;

    static {
        // Configure ObjectMapper once
        objectMapper.registerModule(new JavaTimeModule()); // To handle java.time.Instant
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // Write dates as ISO-8601 strings

        // You might need additional configurations here depending on the types
        // stored in your state maps (e.g., handling polymorphism, custom types).
        // Example: objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);

        objectWriter = objectMapper.writerFor(Session.class);
        objectReader = objectMapper.readerFor(Session.class);
    }

    @Override
    public void serialize(DataOutput2 out, Session value) throws IOException {
        // MapDB provides DataOutput, we need OutputStream for Jackson
        // Wrap DataOutput in a stream
        try (OutputStream os = out) {
            // Write the object as JSON (or other format configured in ObjectMapper) bytes
            objectWriter.writeValue(os, value);
        }
    }

    @Override
    public Session deserialize(DataInput2 in, int available) throws IOException {
        // MapDB provides DataInput, we need InputStream for Jackson
        // Wrap DataInput in a stream
         try (InputStream is = new DataInput2.DataInputToStream(in)) {
            // Read the object from the stream
            // 'available' parameter can be ignored by Jackson stream reading
            return objectReader.readValue(is);
        }
    }

    // Implement equals and hashCode if this serializer instance is used in keys or comparisons
    // For values in a MapDB HTreeMap, this might not be strictly necessary for correctness
    // unless you rely on MapDB's internal object equality checks during operations.
    // If needed, a robust equals compares the serialized byte representations.
    @Override
    public boolean equals(Session first, Session second) {
        if (first == second) return true;
        if (first == null || second == null) return false;
         // Caution: Serializing just for comparison can be slow.
         // A better approach might be to compare object fields if Session has a
         // reliable equals method based on content.
         // Assuming Session.equals() correctly compares meaningful content:
         // return first.equals(second);

         // If Session.equals() is not suitable or you need byte-level comparison:
         try {
             byte[] bytes1 = objectMapper.writeValueAsBytes(first);
             byte[] bytes2 = objectMapper.writeValueAsBytes(second);
             return java.util.Arrays.equals(bytes1, bytes2);
         } catch (IOException e) {
              // Handle the error appropriately, perhaps log and return false or rethrow
              throw new RuntimeException("Error comparing sessions via serialization", e);
         }
    }

    // Optional: Implement hashCode if this serializer is used for keys in maps
    // where hashing is involved (e.g., HTreeMap keys). Not needed for values.
    // @Override
    // public int hashCode(Session value) {
    //     // Implement consistent hash code logic. If Session has a reliable hashCode:
    //     // return value.hashCode();
    //     // If hashing based on serialized form is needed (less common):
    //     try {
    //         byte[] bytes = objectMapper.writeValueAsBytes(value);
    //         return java.util.Arrays.hashCode(bytes);
    //     } catch (IOException e) {
    //         throw new RuntimeException("Error calculating session hash code", e);
    //     }
    // }

    // Optional: Implement fixedSize() or variableSize() if known
    // @Override
    // public int fixedSize() { return -1; } // -1 means variable size
    // @Override
    // public int variableSize() { return -1; } // -1 means unknown variable size



 
}