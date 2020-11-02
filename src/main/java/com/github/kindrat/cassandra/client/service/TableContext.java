package com.github.kindrat.cassandra.client.service;

import com.datastax.oss.driver.api.core.cql.*;
import com.datastax.oss.driver.api.core.metadata.schema.TableMetadata;
import com.datastax.oss.driver.api.core.type.DataType;
import com.datastax.oss.driver.api.core.type.codec.TypeCodec;
import com.datastax.oss.driver.api.core.type.codec.registry.CodecRegistry;
import com.datastax.oss.driver.internal.core.type.PrimitiveType;
import com.datastax.oss.driver.internal.core.util.concurrent.CompletableFutures;
import com.datastax.oss.protocol.internal.ProtocolConstants;
import com.github.kindrat.cassandra.client.ui.DataObject;
import com.github.kindrat.cassandra.client.util.EvenMoreFutures;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import lombok.Getter;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.github.kindrat.cassandra.client.ui.fx.TableColumns.buildColumn;
import static com.github.kindrat.cassandra.client.util.EvenMoreFutures.*;
import static com.github.nginate.commons.lang.NStrings.format;

@Slf4j
public class TableContext {

    private final Map<Integer, String> pagingStates = new HashMap<>();
    @Getter
    private final String table;
    private final String query;
    @Getter
    private final TableMetadata tableMetadata;
    @Getter
    private final List<TableColumn<DataObject, Object>> columns;
    private final CassandraClientAdapter clientAdapter;
    private final int pageSize;

    private int page;
    private CompletableFuture<ObservableList<DataObject>> currentPage;
    private volatile ResultSet resultSet;

    private TableContext(String table, String query, TableMetadata tableMetadata, CassandraClientAdapter clientAdapter,
                         int pageSize) {
        this.table = table;
        this.tableMetadata = tableMetadata;
        this.clientAdapter = clientAdapter;
        this.pageSize = pageSize;
        this.query = query;
        columns = buildColumns();
    }

    public static TableContext fullTable(String table, TableMetadata tableMetadata,
                                         CassandraClientAdapter clientAdapter, int pageSize) {
        return new TableContext(table, "select * from " + table, tableMetadata, clientAdapter, pageSize);
    }

    public static TableContext customView(String table, String query, TableMetadata tableMetadata,
                                          CassandraClientAdapter clientAdapter, int pageSize) {
        return new TableContext(table, query, tableMetadata, clientAdapter, pageSize);
    }

    @Synchronized
    public CompletableFuture<ObservableList<DataObject>> nextPage() {
        if (!hasNextPage()) {
            return currentPage;
        }
        page++;
        currentPage = toCompletable(Futures.immediateFuture(resultSet))
                .thenApply(this::parseResultSet)
                .thenApply(FXCollections::observableList);
        return currentPage;
    }

    @Synchronized
    public boolean hasPreviousPage() {
        return page > 0;
    }

    @Synchronized
    public boolean hasNextPage() {
        return !resultSet.isFullyFetched();
    }

    @Synchronized
    public CompletableFuture<ObservableList<DataObject>> previousPage() {
        page = Math.max(0, page - 1);
        currentPage = loadPage(page);
        return currentPage;
    }

    @Synchronized
    private CompletableFuture<ObservableList<DataObject>> loadPage(int page) {
        log.debug("Loading page {}", page);
        Statement statement = getStatement();
        String rawPagingState = pagingStates.get(page);
        if (rawPagingState != null) {
            log.debug("Loading page {} with pagination state {}", page, rawPagingState);
            statement.setPagingState(ByteBuffer.wrap(rawPagingState.getBytes()));
        }
        return clientAdapter.executeStatement(statement)
                .thenApply(result -> {
                    ExecutionInfo executionInfo = result.getExecutionInfo();
                    executionInfo.getWarnings().forEach(log::warn);
                    printQueryTrace(page, executionInfo.getQueryTraceAsync());
                    this.resultSet = result;
                    return result;
                })
                .thenApply(this::parseResultSet)
                .thenApply(FXCollections::observableList)
                .whenComplete(logErrorIfPresent(format("Page {} load failed", page)));
    }

    private Statement getStatement() {
        return SimpleStatement.builder(query)
                .setPageSize(pageSize)
                .setTracing(true).build();
    }

    private List<DataObject> parseResultSet(ResultSet resultSet) {
        AtomicInteger start = new AtomicInteger(pageSize * page);
        int availableWithoutFetching = resultSet.getAvailableWithoutFetching();
        int currentPageSize = Math.min(availableWithoutFetching, pageSize);
        var nextPageState = resultSet.getExecutionInfo().getPagingState();
        log.debug("Page {} size {}", page, currentPageSize);
        if (nextPageState != null) {
            String value = nextPageState.toString();
            log.debug("Page {} paging state {}", page + 1, value);
            pagingStates.put(page + 1, value);
        } else {
            log.info("No pagination state for remaining entries. Fetching them within current page.");
            currentPageSize = availableWithoutFetching;
        }
        return StreamSupport.stream(resultSet.spliterator(), false)
                .limit(currentPageSize)
                .map(row -> parseRow(start.getAndIncrement(), row))
                .collect(Collectors.toList());
    }

    private DataObject parseRow(int index, Row row) {
        DataObject dataObject = new DataObject(index);
        for (ColumnDefinition definition : row.getColumnDefinitions()) {
            TypeCodec<Object> codec = CodecRegistry.DEFAULT.codecFor(definition.getType());
            dataObject.set(definition.getName().asInternal(), row.get(definition.getName(), codec));
        }
        return dataObject;
    }

    private List<TableColumn<DataObject, Object>> buildColumns() {
        List<TableColumn<DataObject, Object>> columns = new ArrayList<>();

        DataType intCol=new PrimitiveType(ProtocolConstants.DataType.INT);
        TableColumn<DataObject, Object> counterColumn = buildColumn(intCol, "#");
        counterColumn.setCellValueFactory(param -> {
            Integer object = param.getValue().getPosition();
            return new SimpleObjectProperty<>(object);
        });
        counterColumn.setEditable(false);

        columns.add(counterColumn);

        tableMetadata.getColumns().forEach((cqlIdentifier, columnMetadata) -> {
            DataType type = columnMetadata.getType();
            TableColumn<DataObject, Object> column = buildColumn(type, columnMetadata.getName().toString());
            column.setCellValueFactory(param -> {
                Object object = param.getValue().get(columnMetadata.getName().asInternal());
                return new SimpleObjectProperty<>(object);
            });
            column.setOnEditCommit(event -> {
                log.debug("Updating row value with {}", event.getRowValue());
                clientAdapter.update(tableMetadata, event).whenComplete(loggingConsumer(aVoid -> event.getRowValue()));
            });
            columns.add(column);
        });
        return columns;
    }

    private void printQueryTrace(int page, CompletionStage<QueryTrace> listenableFuture) {
        listenableFuture.whenCompleteAsync((trace, throwable) -> {
            if (trace != null) {
                log.info("Executed page {} query trace : [{}] " +
                                "\n\t started at {} and took {} μs " +
                                "\n\t coordinator {}" +
                                "\n\t request type {}" +
                                "\n\t parameters {}" +
                                "\n\t events",
                        page, trace.getTracingId(), trace.getStartedAt(), trace.getDurationMicros(),
                        trace.getCoordinatorAddress().getAddress(), trace.getRequestType(), trace.getParameters(),
                        trace.getEvents());
            }
            if (throwable != null) {
                log.error("Query trace could not be read", throwable);
            }
        });

    }
}
