package com.manish.smartcart.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * CONCEPT: An AttributeConverter is a "translator" between Java and the Database.
 * JPA doesn't natively know how to store a Java float[] in a PostgreSQL vector(1536) column.
 * This class teaches JPA:
 *   "When WRITING to DB → convert float[] to the string [0.021,-0.455,...]"
 *   "When READING from DB → parse that string back into float[]"
 *
 * autoApply = false means we explicitly choose when to use this converter (only on `embedding`)
 */

@Converter
public class VectorAttributeConverter implements AttributeConverter<float[], String> {

    /**
     * Java → Database: float[] becomes "[0.021,-0.455,0.891,...]"
     * This is the exact string format PostgreSQL's vector type expects.
     */
    @Override
    public String convertToDatabaseColumn(float[] vector) {
        if(vector==null){
            return null;
        }
        StringBuilder sb = new StringBuilder("[");
        for(int i=0;i<vector.length;i++){
            sb.append(vector[i]);
            if(i < vector.length-1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Database → Java: "[0.021,-0.455,0.891,...]" becomes float[]
     * Strip the brackets, split by comma, parse each token as a float.
     */
    @Override
    public float[] convertToEntityAttribute(String dbData) {
        if(dbData == null || dbData.isBlank()) return null;
        // Remove the [ ] brackets
        String stripped = dbData.substring(1, dbData.length()-1);
        String[] parts =  stripped.split(",");
        float[] vector = new float[parts.length];
        for(int i=0;i<parts.length;i++){
            vector[i] = Float.parseFloat(parts[i].trim());
        }
        return vector;
    }
}
