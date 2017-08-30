package nl.knmi.geoweb.backend.product.taf;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.jsonpointer.JsonPointer;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FoundJsonField {
    
    private ObjectMapper objectMapper;
    
    @Getter
    private String name;
    
    @Getter
    private JsonPointer pointer;
    
    @Getter
    private JsonNode value;
    
    public FoundJsonField(String name, JsonPointer pointer, JsonNode valueAsJson, ObjectMapper objectMapper) {
        
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.name = name;
        this.pointer = pointer;
        this.value = valueAsJson;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((pointer == null) ? 0 : pointer.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object anotherObject) {

        if (this == anotherObject) {
            return true;
        }
        if (anotherObject == null || !getClass().equals(anotherObject.getClass())) {
            return false;
        }

        @SuppressWarnings("unchecked")
        FoundJsonField otherField = (FoundJsonField) anotherObject;
        if (name == null) {
            if (otherField.name != null) {
                return false;
            }
        } else if (!name.equals(otherField.name)) {
            return false;
        }
        if (pointer == null) {
            if (otherField.pointer != null) {
                return false;
            }
        } else if (!pointer.equals(otherField.pointer)) {
            return false;
        }
        return true;
    }
    
    public <T> T getValue(Class<T> classType) {
        
        try {
            return objectMapper.treeToValue(value, classType);
        } catch (JsonProcessingException e) {
            log.debug(String.format("Could not map the value of the found field to an object of class '%s'", classType.toString()));
        }
        return null;
    }
}
