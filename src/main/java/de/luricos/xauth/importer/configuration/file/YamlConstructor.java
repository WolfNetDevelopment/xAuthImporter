package de.luricos.xauth.importer.configuration.file;

import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;

import java.util.Map;

public class YamlConstructor extends SafeConstructor {

    public YamlConstructor() {
        this.yamlConstructors.put(Tag.MAP, new ConstructCustomObject());
    }

    private class ConstructCustomObject extends ConstructYamlMap {
        @Override
        public Object construct(Node node) {
            if (node.isTwoStepsConstruction()) {
                throw new YAMLException("Unexpected referential mapping structure. Node: " + node);
            }

            Map<?, ?> raw = (Map<?, ?>) super.construct(node);

            return raw;
        }

        @Override
        public void construct2ndStep(Node node, Object object) {
            throw new YAMLException("Unexpected referential mapping structure. Node: " + node);
        }
    }
}
