package com.github.kindrat.cassandra.client.ui.widget.tableedit;

import com.datastax.oss.driver.api.core.type.DataType;
import javafx.beans.property.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
class TableRowEntry {
    private final StringProperty nameProperty = new SimpleStringProperty("");
    private final ObjectProperty<DataType> typeProperty = new SimpleObjectProperty<>();
    private final BooleanProperty isPartitionKeyProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty isClusteringKeyProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty hasIndexProperty = new SimpleBooleanProperty(false);

    TableRowEntry(
            String name,
            DataType type,
            boolean isPartitionKey,
            boolean isClusteringKey,
            boolean hasIndex) {
        nameProperty.setValue(name);
        typeProperty.setValue(type);
        isPartitionKeyProperty.setValue(isPartitionKey);
        isClusteringKeyProperty.setValue(isClusteringKey);
        hasIndexProperty.setValue(hasIndex);
    }

    String getName() {
        return nameProperty.get();
    }

    void setName(String name) {
        nameProperty.setValue(name);
    }

    DataType getType() {
        return typeProperty.get();
    }

    void setType(DataType type) {
        typeProperty.setValue(type);
    }

    void setIsPartitionKey(boolean isPartitionKey) {
        isPartitionKeyProperty.setValue(isPartitionKey);
    }

    boolean isPartitionKey() {
        return isPartitionKeyProperty.get();
    }

    void setIsClusteringKey(boolean isClusteringKey) {
        isClusteringKeyProperty.setValue(isClusteringKey);
    }

    boolean isClusteringKey() {
        return isClusteringKeyProperty.get();
    }

    void setHasIndex(boolean hasIndex) {
        hasIndexProperty.setValue(hasIndex);
    }

    boolean hasIndex() {
        return hasIndexProperty.get();
    }
}
