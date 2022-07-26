package cz.muni.ics.ga4gh.base.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import cz.muni.ics.ga4gh.base.exceptions.InconvertibleValueException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.util.StringUtils;

@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class PerunAttributeValue {

    public final static String STRING_TYPE = "java.lang.String";
    public final static String INTEGER_TYPE = "java.lang.Integer";
    public final static String BOOLEAN_TYPE = "java.lang.Boolean";
    public final static String ARRAY_TYPE = "java.util.ArrayList";
    public final static String MAP_TYPE = "java.util.LinkedHashMap";
    public final static String LARGE_STRING_TYPE = "java.lang.LargeString";
    public final static String LARGE_ARRAY_LIST_TYPE = "java.util.LargeArrayList";

    private String attrName;
    private String type;
    private JsonNode value;

    public void setAttrName(String attrName) {
        if (!StringUtils.hasText(attrName)) {
            throw new IllegalArgumentException("type can't be null or empty");
        }

        this.attrName = attrName;
    }

    public void setType(String type) {
        if (!StringUtils.hasText(type)) {
            throw new IllegalArgumentException("type can't be null or empty");
        }

        this.type = type;
    }

    public void setValue(String type, JsonNode value) {
        if (isNullValue(value)) {
            if (BOOLEAN_TYPE.equals(type)) {
                value = JsonNodeFactory.instance.booleanNode(false);
            } else if (ARRAY_TYPE.equals(type)) {
                value = JsonNodeFactory.instance.arrayNode();
            } else if (MAP_TYPE.equals(type)) {
                value = JsonNodeFactory.instance.objectNode();
            } else {
                value = JsonNodeFactory.instance.nullNode();
            }
        }

        this.value = value;
    }

    public String valueAsString() {
        if ((STRING_TYPE.equals(type) || LARGE_STRING_TYPE.equals(type))) {
            if (isNullValue(value)) {
                return null;
            } else if (value instanceof TextNode) {
                return value.textValue();
            }
        }

        return value.asText();
    }

    public Integer valueAsInteger() {
        if (INTEGER_TYPE.equals(type)) {
            if (isNullValue(value)) {
                return null;
            } else if (value instanceof NumericNode) {
                return value.intValue();
            }
        }

        throw this.inconvertible(Long.class.getName());
    }

    public boolean valueAsBoolean() {
        if (BOOLEAN_TYPE.equals(type)) {
            if (value == null || value instanceof NullNode) {
                return false;
            } else if (value instanceof BooleanNode) {
                return value.asBoolean();
            }
        }

        throw this.inconvertible(Boolean.class.getName());
    }

    public List<String> valueAsList() {
        List<String> arr = new ArrayList<>();

        if ((ARRAY_TYPE.equals(type) || LARGE_ARRAY_LIST_TYPE.equals(type))) {
            if (isNullValue(value)) {
                return null;
            } else if (value instanceof ArrayNode) {
                ArrayNode arrJson = (ArrayNode) value;
                arrJson.forEach(item -> arr.add(item.asText()));
            }
        } else {
            arr.add(this.valueAsString());
        }

        return arr;
    }

    public Map<String, String> valueAsMap() {
        if (MAP_TYPE.equals(type)) {
            if (isNullValue(value)) {
                return new HashMap<>();
            } else if (value instanceof ObjectNode) {
                ObjectNode objJson = (ObjectNode) value;

                Map<String, String> res = new HashMap<>();
                Iterator<String> it = objJson.fieldNames();

                while (it.hasNext()) {
                    String key = it.next();
                    res.put(key, objJson.get(key).asText());
                }

                return res;
            }
        }

        throw this.inconvertible(Map.class.getName());
    }

    public JsonNode valueAsJson() {
        return this.value;
    }

    public boolean isNullValue() {
        return value == null ||
                value instanceof NullNode ||
                value.isNull() ||
                "null".equalsIgnoreCase(value.asText());
    }

    public static boolean isNullValue(JsonNode value) {
        return value == null ||
                value instanceof NullNode ||
                value.isNull() ||
                "null".equalsIgnoreCase(value.asText());
    }

    private InconvertibleValueException inconvertible(String clazzName) {
        return new InconvertibleValueException("Cannot convert value of attribute to " + clazzName +
                " for object: " + this);
    }
}
